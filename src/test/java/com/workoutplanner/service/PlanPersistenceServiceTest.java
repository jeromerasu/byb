package com.workoutplanner.service;

import com.workoutplanner.model.DietProfile;
import com.workoutplanner.model.PlanGenerationQueue;
import com.workoutplanner.model.QueueStatus;
import com.workoutplanner.model.WorkoutProfile;
import com.workoutplanner.repository.DietProfileRepository;
import com.workoutplanner.repository.WorkoutProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TASK-BE-016C: Unit tests for PlanPersistenceService.
 */
@ExtendWith(MockitoExtension.class)
class PlanPersistenceServiceTest {

    private static final Logger log = LoggerFactory.getLogger(PlanPersistenceServiceTest.class);

    @Mock
    private StorageService storageService;
    @Mock
    private WorkoutProfileRepository workoutProfileRepository;
    @Mock
    private DietProfileRepository dietProfileRepository;

    private PlanPersistenceService persistenceService;

    @BeforeEach
    void setUp() {
        persistenceService = new PlanPersistenceService(
                storageService, workoutProfileRepository, dietProfileRepository);
        ReflectionTestUtils.setField(persistenceService, "betaMode", false);
    }

    private PlanGenerationQueue makeClaimedEntry(String userId) {
        PlanGenerationQueue q = new PlanGenerationQueue();
        q.setId("entry-" + userId);
        q.setUserId(userId);
        q.setStatus(QueueStatus.CLAIMED);
        return q;
    }

    // -- persist() -------------------------------------------------------

    @Test
    void persist_HappyPath_ShouldStoreAndUpdateProfiles() {
        String userId = "user-1";
        PlanGenerationQueue entry = makeClaimedEntry(userId);
        Map<String, Object> wp = Map.of("plan", "workout");
        Map<String, Object> dp = Map.of("plan", "diet");

        WorkoutProfile workoutProfile = new WorkoutProfile();
        DietProfile dietProfile = new DietProfile();

        when(storageService.storeWorkoutPlan(eq("workout"), eq(userId), anyString(), eq(wp)))
                .thenReturn("workout/user-1/plan.json");
        when(storageService.storeDietPlan(eq("diet"), eq(userId), anyString(), eq(dp)))
                .thenReturn("diet/user-1/plan.json");
        when(workoutProfileRepository.findByUserId(userId)).thenReturn(Optional.of(workoutProfile));
        when(dietProfileRepository.findByUserId(userId)).thenReturn(Optional.of(dietProfile));
        when(workoutProfileRepository.save(any())).thenReturn(workoutProfile);
        when(dietProfileRepository.save(any())).thenReturn(dietProfile);

        PlanPersistenceService.PersistenceResult result = persistenceService.persist(entry, wp, dp);

        log.info("test.persist.happyPath workoutKey={} dietKey={}", result.getWorkoutStorageKey(), result.getDietStorageKey());
        assertEquals("workout/user-1/plan.json", result.getWorkoutStorageKey());
        assertEquals("diet/user-1/plan.json", result.getDietStorageKey());
    }

