package com.workoutplanner.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workoutplanner.dto.ExerciseMediaMigrationResult;
import com.workoutplanner.model.ExerciseCatalog;
import com.workoutplanner.repository.ExerciseCatalogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExerciseMediaMigrationService.
 * Covers: slugify logic, idempotency, successful migration, error handling,
 * Phase 1 API fallback for dead static URLs, and Phase 2 null-URL resolution.
 */
@ExtendWith(MockitoExtension.class)
class ExerciseMediaMigrationServiceTest {

    private static final Logger log = LoggerFactory.getLogger(ExerciseMediaMigrationServiceTest.class);

    private static final String MINIO_ENDPOINT = "https://minio.example.com";

    /** A live GIF URL returned by the ExerciseDB community API. */
    private static final String LIVE_GIF_URL = "https://static.exercisedb.dev/media/abc123.gif";

    @Mock
    private ExerciseCatalogRepository repository;

    @Mock
    private ObjectStorageService storageService;

    @Mock
    private ExerciseMediaMigrationService.GifDownloader gifDownloader;

    private ExerciseMediaMigrationService service;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new ExerciseMediaMigrationService(
                repository, storageService, objectMapper, MINIO_ENDPOINT, gifDownloader, 0L);
    }

    // -----------------------------------------------------------------------
    // slugify
    // -----------------------------------------------------------------------

    @Test
    void slugify_SimpleSpaces_ShouldReplaceWithHyphens() {
        assertEquals("bench-press", service.slugify("Bench Press"));
        log.info("test.slugify.spaces passed");
    }

    @Test
    void slugify_HyphensPreserved_ShouldKeepHyphens() {
        assertEquals("t-bar-row", service.slugify("T-Bar Row"));
        log.info("test.slugify.hyphens passed");
    }

    @Test
    void slugify_SpecialCharsRemoved_ShouldStripParentheses() {
        assertEquals("dumbbell-fly-incline", service.slugify("Dumbbell Fly (Incline)"));
        log.info("test.slugify.specialChars passed");
    }

    @Test
    void slugify_AlreadySlug_ShouldReturnUnchanged() {
        assertEquals("push-up", service.slugify("Push-up"));
        log.info("test.slugify.alreadySlug passed");
    }

    @Test
    void slugify_MultipleSpaces_ShouldCollapse() {
        assertEquals("bulgarian-split-squat", service.slugify("Bulgarian  Split  Squat"));
        log.info("test.slugify.multipleSpaces passed");
    }

    @Test
    void slugify_LeadingTrailingSpaces_ShouldTrim() {
        assertEquals("squat", service.slugify("  Squat  "));
        log.info("test.slugify.trim passed");
    }

    @Test
    void slugify_NumbersPreserved_ShouldKeepNumbers() {
        assertEquals("lat-pulldown-45", service.slugify("Lat Pulldown 45°"));
        log.info("test.slugify.numbers passed");
    }

    // -----------------------------------------------------------------------
    // migrateAll — idempotency
    // -----------------------------------------------------------------------

    @Test
    void migrateAll_NoExercisesToProcess_ShouldReturnZeroCounts() {
        when(repository.findSystemExercisesWithExerciseDbUrls()).thenReturn(List.of());
        when(repository.findByVideoUrlIsNullAndIsSystemTrue()).thenReturn(List.of());

        ExerciseMediaMigrationResult result = service.migrateAll();

        log.info("test.migrateAll.empty succeeded={} failed={} skipped={}",
                result.getSucceeded(), result.getFailed(), result.getSkipped());
        assertEquals(0, result.getSucceeded());
        assertEquals(0, result.getFailed());
        assertEquals(0, result.getSkipped());
        // No GIF uploads or DB saves — setBucketPublicRead is still called once on entry
        verify(storageService, never()).putBytes(any(), any(), any(), any());
    }

    @Test
    void migrateAll_AlreadyMigratedExercise_ShouldSkipAndNotDownload() throws IOException {
        // Already-migrated exercises don't appear in the exercisedb query
        when(repository.findSystemExercisesWithExerciseDbUrls()).thenReturn(List.of());
        when(repository.findByVideoUrlIsNullAndIsSystemTrue()).thenReturn(List.of());

        ExerciseMediaMigrationResult result = service.migrateAll();

        log.info("test.migrateAll.idempotency skipped={}", result.getSkipped());
        verifyNoInteractions(gifDownloader);
        verify(storageService, never()).putBytes(any(), any(), any(), any());
    }

    @Test
    void migrateAll_ExerciseWithMinioUrl_IsNotReturnedByQuery_ShouldSkip() throws IOException {
        // findSystemExercisesWithExerciseDbUrls only returns exercises with exercisedb URLs,
        // so already-migrated exercises are naturally excluded from the query
        when(repository.findSystemExercisesWithExerciseDbUrls()).thenReturn(List.of());
        when(repository.findByVideoUrlIsNullAndIsSystemTrue()).thenReturn(List.of());

        ExerciseMediaMigrationResult result = service.migrateAll();

        assertEquals(0, result.getSucceeded());
        verifyNoInteractions(gifDownloader);
    }

    // -----------------------------------------------------------------------
    // migrateAll — successful migration (Phase 1 static URL works)
    // -----------------------------------------------------------------------

    @Test
    void migrateAll_SingleExercise_ShouldUploadGifAndMetadataAndUpdateDb() throws IOException {
        ExerciseCatalog exercise = makeExercise(1L, "Bench Press",
                "https://exercisedb.dev/media/bench-press.gif");
        when(repository.findSystemExercisesWithExerciseDbUrls()).thenReturn(List.of(exercise));
        when(repository.findByVideoUrlIsNullAndIsSystemTrue()).thenReturn(List.of());
        when(gifDownloader.download(anyString())).thenReturn(new byte[]{1, 2, 3, 4});
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ExerciseMediaMigrationResult result = service.migrateAll();

        log.info("test.migrateAll.single succeeded={}", result.getSucceeded());
        assertEquals(1, result.getSucceeded());
        assertEquals(0, result.getFailed());

        // GIF uploaded with correct key and content-type
        verify(storageService).putBytes(
                eq(ExerciseMediaMigrationService.BUCKET_NAME),
                eq("exercises/bench-press/animation.gif"),
                eq(new byte[]{1, 2, 3, 4}),
                eq("image/gif"));

        // Metadata JSON uploaded
        verify(storageService).putBytes(
                eq(ExerciseMediaMigrationService.BUCKET_NAME),
                eq("exercises/bench-press/metadata.json"),
                any(byte[].class),
                eq("application/json"));

        // DB updated with MinIO URL
        verify(repository).save(argThat(e ->
                e.getVideoUrl().startsWith(MINIO_ENDPOINT) &&
                e.getVideoUrl().contains("bench-press") &&
                e.getVideoUrl().equals(e.getThumbnailUrl())));
    }

    @Test
    void migrateAll_MultipleExercises_ShouldMigrateAll() throws IOException {
        ExerciseCatalog ex1 = makeExercise(1L, "Squat", "https://exercisedb.dev/squat.gif");
        ExerciseCatalog ex2 = makeExercise(2L, "Deadlift", "https://exercisedb.dev/deadlift.gif");
        when(repository.findSystemExercisesWithExerciseDbUrls()).thenReturn(List.of(ex1, ex2));
        when(repository.findByVideoUrlIsNullAndIsSystemTrue()).thenReturn(List.of());
        when(gifDownloader.download(anyString())).thenReturn(new byte[]{1, 2, 3});
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ExerciseMediaMigrationResult result = service.migrateAll();

        log.info("test.migrateAll.multiple succeeded={}", result.getSucceeded());
        assertEquals(2, result.getSucceeded());
        assertEquals(0, result.getFailed());
        verify(storageService, times(4)).putBytes(any(), any(), any(), any()); // 2 GIFs + 2 metadata
        verify(repository, times(2)).save(any());
    }

    @Test
    void migrateAll_CorrectMinioUrlFormat_ShouldBuildUrlFromEndpointAndBucket() throws IOException {
        ExerciseCatalog exercise = makeExercise(1L, "Lat Pulldown",
                "https://exercisedb.dev/lat-pulldown.gif");
        when(repository.findSystemExercisesWithExerciseDbUrls()).thenReturn(List.of(exercise));
        when(repository.findByVideoUrlIsNullAndIsSystemTrue()).thenReturn(List.of());
        when(gifDownloader.download(anyString())).thenReturn(new byte[]{1});
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.migrateAll();

        String expectedUrl = MINIO_ENDPOINT + "/" + ExerciseMediaMigrationService.BUCKET_NAME
                + "/exercises/lat-pulldown/animation.gif";
        verify(repository).save(argThat(e -> expectedUrl.equals(e.getVideoUrl())));
    }

    // -----------------------------------------------------------------------
    // migrateAll — Phase 1 API fallback (dead static URLs → search by name)
    // -----------------------------------------------------------------------

    @Test
    void migrateAll_Phase1StaticUrlDead_WhenApiSucceeds_ShouldCountAsSucceeded() throws IOException {
        ExerciseCatalog exercise = makeExercise(1L, "Bench Press",
                "https://exercisedb.dev/media/bench-press.gif");
        when(repository.findSystemExercisesWithExerciseDbUrls()).thenReturn(List.of(exercise));
        when(repository.findByVideoUrlIsNullAndIsSystemTrue()).thenReturn(List.of());

        // Static URL is dead
        when(gifDownloader.download("https://exercisedb.dev/media/bench-press.gif"))
                .thenThrow(new IOException("HTTP 404 downloading: https://exercisedb.dev/media/bench-press.gif"));
        // API search returns an envelope with the exercise
        when(gifDownloader.download(contains("exercisedb-api.vercel.app")))
                .thenReturn(apiEnvelopeJson("Bench Press", LIVE_GIF_URL).getBytes());
        // Download the GIF from the new API-provided URL
        when(gifDownloader.download(LIVE_GIF_URL))
                .thenReturn(new byte[]{1, 2, 3, 4});
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ExerciseMediaMigrationResult result = service.migrateAll();

        log.info("test.phase1Fallback.success succeeded={} failed={}", result.getSucceeded(), result.getFailed());
        assertEquals(1, result.getSucceeded());
        assertEquals(0, result.getFailed());
        verify(repository).save(argThat(e ->
                e.getVideoUrl().startsWith(MINIO_ENDPOINT) &&
                e.getVideoUrl().contains("bench-press")));
    }

    @Test
    void migrateAll_Phase1StaticUrlDead_WhenApiAlsoFails_ShouldCountAsFailed() throws IOException {
        ExerciseCatalog exercise = makeExercise(1L, "Squat",
                "https://exercisedb.dev/squat.gif");
        when(repository.findSystemExercisesWithExerciseDbUrls()).thenReturn(List.of(exercise));
        when(repository.findByVideoUrlIsNullAndIsSystemTrue()).thenReturn(List.of());

        // Both static URL and API fallback fail
        when(gifDownloader.download("https://exercisedb.dev/squat.gif"))
                .thenThrow(new IOException("HTTP 404 downloading"));
        when(gifDownloader.download(contains("exercisedb-api.vercel.app")))
                .thenThrow(new IOException("API server unavailable"));

        ExerciseMediaMigrationResult result = service.migrateAll();

        log.info("test.phase1Fallback.apiFails failed={}", result.getFailed());
        assertEquals(0, result.getSucceeded());
        assertEquals(1, result.getFailed());
        verify(repository, never()).save(any());
    }

    @Test
    void migrateAll_Phase1StaticUrlDead_WhenApiReturnsEmptyData_ShouldCountAsFailed() throws IOException {
        ExerciseCatalog exercise = makeExercise(1L, "Squat",
                "https://exercisedb.dev/squat.gif");
        when(repository.findSystemExercisesWithExerciseDbUrls()).thenReturn(List.of(exercise));
        when(repository.findByVideoUrlIsNullAndIsSystemTrue()).thenReturn(List.of());

        when(gifDownloader.download("https://exercisedb.dev/squat.gif"))
                .thenThrow(new IOException("HTTP 404 downloading"));
        when(gifDownloader.download(contains("exercisedb-api.vercel.app")))
                .thenReturn(emptyApiEnvelopeJson().getBytes());

        ExerciseMediaMigrationResult result = service.migrateAll();

        log.info("test.phase1Fallback.apiEmpty failed={}", result.getFailed());
        assertEquals(0, result.getSucceeded());
        assertEquals(1, result.getFailed());
        verify(repository, never()).save(any());
    }

    @Test
    void migrateAll_Phase1_ThreeExercises_OneSucceedsDirect_OneFallback_OneFails() throws IOException {
        ExerciseCatalog ex1 = makeExercise(1L, "Deadlift", "https://exercisedb.dev/deadlift.gif");
        ExerciseCatalog ex2 = makeExercise(2L, "Squat", "https://exercisedb.dev/squat.gif");
        ExerciseCatalog ex3 = makeExercise(3L, "Lunge", "https://exercisedb.dev/lunge.gif");
        when(repository.findSystemExercisesWithExerciseDbUrls()).thenReturn(List.of(ex1, ex2, ex3));
        when(repository.findByVideoUrlIsNullAndIsSystemTrue()).thenReturn(List.of());

        // ex1 Deadlift: static URL works
        when(gifDownloader.download("https://exercisedb.dev/deadlift.gif")).thenReturn(new byte[]{1});
        // ex2 Squat: static dead, API fallback succeeds
        when(gifDownloader.download("https://exercisedb.dev/squat.gif"))
                .thenThrow(new IOException("404"));
        when(gifDownloader.download(contains("search=Squat")))
                .thenReturn(apiEnvelopeJson("squat", LIVE_GIF_URL).getBytes());
        when(gifDownloader.download(LIVE_GIF_URL)).thenReturn(new byte[]{2});
        // ex3 Lunge: static dead, API also fails
        when(gifDownloader.download("https://exercisedb.dev/lunge.gif"))
                .thenThrow(new IOException("404"));
        when(gifDownloader.download(contains("search=Lunge")))
                .thenThrow(new IOException("API unreachable"));

        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ExerciseMediaMigrationResult result = service.migrateAll();

        assertEquals(2, result.getSucceeded()); // deadlift direct + squat via fallback
        assertEquals(1, result.getFailed());    // lunge: both paths failed
        assertEquals(0, result.getSkipped());
    }

    @Test
    void migrateAll_Phase1Fallback_UsesSearchEndpointWithExerciseName() throws IOException {
        ExerciseCatalog exercise = makeExercise(1L, "Bench Press",
                "https://exercisedb.dev/bench-press.gif");
        when(repository.findSystemExercisesWithExerciseDbUrls()).thenReturn(List.of(exercise));
        when(repository.findByVideoUrlIsNullAndIsSystemTrue()).thenReturn(List.of());

        when(gifDownloader.download("https://exercisedb.dev/bench-press.gif"))
                .thenThrow(new IOException("404"));
        // Verify the correct API endpoint is called
        when(gifDownloader.download(contains("exercisedb-api.vercel.app/api/v1/exercises")))
                .thenReturn(emptyApiEnvelopeJson().getBytes());

        service.migrateAll();

        // Verify the API was called (even if it returned empty, that's fine for this test)
        verify(gifDownloader).download(contains("exercisedb-api.vercel.app/api/v1/exercises"));
    }

    // -----------------------------------------------------------------------
    // migrateAll — Phase 1 error handling (static URL fails, no fallback
    //              needed — legacy tests updated for explicit mocking)
    // -----------------------------------------------------------------------

    @Test
    void migrateAll_DownloadFails_ShouldLogAndContinueToNextExercise() throws IOException {
        ExerciseCatalog ex1 = makeExercise(1L, "Squat", "https://exercisedb.dev/squat.gif");
        ExerciseCatalog ex2 = makeExercise(2L, "Deadlift", "https://exercisedb.dev/deadlift.gif");
        when(repository.findSystemExercisesWithExerciseDbUrls()).thenReturn(List.of(ex1, ex2));
        when(repository.findByVideoUrlIsNullAndIsSystemTrue()).thenReturn(List.of());

        // Squat static URL fails; API fallback also fails → counted as failed
        when(gifDownloader.download("https://exercisedb.dev/squat.gif"))
                .thenThrow(new IOException("404 Not Found"));
        when(gifDownloader.download(contains("search=Squat")))
                .thenThrow(new IOException("API fallback failed"));
        // Deadlift succeeds directly
        when(gifDownloader.download("https://exercisedb.dev/deadlift.gif"))
                .thenReturn(new byte[]{1, 2, 3});
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ExerciseMediaMigrationResult result = service.migrateAll();

        log.info("test.migrateAll.downloadFails succeeded={} failed={}", result.getSucceeded(), result.getFailed());
        assertEquals(1, result.getSucceeded());
        assertEquals(1, result.getFailed());
        verify(repository, times(1)).save(any()); // only deadlift saved
    }

    @Test
    void migrateAll_UploadFails_ShouldCountAsFailed() throws IOException {
        ExerciseCatalog exercise = makeExercise(1L, "Bench Press",
                "https://exercisedb.dev/bench-press.gif");
        when(repository.findSystemExercisesWithExerciseDbUrls()).thenReturn(List.of(exercise));
        when(repository.findByVideoUrlIsNullAndIsSystemTrue()).thenReturn(List.of());

        // Download succeeds but upload fails for BOTH the static-URL attempt
        // and the API fallback attempt (both try to putBytes)
        when(gifDownloader.download("https://exercisedb.dev/bench-press.gif"))
                .thenReturn(new byte[]{1, 2, 3});
        doThrow(new RuntimeException("MinIO unavailable"))
                .when(storageService).putBytes(any(), any(), any(), any());

        ExerciseMediaMigrationResult result = service.migrateAll();

        log.info("test.migrateAll.uploadFails failed={}", result.getFailed());
        assertEquals(0, result.getSucceeded());
        assertEquals(1, result.getFailed());
        verify(repository, never()).save(any());
    }

    @Test
    void migrateAll_ThreeExercises_TwoFail_OneSucceeds_ShouldCountCorrectly() throws IOException {
        ExerciseCatalog ex1 = makeExercise(1L, "A", "https://exercisedb.dev/a.gif");
        ExerciseCatalog ex2 = makeExercise(2L, "B", "https://exercisedb.dev/b.gif");
        ExerciseCatalog ex3 = makeExercise(3L, "C", "https://exercisedb.dev/c.gif");
        when(repository.findSystemExercisesWithExerciseDbUrls()).thenReturn(List.of(ex1, ex2, ex3));
        when(repository.findByVideoUrlIsNullAndIsSystemTrue()).thenReturn(List.of());

        when(gifDownloader.download("https://exercisedb.dev/a.gif")).thenReturn(new byte[]{1});
        when(gifDownloader.download("https://exercisedb.dev/b.gif")).thenThrow(new IOException("timeout"));
        when(gifDownloader.download(contains("search=B"))).thenThrow(new IOException("API timeout"));
        when(gifDownloader.download("https://exercisedb.dev/c.gif")).thenThrow(new IOException("403 Forbidden"));
        when(gifDownloader.download(contains("search=C"))).thenThrow(new IOException("API forbidden"));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ExerciseMediaMigrationResult result = service.migrateAll();

        assertEquals(1, result.getSucceeded());
        assertEquals(2, result.getFailed());
    }

    // -----------------------------------------------------------------------
    // migrateAll — Phase 2: null videoUrl → resolve via API
    // -----------------------------------------------------------------------

    @Test
    void migrateAll_NullVideoUrl_WhenApiReturnsValidResult_ShouldSucceed() throws IOException {
        ExerciseCatalog exercise = makeExercise(1L, "Bench Press", null);
        when(repository.findSystemExercisesWithExerciseDbUrls()).thenReturn(List.of());
        when(repository.findByVideoUrlIsNullAndIsSystemTrue()).thenReturn(List.of(exercise));

        when(gifDownloader.download(contains("exercisedb-api.vercel.app")))
                .thenReturn(apiEnvelopeJson("Bench Press", LIVE_GIF_URL).getBytes());
        when(gifDownloader.download(LIVE_GIF_URL)).thenReturn(new byte[]{1, 2, 3, 4});
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ExerciseMediaMigrationResult result = service.migrateAll();

        log.info("test.phase2.success succeeded={}", result.getSucceeded());
        assertEquals(1, result.getSucceeded());
        assertEquals(0, result.getSkipped());
        verify(repository).save(argThat(e ->
                e.getVideoUrl().startsWith(MINIO_ENDPOINT) &&
                e.getVideoUrl().contains("bench-press")));
    }

    @Test
    void migrateAll_NullVideoUrl_WhenApiLookupFails_ShouldCountAsSkipped() throws IOException {
        ExerciseCatalog exercise = makeExercise(1L, "Rare Exercise", null);
        when(repository.findSystemExercisesWithExerciseDbUrls()).thenReturn(List.of());
        when(repository.findByVideoUrlIsNullAndIsSystemTrue()).thenReturn(List.of(exercise));
        when(gifDownloader.download(anyString())).thenThrow(new IOException("API unavailable"));

        ExerciseMediaMigrationResult result = service.migrateAll();

        log.info("test.migrateAll.nullVideoUrl skipped={}", result.getSkipped());
        assertEquals(0, result.getSucceeded());
        assertEquals(1, result.getSkipped()); // can't resolve → skipped (not failed)
        verify(repository, never()).save(any());
    }

    @Test
    void migrateAll_NullVideoUrl_WhenApiReturnsEmptyData_ShouldCountAsSkipped() throws IOException {
        ExerciseCatalog exercise = makeExercise(1L, "Unknown Exercise", null);
        when(repository.findSystemExercisesWithExerciseDbUrls()).thenReturn(List.of());
        when(repository.findByVideoUrlIsNullAndIsSystemTrue()).thenReturn(List.of(exercise));
        when(gifDownloader.download(anyString())).thenReturn(emptyApiEnvelopeJson().getBytes());

        ExerciseMediaMigrationResult result = service.migrateAll();

        assertEquals(0, result.getSucceeded());
        assertEquals(1, result.getSkipped());
    }

    @Test
    void migrateAll_NullVideoUrl_ApiUsesSearchParamWithExerciseName() throws IOException {
        ExerciseCatalog exercise = makeExercise(1L, "Lat Pulldown", null);
        when(repository.findSystemExercisesWithExerciseDbUrls()).thenReturn(List.of());
        when(repository.findByVideoUrlIsNullAndIsSystemTrue()).thenReturn(List.of(exercise));
        when(gifDownloader.download(contains("search=Lat+Pulldown")))
                .thenReturn(emptyApiEnvelopeJson().getBytes());

        service.migrateAll();

        verify(gifDownloader).download(contains("search=Lat+Pulldown"));
    }

    // -----------------------------------------------------------------------
    // migrateAll — no storage service (local profile)
    // -----------------------------------------------------------------------

    @Test
    void migrateAll_WithNullStorageService_ShouldReturnEarlyWithZeroCounts() {
        ExerciseMediaMigrationService localService = new ExerciseMediaMigrationService(
                repository, null, objectMapper, MINIO_ENDPOINT, gifDownloader, 0L);

        ExerciseMediaMigrationResult result = localService.migrateAll();

        log.info("test.migrateAll.localProfile message={}", result.getMessage());
        assertEquals(0, result.getSucceeded());
        assertEquals(0, result.getFailed());
        assertEquals(0, result.getSkipped());
        assertNotNull(result.getMessage());
        verifyNoInteractions(repository);
        verifyNoInteractions(gifDownloader);
    }

    // -----------------------------------------------------------------------
    // Task 1: Alias fallback tests
    // -----------------------------------------------------------------------

    @Test
    void migrateAll_ConventionalDeadlift_ShouldFallbackToAliasWhenExactNameFails() throws IOException {
        ExerciseCatalog exercise = makeExercise(1L, "Conventional Deadlift", null);
        exercise.setInstructions(null);
        when(repository.findSystemExercisesWithExerciseDbUrls()).thenReturn(List.of());
        when(repository.findByVideoUrlIsNullAndIsSystemTrue()).thenReturn(List.of(exercise));
        when(repository.findByIsSystemTrueAndInstructionsIsNull()).thenReturn(List.of());

        // Exact name search returns empty envelope (URLEncoder preserves the exercise name's casing)
        when(gifDownloader.download(contains("Conventional+Deadlift"))).thenReturn(emptyApiEnvelopeJson().getBytes());
        // Alias search returns a valid envelope (alias is lowercase: "barbell deadlift")
        String aliasJson = "{\"success\":true,\"data\":[{\"name\":\"barbell deadlift\"," +
            "\"gifUrl\":\"https://static.exercisedb.dev/media/ila4NZS.gif\"," +
            "\"instructions\":[\"Set up\",\"Lift\"],\"bodyParts\":[\"back\"],\"secondaryMuscles\":[\"hamstrings\"]}]," +
            "\"metadata\":{\"totalPages\":1,\"totalExercises\":1,\"currentPage\":1,\"previousPage\":null,\"nextPage\":null}}";
        when(gifDownloader.download(contains("barbell+deadlift"))).thenReturn(aliasJson.getBytes());
        when(gifDownloader.download("https://static.exercisedb.dev/media/ila4NZS.gif")).thenReturn(new byte[]{1, 2, 3});
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ExerciseMediaMigrationResult result = service.migrateAll();

        log.info("test.alias.conventionalDeadlift succeeded={} skipped={}", result.getSucceeded(), result.getSkipped());
        assertEquals(1, result.getSucceeded());
        assertEquals(0, result.getSkipped());
        verify(repository).save(argThat(e -> e.getVideoUrl().contains("conventional-deadlift")));
    }

    @Test
    void migrateAll_TreadmillRun_ShouldFallbackToAliasWhenExactNameFails() throws IOException {
        ExerciseCatalog exercise = makeExercise(1L, "Treadmill Run", null);
        exercise.setInstructions(null);
        when(repository.findSystemExercisesWithExerciseDbUrls()).thenReturn(List.of());
        when(repository.findByVideoUrlIsNullAndIsSystemTrue()).thenReturn(List.of(exercise));
        when(repository.findByIsSystemTrueAndInstructionsIsNull()).thenReturn(List.of());

        when(gifDownloader.download(contains("Treadmill+Run"))).thenReturn(emptyApiEnvelopeJson().getBytes());
        String aliasJson = "{\"success\":true,\"data\":[{\"name\":\"walking on incline treadmill\"," +
            "\"gifUrl\":\"https://static.exercisedb.dev/media/rjiM4L3.gif\"," +
            "\"instructions\":[\"Step 1\"],\"bodyParts\":[\"cardio\"],\"secondaryMuscles\":[]}]," +
            "\"metadata\":{\"totalPages\":1,\"totalExercises\":1,\"currentPage\":1,\"previousPage\":null,\"nextPage\":null}}";
        when(gifDownloader.download(contains("walking+on+incline+treadmill"))).thenReturn(aliasJson.getBytes());
        when(gifDownloader.download("https://static.exercisedb.dev/media/rjiM4L3.gif")).thenReturn(new byte[]{4, 5, 6});
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ExerciseMediaMigrationResult result = service.migrateAll();

        log.info("test.alias.treadmillRun succeeded={} skipped={}", result.getSucceeded(), result.getSkipped());
        assertEquals(1, result.getSucceeded());
        assertEquals(0, result.getSkipped());
        verify(repository).save(argThat(e -> e.getVideoUrl().contains("treadmill-run")));
    }

    @Test
    void migrateAll_ExerciseWithNoAlias_WhenExactNameFails_ShouldCountAsUnresolved() throws IOException {
        ExerciseCatalog exercise = makeExercise(1L, "Mystery Exercise", null);
        when(repository.findSystemExercisesWithExerciseDbUrls()).thenReturn(List.of());
        when(repository.findByVideoUrlIsNullAndIsSystemTrue()).thenReturn(List.of(exercise));
        when(repository.findByIsSystemTrueAndInstructionsIsNull()).thenReturn(List.of());
        when(gifDownloader.download(anyString())).thenReturn(emptyApiEnvelopeJson().getBytes());

        ExerciseMediaMigrationResult result = service.migrateAll();

        assertEquals(0, result.getSucceeded());
        assertEquals(1, result.getSkipped());
    }

    // -----------------------------------------------------------------------
    // Task 2: API enrichment tests
    // -----------------------------------------------------------------------

    @Test
    void migrateAll_NullVideoUrl_WhenApiReturnsInstructions_ShouldSaveInstructions() throws IOException {
        ExerciseCatalog exercise = makeExercise(1L, "Push Up", null);
        exercise.setInstructions(null);
        when(repository.findSystemExercisesWithExerciseDbUrls()).thenReturn(List.of());
        when(repository.findByVideoUrlIsNullAndIsSystemTrue()).thenReturn(List.of(exercise));
        when(repository.findByIsSystemTrueAndInstructionsIsNull()).thenReturn(List.of());

        String apiJson = "{\"success\":true,\"data\":[{\"name\":\"push up\"," +
            "\"gifUrl\":\"https://exercisedb.dev/push-up.gif\"," +
            "\"instructions\":[\"Get into position\",\"Lower your body\",\"Push back up\"]," +
            "\"bodyParts\":[\"chest\"],\"secondaryMuscles\":[\"triceps\",\"shoulders\"]}]," +
            "\"metadata\":{\"totalPages\":1,\"totalExercises\":1,\"currentPage\":1,\"previousPage\":null,\"nextPage\":null}}";
        when(gifDownloader.download(contains("Push+Up"))).thenReturn(apiJson.getBytes());
        when(gifDownloader.download("https://exercisedb.dev/push-up.gif")).thenReturn(new byte[]{1, 2, 3});
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.migrateAll();

        verify(repository).save(argThat(e ->
            e.getInstructions() != null &&
            e.getInstructions().contains("Get into position") &&
            e.getInstructions().contains("Lower your body")));
    }

    @Test
    void migrateAll_NullVideoUrl_WhenApiReturnsBodyPart_ShouldSaveBodyPart() throws IOException {
        ExerciseCatalog exercise = makeExercise(1L, "Pull Up", null);
        exercise.setInstructions(null);
        when(repository.findSystemExercisesWithExerciseDbUrls()).thenReturn(List.of());
        when(repository.findByVideoUrlIsNullAndIsSystemTrue()).thenReturn(List.of(exercise));
        when(repository.findByIsSystemTrueAndInstructionsIsNull()).thenReturn(List.of());

        String apiJson = "{\"success\":true,\"data\":[{\"name\":\"pull up\"," +
            "\"gifUrl\":\"https://exercisedb.dev/pull-up.gif\"," +
            "\"instructions\":[\"Hang\",\"Pull\"],\"bodyParts\":[\"back\"],\"secondaryMuscles\":[\"biceps\"]}]," +
            "\"metadata\":{\"totalPages\":1,\"totalExercises\":1,\"currentPage\":1,\"previousPage\":null,\"nextPage\":null}}";
        when(gifDownloader.download(contains("Pull+Up"))).thenReturn(apiJson.getBytes());
        when(gifDownloader.download("https://exercisedb.dev/pull-up.gif")).thenReturn(new byte[]{1, 2, 3});
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.migrateAll();

        // bodyParts[0] from community API is "back" → stored in bodyPart field
        verify(repository).save(argThat(e -> "back".equals(e.getBodyPart())));
    }

    @Test
    void migrateAll_NullVideoUrl_WhenApiReturnsSecondaryMuscles_ShouldSaveThem() throws IOException {
        ExerciseCatalog exercise = makeExercise(1L, "Bench Press", null);
        exercise.setInstructions(null);
        when(repository.findSystemExercisesWithExerciseDbUrls()).thenReturn(List.of());
        when(repository.findByVideoUrlIsNullAndIsSystemTrue()).thenReturn(List.of(exercise));
        when(repository.findByIsSystemTrueAndInstructionsIsNull()).thenReturn(List.of());

        String apiJson = "{\"success\":true,\"data\":[{\"name\":\"bench press\"," +
            "\"gifUrl\":\"https://exercisedb.dev/bench.gif\"," +
            "\"instructions\":[\"Lie down\",\"Press\"],\"bodyParts\":[\"chest\"]," +
            "\"secondaryMuscles\":[\"triceps\",\"front deltoid\"]}]," +
            "\"metadata\":{\"totalPages\":1,\"totalExercises\":1,\"currentPage\":1,\"previousPage\":null,\"nextPage\":null}}";
        when(gifDownloader.download(contains("Bench+Press"))).thenReturn(apiJson.getBytes());
        when(gifDownloader.download("https://exercisedb.dev/bench.gif")).thenReturn(new byte[]{1, 2, 3});
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.migrateAll();

        verify(repository).save(argThat(e ->
            e.getSecondaryMuscles() != null &&
            e.getSecondaryMuscles().contains("triceps") &&
            e.getSecondaryMuscles().contains("front deltoid")));
    }

    @Test
    void migrateAll_NullVideoUrl_WhenInstructionsAlreadySet_ShouldNotOverwrite() throws IOException {
        ExerciseCatalog exercise = makeExercise(1L, "Squat", null);
        exercise.setInstructions("Existing instructions");
        when(repository.findSystemExercisesWithExerciseDbUrls()).thenReturn(List.of());
        when(repository.findByVideoUrlIsNullAndIsSystemTrue()).thenReturn(List.of(exercise));
        when(repository.findByIsSystemTrueAndInstructionsIsNull()).thenReturn(List.of());

        String apiJson = "{\"success\":true,\"data\":[{\"name\":\"squat\"," +
            "\"gifUrl\":\"https://exercisedb.dev/squat.gif\"," +
            "\"instructions\":[\"API instructions\"],\"bodyParts\":[\"legs\"],\"secondaryMuscles\":[]}]," +
            "\"metadata\":{\"totalPages\":1,\"totalExercises\":1,\"currentPage\":1,\"previousPage\":null,\"nextPage\":null}}";
        when(gifDownloader.download(contains("Squat"))).thenReturn(apiJson.getBytes());
        when(gifDownloader.download("https://exercisedb.dev/squat.gif")).thenReturn(new byte[]{1, 2, 3});
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.migrateAll();

        verify(repository).save(argThat(e ->
            "Existing instructions".equals(e.getInstructions())));
    }

    @Test
    void migrateAll_Phase3_ShouldEnrichExercisesWithNullInstructions() throws IOException {
        // Exercise already migrated to MinIO but has no instructions yet
        ExerciseCatalog exercise = makeExercise(1L, "Deadlift",
                MINIO_ENDPOINT + "/exercise-media/exercises/deadlift/animation.gif");
        exercise.setInstructions(null);

        when(repository.findSystemExercisesWithExerciseDbUrls()).thenReturn(List.of());
        when(repository.findByVideoUrlIsNullAndIsSystemTrue()).thenReturn(List.of());
        when(repository.findByIsSystemTrueAndInstructionsIsNull()).thenReturn(List.of(exercise));

        String apiJson = "{\"success\":true,\"data\":[{\"name\":\"deadlift\"," +
            "\"gifUrl\":\"https://exercisedb.dev/deadlift.gif\"," +
            "\"instructions\":[\"Step 1\",\"Step 2\"],\"bodyParts\":[\"back\"],\"secondaryMuscles\":[\"hamstrings\"]}]," +
            "\"metadata\":{\"totalPages\":1,\"totalExercises\":1,\"currentPage\":1,\"previousPage\":null,\"nextPage\":null}}";
        when(gifDownloader.download(contains("Deadlift"))).thenReturn(apiJson.getBytes());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ExerciseMediaMigrationResult result = service.migrateAll();

        log.info("test.phase3.enrich succeeded={}", result.getSucceeded());
        assertEquals(1, result.getSucceeded());
        verify(repository).save(argThat(e ->
            e.getInstructions() != null && e.getInstructions().contains("Step 1")));
        // Phase 3 enrichment does NOT upload GIFs
        verify(storageService, never()).putBytes(any(), any(), any(), any());
    }

    @Test
    void migrateAll_Phase3_WhenApiReturnsEmpty_ShouldCountAsSkipped() throws IOException {
        ExerciseCatalog exercise = makeExercise(1L, "Unknown Exercise",
                MINIO_ENDPOINT + "/exercise-media/exercises/unknown/animation.gif");
        exercise.setInstructions(null);

        when(repository.findSystemExercisesWithExerciseDbUrls()).thenReturn(List.of());
        when(repository.findByVideoUrlIsNullAndIsSystemTrue()).thenReturn(List.of());
        when(repository.findByIsSystemTrueAndInstructionsIsNull()).thenReturn(List.of(exercise));
        when(gifDownloader.download(anyString())).thenReturn(emptyApiEnvelopeJson().getBytes());

        ExerciseMediaMigrationResult result = service.migrateAll();

        assertEquals(0, result.getSucceeded());
        assertEquals(1, result.getSkipped());
        verify(repository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private ExerciseCatalog makeExercise(Long id, String name, String videoUrl) {
        ExerciseCatalog e = new ExerciseCatalog();
        e.setId(id);
        e.setName(name);
        e.setVideoUrl(videoUrl);
        e.setSystem(true);
        e.setExerciseType("STRENGTH");
        e.setMuscleGroups(List.of("CHEST"));
        e.setEquipmentRequired(List.of("BARBELL"));
        e.setDifficultyLevel("INTERMEDIATE");
        e.setInstructions("Do the exercise correctly.");
        return e;
    }

    /**
     * Builds a minimal ExerciseDB API v1 envelope JSON containing one exercise.
     */
    private String apiEnvelopeJson(String name, String gifUrl) {
        return String.format(
                "{\"success\":true,\"data\":[{\"exerciseId\":\"abc123\",\"name\":\"%s\",\"gifUrl\":\"%s\"," +
                "\"targetMuscles\":[\"chest\"],\"bodyParts\":[\"upper arms\"],\"equipments\":[\"barbell\"]," +
                "\"secondaryMuscles\":[],\"instructions\":[\"Step 1\"]}]," +
                "\"metadata\":{\"totalPages\":1,\"totalExercises\":1,\"currentPage\":1,\"previousPage\":null,\"nextPage\":null}}",
                name, gifUrl);
    }

    /**
     * Builds an ExerciseDB API v1 envelope JSON with an empty data array.
     */
    private String emptyApiEnvelopeJson() {
        return "{\"success\":true,\"data\":[]," +
               "\"metadata\":{\"totalPages\":0,\"totalExercises\":0,\"currentPage\":1,\"previousPage\":null,\"nextPage\":null}}";
    }
}
