package com.workoutplanner.repository;

import com.workoutplanner.model.ExerciseType;
import com.workoutplanner.model.WorkoutLog;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Repository integration tests for WorkoutLog enhancements (sets, reps, duration, exercise_type, notes)
 * and PR (personal record) query methods.
 */
@DataJpaTest
@ActiveProfiles("test")
class WorkoutLogRepositoryTest {

    private static final Logger log = LoggerFactory.getLogger(WorkoutLogRepositoryTest.class);

    @Autowired
    private WorkoutLogRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    // -- Helpers ---------------------------------------------------------

    private WorkoutLog buildLog(String userId, String exercise, BigDecimal weight, LocalDate date) {
        WorkoutLog log = new WorkoutLog(userId, exercise, weight, date);
        return repository.save(log);
    }

    private WorkoutLog buildStrengthLog(String userId, String exercise, BigDecimal weight,
                                        int sets, int reps, LocalDate date) {
        WorkoutLog entry = new WorkoutLog(userId, exercise, weight, date);
        entry.setSets(sets);
        entry.setReps(reps);
        entry.setExerciseType(ExerciseType.STRENGTH);
        return repository.save(entry);
    }

    private WorkoutLog buildCardioLog(String userId, String exercise, int durationMinutes, LocalDate date) {
        WorkoutLog entry = new WorkoutLog(userId, exercise, null, date);
        entry.setDurationMinutes(durationMinutes);
        entry.setExerciseType(ExerciseType.CARDIO);
        return repository.save(entry);
    }

    // -- New fields: sets, reps, duration, exerciseType, notes ----------

    @Test
    void save_StrengthLog_ShouldPersistAllNewFields() {
        WorkoutLog entry = new WorkoutLog("u1", "Bench Press", new BigDecimal("100.00"), LocalDate.now());
        entry.setSets(4);
        entry.setReps(8);
        entry.setExerciseType(ExerciseType.STRENGTH);
        entry.setNotes("Felt strong today");

        WorkoutLog saved = repository.save(entry);

        log.info("test.save_StrengthLog id={}", saved.getId());
        assertNotNull(saved.getId());
        assertEquals(4, saved.getSets());
        assertEquals(8, saved.getReps());
        assertEquals(ExerciseType.STRENGTH, saved.getExerciseType());
        assertEquals("Felt strong today", saved.getNotes());
        assertNull(saved.getDurationMinutes());
    }

    @Test
    void save_CardioLog_ShouldPersistDurationWithNullSetsReps() {
        WorkoutLog entry = new WorkoutLog("u2", "Running", null, LocalDate.now());
        entry.setDurationMinutes(45);
        entry.setExerciseType(ExerciseType.CARDIO);

        WorkoutLog saved = repository.save(entry);

        log.info("test.save_CardioLog id={}", saved.getId());
        assertEquals(45, saved.getDurationMinutes());
        assertEquals(ExerciseType.CARDIO, saved.getExerciseType());
        assertNull(saved.getSets());
        assertNull(saved.getReps());
        assertNull(saved.getWeight());
    }

    @Test
    void save_FlexibilityLog_ShouldPersistExerciseType() {
        WorkoutLog entry = new WorkoutLog("u3", "Yoga", null, LocalDate.now());
        entry.setExerciseType(ExerciseType.FLEXIBILITY);
        entry.setDurationMinutes(60);

        WorkoutLog saved = repository.save(entry);

        assertEquals(ExerciseType.FLEXIBILITY, saved.getExerciseType());
    }

    @Test
    void save_WithoutNewFields_ShouldPersistSuccessfully() {
        WorkoutLog entry = new WorkoutLog("u4", "Squat", new BigDecimal("80.00"), LocalDate.now());

        WorkoutLog saved = repository.save(entry);

        assertNotNull(saved.getId());
        assertNull(saved.getSets());
        assertNull(saved.getReps());
        assertNull(saved.getDurationMinutes());
        assertNull(saved.getExerciseType());
        assertNull(saved.getNotes());
    }

    // -- findByUserId ---------------------------------------------------

