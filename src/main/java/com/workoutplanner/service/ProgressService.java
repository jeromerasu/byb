package com.workoutplanner.service;

import com.workoutplanner.dto.*;
import com.workoutplanner.model.BodyMetrics;
import com.workoutplanner.model.ExerciseCatalog;
import com.workoutplanner.model.MealLog;
import com.workoutplanner.model.WorkoutLog;
import com.workoutplanner.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProgressService {

    private static final Logger log = LoggerFactory.getLogger(ProgressService.class);

    private final WorkoutLogRepository workoutLogRepository;
    private final MealLogRepository mealLogRepository;
    private final BodyMetricsRepository bodyMetricsRepository;
    private final DietProfileRepository dietProfileRepository;
    private final WorkoutProfileRepository workoutProfileRepository;
    private final ExerciseCatalogRepository exerciseCatalogRepository;

    @Autowired
    public ProgressService(WorkoutLogRepository workoutLogRepository,
                           MealLogRepository mealLogRepository,
                           BodyMetricsRepository bodyMetricsRepository,
                           DietProfileRepository dietProfileRepository,
                           WorkoutProfileRepository workoutProfileRepository,
                           ExerciseCatalogRepository exerciseCatalogRepository) {
        this.workoutLogRepository = workoutLogRepository;
        this.mealLogRepository = mealLogRepository;
        this.bodyMetricsRepository = bodyMetricsRepository;
        this.dietProfileRepository = dietProfileRepository;
        this.workoutProfileRepository = workoutProfileRepository;
        this.exerciseCatalogRepository = exerciseCatalogRepository;
    }

    // -------------------------------------------------------------------------
    // 1. Exercise History
    // -------------------------------------------------------------------------

    public List<ExerciseHistoryResponse> getExerciseHistory(String userId, String exercise,
                                                             LocalDate from, LocalDate to) {
        log.info("progress.exercise_history userId={} exercise={} from={} to={}", userId, exercise, from, to);

        List<WorkoutLog> logs;
        if (exercise != null && !exercise.isBlank()) {
            logs = workoutLogRepository.findByUserIdAndExerciseIgnoreCaseAndDateBetweenOrderByDateAsc(
                    userId, exercise, from, to);
        } else {
            logs = workoutLogRepository.findByUserIdAndDateBetweenOrderByDateAsc(userId, from, to);
        }

        // Build all-time PR map: (exerciseName, reps) → maxWeight
        Map<String, BigDecimal> prMap = buildPrMap(userId);

        return logs.stream()
                .map(log -> toExerciseHistoryResponse(log, prMap))
                .collect(Collectors.toList());
    }

    private Map<String, BigDecimal> buildPrMap(String userId) {
        List<Object[]> rows = workoutLogRepository.findMaxWeightPerExerciseReps(userId);
        Map<String, BigDecimal> map = new HashMap<>();
        for (Object[] row : rows) {
            String ex = (String) row[0];
            Integer reps = (Integer) row[1];
            BigDecimal maxWeight = (BigDecimal) row[2];
            if (ex != null && reps != null && maxWeight != null) {
                map.put(prKey(ex, reps), maxWeight);
            }
        }
        return map;
    }

    private String prKey(String exercise, Integer reps) {
        return exercise.toLowerCase() + "|" + reps;
    }

    private ExerciseHistoryResponse toExerciseHistoryResponse(WorkoutLog entry,
                                                               Map<String, BigDecimal> prMap) {
        boolean isPr = false;
        if (entry.getWeight() != null && entry.getReps() != null) {
            String key = prKey(entry.getExercise(), entry.getReps());
            BigDecimal maxWeight = prMap.get(key);
            isPr = maxWeight != null && entry.getWeight().compareTo(maxWeight) == 0;
        }
        return new ExerciseHistoryResponse(
                entry.getExercise(),
                entry.getDate(),
                entry.getSets(),
                entry.getReps(),
                entry.getWeight(),
                "kg",
                isPr
        );
    }

    // -------------------------------------------------------------------------
    // 2. Workout Heatmap
    // -------------------------------------------------------------------------

    public List<WorkoutHeatmapResponse> getWorkoutHeatmap(String userId, LocalDate from, LocalDate to) {
        log.info("progress.workout_heatmap userId={} from={} to={}", userId, from, to);

        List<WorkoutLog> logs = workoutLogRepository.findByUserIdAndDateBetweenOrderByDateAsc(userId, from, to);

        // Group by date
        Map<LocalDate, List<WorkoutLog>> byDate = logs.stream()
                .collect(Collectors.groupingBy(WorkoutLog::getDate));

        return byDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> aggregateHeatmapEntry(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    private WorkoutHeatmapResponse aggregateHeatmapEntry(LocalDate date, List<WorkoutLog> entries) {
        int workoutCount = entries.size();
        int totalSets = entries.stream().mapToInt(w -> w.getSets() != null ? w.getSets() : 0).sum();
        int totalDuration = entries.stream().mapToInt(w -> w.getDurationMinutes() != null ? w.getDurationMinutes() : 0).sum();
        BigDecimal totalVolume = entries.stream()
                .map(this::computeVolume)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new WorkoutHeatmapResponse(date, workoutCount, totalSets, totalDuration, totalVolume);
    }

    private BigDecimal computeVolume(WorkoutLog w) {
        if (w.getSets() == null || w.getReps() == null || w.getWeight() == null) {
            return BigDecimal.ZERO;
        }
        return w.getWeight()
                .multiply(BigDecimal.valueOf(w.getSets()))
                .multiply(BigDecimal.valueOf(w.getReps()));
    }

    // -------------------------------------------------------------------------
    // 3. Bodyweight
    // -------------------------------------------------------------------------

    public List<BodyweightResponse> getBodyweight(String userId, LocalDate from, LocalDate to) {
        log.info("progress.bodyweight userId={} from={} to={}", userId, from, to);

        List<BodyMetrics> entries;
        if (from != null && to != null) {
            entries = bodyMetricsRepository.findByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(userId, from, to);
        } else {
            entries = bodyMetricsRepository.findByUserIdOrderByRecordedAtAsc(userId);
        }

        return entries.stream()
                .map(e -> new BodyweightResponse(e.getRecordedAt(), e.getWeightKg(), "kg"))
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // 4. Nutrition Adherence
    // -------------------------------------------------------------------------

    public List<NutritionAdherenceResponse> getNutritionAdherence(String userId, LocalDate from, LocalDate to) {
        log.info("progress.nutrition_adherence userId={} from={} to={}", userId, from, to);

        List<MealLog> logs = mealLogRepository.findByUserIdAndDateBetweenOrderByDateAsc(userId, from, to);

        // Fetch targets from DietProfile
        int calorieTarget = 0, proteinTarget = 0, carbsTarget = 0, fatTarget = 0;
        var profileOpt = dietProfileRepository.findByUserId(userId);
        if (profileOpt.isPresent()) {
            var profile = profileOpt.get();
            calorieTarget = profile.getDailyCalorieGoal() != null ? profile.getDailyCalorieGoal() : 0;
            proteinTarget = profile.getProteinGoalGrams() != null ? profile.getProteinGoalGrams() : 0;
            carbsTarget = profile.getCarbGoalGrams() != null ? profile.getCarbGoalGrams() : 0;
            fatTarget = profile.getFatGoalGrams() != null ? profile.getFatGoalGrams() : 0;
        }

        final int ct = calorieTarget, pt = proteinTarget, carbt = carbsTarget, ft = fatTarget;

        Map<LocalDate, List<MealLog>> byDate = logs.stream()
                .collect(Collectors.groupingBy(MealLog::getDate));

        return byDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> aggregateNutritionEntry(e.getKey(), e.getValue(), ct, pt, carbt, ft))
                .collect(Collectors.toList());
    }

    private NutritionAdherenceResponse aggregateNutritionEntry(LocalDate date, List<MealLog> entries,
                                                                int calorieTarget, int proteinTarget,
                                                                int carbsTarget, int fatTarget) {
        double calories = entries.stream()
                .mapToDouble(m -> m.getCalories() != null ? m.getCalories().doubleValue() : 0).sum();
        double protein = entries.stream()
                .mapToDouble(m -> m.getProteins() != null ? m.getProteins().doubleValue() : 0).sum();
        double carbs = entries.stream()
                .mapToDouble(m -> m.getCarbs() != null ? m.getCarbs().doubleValue() : 0).sum();
        double fat = entries.stream()
                .mapToDouble(m -> m.getFats() != null ? m.getFats().doubleValue() : 0).sum();

        double adherenceScore = computeAdherenceScore(calories, calorieTarget, protein, proteinTarget,
                carbs, carbsTarget, fat, fatTarget);

        return new NutritionAdherenceResponse(date, calories, calorieTarget, protein, proteinTarget,
                carbs, carbsTarget, fat, fatTarget, adherenceScore);
    }

    /**
     * Average of (consumed/target) for each macro, each capped at 100% before averaging.
     * Returns 0 when all targets are zero.
     */
    double computeAdherenceScore(double calories, int calorieTarget,
                                  double protein, int proteinTarget,
                                  double carbs, int carbsTarget,
                                  double fat, int fatTarget) {
        double calPct = calorieTarget > 0 ? Math.min(100.0, (calories / calorieTarget) * 100.0) : 0;
        double protPct = proteinTarget > 0 ? Math.min(100.0, (protein / proteinTarget) * 100.0) : 0;
        double carbPct = carbsTarget > 0 ? Math.min(100.0, (carbs / carbsTarget) * 100.0) : 0;
        double fatPct = fatTarget > 0 ? Math.min(100.0, (fat / fatTarget) * 100.0) : 0;

        int activeTargets = (calorieTarget > 0 ? 1 : 0) + (proteinTarget > 0 ? 1 : 0)
                + (carbsTarget > 0 ? 1 : 0) + (fatTarget > 0 ? 1 : 0);
        if (activeTargets == 0) return 0.0;

        double total = calPct + protPct + carbPct + fatPct;
        double raw = total / activeTargets;
        return BigDecimal.valueOf(raw).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    // -------------------------------------------------------------------------
    // 5. Volume Trend
    // -------------------------------------------------------------------------

    public List<VolumeTrendResponse> getVolumeTrend(String userId, LocalDate from, LocalDate to) {
        log.info("progress.volume_trend userId={} from={} to={}", userId, from, to);

        List<WorkoutLog> logs = workoutLogRepository.findByUserIdAndDateBetweenOrderByDateAsc(userId, from, to);

        Map<LocalDate, List<WorkoutLog>> byDate = logs.stream()
                .collect(Collectors.groupingBy(WorkoutLog::getDate));

        return byDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> aggregateVolumeTrend(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    private VolumeTrendResponse aggregateVolumeTrend(LocalDate date, List<WorkoutLog> entries) {
        BigDecimal totalVolume = entries.stream()
                .map(this::computeVolume)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalSets = entries.stream().mapToInt(w -> w.getSets() != null ? w.getSets() : 0).sum();
        int totalReps = entries.stream()
                .mapToInt(w -> (w.getSets() != null && w.getReps() != null) ? w.getSets() * w.getReps() : 0)
                .sum();
        return new VolumeTrendResponse(date, totalVolume, totalSets, totalReps);
    }

    // -------------------------------------------------------------------------
    // 6. Muscle Balance
    // -------------------------------------------------------------------------

    public List<MuscleBalanceResponse> getMuscleBalance(String userId, LocalDate from, LocalDate to) {
        log.info("progress.muscle_balance userId={} from={} to={}", userId, from, to);

        List<WorkoutLog> logs = workoutLogRepository.findByUserIdWithCatalogLinkAndDateBetween(userId, from, to);
        if (logs.isEmpty()) {
            return Collections.emptyList();
        }

        // Collect distinct catalog IDs and fetch them
        Set<Long> catalogIds = logs.stream()
                .map(WorkoutLog::getExerciseCatalogId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, List<String>> catalogMuscleGroups = new HashMap<>();
        for (Long id : catalogIds) {
            exerciseCatalogRepository.findById(id).ifPresent(ec -> {
                if (ec.getMuscleGroups() != null && !ec.getMuscleGroups().isEmpty()) {
                    catalogMuscleGroups.put(id, ec.getMuscleGroups());
                }
            });
        }

        // Group by muscle group
        Map<String, List<WorkoutLog>> byMuscle = new HashMap<>();
        for (WorkoutLog wl : logs) {
            Long catId = wl.getExerciseCatalogId();
            if (catId == null) continue;
            List<String> muscles = catalogMuscleGroups.get(catId);
            if (muscles == null || muscles.isEmpty()) continue;
            for (String muscle : muscles) {
                byMuscle.computeIfAbsent(muscle, k -> new ArrayList<>()).add(wl);
            }
        }

        return byMuscle.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    List<WorkoutLog> entries = e.getValue();
                    int workoutCount = entries.size();
                    int totalSets = entries.stream().mapToInt(w -> w.getSets() != null ? w.getSets() : 0).sum();
                    BigDecimal totalVolume = entries.stream()
                            .map(this::computeVolume)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new MuscleBalanceResponse(e.getKey(), workoutCount, totalSets, totalVolume);
                })
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // 7. Weekly Overview
    // -------------------------------------------------------------------------

    public WeeklyOverviewResponse getWeeklyOverview(String userId) {
        log.info("progress.weekly_overview userId={}", userId);

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);

        // Workouts completed in last 7 days (distinct days with at least one log)
        List<LocalDate> workoutDays = workoutLogRepository
                .findDistinctDatesByUserIdAndDateBetween(userId, weekStart, today);
        int workoutsCompleted = workoutDays.size();

        // Workouts planned from WorkoutProfile
        int workoutsPlanned = workoutProfileRepository.findByUserId(userId)
                .map(wp -> wp.getWorkoutFrequency() != null ? wp.getWorkoutFrequency() : 0)
                .orElse(0);

        // Consistency score
        double consistencyScore = workoutsPlanned > 0
                ? Math.min(100.0, (workoutsCompleted / (double) workoutsPlanned) * 100.0)
                : 0.0;
        consistencyScore = BigDecimal.valueOf(consistencyScore).setScale(1, RoundingMode.HALF_UP).doubleValue();

        // Active streak — consecutive days with a workout log up to and including today
        int activeStreak = computeActiveStreak(userId, today);

        // Nutrition adherence — avg adherenceScore over last 7 days
        double nutritionAdherence = computeNutritionAdherence7d(userId, weekStart, today);

        // Current weight and 7d change
        BigDecimal currentWeight = bodyMetricsRepository
                .findFirstByUserIdOrderByRecordedAtDesc(userId)
                .map(BodyMetrics::getWeightKg)
                .orElse(null);

        BigDecimal weightChange7d = null;
        if (currentWeight != null) {
            // Weight at start of 7-day window (oldest entry on or before weekStart)
            List<BodyMetrics> metricsInRange = bodyMetricsRepository
                    .findByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(userId, weekStart, today);
            if (!metricsInRange.isEmpty()) {
                BigDecimal earliest = metricsInRange.get(0).getWeightKg();
                weightChange7d = currentWeight.subtract(earliest).setScale(1, RoundingMode.HALF_UP);
            }
        }

        return new WeeklyOverviewResponse(workoutsCompleted, workoutsPlanned, consistencyScore,
                activeStreak, nutritionAdherence, currentWeight, weightChange7d);
    }

    /**
     * Counts consecutive days ending at (and including) today where at least one WorkoutLog exists.
     * Streak resets on the first day without a log.
     */
    int computeActiveStreak(String userId, LocalDate today) {
        // Fetch all distinct workout dates, sorted ascending
        List<WorkoutLog> allLogs = workoutLogRepository.findByUserIdOrderByDateAsc(userId);
        Set<LocalDate> workoutDates = allLogs.stream()
                .map(WorkoutLog::getDate)
                .collect(Collectors.toSet());

        int streak = 0;
        LocalDate cursor = today;
        while (workoutDates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private double computeNutritionAdherence7d(String userId, LocalDate from, LocalDate to) {
        List<NutritionAdherenceResponse> adherence = getNutritionAdherence(userId, from, to);
        if (adherence.isEmpty()) return 0.0;
        double avg = adherence.stream().mapToDouble(NutritionAdherenceResponse::getAdherenceScore).average().orElse(0.0);
        return BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
