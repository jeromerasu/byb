package com.workoutplanner.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Service
@ConditionalOnProperty(name = "storage.use-local", havingValue = "true")
public class LocalFileStorageService {

    private final ObjectMapper prettyObjectMapper;
    private final String baseStoragePath;

    public LocalFileStorageService(@Value("${storage.local.path:#{systemProperties['java.io.tmpdir']}/workout-storage}") String baseStoragePath) {
        this.baseStoragePath = baseStoragePath;

        // Create a pretty-printing ObjectMapper for JSON storage
        this.prettyObjectMapper = new ObjectMapper();
        this.prettyObjectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.prettyObjectMapper.registerModule(new JavaTimeModule());
        this.prettyObjectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Initialize storage directories
        initializeStorage();
    }

    private void initializeStorage() {
        try {
            // Log environment information
            System.out.println("🌍 Environment Info:");
            System.out.println("   java.io.tmpdir: " + System.getProperty("java.io.tmpdir"));
            System.out.println("   user.dir: " + System.getProperty("user.dir"));
            System.out.println("   user.home: " + System.getProperty("user.home"));

            // Create base directory
            Path storagePath = Paths.get(baseStoragePath);
            System.out.println("💾 Attempting to create storage directory: " + storagePath.toAbsolutePath());
            Files.createDirectories(storagePath);
            System.out.println("✅ Created base directory: " + storagePath.toAbsolutePath());

            // Create structured subdirectories
            Path workoutPath = storagePath.resolve("workout");
            Path dietPath = storagePath.resolve("diet");

            Files.createDirectories(workoutPath);
            System.out.println("✅ Created workout directory: " + workoutPath.toAbsolutePath());

            Files.createDirectories(dietPath);
            System.out.println("✅ Created diet directory: " + dietPath.toAbsolutePath());

            System.out.println("📁 Local storage initialized successfully at: " + storagePath.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("❌ Failed to initialize local storage at: " + baseStoragePath);
            System.err.println("❌ Error: " + e.getMessage());
            throw new RuntimeException("Failed to initialize local storage: " + e.getMessage(), e);
        }
    }

    public String storeWorkoutPlan(String userId, String planTitle, Object workoutPlan) {
        if (workoutPlan == null) {
            throw new IllegalArgumentException("Workout plan cannot be null");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }

        try {
            // Determine week number (can be enhanced to accept week parameter)
            String weekNumber = "week" + getCurrentWeekNumber();
            System.out.println("💪 Storing workout plan for user: " + userId + ", week: " + weekNumber);

            // Create structured directory: workout/{userId}/weeklyplan/{week}/
            Path weekDir = Paths.get(baseStoragePath, "workout", userId, "weeklyplan", weekNumber);
            System.out.println("📂 Creating week directory: " + weekDir.toAbsolutePath());
            Files.createDirectories(weekDir);

            Path exercisesDir = weekDir.resolve("exercises");
            System.out.println("📂 Creating exercises directory: " + exercisesDir.toAbsolutePath());
            Files.createDirectories(exercisesDir);

            // Convert plan to Map for processing
            Map<String, Object> planMap = convertToMap(workoutPlan);

            // Extract and store exercises separately
            Object exercises = planMap.get("exercises");
            if (exercises instanceof List<?> exercisesList) {
                storeExerciseMetadata(exercisesDir, exercisesList);
                // Remove exercises from main plan (will be loaded separately)
                planMap.put("exercisesStoredSeparately", true);
                planMap.put("exercisesLocation", "exercises/");
                planMap.remove("exercises");
            }

            // Store main plan
            Path planPath = weekDir.resolve("plan.json");
            String jsonContent = prettyObjectMapper.writeValueAsString(planMap);
            Files.write(planPath, jsonContent.getBytes());

            // Generate storage key that points to the week directory
            String storageKey = "workout/" + userId + "/weeklyplan/" + weekNumber;

            System.out.println("💪 Stored workout plan in structured format: " + planPath.toAbsolutePath());
            return storageKey;

        } catch (IOException e) {
            System.err.println("❌ Failed to store workout plan for user: " + userId);
            System.err.println("❌ Base storage path: " + baseStoragePath);
            System.err.println("❌ IOException: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to store workout plan: " + e.getMessage(), e);
        }
    }

    public String storeDietPlan(String userId, String planTitle, Object dietPlan) {
        if (dietPlan == null) {
            throw new IllegalArgumentException("Diet plan cannot be null");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }

        try {
            // Determine week number (can be enhanced to accept week parameter)
            String weekNumber = "week" + getCurrentWeekNumber();

            // Create structured directory: diet/{userId}/weeklyplan/{week}/
            Path weekDir = Paths.get(baseStoragePath, "diet", userId, "weeklyplan", weekNumber);
            Files.createDirectories(weekDir);

            Path mealsDir = weekDir.resolve("meals");
            Files.createDirectories(mealsDir);

            // Convert plan to Map for processing
            Map<String, Object> planMap = convertToMap(dietPlan);

            // Extract and store meal metadata separately
            Object weeklyPlan = planMap.get("weeklyPlan");
            if (weeklyPlan instanceof List<?> weeklyPlanList) {
                storeMealMetadata(mealsDir, weeklyPlanList);
                // Remove detailed meals from main plan (will be loaded separately)
                planMap.put("mealsStoredSeparately", true);
                planMap.put("mealsLocation", "meals/");
                planMap.remove("weeklyPlan");
            }

            // Store main plan
            Path planPath = weekDir.resolve("plan.json");
            String jsonContent = prettyObjectMapper.writeValueAsString(planMap);
            Files.write(planPath, jsonContent.getBytes());

            // Generate storage key that points to the week directory
            String storageKey = "diet/" + userId + "/weeklyplan/" + weekNumber;

            System.out.println("🥗 Stored diet plan in structured format: " + planPath.toAbsolutePath());
            return storageKey;

        } catch (IOException e) {
            throw new RuntimeException("Failed to store diet plan", e);
        }
    }

    public Map<String, Object> retrieveWorkoutPlan(String userId, String storageKey) {
        try {
            // storageKey format: "workout/{userId}/weeklyplan/{week}"
            Path weekDir = Paths.get(baseStoragePath, storageKey);
            Path planPath = weekDir.resolve("plan.json");

            if (!Files.exists(planPath)) {
                throw new RuntimeException("Workout plan not found: " + storageKey);
            }

            // Load main plan
            String jsonContent = Files.readString(planPath);
            Map<String, Object> plan = prettyObjectMapper.readValue(jsonContent, Map.class);

            // If exercises are stored separately, load them
            if (Boolean.TRUE.equals(plan.get("exercisesStoredSeparately"))) {
                Path exercisesDir = weekDir.resolve("exercises");
                List<Map<String, Object>> exercises = loadExerciseMetadata(exercisesDir);
                plan.put("exercises", exercises);
                // Clean up metadata fields
                plan.remove("exercisesStoredSeparately");
                plan.remove("exercisesLocation");
            }

            return plan;

        } catch (IOException e) {
            throw new RuntimeException("Failed to retrieve workout plan", e);
        }
    }

    public Map<String, Object> retrieveDietPlan(String userId, String storageKey) {
        try {
            // storageKey format: "diet/{userId}/weeklyplan/{week}"
            Path weekDir = Paths.get(baseStoragePath, storageKey);
            Path planPath = weekDir.resolve("plan.json");

            if (!Files.exists(planPath)) {
                throw new RuntimeException("Diet plan not found: " + storageKey);
            }

            // Load main plan
            String jsonContent = Files.readString(planPath);
            Map<String, Object> plan = prettyObjectMapper.readValue(jsonContent, Map.class);

            // If meals are stored separately, load them
            if (Boolean.TRUE.equals(plan.get("mealsStoredSeparately"))) {
                Path mealsDir = weekDir.resolve("meals");
                List<Map<String, Object>> weeklyPlan = loadMealMetadata(mealsDir);
                plan.put("weeklyPlan", weeklyPlan);
                // Clean up metadata fields
                plan.remove("mealsStoredSeparately");
                plan.remove("mealsLocation");
            }

            return plan;

        } catch (IOException e) {
            throw new RuntimeException("Failed to retrieve diet plan", e);
        }
    }

    public List<String> listUserPlans(String userId, String planType) {
        try {
            // planType should be "workout" or "diet"
            Path weeklyPlanDir = Paths.get(baseStoragePath, planType, userId, "weeklyplan");
            if (!Files.exists(weeklyPlanDir)) {
                return new ArrayList<>();
            }

            return Files.list(weeklyPlanDir)
                    .filter(Files::isDirectory)
                    .filter(path -> path.getFileName().toString().startsWith("week"))
                    .map(path -> planType + "/" + userId + "/weeklyplan/" + path.getFileName().toString())
                    .sorted()
                    .toList();

        } catch (IOException e) {
            throw new RuntimeException("Failed to list user plans", e);
        }
    }

    // Helper methods for the new structured storage approach

    private String getCurrentWeekNumber() {
        // Simple implementation - can be enhanced to use actual week numbers or date-based
        // For now, return current week of year
        LocalDateTime now = LocalDateTime.now();
        int dayOfYear = now.getDayOfYear();
        return String.valueOf((dayOfYear / 7) + 1);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToMap(Object obj) {
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        } else {
            // Convert via JSON serialization
            try {
                String json = prettyObjectMapper.writeValueAsString(obj);
                return prettyObjectMapper.readValue(json, Map.class);
            } catch (IOException e) {
                throw new RuntimeException("Failed to convert object to map", e);
            }
        }
    }

    private void storeExerciseMetadata(Path exercisesDir, List<?> exercisesList) throws IOException {
        for (int i = 0; i < exercisesList.size(); i++) {
            Object exercise = exercisesList.get(i);
            Path exerciseFile = exercisesDir.resolve("exercise_" + (i + 1) + ".json");
            String exerciseJson = prettyObjectMapper.writeValueAsString(exercise);
            Files.write(exerciseFile, exerciseJson.getBytes());
        }
    }

    private void storeMealMetadata(Path mealsDir, List<?> weeklyPlanList) throws IOException {
        for (int i = 0; i < weeklyPlanList.size(); i++) {
            Object dayPlan = weeklyPlanList.get(i);
            if (dayPlan instanceof Map<?, ?> dayMap) {
                String dayName = String.valueOf(dayMap.get("day"));
                Path dayFile = mealsDir.resolve("day_" + (i + 1) + "_" + dayName + ".json");
                String dayJson = prettyObjectMapper.writeValueAsString(dayPlan);
                Files.write(dayFile, dayJson.getBytes());
            }
        }
    }

    private List<Map<String, Object>> loadExerciseMetadata(Path exercisesDir) throws IOException {
        List<Map<String, Object>> exercises = new ArrayList<>();
        if (!Files.exists(exercisesDir)) {
            return exercises;
        }

        Files.list(exercisesDir)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".json"))
                .sorted()
                .forEach(exerciseFile -> {
                    try {
                        String json = Files.readString(exerciseFile);
                        Map<String, Object> exercise = prettyObjectMapper.readValue(json, Map.class);
                        exercises.add(exercise);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to load exercise metadata", e);
                    }
                });

        return exercises;
    }

    private List<Map<String, Object>> loadMealMetadata(Path mealsDir) throws IOException {
        List<Map<String, Object>> weeklyPlan = new ArrayList<>();
        if (!Files.exists(mealsDir)) {
            return weeklyPlan;
        }

        Files.list(mealsDir)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".json"))
                .sorted()
                .forEach(dayFile -> {
                    try {
                        String json = Files.readString(dayFile);
                        Map<String, Object> dayPlan = prettyObjectMapper.readValue(json, Map.class);
                        weeklyPlan.add(dayPlan);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to load meal metadata", e);
                    }
                });

        return weeklyPlan;
    }
}