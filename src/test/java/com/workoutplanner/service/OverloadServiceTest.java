package com.workoutplanner.service;

import com.workoutplanner.model.WorkoutRating;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P1-011: Unit tests for OverloadService progressive overload rule engine.
 * Covers all 8 rule branches, completionRate edge cases, and keyword detection.
 */
@ExtendWith(MockitoExtension.class)
class OverloadServiceTest {

    private static final Logger log = LoggerFactory.getLogger(OverloadServiceTest.class);

    @Mock
    private com.workoutplanner.repository.WorkoutLogRepository workoutLogRepository;
    @Mock
    private com.workoutplanner.repository.MealLogRepository mealLogRepository;
    @Mock
    private com.workoutplanner.repository.WorkoutProfileRepository workoutProfileRepository;
    @Mock
    private StorageService storageService;
    @Mock
    private com.workoutplanner.repository.WorkoutFeedbackRepository workoutFeedbackRepository;
    @Mock
    private com.workoutplanner.repository.DietFeedbackRepository dietFeedbackRepository;

    private OverloadService service;

    @BeforeEach
    void setUp() {
        service = new OverloadService(
                workoutLogRepository, mealLogRepository, workoutProfileRepository, storageService,
                workoutFeedbackRepository, dietFeedbackRepository);
    }

    // -------------------------------------------------------------------------
    // Rule 1: painFlag = true → SUBSTITUTE
    // -------------------------------------------------------------------------

    @Test
    void suggestProgression_PainFlag_ReturnsSUBSTITUTE() {
        String result = service.suggestProgression(true, false, null, WorkoutRating.TOO_HARD, 100.0);
        log.info("test.rule1 result={}", result);
        assertEquals("SUBSTITUTE", result);
    }

    // -------------------------------------------------------------------------
    // Rule 2: substitutionRequested = true → SUBSTITUTE
    // -------------------------------------------------------------------------

    @Test
    void suggestProgression_SubstitutionRequested_ReturnsSUBSTITUTE() {
        String result = service.suggestProgression(false, true, null, WorkoutRating.TOO_HARD, 100.0);
        log.info("test.rule2 result={}", result);
        assertEquals("SUBSTITUTE", result);
    }

    // -------------------------------------------------------------------------
    // Rule 3: negative keyword in comment → SUBSTITUTE
    // -------------------------------------------------------------------------

    @Test
    void suggestProgression_CommentHate_ReturnsSUBSTITUTE() {
        String result = service.suggestProgression(false, false, "I hate this exercise", null, 100.0);
        log.info("test.rule3.hate result={}", result);
        assertEquals("SUBSTITUTE", result);
    }

    @Test
    void suggestProgression_CommentInjury_ReturnsSUBSTITUTE() {
        String result = service.suggestProgression(false, false, "Minor injury to knee", null, 100.0);
        log.info("test.rule3.injury result={}", result);
        assertEquals("SUBSTITUTE", result);
    }

    @Test
    void suggestProgression_CommentSkip_ReturnsSUBSTITUTE() {
        String result = service.suggestProgression(false, false, "want to skip next time", null, 100.0);
        log.info("test.rule3.skip result={}", result);
        assertEquals("SUBSTITUTE", result);
    }

    @Test
    void suggestProgression_CommentNeverAgain_ReturnsSUBSTITUTE() {
        String result = service.suggestProgression(false, false, "never again doing this", null, 100.0);
        log.info("test.rule3.neverAgain result={}", result);
        assertEquals("SUBSTITUTE", result);
    }

    @Test
    void suggestProgression_CommentNegativeKeyword_CaseInsensitive() {
        String result = service.suggestProgression(false, false, "HATE IT", null, 100.0);
        assertEquals("SUBSTITUTE", result);
    }

    // -------------------------------------------------------------------------
    // Rule 4+5: incomplete sets/reps → HOLD
    // -------------------------------------------------------------------------

    @Test
    void suggestProgression_PartialCompletion_ReturnsHOLD() {
        String result = service.suggestProgression(false, false, null, WorkoutRating.TOO_HARD, 66.7);
        log.info("test.rule4 result={}", result);
        assertEquals("HOLD", result);
    }

    @Test
    void suggestProgression_NoRatingAndIncomplete_ReturnsHOLD() {
        String result = service.suggestProgression(false, false, null, null, 50.0);
        assertEquals("HOLD", result);
    }

    // -------------------------------------------------------------------------
    // Rule 6: completed + TOO_HARD (no pain) → DECREASE
    // -------------------------------------------------------------------------

    @Test
    void suggestProgression_CompletedTooHard_ReturnsDECREASE() {
        String result = service.suggestProgression(false, false, null, WorkoutRating.TOO_HARD, 100.0);
        log.info("test.rule6 result={}", result);
        assertEquals("DECREASE", result);
    }

    // -------------------------------------------------------------------------
    // Rule 7: completed + JUST_RIGHT → INCREASE_WEIGHT
    // -------------------------------------------------------------------------

    @Test
    void suggestProgression_CompletedJustRight_ReturnsINCREASE_WEIGHT() {
        String result = service.suggestProgression(false, false, null, WorkoutRating.JUST_RIGHT, 100.0);
        log.info("test.rule7 result={}", result);
        assertEquals("INCREASE_WEIGHT", result);
    }

