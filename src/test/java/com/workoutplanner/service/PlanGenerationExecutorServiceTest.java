package com.workoutplanner.service;

import com.workoutplanner.model.DietProfile;
import com.workoutplanner.model.PlanGenerationQueue;
import com.workoutplanner.model.QueueStatus;
import com.workoutplanner.model.WorkoutProfile;
import com.workoutplanner.repository.DietProfileRepository;
import com.workoutplanner.repository.WorkoutProfileRepository;
import com.workoutplanner.service.OverloadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TASK-BE-016B: Unit tests for PlanGenerationExecutorService.
 */
@ExtendWith(MockitoExtension.class)
class PlanGenerationExecutorServiceTest {

    private static final Logger log = LoggerFactory.getLogger(PlanGenerationExecutorServiceTest.class);

    @Mock
    private WorkoutProfileRepository workoutProfileRepository;
    @Mock
    private DietProfileRepository dietProfileRepository;
    @Mock
    private OpenAIService openAIService;
    @Mock
    private OverloadService overloadService;

    private PlanGenerationExecutorService executorService;

    @BeforeEach
    void setUp() {
        executorService = new PlanGenerationExecutorService(
                workoutProfileRepository, dietProfileRepository, openAIService, overloadService);
        // Default: no feedback (lenient to avoid UnnecessaryStubbingException in non-execute tests)
        lenient().when(overloadService.buildFeedbackBlock(any(), any(), any())).thenReturn("");
    }

    private PlanGenerationQueue makeClaimedEntry(String userId) {
        PlanGenerationQueue q = new PlanGenerationQueue();
        q.setId("entry-" + userId);
        q.setUserId(userId);
        q.setStatus(QueueStatus.CLAIMED);
        q.setAttemptCount(1);
        return q;
    }

    // -- execute() -------------------------------------------------------

    @Test
    void execute_HappyPath_ShouldReturnGenerationResult() {
        String userId = "user-happy";
        PlanGenerationQueue entry = makeClaimedEntry(userId);
        WorkoutProfile wp = new WorkoutProfile();
        DietProfile dp = new DietProfile();
        Map<String, Object> workoutPlan = Map.of("day1", "pushups");
        Map<String, Object> dietPlan = Map.of("calories", 2000);

        when(workoutProfileRepository.findByUserId(userId)).thenReturn(Optional.of(wp));
        when(dietProfileRepository.findByUserId(userId)).thenReturn(Optional.of(dp));
        OpenAIService.CombinedPlanResult combined =
                new OpenAIService.CombinedPlanResult(workoutPlan, dietPlan);
        when(openAIService.generateCombinedPlans(any(), any(), any())).thenReturn(combined);

        PlanGenerationExecutorService.GenerationResult result = executorService.execute(entry);

        log.info("test.execute.happyPath workoutPlan={} dietPlan={}", result.getWorkoutPlan(), result.getDietPlan());
        assertNotNull(result);
        assertEquals(workoutPlan, result.getWorkoutPlan());
        assertEquals(dietPlan, result.getDietPlan());
    }

    @Test
    void execute_MissingWorkoutProfile_ShouldThrowFatalException() {
        String userId = "user-no-workout";
        PlanGenerationQueue entry = makeClaimedEntry(userId);
        when(workoutProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        PlanGenerationExecutorService.PlanGenerationException ex = assertThrows(
                PlanGenerationExecutorService.PlanGenerationException.class,
                () -> executorService.execute(entry));

        log.info("test.execute.noWorkoutProfile fatal={} msg={}", ex.isFatal(), ex.getMessage());
        assertTrue(ex.isFatal());
        assertTrue(ex.getMessage().contains("Workout profile not found"));
        verifyNoInteractions(openAIService);
    }

    @Test
    void execute_MissingDietProfile_ShouldThrowFatalException() {
        String userId = "user-no-diet";
        PlanGenerationQueue entry = makeClaimedEntry(userId);
        WorkoutProfile wp = new WorkoutProfile();
        when(workoutProfileRepository.findByUserId(userId)).thenReturn(Optional.of(wp));
        when(dietProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        PlanGenerationExecutorService.PlanGenerationException ex = assertThrows(
                PlanGenerationExecutorService.PlanGenerationException.class,
                () -> executorService.execute(entry));

        log.info("test.execute.noDietProfile fatal={} msg={}", ex.isFatal(), ex.getMessage());
        assertTrue(ex.isFatal());
        assertTrue(ex.getMessage().contains("Diet profile not found"));
        verifyNoInteractions(openAIService);
    }

    @Test
    void execute_OpenAIFailure_ShouldThrowNonFatalException() {
        String userId = "user-openai-fail";
        PlanGenerationQueue entry = makeClaimedEntry(userId);
        WorkoutProfile wp = new WorkoutProfile();
        DietProfile dp = new DietProfile();
        when(workoutProfileRepository.findByUserId(userId)).thenReturn(Optional.of(wp));
        when(dietProfileRepository.findByUserId(userId)).thenReturn(Optional.of(dp));
        when(openAIService.generateCombinedPlans(any(), any(), any()))
                .thenThrow(new RuntimeException("Connection timeout"));

        PlanGenerationExecutorService.PlanGenerationException ex = assertThrows(
                PlanGenerationExecutorService.PlanGenerationException.class,
                () -> executorService.execute(entry));

        log.info("test.execute.openAIFail fatal={} msg={}", ex.isFatal(), ex.getMessage());
        assertFalse(ex.isFatal());
        assertTrue(ex.getMessage().contains("OpenAI generation failed"));
    }

    // -- GenerationResult ------------------------------------------------

    @Test
    void generationResult_ShouldStoreWorkoutAndDietPlans() {
        Map<String, Object> wp = Map.of("a", 1);
        Map<String, Object> dp = Map.of("b", 2);
        PlanGenerationExecutorService.GenerationResult result =
                new PlanGenerationExecutorService.GenerationResult(wp, dp);

        assertEquals(wp, result.getWorkoutPlan());
        assertEquals(dp, result.getDietPlan());
    }

    // -- PlanGenerationException -----------------------------------------

    @Test
    void planGenerationException_FatalConstructor_ShouldBeMarkedFatal() {
        PlanGenerationExecutorService.PlanGenerationException ex =
                new PlanGenerationExecutorService.PlanGenerationException("Fatal error", true);
        assertTrue(ex.isFatal());
        assertEquals("Fatal error", ex.getMessage());
    }

    @Test
    void planGenerationException_CauseConstructor_ShouldNotBeFatal() {
        RuntimeException cause = new RuntimeException("cause");
        PlanGenerationExecutorService.PlanGenerationException ex =
                new PlanGenerationExecutorService.PlanGenerationException("Transient error", cause);
        assertFalse(ex.isFatal());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void planGenerationException_NonFatalConstructor() {
        PlanGenerationExecutorService.PlanGenerationException ex =
                new PlanGenerationExecutorService.PlanGenerationException("Not fatal", false);
        assertFalse(ex.isFatal());
    }
}
