package com.workoutplanner.service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PlanValidationService {

    /**
     * Validates workout plan follows 30-day schema structure
     * Required: weeks.week_1.day_1.exercises[] through week_4/day_7
     */
    public boolean isValid30DayWorkoutPlan(Map<String, Object> plan) {
        if (plan == null || plan.isEmpty()) {
            return false;
        }

        // Check for weeks structure
        Object weeksObj = plan.get("weeks");
        if (!(weeksObj instanceof Map<?, ?> weeks)) {
            return false;
        }

        // Validate 4 weeks exist
        for (int week = 1; week <= 4; week++) {
            String weekKey = "week_" + week;
            Object weekObj = weeks.get(weekKey);
            if (!(weekObj instanceof Map<?, ?> weekMap)) {
                return false;
            }

            // Validate 7 days in each week
            for (int day = 1; day <= 7; day++) {
                String dayKey = "day_" + day;
                Object dayObj = weekMap.get(dayKey);
                if (!(dayObj instanceof Map<?, ?> dayMap)) {
                    return false;
                }

                // Validate exercises array exists (can be empty for rest days)
                Object exercisesObj = dayMap.get("exercises");
                if (!(exercisesObj instanceof List<?> exercises)) {
                    return false;
                }

                // Validate exercise structure if exercises exist
                for (Object exObj : exercises) {
                    if (!(exObj instanceof Map<?, ?> exercise)) {
                        return false;
                    }

                    // Check required exercise fields
                    if (!exercise.containsKey("name") ||
                        !exercise.containsKey("sets") ||
                        !exercise.containsKey("reps") ||
                        !exercise.containsKey("weight_type")) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Validates that content is proper JSON and not freeform text
     */
    public boolean isValidJsonStructure(Object content) {
        if (content == null) {
            return false;
        }

        // Must be a Map (JSON object) or List (JSON array)
        return content instanceof Map<?, ?> || content instanceof List<?>;
    }

    /**
     * Attempts to repair a malformed workout plan by providing safe fallback structure
     */
    public Map<String, Object> repairWorkoutPlan(Map<String, Object> malformedPlan, Map<String, Object> profile) {
        Map<String, Object> repairedPlan = new HashMap<>();

        // Preserve valid metadata
        repairedPlan.put("title", getStringValue(malformedPlan, "title", "30-Day Workout Plan"));
        repairedPlan.put("type", "30_DAY_STRUCTURED");
        repairedPlan.put("version", "2.0");
        repairedPlan.put("totalWeeks", 4);
        repairedPlan.put("totalDays", 28);

        // Create minimal valid weeks structure
        Map<String, Object> weeks = new LinkedHashMap<>();
        for (int week = 1; week <= 4; week++) {
            Map<String, Object> weekData = new LinkedHashMap<>();
            weekData.put("weekNumber", week);
            weekData.put("focus", "General Fitness");
            weekData.put("intensity", "Moderate");

            for (int day = 1; day <= 7; day++) {
                Map<String, Object> dayData = new LinkedHashMap<>();
                dayData.put("dayNumber", day);
                dayData.put("weekNumber", week);
                dayData.put("isWorkoutDay", day <= 3); // Mon, Tue, Wed only

                if (day <= 3) {
                    List<Map<String, Object>> exercises = createSafeExercises();
                    dayData.put("exercises", exercises);
                    dayData.put("estimatedDuration", 30);
                } else {
                    dayData.put("exercises", new ArrayList<>());
                    dayData.put("restDay", true);
                    dayData.put("recommendedActivity", "Light stretching or walking");
                }

                weekData.put("day_" + day, dayData);
            }

            weeks.put("week_" + week, weekData);
        }

        repairedPlan.put("weeks", weeks);
        return repairedPlan;
    }

    private List<Map<String, Object>> createSafeExercises() {
        List<Map<String, Object>> exercises = new ArrayList<>();

        // Basic safe exercises
        exercises.add(createSafeExercise("Push-ups", 3, 10, "bodyweight"));
        exercises.add(createSafeExercise("Bodyweight Squats", 3, 12, "bodyweight"));
        exercises.add(createSafeExercise("Plank", 3, 30, "time_seconds"));

        return exercises;
    }

    private Map<String, Object> createSafeExercise(String name, int sets, int reps, String weightType) {
        Map<String, Object> exercise = new LinkedHashMap<>();
        exercise.put("name", name);
        exercise.put("sets", sets);
        exercise.put("reps", reps);
        exercise.put("weight_lbs", 0);
        exercise.put("weight_type", weightType);
        exercise.put("rest_seconds", 60);
        exercise.put("instructions", "Perform with proper form");
        exercise.put("muscle_groups", List.of("full_body"));
        return exercise;
    }

    private String getStringValue(Map<String, Object> map, String key, String fallback) {
        Object value = map != null ? map.get(key) : null;
        return value != null ? String.valueOf(value) : fallback;
    }

    /**
     * Validates diet plan structure (basic validation for future use)
     */
    public boolean isValidDietPlan(Map<String, Object> plan) {
        if (plan == null || plan.isEmpty()) {
            return false;
        }

        // Check for essential diet plan fields
        return plan.containsKey("title") &&
               plan.containsKey("calories") &&
               isValidJsonStructure(plan);
    }

    /**
     * Checks if AI response contains only valid JSON and no freeform text
     */
    public ValidationResult validateAIResponse(Object aiResponse) {
        if (aiResponse == null) {
            return ValidationResult.invalid("AI response is null");
        }

        if (!isValidJsonStructure(aiResponse)) {
            return ValidationResult.invalid("AI response is not valid JSON structure");
        }

        if (aiResponse instanceof Map<?, ?> planMap) {
            @SuppressWarnings("unchecked")
            Map<String, Object> plan = (Map<String, Object>) planMap;

            if (isValid30DayWorkoutPlan(plan)) {
                return ValidationResult.valid();
            } else {
                return ValidationResult.invalid("Plan does not follow 30-day schema structure");
            }
        }

        return ValidationResult.invalid("AI response is not a valid plan object");
    }

    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}