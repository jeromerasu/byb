package com.workoutplanner.service;

import com.workoutplanner.dto.CurrentWeekResponseDto;
import com.workoutplanner.dto.DietFoodCatalogResponseDto;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PlanParsingService {

    /**
     * Extract current week data from workout and diet plans
     */
    public CurrentWeekResponseDto extractCurrentWeek(Map<String, Object> workoutPlan, Map<String, Object> dietPlan) {
        return extractCurrentWeek(workoutPlan, dietPlan, 1); // Default to week 1
    }

    /**
     * Extract specific week data from workout and diet plans
     */
    public CurrentWeekResponseDto extractCurrentWeek(Map<String, Object> workoutPlan, Map<String, Object> dietPlan, int weekIndex) {
        String weekKey = "week_" + weekIndex;
        CurrentWeekResponseDto response = new CurrentWeekResponseDto("current_combined_plan", weekIndex, weekKey);

        // Extract workout week
        CurrentWeekResponseDto.WorkoutWeekDto workoutWeek = extractWorkoutWeek(workoutPlan, weekKey);
        response.setWorkoutWeek(workoutWeek);

        // Extract diet week
        CurrentWeekResponseDto.DietWeekDto dietWeek = extractDietWeek(dietPlan, weekKey);
        response.setDietWeek(dietWeek);

        // Build catalogs
        List<CurrentWeekResponseDto.ExerciseCatalogDto> exerciseCatalog = buildExerciseCatalog(workoutPlan);
        List<CurrentWeekResponseDto.DietFoodCatalogDto> dietFoodCatalog = buildDietFoodCatalog(dietPlan);

        response.setExerciseCatalog(exerciseCatalog);
        response.setDietFoodCatalog(dietFoodCatalog);

        return response;
    }

    /**
     * Extract workout week data supporting both 30-day structure and legacy formats
     */
    private CurrentWeekResponseDto.WorkoutWeekDto extractWorkoutWeek(Map<String, Object> workoutPlan, String weekKey) {
        if (workoutPlan == null) {
            return new CurrentWeekResponseDto.WorkoutWeekDto(false, new LinkedHashMap<>());
        }

        // Try new 30-day structured format first
        Object weeksObj = workoutPlan.get("weeks");
        if (weeksObj instanceof Map<?, ?> weeks) {
            Object weekObj = weeks.get(weekKey);
            if (weekObj instanceof Map<?, ?> weekMap) {
                return parseStructuredWorkoutWeek(weekMap);
            }
        }

        // Fallback to legacy workoutDays format
        Object workoutDaysObj = workoutPlan.get("workoutDays");
        if (workoutDaysObj instanceof List<?> workoutDays) {
            return parseLegacyWorkoutWeek(workoutDays);
        }

        return new CurrentWeekResponseDto.WorkoutWeekDto(false, new LinkedHashMap<>());
    }

    /**
     * Parse structured 30-day workout week format
     */
    @SuppressWarnings("unchecked")
    private CurrentWeekResponseDto.WorkoutWeekDto parseStructuredWorkoutWeek(Map<?, ?> weekMap) {
        Boolean weekDone = getBooleanValue(weekMap.get("done"), false);
        Map<String, CurrentWeekResponseDto.WorkoutDayDto> days = new LinkedHashMap<>();

        String[] dayKeys = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};

        for (String dayKey : dayKeys) {
            Object dayObj = weekMap.get(dayKey);
            if (dayObj == null) {
                // Try day_1, day_2, etc. format as fallback
                for (int i = 1; i <= 7; i++) {
                    if (weekMap.get("day_" + i) != null) {
                        dayObj = weekMap.get("day_" + i);
                        break;
                    }
                }
            }

            if (dayObj instanceof Map<?, ?> dayMap) {
                Boolean dayDone = getBooleanValue(dayMap.get("done"), false);
                String focus = getStringValue(dayMap.get("focus"), "");
                List<CurrentWeekResponseDto.ExerciseDto> exercises = parseExercises(dayMap.get("exercises"));

                days.put(dayKey, new CurrentWeekResponseDto.WorkoutDayDto(dayDone, focus, exercises));
            } else {
                // Empty day
                days.put(dayKey, new CurrentWeekResponseDto.WorkoutDayDto(false, "", new ArrayList<>()));
            }
        }

        return new CurrentWeekResponseDto.WorkoutWeekDto(weekDone, days);
    }

    /**
     * Parse legacy workout format and convert to current week
     */
    @SuppressWarnings("unchecked")
    private CurrentWeekResponseDto.WorkoutWeekDto parseLegacyWorkoutWeek(List<?> workoutDays) {
        Map<String, CurrentWeekResponseDto.WorkoutDayDto> days = new LinkedHashMap<>();
        String[] dayKeys = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};

        for (int i = 0; i < Math.min(7, workoutDays.size()); i++) {
            Object dayObj = workoutDays.get(i);
            if (dayObj instanceof Map<?, ?> dayMap) {
                String focus = getStringValue(dayMap.get("focus"), "");
                List<CurrentWeekResponseDto.ExerciseDto> exercises = parseExercises(dayMap.get("exercises"));

                days.put(dayKeys[i], new CurrentWeekResponseDto.WorkoutDayDto(false, focus, exercises));
            }
        }

        // Fill remaining days
        for (int i = workoutDays.size(); i < 7; i++) {
            days.put(dayKeys[i], new CurrentWeekResponseDto.WorkoutDayDto(false, "", new ArrayList<>()));
        }

        return new CurrentWeekResponseDto.WorkoutWeekDto(false, days);
    }

    /**
     * Extract diet week data
     */
    @SuppressWarnings("unchecked")
    private CurrentWeekResponseDto.DietWeekDto extractDietWeek(Map<String, Object> dietPlan, String weekKey) {
        if (dietPlan == null) {
            return new CurrentWeekResponseDto.DietWeekDto(false, new LinkedHashMap<>());
        }

        // Try structured format
        Object weeksObj = dietPlan.get("weeks");
        if (weeksObj instanceof Map<?, ?> weeks) {
            Object weekObj = weeks.get(weekKey);
            if (weekObj instanceof Map<?, ?> weekMap) {
                return parseStructuredDietWeek(weekMap);
            }
        }

        // Fallback to legacy format
        return parseLegacyDietWeek(dietPlan);
    }

    /**
     * Parse structured diet week
     */
    @SuppressWarnings("unchecked")
    private CurrentWeekResponseDto.DietWeekDto parseStructuredDietWeek(Map<?, ?> weekMap) {
        Boolean weekDone = getBooleanValue(weekMap.get("done"), false);
        Map<String, CurrentWeekResponseDto.DietDayDto> days = new LinkedHashMap<>();

        String[] dayKeys = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};

        for (String dayKey : dayKeys) {
            Object dayObj = weekMap.get(dayKey);
            if (dayObj instanceof Map<?, ?> dayMap) {
                Boolean dayDone = getBooleanValue(dayMap.get("done"), false);
                List<CurrentWeekResponseDto.MealDto> meals = parseMeals(dayMap.get("meals"));

                days.put(dayKey, new CurrentWeekResponseDto.DietDayDto(dayDone, meals));
            } else {
                days.put(dayKey, new CurrentWeekResponseDto.DietDayDto(false, new ArrayList<>()));
            }
        }

        return new CurrentWeekResponseDto.DietWeekDto(weekDone, days);
    }

    /**
     * Parse legacy diet format
     */
    @SuppressWarnings("unchecked")
    private CurrentWeekResponseDto.DietWeekDto parseLegacyDietWeek(Map<String, Object> dietPlan) {
        Map<String, CurrentWeekResponseDto.DietDayDto> days = new LinkedHashMap<>();
        String[] dayKeys = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};

        // Look for weeklyPlan or similar structures
        Object weeklyPlanObj = dietPlan.get("weeklyPlan");
        if (weeklyPlanObj instanceof List<?> weeklyPlan) {
            for (int i = 0; i < Math.min(7, weeklyPlan.size()); i++) {
                Object dayObj = weeklyPlan.get(i);
                if (dayObj instanceof Map<?, ?> dayMap) {
                    List<CurrentWeekResponseDto.MealDto> meals = parseMeals(dayMap.get("meals"));
                    days.put(dayKeys[i], new CurrentWeekResponseDto.DietDayDto(false, meals));
                }
            }
        }

        // Fill remaining days
        for (int i = 0; i < 7; i++) {
            if (!days.containsKey(dayKeys[i])) {
                days.put(dayKeys[i], new CurrentWeekResponseDto.DietDayDto(false, new ArrayList<>()));
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
     * Parse meals from various formats
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
                meal.setMealType(getStringValue(mealMap.get("mealType"), "snack"));
                meal.setCalories(getIntegerValue(mealMap.get("calories"), 0));
                meal.setProteinGrams(getIntegerValue(mealMap.get("proteinGrams"), 0));
                meal.setCarbsGrams(getIntegerValue(mealMap.get("carbsGrams"), 0));
                meal.setFatGrams(getIntegerValue(mealMap.get("fatGrams"), 0));

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
                                        String mealType = getStringValue(mealMap.get("mealType"), "snack");
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
                                                food.setMealType(getStringValue(mealMap.get("mealType"), "snack"));
                                                food.setCalories(getIntegerValue(mealMap.get("calories"), 0));
                                                food.setProteinGrams(getIntegerValue(mealMap.get("proteinGrams"), 0));
                                                food.setCarbsGrams(getIntegerValue(mealMap.get("carbsGrams"), 0));
                                                food.setFatGrams(getIntegerValue(mealMap.get("fatGrams"), 0));
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