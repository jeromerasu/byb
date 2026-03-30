package com.workoutplanner.service;

import com.workoutplanner.dto.*;
import com.workoutplanner.model.*;
import com.workoutplanner.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * P1-004: Unit tests for ProgressService.
 */
@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    private static final Logger log = LoggerFactory.getLogger(ProgressServiceTest.class);

    @Mock private WorkoutLogRepository workoutLogRepository;
    @Mock private MealLogRepository mealLogRepository;
    @Mock private BodyMetricsRepository bodyMetricsRepository;
    @Mock private DietProfileRepository dietProfileRepository;
    @Mock private WorkoutProfileRepository workoutProfileRepository;
    @Mock private ExerciseCatalogRepository exerciseCatalogRepository;

    private ProgressService service;

    private static final String USER = "user-test-id";
    private static final LocalDate DATE_1 = LocalDate.of(2026, 3, 15);
    private static final LocalDate DATE_2 = LocalDate.of(2026, 3, 20);
    private static final LocalDate TODAY = LocalDate.of(2026, 3, 30);

    @BeforeEach
    void setUp() {
        service = new ProgressService(workoutLogRepository, mealLogRepository,
                bodyMetricsRepository, dietProfileRepository,
                workoutProfileRepository, exerciseCatalogRepository);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private WorkoutLog makeLog(String exercise, LocalDate date, Integer sets, Integer reps, double weight) {
        WorkoutLog w = new WorkoutLog();
        w.setUserId(USER);
        w.setExercise(exercise);
        w.setDate(date);
        w.setSets(sets);
        w.setReps(reps);
        w.setWeight(BigDecimal.valueOf(weight));
        return w;
    }

    private WorkoutLog makeLogWithDuration(String exercise, LocalDate date, Integer sets, Integer reps,
                                            double weight, int durationMinutes) {
        WorkoutLog w = makeLog(exercise, date, sets, reps, weight);
        w.setDurationMinutes(durationMinutes);
        return w;
    }

    private WorkoutLog makeLogWithCatalog(String exercise, LocalDate date, Integer sets, Integer reps,
                                           double weight, Long catalogId) {
        WorkoutLog w = makeLog(exercise, date, sets, reps, weight);
        w.setExerciseCatalogId(catalogId);
        return w;
    }

    private MealLog makeMeal(LocalDate date, double calories, double protein, double carbs, double fat) {
        MealLog m = new MealLog();
        m.setUserId(USER);
        m.setMealName("test-meal");
        m.setDate(date);
        m.setCalories(BigDecimal.valueOf(calories));
        m.setProteins(BigDecimal.valueOf(protein));
        m.setCarbs(BigDecimal.valueOf(carbs));
        m.setFats(BigDecimal.valueOf(fat));
        return m;
    }

    private BodyMetrics makeMetric(LocalDate date, double weight) {
        BodyMetrics bm = new BodyMetrics();
        bm.setUserId(USER);
        bm.setRecordedAt(date);
        bm.setWeightKg(BigDecimal.valueOf(weight));
        return bm;
    }

    // =========================================================================
    // 1. Exercise History
    // =========================================================================

    @Test
    void exerciseHistory_specificExercise_returnsFilteredEntries() {
        WorkoutLog bench1 = makeLog("Bench Press", DATE_1, 4, 8, 80.0);
        WorkoutLog bench2 = makeLog("Bench Press", DATE_2, 4, 8, 90.0);
        when(workoutLogRepository.findByUserIdAndExerciseIgnoreCaseAndDateBetweenOrderByDateAsc(
                eq(USER), eq("Bench Press"), any(), any())).thenReturn(List.of(bench1, bench2));

        // All-time PR data: Bench Press / 8 reps → max 90
        List<Object[]> prData = new ArrayList<>();
        prData.add(new Object[]{"Bench Press", 8, BigDecimal.valueOf(90.0)});
        when(workoutLogRepository.findMaxWeightPerExerciseReps(USER)).thenReturn(prData);

        List<ExerciseHistoryResponse> results = service.getExerciseHistory(USER, "Bench Press", DATE_1, DATE_2);

        log.info("test.exerciseHistory count={}", results.size());
        assertEquals(2, results.size());
        assertFalse(results.get(0).getIsPersonalRecord()); // 80kg is not max
        assertTrue(results.get(1).getIsPersonalRecord());  // 90kg is max
    }

    @Test
    void exerciseHistory_noExerciseParam_returnsAllExercises() {
        WorkoutLog w1 = makeLog("Squat", DATE_1, 3, 5, 100.0);
        WorkoutLog w2 = makeLog("Bench Press", DATE_2, 4, 8, 80.0);
        when(workoutLogRepository.findByUserIdAndDateBetweenOrderByDateAsc(USER, DATE_1, DATE_2))
                .thenReturn(List.of(w1, w2));
        when(workoutLogRepository.findMaxWeightPerExerciseReps(USER)).thenReturn(Collections.emptyList());

        List<ExerciseHistoryResponse> results = service.getExerciseHistory(USER, null, DATE_1, DATE_2);

        assertEquals(2, results.size());
        assertEquals("Squat", results.get(0).getExerciseName());
        assertEquals("Bench Press", results.get(1).getExerciseName());
    }

    @Test
    void exerciseHistory_emptyLogs_returnsEmptyList() {
        when(workoutLogRepository.findByUserIdAndDateBetweenOrderByDateAsc(USER, DATE_1, DATE_2))
                .thenReturn(Collections.emptyList());
        when(workoutLogRepository.findMaxWeightPerExerciseReps(USER)).thenReturn(Collections.emptyList());

        List<ExerciseHistoryResponse> results = service.getExerciseHistory(USER, null, DATE_1, DATE_2);

        log.info("test.exerciseHistory.empty count={}", results.size());
        assertEquals(0, results.size());
    }

    @Test
    void exerciseHistory_prPerRepCount_notJustGlobalMax() {
        // At 5 reps: max is 120kg; at 8 reps: max is 90kg
        // Entry with 90kg at 8 reps IS a PR; entry with 100kg at 5 reps is NOT a PR (120kg is max at 5 reps)
        WorkoutLog entry1 = makeLog("Squat", DATE_1, 3, 8, 90.0);
        WorkoutLog entry2 = makeLog("Squat", DATE_1, 3, 5, 100.0);
        when(workoutLogRepository.findByUserIdAndDateBetweenOrderByDateAsc(USER, DATE_1, DATE_2))
                .thenReturn(List.of(entry1, entry2));

        List<Object[]> prData2 = new ArrayList<>();
        prData2.add(new Object[]{"Squat", 8, BigDecimal.valueOf(90.0)});
        prData2.add(new Object[]{"Squat", 5, BigDecimal.valueOf(120.0)});
        when(workoutLogRepository.findMaxWeightPerExerciseReps(USER)).thenReturn(prData2);

        List<ExerciseHistoryResponse> results = service.getExerciseHistory(USER, null, DATE_1, DATE_2);

        assertTrue(results.get(0).getIsPersonalRecord());  // 90kg at 8 reps = max for 8 reps
        assertFalse(results.get(1).getIsPersonalRecord()); // 100kg at 5 reps != 120kg max for 5 reps
    }

    // =========================================================================
    // 2. Workout Heatmap
    // =========================================================================

    @Test
    void workoutHeatmap_aggregatesByDay() {
        WorkoutLog w1 = makeLogWithDuration("Bench Press", DATE_1, 4, 8, 80.0, 30);
        WorkoutLog w2 = makeLogWithDuration("Squat", DATE_1, 3, 5, 100.0, 25);
        when(workoutLogRepository.findByUserIdAndDateBetweenOrderByDateAsc(USER, DATE_1, DATE_2))
                .thenReturn(List.of(w1, w2));

        List<WorkoutHeatmapResponse> results = service.getWorkoutHeatmap(USER, DATE_1, DATE_2);

        assertEquals(1, results.size());
        WorkoutHeatmapResponse day = results.get(0);
        assertEquals(DATE_1, day.getDate());
        assertEquals(2, day.getWorkoutCount());
        assertEquals(7, day.getTotalSets()); // 4 + 3
        assertEquals(55, day.getTotalDuration()); // 30 + 25
        // volume: 80*4*8 + 100*3*5 = 2560 + 1500 = 4060
        assertEquals(new BigDecimal("4060.00"), day.getTotalVolume().setScale(2, java.math.RoundingMode.HALF_UP));
    }

    @Test
    void workoutHeatmap_emptyLogs_returnsEmptyList() {
        when(workoutLogRepository.findByUserIdAndDateBetweenOrderByDateAsc(USER, DATE_1, DATE_2))
                .thenReturn(Collections.emptyList());

        List<WorkoutHeatmapResponse> results = service.getWorkoutHeatmap(USER, DATE_1, DATE_2);

        assertEquals(0, results.size());
    }

    // =========================================================================
    // 3. Bodyweight
    // =========================================================================

    @Test
    void bodyweight_withDateRange_returnsFilteredEntries() {
        BodyMetrics bm1 = makeMetric(DATE_1, 84.0);
        BodyMetrics bm2 = makeMetric(DATE_2, 83.2);
        when(bodyMetricsRepository.findByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(USER, DATE_1, DATE_2))
                .thenReturn(List.of(bm1, bm2));

        List<BodyweightResponse> results = service.getBodyweight(USER, DATE_1, DATE_2);

        assertEquals(2, results.size());
        assertEquals(DATE_1, results.get(0).getDate());
        assertEquals(BigDecimal.valueOf(84.0), results.get(0).getWeight());
        assertEquals("kg", results.get(0).getUnit());
        assertEquals(DATE_2, results.get(1).getDate());
    }

    @Test
    void bodyweight_noDateRange_returnsAllEntries() {
        BodyMetrics bm = makeMetric(DATE_1, 84.0);
        when(bodyMetricsRepository.findByUserIdOrderByRecordedAtAsc(USER)).thenReturn(List.of(bm));

        List<BodyweightResponse> results = service.getBodyweight(USER, null, null);

        assertEquals(1, results.size());
        verify(bodyMetricsRepository).findByUserIdOrderByRecordedAtAsc(USER);
        verify(bodyMetricsRepository, never()).findByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(any(), any(), any());
    }

    @Test
    void bodyweight_empty_returnsEmptyList() {
        when(bodyMetricsRepository.findByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(USER, DATE_1, DATE_2))
                .thenReturn(Collections.emptyList());

        List<BodyweightResponse> results = service.getBodyweight(USER, DATE_1, DATE_2);

        assertEquals(0, results.size());
    }

    // =========================================================================
    // 4. Nutrition Adherence — adherenceScore calculation
    // =========================================================================

    @Test
    void computeAdherenceScore_allTargetsMet_returns100() {
        double score = service.computeAdherenceScore(2400, 2400, 180, 180, 250, 250, 70, 70);
        log.info("test.adherence.allMet score={}", score);
        assertEquals(100.0, score);
    }

    @Test
    void computeAdherenceScore_exceedsTarget_cappedAt100() {
        // All macros exceed target — should be capped and return 100%
        double score = service.computeAdherenceScore(3000, 2400, 220, 180, 300, 250, 90, 70);
        log.info("test.adherence.exceeded score={}", score);
        assertEquals(100.0, score);
    }

    @Test
    void computeAdherenceScore_partial_correctAverage() {
        // calories: 2100/2400 = 87.5%; protein: 155/180 = 86.1%; carbs: 200/250 = 80%; fat: 60/70 = 85.7%
        // avg ≈ (87.5 + 86.1 + 80.0 + 85.7) / 4 ≈ 84.8%
        double score = service.computeAdherenceScore(2100, 2400, 155, 180, 200, 250, 60, 70);
        log.info("test.adherence.partial score={}", score);
        assertTrue(score > 80.0 && score < 90.0);
    }

    @Test
    void computeAdherenceScore_zeroTargets_returnsZero() {
        double score = service.computeAdherenceScore(2100, 0, 155, 0, 200, 0, 60, 0);
        log.info("test.adherence.zeroTargets score={}", score);
        assertEquals(0.0, score);
    }

    @Test
    void computeAdherenceScore_someZeroTargets_ignoresZeroTargets() {
        // Only calorie target set; protein/carbs/fat targets = 0
        double score = service.computeAdherenceScore(2400, 2400, 0, 0, 0, 0, 0, 0);
        log.info("test.adherence.partialTargets score={}", score);
        assertEquals(100.0, score);
    }

    @Test
    void nutritionAdherence_aggregatesByDay() {
        MealLog m1 = makeMeal(DATE_1, 800, 60, 90, 25);
        MealLog m2 = makeMeal(DATE_1, 700, 55, 80, 20);
        when(mealLogRepository.findByUserIdAndDateBetweenOrderByDateAsc(USER, DATE_1, DATE_2))
                .thenReturn(List.of(m1, m2));

        DietProfile dp = new DietProfile();
        dp.setDailyCalorieGoal(2400);
        dp.setProteinGoalGrams(180);
        dp.setCarbGoalGrams(250);
        dp.setFatGoalGrams(70);
        when(dietProfileRepository.findByUserId(USER)).thenReturn(Optional.of(dp));

        List<NutritionAdherenceResponse> results = service.getNutritionAdherence(USER, DATE_1, DATE_2);

        assertEquals(1, results.size());
        NutritionAdherenceResponse day = results.get(0);
        assertEquals(DATE_1, day.getDate());
        assertEquals(1500.0, day.getCaloriesConsumed(), 0.01); // 800 + 700
        assertEquals(115.0, day.getProteinConsumed(), 0.01);  // 60 + 55
        assertEquals(170.0, day.getCarbsConsumed(), 0.01);   // 90 + 80
        assertEquals(45.0, day.getFatConsumed(), 0.01);      // 25 + 20
    }

    @Test
    void nutritionAdherence_empty_returnsEmptyList() {
        when(mealLogRepository.findByUserIdAndDateBetweenOrderByDateAsc(USER, DATE_1, DATE_2))
                .thenReturn(Collections.emptyList());
        when(dietProfileRepository.findByUserId(USER)).thenReturn(Optional.empty());

        List<NutritionAdherenceResponse> results = service.getNutritionAdherence(USER, DATE_1, DATE_2);

        assertEquals(0, results.size());
    }

    // =========================================================================
    // 5. Volume Trend
    // =========================================================================

    @Test
    void volumeTrend_aggregatesByDay() {
        WorkoutLog w1 = makeLog("Bench", DATE_1, 4, 8, 80.0);
        WorkoutLog w2 = makeLog("Squat", DATE_2, 3, 5, 100.0);
        when(workoutLogRepository.findByUserIdAndDateBetweenOrderByDateAsc(USER, DATE_1, DATE_2))
                .thenReturn(List.of(w1, w2));

        List<VolumeTrendResponse> results = service.getVolumeTrend(USER, DATE_1, DATE_2);

        assertEquals(2, results.size());
        VolumeTrendResponse day1 = results.get(0);
        assertEquals(DATE_1, day1.getDate());
        // volume = 80 * 4 * 8 = 2560
        assertEquals(0, new BigDecimal("2560.00").compareTo(day1.getTotalVolume().setScale(2, java.math.RoundingMode.HALF_UP)));
        assertEquals(4, day1.getTotalSets());
        assertEquals(32, day1.getTotalReps());

        VolumeTrendResponse day2 = results.get(1);
        assertEquals(DATE_2, day2.getDate());
        assertEquals(3, day2.getTotalSets());
        assertEquals(15, day2.getTotalReps()); // 3 * 5
    }

    @Test
    void volumeTrend_empty_returnsEmptyList() {
        when(workoutLogRepository.findByUserIdAndDateBetweenOrderByDateAsc(USER, DATE_1, DATE_2))
                .thenReturn(Collections.emptyList());

        List<VolumeTrendResponse> results = service.getVolumeTrend(USER, DATE_1, DATE_2);

        assertEquals(0, results.size());
    }

    // =========================================================================
    // 6. Muscle Balance
    // =========================================================================

    @Test
    void muscleBalance_groupsByMuscleGroup() {
        WorkoutLog w1 = makeLogWithCatalog("Bench Press", DATE_1, 4, 8, 80.0, 1L);
        WorkoutLog w2 = makeLogWithCatalog("Incline Press", DATE_2, 3, 10, 60.0, 2L);
        when(workoutLogRepository.findByUserIdWithCatalogLinkAndDateBetween(USER, DATE_1, DATE_2))
                .thenReturn(List.of(w1, w2));

        ExerciseCatalog ec1 = new ExerciseCatalog();
        ec1.setName("Bench Press");
        ec1.setMuscleGroups(List.of("Chest", "Triceps"));

        ExerciseCatalog ec2 = new ExerciseCatalog();
        ec2.setName("Incline Press");
        ec2.setMuscleGroups(List.of("Chest", "Shoulders"));

        when(exerciseCatalogRepository.findById(1L)).thenReturn(Optional.of(ec1));
        when(exerciseCatalogRepository.findById(2L)).thenReturn(Optional.of(ec2));

        List<MuscleBalanceResponse> results = service.getMuscleBalance(USER, DATE_1, DATE_2);

        log.info("test.muscleBalance count={}", results.size());
        assertEquals(3, results.size()); // Chest, Triceps, Shoulders

        Optional<MuscleBalanceResponse> chest = results.stream()
                .filter(r -> r.getMuscleGroup().equals("Chest")).findFirst();
        assertTrue(chest.isPresent());
        assertEquals(2, chest.get().getWorkoutCount()); // 2 exercises hit Chest
    }

    @Test
    void muscleBalance_noLinkedExercises_returnsEmpty() {
        when(workoutLogRepository.findByUserIdWithCatalogLinkAndDateBetween(USER, DATE_1, DATE_2))
                .thenReturn(Collections.emptyList());

        List<MuscleBalanceResponse> results = service.getMuscleBalance(USER, DATE_1, DATE_2);

        assertEquals(0, results.size());
    }

    // =========================================================================
    // 7. Active Streak Calculation
    // =========================================================================

    @Test
    void activeStreak_consecutiveDays_countsCorrectly() {
        // TODAY = March 30; workouts on March 28, 29, 30 → streak = 3
        LocalDate today = LocalDate.of(2026, 3, 30);
        WorkoutLog w1 = makeLog("Squat", LocalDate.of(2026, 3, 28), 3, 5, 100.0);
        WorkoutLog w2 = makeLog("Bench", LocalDate.of(2026, 3, 29), 4, 8, 80.0);
        WorkoutLog w3 = makeLog("Deadlift", LocalDate.of(2026, 3, 30), 3, 5, 120.0);
        when(workoutLogRepository.findByUserIdOrderByDateAsc(USER)).thenReturn(List.of(w1, w2, w3));

        int streak = service.computeActiveStreak(USER, today);

        log.info("test.streak.consecutive streak={}", streak);
        assertEquals(3, streak);
    }

    @Test
    void activeStreak_gapBreaksStreak() {
        // Gap on March 28 → streak = 1 (only March 30)
        LocalDate today = LocalDate.of(2026, 3, 30);
        WorkoutLog w1 = makeLog("Squat", LocalDate.of(2026, 3, 27), 3, 5, 100.0);
        WorkoutLog w2 = makeLog("Deadlift", LocalDate.of(2026, 3, 30), 3, 5, 120.0);
        when(workoutLogRepository.findByUserIdOrderByDateAsc(USER)).thenReturn(List.of(w1, w2));

        int streak = service.computeActiveStreak(USER, today);

        log.info("test.streak.gapBreaks streak={}", streak);
        assertEquals(1, streak);
    }

    @Test
    void activeStreak_noWorkoutToday_returnsZero() {
        LocalDate today = LocalDate.of(2026, 3, 30);
        WorkoutLog w1 = makeLog("Squat", LocalDate.of(2026, 3, 28), 3, 5, 100.0);
        when(workoutLogRepository.findByUserIdOrderByDateAsc(USER)).thenReturn(List.of(w1));

        int streak = service.computeActiveStreak(USER, today);

        log.info("test.streak.noToday streak={}", streak);
        assertEquals(0, streak); // No workout today → streak = 0
    }

    @Test
    void activeStreak_noData_returnsZero() {
        when(workoutLogRepository.findByUserIdOrderByDateAsc(USER)).thenReturn(Collections.emptyList());

        int streak = service.computeActiveStreak(USER, LocalDate.of(2026, 3, 30));

        assertEquals(0, streak);
    }

    // =========================================================================
    // 7. Weekly Overview
    // =========================================================================

    @Test
    void weeklyOverview_returnsCorrectSummary() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);

        // 3 workout days this week
        when(workoutLogRepository.findDistinctDatesByUserIdAndDateBetween(eq(USER), any(), any()))
                .thenReturn(List.of(today.minusDays(2), today.minusDays(1), today));

        WorkoutProfile wp = new WorkoutProfile();
        wp.setWorkoutFrequency(5);
        when(workoutProfileRepository.findByUserId(USER)).thenReturn(Optional.of(wp));

        // Streak: workouts on today and today-1 → streak = 2 at minimum
        WorkoutLog wToday = makeLog("Bench", today, 4, 8, 80.0);
        WorkoutLog wYesterday = makeLog("Squat", today.minusDays(1), 3, 5, 100.0);
        when(workoutLogRepository.findByUserIdOrderByDateAsc(USER)).thenReturn(List.of(wYesterday, wToday));

        // Nutrition
        when(mealLogRepository.findByUserIdAndDateBetweenOrderByDateAsc(eq(USER), any(), any()))
                .thenReturn(Collections.emptyList());
        when(dietProfileRepository.findByUserId(USER)).thenReturn(Optional.empty());

        // Weight
        BodyMetrics currentMetric = makeMetric(today, 83.2);
        when(bodyMetricsRepository.findFirstByUserIdOrderByRecordedAtDesc(USER))
                .thenReturn(Optional.of(currentMetric));
        BodyMetrics earlierMetric = makeMetric(weekStart, 84.0);
        when(bodyMetricsRepository.findByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(eq(USER), any(), any()))
                .thenReturn(List.of(earlierMetric, currentMetric));

        WeeklyOverviewResponse result = service.getWeeklyOverview(USER);

        log.info("test.weeklyOverview completed={} planned={} streak={}", result.getWorkoutsCompleted(), result.getWorkoutsPlanned(), result.getActiveStreak());
        assertEquals(3, result.getWorkoutsCompleted());
        assertEquals(5, result.getWorkoutsPlanned());
        assertEquals(60.0, result.getConsistencyScore()); // 3/5 = 60%
        assertTrue(result.getActiveStreak() >= 2);
        assertEquals(BigDecimal.valueOf(83.2), result.getCurrentWeight());
        assertNotNull(result.getWeightChange7d());
    }

    @Test
    void weeklyOverview_noData_returnsZeroValues() {
        when(workoutLogRepository.findDistinctDatesByUserIdAndDateBetween(eq(USER), any(), any()))
                .thenReturn(Collections.emptyList());
        when(workoutProfileRepository.findByUserId(USER)).thenReturn(Optional.empty());
        when(workoutLogRepository.findByUserIdOrderByDateAsc(USER)).thenReturn(Collections.emptyList());
        when(mealLogRepository.findByUserIdAndDateBetweenOrderByDateAsc(eq(USER), any(), any()))
                .thenReturn(Collections.emptyList());
        when(dietProfileRepository.findByUserId(USER)).thenReturn(Optional.empty());
        when(bodyMetricsRepository.findFirstByUserIdOrderByRecordedAtDesc(USER)).thenReturn(Optional.empty());

        WeeklyOverviewResponse result = service.getWeeklyOverview(USER);

        log.info("test.weeklyOverview.noData completed={}", result.getWorkoutsCompleted());
        assertEquals(0, result.getWorkoutsCompleted());
        assertEquals(0, result.getWorkoutsPlanned());
        assertEquals(0.0, result.getConsistencyScore());
        assertEquals(0, result.getActiveStreak());
        assertEquals(0.0, result.getNutritionAdherence());
        assertNull(result.getCurrentWeight());
        assertNull(result.getWeightChange7d());
    }

    // =========================================================================
    // WeightChange7d
    // =========================================================================

    @Test
    void weightChange7d_correctDelta() {
        // This is tested via weeklyOverview but let's verify the delta logic:
        // earlier = 84.0, current = 83.2 → delta = -0.8
        BodyMetrics current = makeMetric(TODAY, 83.2);
        BodyMetrics earlier = makeMetric(TODAY.minusDays(6), 84.0);

        when(workoutLogRepository.findDistinctDatesByUserIdAndDateBetween(eq(USER), any(), any()))
                .thenReturn(Collections.emptyList());
        when(workoutProfileRepository.findByUserId(USER)).thenReturn(Optional.empty());
        when(workoutLogRepository.findByUserIdOrderByDateAsc(USER)).thenReturn(Collections.emptyList());
        when(mealLogRepository.findByUserIdAndDateBetweenOrderByDateAsc(eq(USER), any(), any()))
                .thenReturn(Collections.emptyList());
        when(dietProfileRepository.findByUserId(USER)).thenReturn(Optional.empty());
        when(bodyMetricsRepository.findFirstByUserIdOrderByRecordedAtDesc(USER)).thenReturn(Optional.of(current));
        when(bodyMetricsRepository.findByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(eq(USER), any(), any()))
                .thenReturn(List.of(earlier, current));

        WeeklyOverviewResponse result = service.getWeeklyOverview(USER);

        log.info("test.weightChange7d change={}", result.getWeightChange7d());
        assertNotNull(result.getWeightChange7d());
        assertEquals(0, new BigDecimal("-0.8").compareTo(result.getWeightChange7d()));
    }
}