    @Test
    void persist_BetaMode_ShouldUseBetaBuckets() {
        ReflectionTestUtils.setField(persistenceService, "betaMode", true);
        String userId = "user-beta";
        PlanGenerationQueue entry = makeClaimedEntry(userId);
        Map<String, Object> wp = Map.of();
        Map<String, Object> dp = Map.of();

        when(storageService.storeWorkoutPlan(eq("workoutbeta"), eq(userId), anyString(), eq(wp)))
                .thenReturn("workoutbeta/key");
        when(storageService.storeDietPlan(eq("dietbeta"), eq(userId), anyString(), eq(dp)))
                .thenReturn("dietbeta/key");
        when(workoutProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(dietProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        PlanPersistenceService.PersistenceResult result = persistenceService.persist(entry, wp, dp);

        log.info("test.persist.betaMode workoutKey={}", result.getWorkoutStorageKey());
        verify(storageService).storeWorkoutPlan(eq("workoutbeta"), eq(userId), anyString(), eq(wp));
        verify(storageService).storeDietPlan(eq("dietbeta"), eq(userId), anyString(), eq(dp));
    }

    @Test
    void persist_WorkoutStorageFailure_ShouldThrowRuntimeException() {
        String userId = "user-fail";
        PlanGenerationQueue entry = makeClaimedEntry(userId);
        when(storageService.storeWorkoutPlan(anyString(), anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Storage unavailable"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> persistenceService.persist(entry, Map.of(), Map.of()));

        log.info("test.persist.storageFailure msg={}", ex.getMessage());
        assertTrue(ex.getMessage().contains("Failed to store workout plan"));
    }

    @Test
    void persist_DietStorageFailure_ShouldThrowRuntimeException() {
        String userId = "user-diet-fail";
        PlanGenerationQueue entry = makeClaimedEntry(userId);
        when(storageService.storeWorkoutPlan(anyString(), anyString(), anyString(), any()))
                .thenReturn("workout/key");
        when(storageService.storeDietPlan(anyString(), anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Diet storage error"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> persistenceService.persist(entry, Map.of(), Map.of()));

        log.info("test.persist.dietStorageFailure msg={}", ex.getMessage());
        assertTrue(ex.getMessage().contains("Failed to store diet plan"));
    }

    @Test
    void persist_MissingWorkoutProfile_ShouldContinueWithoutFailure() {
        String userId = "user-no-wp";
        PlanGenerationQueue entry = makeClaimedEntry(userId);
        when(storageService.storeWorkoutPlan(anyString(), anyString(), anyString(), any()))
                .thenReturn("wk");
        when(storageService.storeDietPlan(anyString(), anyString(), anyString(), any()))
                .thenReturn("dk");
        when(workoutProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(dietProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        PlanPersistenceService.PersistenceResult result = persistenceService.persist(entry, Map.of(), Map.of());

        log.info("test.persist.missingWorkoutProfile completed without error");
        assertNotNull(result);
        verify(workoutProfileRepository, never()).save(any());
        verify(dietProfileRepository, never()).save(any());
    }

    @Test
    void persist_ShouldUpdateWorkoutProfileFields() {
        String userId = "user-update";
        PlanGenerationQueue entry = makeClaimedEntry(userId);
        WorkoutProfile wp = new WorkoutProfile();
        DietProfile dp = new DietProfile();

        when(storageService.storeWorkoutPlan(anyString(), anyString(), anyString(), any()))
                .thenReturn("workout/updated.json");
        when(storageService.storeDietPlan(anyString(), anyString(), anyString(), any()))
                .thenReturn("diet/updated.json");
        when(workoutProfileRepository.findByUserId(userId)).thenReturn(Optional.of(wp));
        when(dietProfileRepository.findByUserId(userId)).thenReturn(Optional.of(dp));
        when(workoutProfileRepository.save(any())).thenReturn(wp);
        when(dietProfileRepository.save(any())).thenReturn(dp);

        persistenceService.persist(entry, Map.of(), Map.of());

        ArgumentCaptor<WorkoutProfile> wpCaptor = ArgumentCaptor.forClass(WorkoutProfile.class);
        verify(workoutProfileRepository).save(wpCaptor.capture());
        assertEquals("workout/updated.json", wpCaptor.getValue().getCurrentPlanStorageKey());
        assertNotNull(wpCaptor.getValue().getCurrentPlanTitle());
        assertNotNull(wpCaptor.getValue().getCurrentPlanCreatedAt());

        ArgumentCaptor<DietProfile> dpCaptor = ArgumentCaptor.forClass(DietProfile.class);
        verify(dietProfileRepository).save(dpCaptor.capture());
        assertEquals("diet/updated.json", dpCaptor.getValue().getCurrentPlanStorageKey());
    }

    // -- PersistenceResult -----------------------------------------------

    @Test
    void persistenceResult_ShouldStoreKeys() {
        PlanPersistenceService.PersistenceResult result =
                new PlanPersistenceService.PersistenceResult("workout-key", "diet-key");
        assertEquals("workout-key", result.getWorkoutStorageKey());
        assertEquals("diet-key", result.getDietStorageKey());
    }
}