    // -------------------------------------------------------------------------
    // Rule 8: completed + TOO_EASY → INCREASE_WEIGHT
    // -------------------------------------------------------------------------

    @Test
    void suggestProgression_CompletedTooEasy_ReturnsINCREASE_WEIGHT() {
        String result = service.suggestProgression(false, false, null, WorkoutRating.TOO_EASY, 100.0);
        log.info("test.rule8 result={}", result);
        assertEquals("INCREASE_WEIGHT", result);
    }

    // -------------------------------------------------------------------------
    // Default: no rating, completed → HOLD
    // -------------------------------------------------------------------------

    @Test
    void suggestProgression_CompletedNoRating_ReturnsHOLD() {
        String result = service.suggestProgression(false, false, null, null, 100.0);
        assertEquals("HOLD", result);
    }

    // -------------------------------------------------------------------------
    // Priority order: pain flag takes precedence over everything
    // -------------------------------------------------------------------------

    @Test
    void suggestProgression_PainFlagOverridesSubstitutionRequested() {
        String result = service.suggestProgression(true, true, "injury", WorkoutRating.TOO_HARD, 100.0);
        assertEquals("SUBSTITUTE", result);
    }

    @Test
    void suggestProgression_SubstitutionOverridesKeyword() {
        String result = service.suggestProgression(false, true, "hate", WorkoutRating.JUST_RIGHT, 100.0);
        assertEquals("SUBSTITUTE", result);
    }

    @Test
    void suggestProgression_KeywordOverridesCompletion() {
        String result = service.suggestProgression(false, false, "skip", null, 50.0);
        assertEquals("SUBSTITUTE", result);
    }

    // -------------------------------------------------------------------------
    // computeCompletionRate edge cases
    // -------------------------------------------------------------------------

    @Test
    void computeCompletionRate_NullPrescribed_Returns100() {
        double rate = service.computeCompletionRate(3, 10, null);
        log.info("test.completionRate.nullPrescribed rate={}", rate);
        assertEquals(100.0, rate, 0.01);
    }

    @Test
    void computeCompletionRate_ZeroPrescribed_Returns100() {
        OverloadService.PrescribedValues pv = new OverloadService.PrescribedValues(0, 0, null);
        double rate = service.computeCompletionRate(3, 10, pv);
        log.info("test.completionRate.zeroPrescribed rate={}", rate);
        assertEquals(100.0, rate, 0.01);
    }

    @Test
    void computeCompletionRate_ExactCompletion_Returns100() {
        OverloadService.PrescribedValues pv = new OverloadService.PrescribedValues(3, 10, null);
        double rate = service.computeCompletionRate(3, 10, pv);
        log.info("test.completionRate.exact rate={}", rate);
        assertEquals(100.0, rate, 0.01);
    }

    @Test
    void computeCompletionRate_PartialCompletion_ReturnsCorrectRate() {
        OverloadService.PrescribedValues pv = new OverloadService.PrescribedValues(3, 10, null);
        double rate = service.computeCompletionRate(2, 8, pv);
        // (2*8) / (3*10) * 100 = 16/30 * 100 ≈ 53.33
        log.info("test.completionRate.partial rate={}", rate);
        assertEquals(53.33, rate, 0.01);
    }

    @Test
    void computeCompletionRate_OverCompletion_CappedAt100() {
        OverloadService.PrescribedValues pv = new OverloadService.PrescribedValues(3, 10, null);
        double rate = service.computeCompletionRate(4, 12, pv);
        // (4*12) / (3*10) * 100 = 48/30 * 100 = 160 → capped at 100
        log.info("test.completionRate.over rate={}", rate);
        assertEquals(100.0, rate, 0.01);
    }

    @Test
    void computeCompletionRate_ZeroActual_ReturnsZero() {
        OverloadService.PrescribedValues pv = new OverloadService.PrescribedValues(3, 10, null);
        double rate = service.computeCompletionRate(0, 0, pv);
        log.info("test.completionRate.zeroActual rate={}", rate);
        assertEquals(0.0, rate, 0.01);
    }

    // -------------------------------------------------------------------------
    // containsNegativeKeyword
    // -------------------------------------------------------------------------

    @Test
    void containsNegativeKeyword_NullComment_ReturnsFalse() {
        assertFalse(service.containsNegativeKeyword(null));
    }

    @Test
    void containsNegativeKeyword_BlankComment_ReturnsFalse() {
        assertFalse(service.containsNegativeKeyword("   "));
    }

    @Test
    void containsNegativeKeyword_PositiveComment_ReturnsFalse() {
        assertFalse(service.containsNegativeKeyword("Felt great, loved it"));
    }

    @Test
    void containsNegativeKeyword_CommentWithHate_ReturnsTrue() {
        assertTrue(service.containsNegativeKeyword("I hate deadlifts"));
    }

    @Test
    void containsNegativeKeyword_CommentWithNeverAgain_ReturnsTrue() {
        assertTrue(service.containsNegativeKeyword("Never again will I do this"));
    }
}
