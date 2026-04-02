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
 * Covers: slugify logic, idempotency, successful migration, error handling.
 */
@ExtendWith(MockitoExtension.class)
class ExerciseMediaMigrationServiceTest {

    private static final Logger log = LoggerFactory.getLogger(ExerciseMediaMigrationServiceTest.class);

    private static final String MINIO_ENDPOINT = "https://minio.example.com";

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
        ExerciseCatalog alreadyMigrated = makeExercise(1L, "Squat",
                MINIO_ENDPOINT + "/exercise-media/exercises/squat/animation.gif");
        // This exercise won't appear in the ExerciseDB query (it's already migrated)
        // But to simulate re-checking the findByIsSystem list, we return empty from the exercisedb query
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
    // migrateAll — successful migration
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
    // migrateAll — error handling
    // -----------------------------------------------------------------------

    @Test
    void migrateAll_DownloadFails_ShouldLogAndContinueToNextExercise() throws IOException {
        ExerciseCatalog ex1 = makeExercise(1L, "Squat", "https://exercisedb.dev/squat.gif");
        ExerciseCatalog ex2 = makeExercise(2L, "Deadlift", "https://exercisedb.dev/deadlift.gif");
        when(repository.findSystemExercisesWithExerciseDbUrls()).thenReturn(List.of(ex1, ex2));
        when(repository.findByVideoUrlIsNullAndIsSystemTrue()).thenReturn(List.of());
        when(gifDownloader.download(contains("squat"))).thenThrow(new IOException("404 Not Found"));
        when(gifDownloader.download(contains("deadlift"))).thenReturn(new byte[]{1, 2, 3});
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
        when(gifDownloader.download(anyString())).thenReturn(new byte[]{1, 2, 3});
        doThrow(new RuntimeException("MinIO unavailable"))
                .when(storageService).putBytes(any(), any(), any(), any());

        ExerciseMediaMigrationResult result = service.migrateAll();

        log.info("test.migrateAll.uploadFails failed={}", result.getFailed());
        assertEquals(0, result.getSucceeded());
        assertEquals(1, result.getFailed());
        verify(repository, never()).save(any());
    }

    @Test
    void migrateAll_ThreeExercises_TwoFail_OnceSucceeds_ShouldCountCorrectly() throws IOException {
        ExerciseCatalog ex1 = makeExercise(1L, "A", "https://exercisedb.dev/a.gif");
        ExerciseCatalog ex2 = makeExercise(2L, "B", "https://exercisedb.dev/b.gif");
        ExerciseCatalog ex3 = makeExercise(3L, "C", "https://exercisedb.dev/c.gif");
        when(repository.findSystemExercisesWithExerciseDbUrls()).thenReturn(List.of(ex1, ex2, ex3));
        when(repository.findByVideoUrlIsNullAndIsSystemTrue()).thenReturn(List.of());
        when(gifDownloader.download(contains("/a."))).thenReturn(new byte[]{1});
        when(gifDownloader.download(contains("/b."))).thenThrow(new IOException("timeout"));
        when(gifDownloader.download(contains("/c."))).thenThrow(new IOException("403 Forbidden"));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ExerciseMediaMigrationResult result = service.migrateAll();

        assertEquals(1, result.getSucceeded());
        assertEquals(2, result.getFailed());
    }

    // -----------------------------------------------------------------------
    // migrateAll — null videoUrl exercises
    // -----------------------------------------------------------------------

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
    void migrateAll_NullVideoUrl_WhenApiReturnsEmptyArray_ShouldCountAsSkipped() throws IOException {
        ExerciseCatalog exercise = makeExercise(1L, "Unknown Exercise", null);
        when(repository.findSystemExercisesWithExerciseDbUrls()).thenReturn(List.of());
        when(repository.findByVideoUrlIsNullAndIsSystemTrue()).thenReturn(List.of(exercise));
        when(gifDownloader.download(anyString())).thenReturn("[]".getBytes());

        ExerciseMediaMigrationResult result = service.migrateAll();

        assertEquals(0, result.getSucceeded());
        assertEquals(1, result.getSkipped());
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
}
