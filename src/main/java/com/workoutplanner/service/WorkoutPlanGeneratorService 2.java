package com.workoutplanner.service;

import com.workoutplanner.model.WorkoutProfile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class WorkoutPlanGeneratorService {

    /**
     * Generates a standardized 30-day workout plan following the required schema:
     * weeks.week_1.day_1.exercises[] through week_4/day_7
     */
    public Map<String, Object> generateStructured30DayPlan(WorkoutProfile profile) {
        Map<String, Object> plan = new HashMap<>();

        // Plan metadata
        plan.put("title", "30-Day Personalized Workout Plan");
        plan.put("type", "30_DAY_STRUCTURED");
        plan.put("version", "2.0");
        plan.put("generatedAt", LocalDateTime.now());
        plan.put("totalWeeks", 4);
        plan.put("totalDays", 28);

        // Profile context
        plan.put("fitnessLevel", profile.getFitnessLevel() != null ? profile.getFitnessLevel().name() : "BEGINNER");
        plan.put("targetGoals", profile.getTargetGoals());
        plan.put("availableEquipment", profile.getAvailableEquipment());
        plan.put("sessionDuration", profile.getSessionDuration() != null ? profile.getSessionDuration() : 45);

        // Generate structured weeks
        Map<String, Object> weeks = new LinkedHashMap<>();
        for (int week = 1; week <= 4; week++) {
            weeks.put("week_" + week, generateWeek(week, profile));
        }
        plan.put("weeks", weeks);

        // Plan summary
        plan.put("summary", generatePlanSummary(profile));

        return plan;
    }

    private Map<String, Object> generateWeek(int weekNumber, WorkoutProfile profile) {
        Map<String, Object> week = new LinkedHashMap<>();

        week.put("weekNumber", weekNumber);
        week.put("focus", getWeekFocus(weekNumber));
        week.put("intensity", getWeekIntensity(weekNumber));

        // Generate 7 days for the week
        for (int day = 1; day <= 7; day++) {
            String dayKey = "day_" + day;
            week.put(dayKey, generateDay(weekNumber, day, profile));
        }

        return week;
    }

    private Map<String, Object> generateDay(int weekNumber, int dayNumber, WorkoutProfile profile) {
        Map<String, Object> day = new LinkedHashMap<>();

        day.put("dayNumber", dayNumber);
        day.put("weekNumber", weekNumber);
        day.put("isWorkoutDay", isWorkoutDay(dayNumber, profile.getWorkoutFrequency()));
        day.put("focus", getDayFocus(dayNumber));

        if (isWorkoutDay(dayNumber, profile.getWorkoutFrequency())) {
            day.put("exercises", generateDayExercises(weekNumber, dayNumber, profile));
            day.put("estimatedDuration", profile.getSessionDuration() != null ? profile.getSessionDuration() : 45);
            day.put("estimatedCalories", estimateCaloriesForDay(profile));
        } else {
            day.put("exercises", new ArrayList<>());
            day.put("restDay", true);
            day.put("recommendedActivity", "Light stretching or walking");
        }

        return day;
    }

    private List<Map<String, Object>> generateDayExercises(int weekNumber, int dayNumber, WorkoutProfile profile) {
        List<Map<String, Object>> exercises = new ArrayList<>();
        String fitnessLevel = profile.getFitnessLevel() != null ? profile.getFitnessLevel().name() : "BEGINNER";
        String dayFocus = getDayFocus(dayNumber);

        // Progressive difficulty based on week
        int progressionMultiplier = weekNumber; // Week 1 = 1, Week 4 = 4

        switch (dayFocus.toLowerCase()) {
            case "upper body":
                exercises.addAll(generateUpperBodyExercises(fitnessLevel, progressionMultiplier));
                break;
            case "lower body":
                exercises.addAll(generateLowerBodyExercises(fitnessLevel, progressionMultiplier));
                break;
            case "cardio":
                exercises.addAll(generateCardioExercises(fitnessLevel, progressionMultiplier));
                break;
            case "full body":
                exercises.addAll(generateFullBodyExercises(fitnessLevel, progressionMultiplier));
                break;
            default:
                exercises.addAll(generateCoreExercises(fitnessLevel, progressionMultiplier));
        }

        return exercises;
    }

    private List<Map<String, Object>> generateUpperBodyExercises(String fitnessLevel, int progressionMultiplier) {
        List<Map<String, Object>> exercises = new ArrayList<>();

        switch (fitnessLevel.toUpperCase()) {
            case "BEGINNER":
                exercises.add(createStructuredExercise("Push-ups",
                    Math.min(3 + progressionMultiplier, 4),
                    Math.min(8 + (progressionMultiplier * 2), 15),
                    "bodyweight", 60));
                exercises.add(createStructuredExercise("Wall Push-ups", 3, 10 + progressionMultiplier, "bodyweight", 45));
                exercises.add(createStructuredExercise("Arm Circles", 3, 15, "bodyweight", 30));
                break;
            case "INTERMEDIATE":
                exercises.add(createStructuredExercise("Standard Push-ups",
                    3 + progressionMultiplier,
                    12 + (progressionMultiplier * 3),
                    "bodyweight", 60));
                exercises.add(createStructuredExercise("Pike Push-ups", 3, 8 + progressionMultiplier, "bodyweight", 90));
                exercises.add(createStructuredExercise("Tricep Dips", 3, 10 + (progressionMultiplier * 2), "bodyweight", 60));
                break;
            default: // ADVANCED
                exercises.add(createStructuredExercise("Diamond Push-ups",
                    4 + progressionMultiplier,
                    10 + (progressionMultiplier * 3),
                    "bodyweight", 90));
                exercises.add(createStructuredExercise("Handstand Push-ups", 3, 5 + progressionMultiplier, "bodyweight", 120));
                exercises.add(createStructuredExercise("Archer Push-ups", 3, 6 + progressionMultiplier, "bodyweight", 90));
        }

        return exercises;
    }

    private List<Map<String, Object>> generateLowerBodyExercises(String fitnessLevel, int progressionMultiplier) {
        List<Map<String, Object>> exercises = new ArrayList<>();

        switch (fitnessLevel.toUpperCase()) {
            case "BEGINNER":
                exercises.add(createStructuredExercise("Bodyweight Squats",
                    3 + progressionMultiplier,
                    12 + (progressionMultiplier * 3),
                    "bodyweight", 60));
                exercises.add(createStructuredExercise("Lunges", 3, 10 + (progressionMultiplier * 2), "bodyweight", 60));
                exercises.add(createStructuredExercise("Glute Bridges", 3, 15 + progressionMultiplier, "bodyweight", 45));
                break;
            case "INTERMEDIATE":
                exercises.add(createStructuredExercise("Jump Squats",
                    3 + progressionMultiplier,
                    15 + (progressionMultiplier * 2),
                    "bodyweight", 75));
                exercises.add(createStructuredExercise("Reverse Lunges", 3, 12 + (progressionMultiplier * 2), "bodyweight", 60));
                exercises.add(createStructuredExercise("Single-leg Calf Raises", 3, 15 + progressionMultiplier, "bodyweight", 45));
                break;
            default: // ADVANCED
                exercises.add(createStructuredExercise("Pistol Squats",
                    3 + progressionMultiplier,
                    5 + progressionMultiplier,
                    "bodyweight", 120));
                exercises.add(createStructuredExercise("Bulgarian Split Squats", 4, 12 + (progressionMultiplier * 2), "bodyweight", 90));
                exercises.add(createStructuredExercise("Single-leg Glute Bridges", 3, 12 + progressionMultiplier, "bodyweight", 60));
        }

        return exercises;
    }

    private List<Map<String, Object>> generateCardioExercises(String fitnessLevel, int progressionMultiplier) {
        List<Map<String, Object>> exercises = new ArrayList<>();

        switch (fitnessLevel.toUpperCase()) {
            case "BEGINNER":
                exercises.add(createStructuredExercise("Marching in Place",
                    3,
                    30 + (progressionMultiplier * 15),
                    "time_seconds", 45));
                exercises.add(createStructuredExercise("Step-ups", 3, 15 + (progressionMultiplier * 5), "bodyweight", 60));
                break;
            case "INTERMEDIATE":
                exercises.add(createStructuredExercise("High Knees",
                    3 + progressionMultiplier,
                    30 + (progressionMultiplier * 10),
                    "time_seconds", 60));
                exercises.add(createStructuredExercise("Burpees", 3, 8 + (progressionMultiplier * 2), "bodyweight", 90));
                break;
            default: // ADVANCED
                exercises.add(createStructuredExercise("Mountain Climbers",
                    4 + progressionMultiplier,
                    45 + (progressionMultiplier * 15),
                    "time_seconds", 90));
                exercises.add(createStructuredExercise("Burpee Box Jumps", 3, 10 + (progressionMultiplier * 3), "bodyweight", 120));
        }

        return exercises;
    }

    private List<Map<String, Object>> generateFullBodyExercises(String fitnessLevel, int progressionMultiplier) {
        List<Map<String, Object>> exercises = new ArrayList<>();

        exercises.add(createStructuredExercise("Burpees", 3 + progressionMultiplier, 6 + (progressionMultiplier * 2), "bodyweight", 90));
        exercises.add(createStructuredExercise("Mountain Climbers", 3, 30 + (progressionMultiplier * 10), "time_seconds", 60));
        exercises.add(createStructuredExercise("Plank to Push-up", 3, 8 + progressionMultiplier, "bodyweight", 75));

        return exercises;
    }

    private List<Map<String, Object>> generateCoreExercises(String fitnessLevel, int progressionMultiplier) {
        List<Map<String, Object>> exercises = new ArrayList<>();

        switch (fitnessLevel.toUpperCase()) {
            case "BEGINNER":
                exercises.add(createStructuredExercise("Plank",
                    3 + progressionMultiplier,
                    30 + (progressionMultiplier * 15),
                    "time_seconds", 30));
                exercises.add(createStructuredExercise("Dead Bug", 3, 10 + (progressionMultiplier * 5), "bodyweight", 45));
                break;
            case "INTERMEDIATE":
                exercises.add(createStructuredExercise("Plank",
                    3 + progressionMultiplier,
                    45 + (progressionMultiplier * 15),
                    "time_seconds", 45));
                exercises.add(createStructuredExercise("Russian Twists", 3, 20 + (progressionMultiplier * 10), "bodyweight", 60));
                break;
            default: // ADVANCED
                exercises.add(createStructuredExercise("Plank to Pike",
                    3 + progressionMultiplier,
                    12 + (progressionMultiplier * 3),
                    "bodyweight", 90));
                exercises.add(createStructuredExercise("L-Sit Hold", 3, 15 + (progressionMultiplier * 10), "time_seconds", 120));
        }

        return exercises;
    }

    /**
     * Creates a structured exercise object following the required schema
     */
    private Map<String, Object> createStructuredExercise(String name, int sets, int reps, String weightType, int restSeconds) {
        Map<String, Object> exercise = new LinkedHashMap<>();

        exercise.put("name", name);
        exercise.put("sets", sets);
        exercise.put("reps", reps);
        exercise.put("weight_lbs", weightType.equals("bodyweight") ? 0 : null);
        exercise.put("weight_type", weightType);
        exercise.put("rest_seconds", restSeconds);
        exercise.put("instructions", generateExerciseInstructions(name));
        exercise.put("muscle_groups", getMuscleGroups(name));

        return exercise;
    }

    private String generateExerciseInstructions(String exerciseName) {
        Map<String, String> instructions = Map.of(
            "Push-ups", "Start in plank position, lower chest to floor, push back up",
            "Squats", "Feet shoulder-width apart, lower until thighs parallel, return to standing",
            "Burpees", "Squat down, jump back to plank, do push-up, jump feet forward, jump up",
            "Plank", "Hold straight line from head to heels, engage core",
            "Lunges", "Step forward, lower until both knees at 90 degrees, return to start"
        );

        return instructions.getOrDefault(exerciseName, "Perform exercise with proper form");
    }

    private List<String> getMuscleGroups(String exerciseName) {
        Map<String, List<String>> muscleMap = Map.of(
            "Push-ups", List.of("chest", "shoulders", "triceps"),
            "Squats", List.of("quadriceps", "glutes", "calves"),
            "Burpees", List.of("full_body", "cardio"),
            "Plank", List.of("core", "shoulders"),
            "Lunges", List.of("quadriceps", "glutes", "hamstrings")
        );

        return muscleMap.getOrDefault(exerciseName, List.of("full_body"));
    }

    private boolean isWorkoutDay(int dayNumber, Integer workoutFrequency) {
        if (workoutFrequency == null) workoutFrequency = 3;

        // Distribute workout days throughout the week
        switch (workoutFrequency) {
            case 3: return dayNumber == 1 || dayNumber == 3 || dayNumber == 5; // Mon, Wed, Fri
            case 4: return dayNumber == 1 || dayNumber == 3 || dayNumber == 5 || dayNumber == 7; // Mon, Wed, Fri, Sun
            case 5: return dayNumber != 6 && dayNumber != 7; // Mon-Fri
            case 6: return dayNumber != 7; // Mon-Sat
            case 7: return true; // Every day
            default: return dayNumber == 1 || dayNumber == 3 || dayNumber == 5; // Default to 3 days
        }
    }

    private String getWeekFocus(int weekNumber) {
        return switch (weekNumber) {
            case 1 -> "Foundation & Form";
            case 2 -> "Building Strength";
            case 3 -> "Increasing Intensity";
            case 4 -> "Peak Performance";
            default -> "General Fitness";
        };
    }

    private String getWeekIntensity(int weekNumber) {
        return switch (weekNumber) {
            case 1 -> "Low";
            case 2 -> "Moderate";
            case 3 -> "High";
            case 4 -> "Peak";
            default -> "Moderate";
        };
    }

    private String getDayFocus(int dayNumber) {
        String[] focuses = {"Upper Body", "Lower Body", "Cardio", "Full Body", "Core & Flexibility", "Upper Body", "Lower Body"};
        return focuses[dayNumber - 1];
    }

    private int estimateCaloriesForDay(WorkoutProfile profile) {
        Integer sessionDuration = profile.getSessionDuration() != null ? profile.getSessionDuration() : 45;
        return sessionDuration * 6; // Rough estimate: 6 calories per minute
    }

    private Map<String, Object> generatePlanSummary(WorkoutProfile profile) {
        Map<String, Object> summary = new LinkedHashMap<>();

        summary.put("totalWorkoutDays", calculateTotalWorkoutDays(profile.getWorkoutFrequency()));
        summary.put("totalRestDays", 28 - calculateTotalWorkoutDays(profile.getWorkoutFrequency()));
        summary.put("avgDurationPerSession", profile.getSessionDuration() != null ? profile.getSessionDuration() : 45);
        summary.put("estimatedTotalCalories", calculateTotalCalories(profile));
        summary.put("primaryMuscleGroups", List.of("chest", "back", "legs", "shoulders", "core", "arms"));
        summary.put("recommendedEquipment", profile.getAvailableEquipment());

        return summary;
    }

    private int calculateTotalWorkoutDays(Integer workoutFrequency) {
        if (workoutFrequency == null) workoutFrequency = 3;
        return workoutFrequency * 4; // 4 weeks
    }

    private int calculateTotalCalories(WorkoutProfile profile) {
        int workoutDays = calculateTotalWorkoutDays(profile.getWorkoutFrequency());
        int caloriesPerDay = estimateCaloriesForDay(profile);
        return workoutDays * caloriesPerDay;
    }
}