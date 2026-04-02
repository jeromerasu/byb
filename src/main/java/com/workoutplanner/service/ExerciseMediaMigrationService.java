package com.workoutplanner.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workoutplanner.dto.ExerciseMediaMigrationResult;
import com.workoutplanner.model.ExerciseCatalog;
import com.workoutplanner.repository.ExerciseCatalogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Migrates ExerciseDB GIF URLs from the {@code exercise_catalog} table into the
 * project's own MinIO object storage so they can't expire or break.
 *
 * <p>Migration phases:
 * <ol>
 *   <li>Find all system exercises whose {@code video_url} still points to {@code exercisedb}.
 *       Try to download the GIF directly. If the static URL is dead (the original CDN is down),
 *       fall back to the ExerciseDB community API to find a working GIF URL by name.
 *       On success: upload to MinIO and update the DB row.</li>
 *   <li>Find system exercises with a {@code NULL} {@code video_url}.  Attempt to resolve a GIF URL
 *       via the ExerciseDB community API, then follow the same download-upload-update path.</li>
 * </ol>
 *
 * <p>The service is idempotent: exercises whose {@code video_url} already points to the MinIO
 * endpoint are skipped (not returned by the repository queries).
 * Failed individual exercises are logged and counted; the batch always completes.
 *
 * <p>ExerciseDB community API (open-source, no auth required):
 * {@code https://exercisedb-api.vercel.app/api/v1/exercises?search=<name>&limit=5&offset=0}
 * Response envelope: {@code { "success": true, "data": [ { "gifUrl": "...", "name": "..." } ] }}
 */
@Service
public class ExerciseMediaMigrationService {

    private static final Logger log = LoggerFactory.getLogger(ExerciseMediaMigrationService.class);

    public static final String BUCKET_NAME = "exercise-media";

    /**
     * Search endpoint of the ExerciseDB open-source community API (v1).
     * The original exercisedb.dev CDN and v2 API are no longer reliable.
     */
    private static final String EXERCISEDB_SEARCH_API =
            "https://exercisedb-api.vercel.app/api/v1/exercises?search=";

    // -----------------------------------------------------------------------
    // Dependencies
    // -----------------------------------------------------------------------

    private final ExerciseCatalogRepository repository;
    private final ObjectStorageService storageService; // null on local/test profile
    private final ObjectMapper objectMapper;
    private final String minioEndpoint;
    private final GifDownloader gifDownloader;
    private final long delayBetweenDownloadsMs;

    // -----------------------------------------------------------------------
    // Functional interface — swapped out in tests
    // -----------------------------------------------------------------------

    /**
     * Downloads bytes from a URL.  Implementations may throw {@link IOException} for HTTP errors
     * (4xx / 5xx) or network failures.
     */
    @FunctionalInterface
    public interface GifDownloader {
        byte[] download(String url) throws IOException;
    }

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    /**
     * Production constructor — Spring uses this.
     * {@code storageService} is injected via {@code Optional} so that on the local/test profile
     * (where {@link ObjectStorageService} is not a bean) it is {@code null}, and
     * {@link #migrateAll()} returns early.
     */
    @Autowired
    public ExerciseMediaMigrationService(
            ExerciseCatalogRepository repository,
            java.util.Optional<ObjectStorageService> storageService,
            ObjectMapper objectMapper,
            @Value("${MINIO_ENDPOINT:http://localhost:9000}") String minioEndpoint) {
        this(repository, storageService.orElse(null), objectMapper, minioEndpoint,
                ExerciseMediaMigrationService::httpGet, 300L);
    }

    /**
     * Package-private constructor used in unit tests — allows injecting mock collaborators
     * without starting a Spring context.
     */
    ExerciseMediaMigrationService(
            ExerciseCatalogRepository repository,
            ObjectStorageService storageService,
            ObjectMapper objectMapper,
            String minioEndpoint,
            GifDownloader gifDownloader,
            long delayBetweenDownloadsMs) {
        this.repository = repository;
        this.storageService = storageService;
        this.objectMapper = objectMapper;
        this.minioEndpoint = minioEndpoint;
        this.gifDownloader = gifDownloader;
        this.delayBetweenDownloadsMs = delayBetweenDownloadsMs;
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Runs the full migration and returns a summary.
     *
     * <p>Returns immediately (with a descriptive message) when running on the local/test profile
     * where MinIO is not configured.
     */
    public ExerciseMediaMigrationResult migrateAll() {
        if (storageService == null) {
            log.warn("media.migration.skipped reason=local_profile");
            return new ExerciseMediaMigrationResult(0, 0, 0,
                    "Skipped: ObjectStorageService not available (local storage profile)");
        }

        // Ensure the bucket exists and has a public-read policy so the frontend can load GIFs directly
        try {
            storageService.setBucketPublicRead(BUCKET_NAME);
        } catch (Exception e) {
            log.warn("media.migration.bucketPolicy.warn bucket={} error={}", BUCKET_NAME, e.getMessage());
        }

        int succeeded = 0;
        int failed    = 0;
        int skipped   = 0;

        // ---- Phase 1: exercises that still have ExerciseDB URLs ----
        //
        // The original exercisedb.dev static CDN is down, so direct downloads will
        // fail for many exercises. When that happens we fall back to the community
        // API (exercisedb-api.vercel.app) and search by name.
        List<ExerciseCatalog> withExerciseDbUrls = repository.findSystemExercisesWithExerciseDbUrls();
        log.info("media.migration.phase1.start count={}", withExerciseDbUrls.size());

        for (ExerciseCatalog exercise : withExerciseDbUrls) {
            boolean resolved = false;
            try {
                migrateExercise(exercise);
                succeeded++;
                resolved = true;
            } catch (Exception e) {
                log.warn("media.migration.exercise.phase1.failed name='{}' url='{}' error={} — trying API fallback",
                        exercise.getName(), exercise.getVideoUrl(), e.getMessage());
                try {
                    resolved = resolveViaApi(exercise);
                    if (resolved) {
                        succeeded++;
                    } else {
                        log.warn("media.migration.exercise.unresolvable name='{}'", exercise.getName());
                        failed++;
                    }
                } catch (Exception ex) {
                    log.warn("media.migration.exercise.apiFallback.failed name='{}' error={}",
                            exercise.getName(), ex.getMessage());
                    failed++;
                }
            }
            delay();
        }

        // ---- Phase 2: exercises with NULL video_url ----
        List<ExerciseCatalog> nullVideoExercises = repository.findByVideoUrlIsNullAndIsSystemTrue();
        log.info("media.migration.phase2.start count={}", nullVideoExercises.size());

        for (ExerciseCatalog exercise : nullVideoExercises) {
            try {
                boolean resolved = resolveViaApi(exercise);
                if (resolved) {
                    succeeded++;
                } else {
                    log.info("media.migration.exercise.unresolved name='{}'", exercise.getName());
                    skipped++;
                }
            } catch (Exception e) {
                log.warn("media.migration.exercise.nullVideo.failed name='{}' error={}",
                        exercise.getName(), e.getMessage());
                skipped++; // can't resolve → treat as skipped, not failed
            }
            delay();
        }

        log.info("media.migration.complete succeeded={} failed={} skipped={}",
                succeeded, failed, skipped);
        return new ExerciseMediaMigrationResult(succeeded, failed, skipped);
    }

    // -----------------------------------------------------------------------
    // Package-private helpers (accessible from tests)
    // -----------------------------------------------------------------------

    /**
     * Converts an exercise name into a URL-safe slug.
     * <ul>
     *   <li>Lowercases the name.</li>
     *   <li>Removes characters that are not letters, digits, spaces, or hyphens.</li>
     *   <li>Replaces runs of whitespace with a single hyphen.</li>
     *   <li>Collapses consecutive hyphens.</li>
     * </ul>
     * Examples: {@code "T-Bar Row"} → {@code "t-bar-row"},
     *           {@code "Dumbbell Fly (Incline)"} → {@code "dumbbell-fly-incline"}.
     */
    String slugify(String name) {
        if (name == null) return "";
        return name.toLowerCase()
                   .replaceAll("[^a-z0-9\\s-]", "")
                   .trim()
                   .replaceAll("\\s+", "-")
                   .replaceAll("-+", "-");
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Downloads the GIF from {@code exercise.videoUrl}, uploads it (and a metadata JSON) to MinIO,
     * and updates the DB row.
     */
    private void migrateExercise(ExerciseCatalog exercise) throws IOException {
        String slug   = slugify(exercise.getName());
        String gifKey = "exercises/" + slug + "/animation.gif";

        // 1. Download GIF
        byte[] gifBytes = gifDownloader.download(exercise.getVideoUrl());

        // 2. Upload GIF
        storageService.putBytes(BUCKET_NAME, gifKey, gifBytes, "image/gif");

        // 3. Upload metadata JSON
        byte[] metadataBytes = buildMetadataJson(exercise);
        storageService.putBytes(BUCKET_NAME, "exercises/" + slug + "/metadata.json",
                metadataBytes, "application/json");

        // 4. Update DB: both columns point to the same MinIO URL
        String minioUrl = minioEndpoint + "/" + BUCKET_NAME + "/" + gifKey;
        exercise.setVideoUrl(minioUrl);
        exercise.setThumbnailUrl(minioUrl);
        repository.save(exercise);

        log.info("media.migration.exercise.ok name='{}' url={}", exercise.getName(), minioUrl);
    }

    /**
     * Attempts to find a working GIF URL for the given exercise by searching the ExerciseDB
     * community API (v1) by name. If a match is found, delegates to
     * {@link #migrateExercise(ExerciseCatalog)} to download, upload, and update the DB.
     *
     * <p>Used in two scenarios:
     * <ul>
     *   <li>Phase 1 fallback: static exercisedb.dev URL returned an error.</li>
     *   <li>Phase 2: exercise has no {@code video_url} at all.</li>
     * </ul>
     *
     * @return {@code true} if the exercise was successfully migrated, {@code false} if no match found.
     */
    private boolean resolveViaApi(ExerciseCatalog exercise) throws IOException {
        String encodedName = URLEncoder.encode(exercise.getName(), StandardCharsets.UTF_8);
        String apiUrl = EXERCISEDB_SEARCH_API + encodedName + "&limit=5&offset=0";

        byte[] responseBytes = gifDownloader.download(apiUrl);
        JsonNode root = objectMapper.readTree(responseBytes);

        // Response envelope: { "success": true, "data": [ {...}, ... ], "metadata": {...} }
        JsonNode data = root.path("data");
        if (!data.isArray() || data.isEmpty()) {
            return false;
        }

        String gifUrl = findBestMatch(data, exercise.getName());
        if (gifUrl == null || gifUrl.isEmpty()) {
            return false;
        }

        // Set the resolved URL so migrateExercise can download it
        exercise.setVideoUrl(gifUrl);
        migrateExercise(exercise);
        return true;
    }

    /**
     * Finds the best GIF URL from the API result set.
     * Prefers an exact name match (case-insensitive); falls back to the first result.
     */
    private String findBestMatch(JsonNode exercises, String exerciseName) {
        for (JsonNode node : exercises) {
            if (exerciseName.equalsIgnoreCase(node.path("name").asText())) {
                String url = node.path("gifUrl").asText(null);
                if (url != null && !url.isEmpty()) return url;
            }
        }
        // No exact match — use first result's GIF
        String url = exercises.get(0).path("gifUrl").asText(null);
        return (url != null && !url.isEmpty()) ? url : null;
    }

    private byte[] buildMetadataJson(ExerciseCatalog exercise) throws IOException {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("name", exercise.getName());
        metadata.put("exerciseType", exercise.getExerciseType());
        metadata.put("muscleGroups", exercise.getMuscleGroups());
        metadata.put("equipment", exercise.getEquipmentRequired());
        metadata.put("difficulty", exercise.getDifficultyLevel());
        metadata.put("instructions", exercise.getInstructions());
        return objectMapper.writeValueAsBytes(metadata);
    }

    private void delay() {
        if (delayBetweenDownloadsMs <= 0) return;
        try {
            Thread.sleep(delayBetweenDownloadsMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // -----------------------------------------------------------------------
    // Default HTTP downloader (used in production)
    // -----------------------------------------------------------------------

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static byte[] httpGet(String url) throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        try {
            HttpResponse<byte[]> response = HTTP_CLIENT.send(
                    request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            }
            throw new IOException("HTTP " + response.statusCode() + " downloading: " + url);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Download interrupted: " + url, e);
        }
    }
}
