package com.workoutplanner.service;

import com.workoutplanner.dto.CurrentWeekResponseDto;
import com.workoutplanner.dto.DietFoodCatalogResponseDto;
import com.workoutplanner.model.ExerciseCatalog;
import com.workoutplanner.repository.ExerciseCatalogRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class PlanParsingService {

    private final ExerciseCatalogRepository exerciseCatalogRepository;

    public PlanParsingService(ExerciseCatalogRepository exerciseCatalogRepository) {
        this.exerciseCatalogRepository = exerciseCatalogRepository;
    }

    /**
     * Extract current week data from workout and diet plans
     */
    public CurrentWeekResponseDto extractCurrentWeek(Map<String, Object> workoutPlan, Map<String, Object> dietPlan) {
        return extractCurrentWeek(workoutPlan, dietPlan, 1); // Default to week 1
    }

    /**
     * Extract current week data with custom plan start date
     */
    public CurrentWeekResponseDto extractCurrentWeek(Map<String, Object> workoutPlan, Map<String, Object> dietPlan, LocalDate planStartDate) {
        return extractCurrentWeek(workoutPlan, dietPlan, 1, planStartDate);
    }

    /**
     * Extract specific week data from workout and diet plans
     */
    public CurrentWeekResponseDto extractCurrentWeek(Map<String, Object> workoutPlan, Map<String, Object> dietPlan, int weekIndex) {
        return extractCurrentWeek(workoutPlan, dietPlan, weekIndex, LocalDate.now());
    }

    /**
     * Extract specific week data from workout and diet plans with custom start date
     */
    public CurrentWeekResponseDto extractCurrentWeek(Map<String, Object> workoutPlan, Map<String, Object> dietPlan, int weekIndex, LocalDate planStartDate) {
        String weekKey = "week_" + weekIndex;
        CurrentWeekResponseDto response = new CurrentWeekResponseDto("current_combined_plan", weekIndex, weekKey);

        // Set plan start and end dates
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate weekStartDate = planStartDate.plusWeeks(weekIndex - 1);
        LocalDate weekEndDate = weekStartDate.plusDays(6);

        response.setPlanStartDate(weekStartDate.format(formatter));
        response.setPlanEndDate(weekEndDate.format(formatter));

        // Build exercise media lookup maps from catalog.
        // Two maps: exact (normalized) for precise matches, plus a
        // "contains" fallback list for AI-generated name variations
        // like "Push-Ups" vs catalog "Push Up", "Dips" vs "Dip".
        List<ExerciseCatalog> allCatalogExercises = exerciseCatalogRepository.findByIsSystemTrue();
        Map<String, ExerciseCatalog> catalogMap = allCatalogExercises.stream()
                .collect(Collectors.toMap(
                        e -> normalizeExerciseName(e.getName()),
                        e -> e,
                        (e1, e2) -> e1));

        // Extract workout week with dates
        CurrentWeekResponseDto.WorkoutWeekDto workoutWeek = extractWorkoutWeek(workoutPlan, weekKey, weekStartDate);

        // Enrich exercises with videoUrl/thumbnailUrl from catalog
        if (workoutWeek != null && workoutWeek.getDays() != null) {
            for (CurrentWeekResponseDto.WorkoutDayDto day : workoutWeek.getDays().values()) {
                if (day.getExercises() != null) {
                    for (CurrentWeekResponseDto.ExerciseDto exercise : day.getExercises()) {
                        ExerciseCatalog entry = findCatalogEntry(exercise.getName(), catalogMap, allCatalogExercises);
                        if (entry != null && entry.getVideoUrl() != null) {
                            exercise.setVideoUrl(entry.getVideoUrl());
                            exercise.setThumbnailUrl(entry.getThumbnailUrl());
                        }
                    }
                }
            }
        }

        response.setWorkoutWeek(workoutWeek);

        // Extract diet week with dates
        CurrentWeekResponseDto.DietWeekDto dietWeek = extractDietWeek(dietPlan, weekKey, weekStartDate);
        response.setDietWeek(dietWeek);

        // Build catalogs
        List<CurrentWeekResponseDto.ExerciseCatalogDto> exerciseCatalog = buildExerciseCatalog(workoutPlan);
        List<CurrentWeekResponseDto.DietFoodCatalogDto> dietFoodCatalog = buildDietFoodCatalog(dietPlan);

        response.setExerciseCatalog(exerciseCatalog);
        response.setDietFoodCatalog(dietFoodCatalog);

        return response;
    }

    /**
     * Normalize an exercise name for matching: lowercase, strip hyphens,
     * collapse whitespace, and remove trailing 's' (simple plural strip).
     */
    private static String normalizeExerciseName(String name) {
        if (name == null) return "";
        return name.toLowerCase()
                .replace("-", " ")
                .replace("_", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .replaceAll("s$", "");
    }

    /**
     * Find the best matching catalog entry for an AI-generated exercise name.
     * 1. Try exact normalized match.
     * 2. Fall back to the first catalog entry whose normalized name is contained
     *    in the exercise name, or vice-versa (handles "Barbell Back Squat" vs "Back Squat").
     */
    private static ExerciseCatalog findCatalogEntry(
            String exerciseName,
            Map<String, ExerciseCatalog> catalogMap,
            List<ExerciseCatalog> allCatalogExercises) {
        if (exerciseName == null) return null;
        String normalized = normalizeExerciseName(exerciseName);

        // 1. Exact normalized match
        ExerciseCatalog exact = catalogMap.get(normalized);
        if (exact != null) return exact;

        // 2. Substring / contains fallback (only if video URL exists)
        ExerciseCatalog best = null;
        int bestLen = 0;
        for (ExerciseCatalog entry : allCatalogExercises) {
            if (entry.getVideoUrl() == null) continue;
            String catNorm = normalizeExerciseName(entry.getName());
            if (normalized.contains(catNorm) || catNorm.contains(normalized)) {
                // Prefer the longest matching catalog name (most specific)
                if (catNorm.length() > bestLen) {
                    best = entry;
                    bestLen = catNorm.length();
                }
            }
        }
        return best;
    }

    /**
     * Extract workout week data supporting both 30-day structure and legacy formats
     */
    private CurrentWeekResponseDto.WorkoutWeekDto extractWorkoutWeek(Map<String, Object> workoutPlan, String weekKey) {
        return extractWorkoutWeek(workoutPlan, weekKey, LocalDate.now());
    }

    /**
     * Extract workout week data with date calculation
     */
    private CurrentWeekResponseDto.WorkoutWeekDto extractWorkoutWeek(Map<String, Object> workoutPlan, String weekKey, LocalDate weekStartDate) {
        if (workoutPlan == null) {
            return new CurrentWeekResponseDto.WorkoutWeekDto(false, new LinkedHashMap<>());
        }

        // Try new 30-day structured format first
        Object weeksObj = workoutPlan.get("weeks");
        if (weeksObj instanceof Map<?, ?> weeks) {
            Object weekObj = weeks.get(weekKey);
            if (weekObj instanceof Map<?, ?> weekMap) {
                return parseStructuredWorkoutWeek(weekMap, weekStartDate);
            }
        }

        // Fallback to legacy workoutDays format
        Object workoutDaysObj = workoutPlan.get("workoutDays");
        if (workoutDaysObj instanceof List<?> workoutDays) {
            return parseLegacyWorkoutWeek(workoutDays, weekStartDate);
        }

        return new CurrentWeekResponseDto.WorkoutWeekDto(false, new LinkedHashMap<>());
    }

    /**
     * Parse structured 30-day workout week format
     */
    @SuppressWarnings("unchecked")
    private CurrentWeekResponseDto.WorkoutWeekDto parseStructuredWorkoutWeek(Map<?, ?> weekMap) {
        return parseStructuredWorkoutWeek(weekMap, LocalDate.now());
    }

    /**
     * Parse structured 30-day workout week format with date calculation
     */
    @SuppressWarnings("unchecked")
    private CurrentWeekResponseDto.WorkoutWeekDto parseStructuredWorkoutWeek(Map<?, ?> weekMap, LocalDate weekStartDate) {
        Boolean weekDone = getBooleanValue(weekMap.get("done"), false);
        Map<String, CurrentWeekResponseDto.WorkoutDayDto> days = new LinkedHashMap<>();

        String[] dayKeys = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = 0; i < dayKeys.length; i++) {
            String dayKey = dayKeys[i];
            LocalDate dayDate = weekStartDate.plusDays(i);
            String dateString = dayDate.format(formatter);

            Object dayObj = weekMap.get(dayKey);
            if (dayObj == null) {
                // Try day_1, day_2, etc. format as fallback
                for (int j = 1; j <= 7; j++) {
                    if (weekMap.get("day_" + j) != null) {
                        dayObj = weekMap.get("day_" + j);
                        break;
                    }
                }
            }

            if (dayObj instanceof Map<?, ?> dayMap) {
                Boolean dayDone = getBooleanValue(dayMap.get("done"), false);
                String focus = getStringValue(dayMap.get("focus"), "");
                List<CurrentWeekResponseDto.ExerciseDto> exercises = parseExercises(dayMap.get("exercises"));

                CurrentWeekResponseDto.WorkoutDayDto workoutDay = new CurrentWeekResponseDto.WorkoutDayDto(dayDone, focus, exercises);
                workoutDay.setDate(dateString);
                days.put(dayKey, workoutDay);
            } else {
                // Empty day
                CurrentWeekResponseDto.WorkoutDayDto workoutDay = new CurrentWeekResponseDto.WorkoutDayDto(false, "", new ArrayList<>());
                workoutDay.setDate(dateString);
                days.put(dayKey, workoutDay);
            }
        }

        return new CurrentWeekResponseDto.WorkoutWeekDto(weekDone, days);
    }

    /**
     * Parse legacy workout format and convert to current week
     */
    @SuppressWarnings("unchecked")
    private CurrentWeekResponseDto.WorkoutWeekDto parseLegacyWorkoutWeek(List<?> workoutDays) {
        return parseLegacyWorkoutWeek(workoutDays, LocalDate.now());
    }

    /**
     * Parse legacy workout format and convert to current week with date calculation
     */
    @SuppressWarnings("unchecked")
    private CurrentWeekResponseDto.WorkoutWeekDto parseLegacyWorkoutWeek(List<?> workoutDays, LocalDate weekStartDate) {
        Map<String, CurrentWeekResponseDto.WorkoutDayDto> days = new LinkedHashMap<>();
        String[] dayKeys = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = 0; i < Math.min(7, workoutDays.size()); i++) {
            LocalDate dayDate = weekStartDate.plusDays(i);
            String dateString = dayDate.format(formatter);

            Object dayObj = workoutDays.get(i);
            if (dayObj instanceof Map<?, ?> dayMap) {
                String focus = getStringValue(dayMap.get("focus"), "");
                List<CurrentWeekResponseDto.ExerciseDto> exercises = parseExercises(dayMap.get("exercises"));

                CurrentWeekResponseDto.WorkoutDayDto workoutDay = new CurrentWeekResponseDto.WorkoutDayDto(false, focus, exercises);
                workoutDay.setDate(dateString);
                days.put(dayKeys[i], workoutDay);
            }
        }

        // Fill remaining days
        for (int i = workoutDays.size(); i < 7; i++) {
            LocalDate dayDate = weekStartDate.plusDays(i);
            String dateString = dayDate.format(formatter);

            CurrentWeekResponseDto.WorkoutDayDto workoutDay = new CurrentWeekResponseDto.WorkoutDayDto(false, "", new ArrayList<>());
            workoutDay.setDate(dateString);
            days.put(dayKeys[i], workoutDay);
        }

        return new CurrentWeekResponseDto.WorkoutWeekDto(false, days);
    }

    /**
     * Extract diet week data
     */
    @SuppressWarnings("unchecked")
    private CurrentWeekResponseDto.DietWeekDto extractDietWeek(Map<String, Object> dietPlan, String weekKey) {
        return extractDietWeek(dietPlan, weekKey, LocalDate.now());
    }

    /**
     * Extract diet week data with date calculation
     */
    @SuppressWarnings("unchecked")
    private CurrentWeekResponseDto.DietWeekDto extractDietWeek(Map<String, Object> dietPlan, String weekKey, LocalDate weekStartDate) {
        if (dietPlan == null) {
            return new CurrentWeekResponseDto.DietWeekDto(false, new LinkedHashMap<>());
        }

        // Try structured format
        Object weeksObj = dietPlan.get("weeks");
        if (weeksObj instanceof Map<?, ?> weeks) {
            Object weekObj = weeks.get(weekKey);
            if (weekObj instanceof Map<?, ?> weekMap) {
                return parseStructuredDietWeek(weekMap, weekStartDate);
            }
        }

        // Fallback to legacy format
        return parseLegacyDietWeek(dietPlan, weekStartDate);
    }

    /**
     * Parse structured diet week
     */
    @SuppressWarnings("unchecked")
    private CurrentWeekResponseDto.DietWeekDto parseStructuredDietWeek(Map<?, ?> weekMap) {
        return parseStructuredDietWeek(weekMap, LocalDate.now());
    }

    /**
     * Parse structured diet week with date calculation
     */
    @SuppressWarnings("unchecked")
    private CurrentWeekResponseDto.DietWeekDto parseStructuredDietWeek(Map<?, ?> weekMap, LocalDate weekStartDate) {
        Boolean weekDone = getBooleanValue(weekMap.get("done"), false);
        Map<String, CurrentWeekResponseDto.DietDayDto> days = new LinkedHashMap<>();

        String[] dayKeys = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = 0; i < dayKeys.length; i++) {
            String dayKey = dayKeys[i];
            LocalDate dayDate = weekStartDate.plusDays(i);
            String dateString = dayDate.format(formatter);

            Object dayObj = weekMap.get(dayKey);
            if (dayObj instanceof Map<?, ?> dayMap) {
                Boolean dayDone = getBooleanValue(dayMap.get("done"), false);
                List<CurrentWeekResponseDto.MealDto> meals = parseMeals(dayMap.get("meals"));

                CurrentWeekResponseDto.DietDayDto dietDay = new CurrentWeekResponseDto.DietDayDto(dayDone, meals);
                dietDay.setDate(dateString);
                days.put(dayKey, dietDay);
            } else {
                CurrentWeekResponseDto.DietDayDto dietDay = new CurrentWeekResponseDto.DietDayDto(false, new ArrayList<>());
                dietDay.setDate(dateString);
                days.put(dayKey, dietDay);
            }
        }

        return new CurrentWeekResponseDto.DietWeekDto(weekDone, days);
    }

    /**
     * Parse legacy diet format
     */
    @SuppressWarnings("unchecked")
    private CurrentWeekResponseDto.DietWeekDto parseLegacyDietWeek(Map<String, Object> dietPlan) {
        return parseLegacyDietWeek(dietPlan, LocalDate.now());
    }

    /**
     * Parse legacy diet format with date calculation
     */
    @SuppressWarnings("unchecked")
    private CurrentWeekResponseDto.DietWeekDto parseLegacyDietWeek(Map<String, Object> dietPlan, LocalDate weekStartDate) {
        Map<String, CurrentWeekResponseDto.DietDayDto> days = new LinkedHashMap<>();
        String[] dayKeys = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Look for weeklyPlan or similar structures
        Object weeklyPlanObj = dietPlan.get("weeklyPlan");
        if (weeklyPlanObj instanceof List<?> weeklyPlan) {
            for (int i = 0; i < Math.min(7, weeklyPlan.size()); i++) {
                LocalDate dayDate = weekStartDate.plusDays(i);
                String dateString = dayDate.format(formatter);

                Object dayObj = weeklyPlan.get(i);
                if (dayObj instanceof Map<?, ?> dayMap) {
                    List<CurrentWeekResponseDto.MealDto> meals = parseMeals(dayMap.get("meals"));
                    CurrentWeekResponseDto.DietDayDto dietDay = new CurrentWeekResponseDto.DietDayDto(false, meals);
                    dietDay.setDate(dateString);
                    days.put(dayKeys[i], dietDay);
                }
            }
        }

        // Fill remaining days
        for (int i = 0; i < 7; i++) {
            if (!days.containsKey(dayKeys[i])) {
                LocalDate dayDate = weekStartDate.plusDays(i);
                String dateString = dayDate.format(formatter);

                CurrentWeekResponseDto.DietDayDto dietDay = new CurrentWeekResponseDto.DietDayDto(false, new ArrayList<>());
                dietDay.setDate(dateString);
                days.put(dayKeys[i], dietDay);
            }
        }

        return new CurrentWeekResponseDto.DietWeekDto(false, days);
    }

    /**
     * Parse exercises from various formats
     */
    @SuppressWarnings("unchecked")
    private List<CurrentWeekResponseDto.ExerciseDto> parseExercises(Object exercisesObj) {
        if (!(exercisesObj instanceof List<?> exercises)) {
            return new ArrayList<>();
        }

        List<CurrentWeekResponseDto.ExerciseDto> result = new ArrayList<>();
        for (Object exObj : exercises) {
            if (exObj instanceof Map<?, ?> exerciseMap) {
                CurrentWeekResponseDto.ExerciseDto exercise = new CurrentWeekResponseDto.ExerciseDto();
                exercise.setName(getStringValue(exerciseMap.get("name"), "Unknown Exercise"));
                exercise.setSets(getIntegerValue(exerciseMap.get("sets"), 1));
                exercise.setReps(getStringValue(exerciseMap.get("reps"), "1"));
                exercise.setWeightLbs(getIntegerValue(exerciseMap.get("weight_lbs"), 0));
                exercise.setWeightType(getStringValue(exerciseMap.get("weight_type"), "bodyweight"));
                exercise.setRestSeconds(getIntegerValue(exerciseMap.get("rest_seconds"), 60));

                result.add(exercise);
            }
        }
        return result;
    }

    /**
     * Parse meals from various formats.
     * Tries all known field-name variants that OpenAI may return for macros and meal type.
     */
    @SuppressWarnings("unchecked")
    private List<CurrentWeekResponseDto.MealDto> parseMeals(Object mealsObj) {
        if (!(mealsObj instanceof List<?> meals)) {
            return new ArrayList<>();
        }

        List<CurrentWeekResponseDto.MealDto> result = new ArrayList<>();
        for (Object mealObj : meals) {
            if (mealObj instanceof Map<?, ?> mealMap) {
                CurrentWeekResponseDto.MealDto meal = new CurrentWeekResponseDto.MealDto();
                meal.setName(getStringValue(mealMap.get("name"), "Unknown Food"));
                meal.setMealType(getStringValue(
                        firstNonNull(mealMap.get("meal_type"), mealMap.get("mealType"), mealMap.get("type")),
                        "snack"));
                meal.setCalories(getIntegerValue(mealMap.get("calories"), 0));
                meal.setProteinGrams(getIntegerValue(
                        firstNonNull(mealMap.get("proteins"), mealMap.get("protein"),
                                mealMap.get("protein_grams"), mealMap.get("proteinGrams")),
                        0));
                meal.setCarbsGrams(getIntegerValue(
                        firstNonNull(mealMap.get("carbs"), mealMap.get("carbohydrates"),
                                mealMap.get("carbs_grams"), mealMap.get("carbsGrams")),
                        0));
                meal.setFatGrams(getIntegerValue(
                        firstNonNull(mealMap.get("fats"), mealMap.get("fat"),
                                mealMap.get("fat_grams"), mealMap.get("fatGrams")),
                        0));

                result.add(meal);
            }
        }
        return result;
    }

    /**
     * Build exercise catalog from workout plan
     */
    @SuppressWarnings("unchecked")
    private List<CurrentWeekResponseDto.ExerciseCatalogDto> buildExerciseCatalog(Map<String, Object> workoutPlan) {
        Set<String> exerciseNames = new HashSet<>();

        if (workoutPlan == null) {
            return new ArrayList<>();
        }

        // Extract from 30-day structure
        Object weeksObj = workoutPlan.get("weeks");
        if (weeksObj instanceof Map<?, ?> weeks) {
            for (Object weekObj : weeks.values()) {
                if (weekObj instanceof Map<?, ?> weekMap) {
                    for (Object dayObj : weekMap.values()) {
                        if (dayObj instanceof Map<?, ?> dayMap) {
                            Object exercisesObj = dayMap.get("exercises");
                            if (exercisesObj instanceof List<?> exercises) {
                                for (Object exObj : exercises) {
                                    if (exObj instanceof Map<?, ?> exerciseMap) {
                                        String name = getStringValue(exerciseMap.get("name"), null);
                                        if (name != null) {
                                            exerciseNames.add(name);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Extract from legacy structure
        Object workoutDaysObj = workoutPlan.get("workoutDays");
        if (workoutDaysObj instanceof List<?> workoutDays) {
            for (Object dayObj : workoutDays) {
                if (dayObj instanceof Map<?, ?> dayMap) {
                    Object exercisesObj = dayMap.get("exercises");
                    if (exercisesObj instanceof List<?> exercises) {
                        for (Object exObj : exercises) {
                            if (exObj instanceof Map<?, ?> exerciseMap) {
                                String name = getStringValue(exerciseMap.get("name"), null);
                                if (name != null) {
                                    exerciseNames.add(name);
                                }
                            }
                        }
                    }
                }
            }
        }

        return exerciseNames.stream()
                .sorted()
                .map(name -> new CurrentWeekResponseDto.ExerciseCatalogDto(generateId(name), name))
                .collect(Collectors.toList());
    }

    /**
     * Build diet food catalog from diet plan
     */
    @SuppressWarnings("unchecked")
    private List<CurrentWeekResponseDto.DietFoodCatalogDto> buildDietFoodCatalog(Map<String, Object> dietPlan) {
        Set<String> foodNames = new HashSet<>();
        Map<String, String> foodMealTypes = new HashMap<>();

        if (dietPlan == null) {
            return new ArrayList<>();
        }

        // Extract from structured format
        Object weeksObj = dietPlan.get("weeks");
        if (weeksObj instanceof Map<?, ?> weeks) {
            for (Object weekObj : weeks.values()) {
                if (weekObj instanceof Map<?, ?> weekMap) {
                    for (Object dayObj : weekMap.values()) {
                        if (dayObj instanceof Map<?, ?> dayMap) {
                            Object mealsObj = dayMap.get("meals");
                            if (mealsObj instanceof List<?> meals) {
                                for (Object mealObj : meals) {
                                    if (mealObj instanceof Map<?, ?> mealMap) {
                                        String name = getStringValue(mealMap.get("name"), null);
                                        String mealType = getStringValue(
                                                firstNonNull(mealMap.get("meal_type"), mealMap.get("mealType"), mealMap.get("type")),
                                                "snack");
                                        if (name != null) {
                                            foodNames.add(name);
                                            foodMealTypes.put(name, mealType);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return foodNames.stream()
                .sorted()
                .map(name -> new CurrentWeekResponseDto.DietFoodCatalogDto(
                    generateId(name),
                    name,
                    foodMealTypes.getOrDefault(name, "snack")
                ))
                .collect(Collectors.toList());
    }

    /**
     * Extract full diet food catalog for TASK-BE-010
     */
    @SuppressWarnings("unchecked")
    public DietFoodCatalogResponseDto extractDietFoodCatalog(Map<String, Object> dietPlan) {
        if (dietPlan == null) {
            return new DietFoodCatalogResponseDto("current_diet_plan", 0, new ArrayList<>());
        }

        Map<String, DietFoodCatalogResponseDto.FoodDto> foodMap = new LinkedHashMap<>();

        // Extract from structured format
        Object weeksObj = dietPlan.get("weeks");
        if (weeksObj instanceof Map<?, ?> weeks) {
            for (Map.Entry<?, ?> weekEntry : weeks.entrySet()) {
                String weekKey = String.valueOf(weekEntry.getKey());
                Object weekObj = weekEntry.getValue();

                if (weekObj instanceof Map<?, ?> weekMap) {
                    for (Map.Entry<?, ?> dayEntry : weekMap.entrySet()) {
                        String dayKey = String.valueOf(dayEntry.getKey());
                        Object dayObj = dayEntry.getValue();

                        if (dayObj instanceof Map<?, ?> dayMap) {
                            Object mealsObj = dayMap.get("meals");
                            if (mealsObj instanceof List<?> meals) {
                                for (Object mealObj : meals) {
                                    if (mealObj instanceof Map<?, ?> mealMap) {
                                        String name = getStringValue(mealMap.get("name"), null);
                                        if (name != null) {
                                            String foodId = generateId(name);

                                            if (!foodMap.containsKey(foodId)) {
                                                DietFoodCatalogResponseDto.FoodDto food = new DietFoodCatalogResponseDto.FoodDto();
                                                food.setFoodId(foodId);
                                                food.setName(name);
                                                food.setMealType(getStringValue(
                                                        firstNonNull(mealMap.get("meal_type"), mealMap.get("mealType"), mealMap.get("type")),
                                                        "snack"));
                                                food.setCalories(getIntegerValue(mealMap.get("calories"), 0));
                                                food.setProteinGrams(getIntegerValue(
                                                        firstNonNull(mealMap.get("proteins"), mealMap.get("protein"),
                                                                mealMap.get("protein_grams"), mealMap.get("proteinGrams")),
                                                        0));
                                                food.setCarbsGrams(getIntegerValue(
                                                        firstNonNull(mealMap.get("carbs"), mealMap.get("carbohydrates"),
                                                                mealMap.get("carbs_grams"), mealMap.get("carbsGrams")),
                                                        0));
                                                food.setFatGrams(getIntegerValue(
                                                        firstNonNull(mealMap.get("fats"), mealMap.get("fat"),
                                                                mealMap.get("fat_grams"), mealMap.get("fatGrams")),
                                                        0));
                                                food.setLastSeenWeek(weekKey);
                                                food.setLastSeenDay(dayKey);

                                                foodMap.put(foodId, food);
                                            } else {
                                                // Update last seen
                                                foodMap.get(foodId).setLastSeenWeek(weekKey);
                                                foodMap.get(foodId).setLastSeenDay(dayKey);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        List<DietFoodCatalogResponseDto.FoodDto> foods = new ArrayList<>(foodMap.values());
        return new DietFoodCatalogResponseDto("current_diet_plan", foods.size(), foods);
    }

    // Utility methods
    private Object firstNonNull(Object... values) {
        for (Object v : values) {
            if (v != null) return v;
        }
        return null;
    }

    private String getStringValue(Object value, String defaultValue) {
        if (value == null) return defaultValue;
        return String.valueOf(value).trim();
    }

    private Integer getIntegerValue(Object value, Integer defaultValue) {
        if (value == null) return defaultValue;
        try {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Boolean getBooleanValue(Object value, Boolean defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Boolean) return (Boolean) value;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private String generateId(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "unknown";
        }
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "-")
                .trim();
    }
}