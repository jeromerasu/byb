package com.workoutplanner.repository;

import com.workoutplanner.model.WorkoutLog;
import com.workoutplanner.model.WorkoutRating;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Repository tests for TASK-API-005 — feedback fields on WorkoutLog
 * (rating, feedback_comment, pain_flag, substitution_requested).
 */
@DataJpaTest
@ActiveProfiles("test")
class WorkoutLogFeedbackRepositoryTest {

    private static final Logger log = LoggerFactory.getLogger(WorkoutLogFeedbackRepositoryTest.class);

    @Autowired
    private WorkoutLogRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    // -- Helpers ---------------------------------------------------------

    private WorkoutLog buildLog(String userId, String exercise, LocalDate date) {
        WorkoutLog entry = new WorkoutLog(userId, exercise, new BigDecimal("80.00"), date);
        return repository.save(entry);
    }

    private WorkoutLog buildLogWithFeedback(String userId, String exercise, LocalDate date,
                                            WorkoutRating rating, String comment,
                                            boolean painFlag, boolean substitutionRequested) {
        WorkoutLog entry = new WorkoutLog(userId, exercise, new BigDecimal("80.00"), date);
        entry.setRating(rating);
        entry.setFeedbackComment(comment);
        entry.setPainFlag(painFlag);
        entry.setSubstitutionRequested(substitutionRequested);
        return repository.save(entry);
    }

    // -- New fields: rating, feedback_comment, pain_flag, substitution_requested ----------

    @Test
    void save_WithTooHardRating_ShouldPersistFeedbackFields() {
        WorkoutLog entry = new WorkoutLog("u1", "Deadlift", new BigDecimal("140.00"), LocalDate.now());
        entry.setRating(WorkoutRating.TOO_HARD);
        entry.setFeedbackComment("Too heavy, back was struggling");
        entry.setPainFlag(true);
        entry.setSubstitutionRequested(true);

        WorkoutLog saved = repository.save(entry);

        log.info("test.save_TooHardRating id={}", saved.getId());
        assertNotNull(saved.getId());
        assertEquals(WorkoutRating.TOO_HARD, saved.getRating());
        assertEquals("Too heavy, back was struggling", saved.getFeedbackComment());
        assertTrue(saved.isPainFlag());
        assertTrue(saved.isSubstitutionRequested());
    }

    @Test
    void save_WithJustRightRating_ShouldPersistRatingOnly() {
        WorkoutLog entry = new WorkoutLog("u2", "Squat", new BigDecimal("100.00"), LocalDate.now());
        entry.setRating(WorkoutRating.JUST_RIGHT);

        WorkoutLog saved = repository.save(entry);

        assertEquals(WorkoutRating.JUST_RIGHT, saved.getRating());
        assertNull(saved.getFeedbackComment());
        assertFalse(saved.isPainFlag());
        assertFalse(saved.isSubstitutionRequested());
    }

    @Test
    void save_WithTooEasyRating_ShouldPersistRating() {
        WorkoutLog entry = new WorkoutLog("u3", "Curl", new BigDecimal("15.00"), LocalDate.now());
        entry.setRating(WorkoutRating.TOO_EASY);
        entry.setFeedbackComment("Need more weight next time");

        WorkoutLog saved = repository.save(entry);

        assertEquals(WorkoutRating.TOO_EASY, saved.getRating());
        assertEquals("Need more weight next time", saved.getFeedbackComment());
        assertFalse(saved.isPainFlag());
        assertFalse(saved.isSubstitutionRequested());
    }

    @Test
    void save_WithoutFeedback_ShouldDefaultToNullRatingAndFalseFlags() {
        WorkoutLog entry = new WorkoutLog("u4", "OHP", new BigDecimal("60.00"), LocalDate.now());

        WorkoutLog saved = repository.save(entry);

        assertNull(saved.getRating());
        assertNull(saved.getFeedbackComment());
        assertFalse(saved.isPainFlag());
        assertFalse(saved.isSubstitutionRequested());
    }

    @Test
    void save_PainFlag_WithoutRating_ShouldPersistPainFlagOnly() {
        WorkoutLog entry = new WorkoutLog("u5", "Row", new BigDecimal("70.00"), LocalDate.now());
        entry.setPainFlag(true);

        WorkoutLog saved = repository.save(entry);

        assertNull(saved.getRating());
        assertTrue(saved.isPainFlag());
        assertFalse(saved.isSubstitutionRequested());
    }

    @Test
    void save_SubstitutionRequested_ShouldPersist() {
        WorkoutLog entry = new WorkoutLog("u6", "Leg Press", new BigDecimal("120.00"), LocalDate.now());
        entry.setSubstitutionRequested(true);

        WorkoutLog saved = repository.save(entry);

        assertTrue(saved.isSubstitutionRequested());
        assertFalse(saved.isPainFlag());
        assertNull(saved.getRating());
    }

    // -- Feedback query: findByUserIdAndRatingIsNotNullAndDateBetween ---

    @Test
    void findFeedbackByDateRange_ShouldReturnOnlyRatedEntries() {
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);

        buildLogWithFeedback("query-user", "Squat", today, WorkoutRating.TOO_HARD, "Hard", false, false);
        buildLogWithFeedback("query-user", "Bench", weekAgo, WorkoutRating.JUST_RIGHT, null, false, false);
        buildLog("query-user", "Deadlift", today); // no feedback

        List<WorkoutLog> result = repository.findByUserIdAndRatingIsNotNullAndDateBetween(
                "query-user", weekAgo, today);

        log.info("test.findFeedback count={}", result.size());
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(w -> w.getRating() != null));
    }

    @Test
    void findFeedbackByDateRange_ShouldNotReturnOtherUsersEntries() {
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);

        buildLogWithFeedback("user-A", "Squat", today, WorkoutRating.TOO_EASY, "Easy", false, false);
        buildLogWithFeedback("user-B", "Bench", today, WorkoutRating.TOO_HARD, "Hard", true, false);

        List<WorkoutLog> resultA = repository.findByUserIdAndRatingIsNotNullAndDateBetween(
                "user-A", weekAgo, today);

        assertEquals(1, resultA.size());
        assertEquals("user-A", resultA.get(0).getUserId());
    }

    @Test
    void findFeedbackByDateRange_OutsideRange_ShouldReturnEmpty() {
        LocalDate today = LocalDate.now();
        LocalDate twoWeeksAgo = today.minusDays(14);
        LocalDate threeWeeksAgo = today.minusDays(21);

        buildLogWithFeedback("range-user", "Squat", today, WorkoutRating.JUST_RIGHT, null, false, false);

        List<WorkoutLog> result = repository.findByUserIdAndRatingIsNotNullAndDateBetween(
                "range-user", threeWeeksAgo, twoWeeksAgo);

        assertTrue(result.isEmpty());
    }

    @Test
    void findFeedbackByDateRange_NoFeedback_ShouldReturnEmpty() {
        buildLog("no-feedback-user", "Squat", LocalDate.now());

        List<WorkoutLog> result = repository.findByUserIdAndRatingIsNotNullAndDateBetween(
                "no-feedback-user", LocalDate.now().minusDays(7), LocalDate.now());

        assertTrue(result.isEmpty());
    }
}