    @Test
    void findByUserId_ShouldReturnAllLogsForUser() {
        buildLog("user-A", "Squat", new BigDecimal("100"), LocalDate.now());
        buildLog("user-A", "Deadlift", new BigDecimal("120"), LocalDate.now());
        buildLog("user-B", "Squat", new BigDecimal("80"), LocalDate.now());

        List<WorkoutLog> result = repository.findByUserId("user-A");

        log.info("test.findByUserId count={}", result.size());
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(w -> "user-A".equals(w.getUserId())));
    }

    @Test
    void findByUserIdAndDate_ShouldReturnOnlyMatchingDate() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        buildLog("user-C", "Press", new BigDecimal("60"), today);
        buildLog("user-C", "Row", new BigDecimal("70"), yesterday);

        List<WorkoutLog> result = repository.findByUserIdAndDate("user-C", today);

        assertEquals(1, result.size());
        assertEquals("Press", result.get(0).getExercise());
    }

    @Test
    void findByUserIdAndExerciseType_ShouldFilterByType() {
        buildStrengthLog("user-D", "Bench Press", new BigDecimal("90"), 3, 10, LocalDate.now());
        buildCardioLog("user-D", "Cycling", 30, LocalDate.now());

        List<WorkoutLog> strength = repository.findByUserIdAndExerciseType("user-D", ExerciseType.STRENGTH);
        List<WorkoutLog> cardio = repository.findByUserIdAndExerciseType("user-D", ExerciseType.CARDIO);

        log.info("test.findByExerciseType strength={} cardio={}", strength.size(), cardio.size());
        assertEquals(1, strength.size());
        assertEquals(1, cardio.size());
    }

    // -- PR (personal record) queries -----------------------------------

    @Test
    void findMaxWeightByUserIdAndExercise_ShouldReturnHighestWeight() {
        buildLog("pr-user", "Squat", new BigDecimal("80.00"), LocalDate.now().minusDays(10));
        buildLog("pr-user", "Squat", new BigDecimal("100.00"), LocalDate.now().minusDays(5));
        buildLog("pr-user", "Squat", new BigDecimal("95.00"), LocalDate.now());

        Optional<BigDecimal> pr = repository.findMaxWeightByUserIdAndExercise("pr-user", "Squat");

        log.info("test.findMaxWeight pr={}", pr.orElse(null));
        assertTrue(pr.isPresent());
        assertEquals(0, new BigDecimal("100.00").compareTo(pr.get()));
    }

    @Test
    void findMaxWeightByUserIdAndExercise_DifferentExercises_ShouldNotMix() {
        buildLog("pr-user2", "Squat", new BigDecimal("120.00"), LocalDate.now());
        buildLog("pr-user2", "Deadlift", new BigDecimal("150.00"), LocalDate.now());

        Optional<BigDecimal> squatPr = repository.findMaxWeightByUserIdAndExercise("pr-user2", "Squat");
        Optional<BigDecimal> deadliftPr = repository.findMaxWeightByUserIdAndExercise("pr-user2", "Deadlift");

        assertTrue(squatPr.isPresent());
        assertTrue(deadliftPr.isPresent());
        assertEquals(0, new BigDecimal("120.00").compareTo(squatPr.get()));
        assertEquals(0, new BigDecimal("150.00").compareTo(deadliftPr.get()));
    }

    @Test
    void findMaxWeightByUserIdAndExercise_NoLogs_ShouldReturnEmpty() {
        Optional<BigDecimal> pr = repository.findMaxWeightByUserIdAndExercise("no-user", "Bench Press");
        assertTrue(pr.isEmpty());
    }

    @Test
    void findMaxWeightByUserIdAndExercise_DifferentUser_ShouldNotReturn() {
        buildLog("user-X", "Squat", new BigDecimal("200.00"), LocalDate.now());

        Optional<BigDecimal> pr = repository.findMaxWeightByUserIdAndExercise("user-Y", "Squat");
        assertTrue(pr.isEmpty());
    }

    @Test
    void findDistinctExercisesByUserId_ShouldReturnUniqueExerciseNames() {
        buildLog("ex-user", "Squat", new BigDecimal("100"), LocalDate.now());
        buildLog("ex-user", "Squat", new BigDecimal("110"), LocalDate.now().minusDays(1));
        buildLog("ex-user", "Deadlift", new BigDecimal("120"), LocalDate.now());
        buildLog("other-user", "Bench Press", new BigDecimal("80"), LocalDate.now());

        List<String> exercises = repository.findDistinctExercisesByUserId("ex-user");

        log.info("test.findDistinctExercises exercises={}", exercises);
        assertEquals(2, exercises.size());
        assertTrue(exercises.contains("Squat"));
        assertTrue(exercises.contains("Deadlift"));
        assertFalse(exercises.contains("Bench Press"));
    }

    @Test
    void findPersonalRecordEntriesByUserIdAndExercise_ShouldReturnEntryWithMaxWeight() {
        buildLog("pr-entry-user", "Squat", new BigDecimal("80.00"), LocalDate.now().minusDays(10));
        WorkoutLog pr = buildLog("pr-entry-user", "Squat", new BigDecimal("105.00"), LocalDate.now().minusDays(5));
        buildLog("pr-entry-user", "Squat", new BigDecimal("90.00"), LocalDate.now());

        List<WorkoutLog> prEntries = repository.findPersonalRecordEntriesByUserIdAndExercise("pr-entry-user", "Squat");

        log.info("test.findPREntries count={}", prEntries.size());
        assertFalse(prEntries.isEmpty());
        assertEquals(0, new BigDecimal("105.00").compareTo(prEntries.get(0).getWeight()));
    }

    // -- Persistence basics ---------------------------------------------

    @Test
    void save_ShouldPersistAndAssignId() {
        WorkoutLog entry = buildLog("p-user", "OHP", new BigDecimal("60"), LocalDate.now());
        assertNotNull(entry.getId());
    }

    @Test
    void delete_ShouldRemoveEntry() {
        WorkoutLog entry = buildLog("del-user", "Curl", new BigDecimal("20"), LocalDate.now());
        String id = entry.getId();
        repository.delete(entry);
        assertTrue(repository.findById(id).isEmpty());
    }
}
