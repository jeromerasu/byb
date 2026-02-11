package com.workoutplanner.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class LocalFileStorageService {

    private final ObjectMapper prettyObjectMapper;
    private final String baseStoragePath;

    public LocalFileStorageService(@Value("${storage.local.path:./local-storage}") String baseStoragePath) {
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
            // Create base directory
            Path storagePath = Paths.get(baseStoragePath);
            Files.createDirectories(storagePath);

            // Create subdirectories
            Files.createDirectories(storagePath.resolve("workout-plans"));
            Files.createDirectories(storagePath.resolve("diet-plans"));

            System.out.println("📁 Local storage initialized at: " + storagePath.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize local storage", e);
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
            // Generate storage key
            String timestamp = LocalDateTime.now().toString().replace(":", "-");
            String storageKey = "workout-plans/" + userId + "/" + timestamp + "_" + UUID.randomUUID() + ".json";

            // Create user directory if it doesn't exist
            Path userDir = Paths.get(baseStoragePath, "workout-plans", userId);
            Files.createDirectories(userDir);

            // Write file
            Path filePath = Paths.get(baseStoragePath, storageKey);
            String jsonContent = prettyObjectMapper.writeValueAsString(workoutPlan);
            Files.write(filePath, jsonContent.getBytes());

            System.out.println("💪 Stored workout plan: " + filePath.toAbsolutePath());
            return storageKey;

        } catch (IOException e) {
            throw new RuntimeException("Failed to store workout plan", e);
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
            // Generate storage key
            String timestamp = LocalDateTime.now().toString().replace(":", "-");
            String storageKey = "diet-plans/" + userId + "/" + timestamp + "_" + UUID.randomUUID() + ".json";

            // Create user directory if it doesn't exist
            Path userDir = Paths.get(baseStoragePath, "diet-plans", userId);
            Files.createDirectories(userDir);

            // Write file
            Path filePath = Paths.get(baseStoragePath, storageKey);
            String jsonContent = prettyObjectMapper.writeValueAsString(dietPlan);
            Files.write(filePath, jsonContent.getBytes());

            System.out.println("🥗 Stored diet plan: " + filePath.toAbsolutePath());
            return storageKey;

        } catch (IOException e) {
            throw new RuntimeException("Failed to store diet plan", e);
        }
    }

    public Map<String, Object> retrieveWorkoutPlan(String userId, String storageKey) {
        try {
            Path filePath = Paths.get(baseStoragePath, storageKey);
            if (!Files.exists(filePath)) {
                throw new RuntimeException("Workout plan not found: " + storageKey);
            }

            String jsonContent = Files.readString(filePath);
            return prettyObjectMapper.readValue(jsonContent, Map.class);

        } catch (IOException e) {
            throw new RuntimeException("Failed to retrieve workout plan", e);
        }
    }

    public Map<String, Object> retrieveDietPlan(String userId, String storageKey) {
        try {
            Path filePath = Paths.get(baseStoragePath, storageKey);
            if (!Files.exists(filePath)) {
                throw new RuntimeException("Diet plan not found: " + storageKey);
            }

            String jsonContent = Files.readString(filePath);
            return prettyObjectMapper.readValue(jsonContent, Map.class);

        } catch (IOException e) {
            throw new RuntimeException("Failed to retrieve diet plan", e);
        }
    }

    public List<String> listUserPlans(String userId, String planType) {
        try {
            Path userDir = Paths.get(baseStoragePath, planType, userId);
            if (!Files.exists(userDir)) {
                return new ArrayList<>();
            }

            return Files.list(userDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .map(path -> planType + "/" + userId + "/" + path.getFileName().toString())
                    .sorted()
                    .toList();

        } catch (IOException e) {
            throw new RuntimeException("Failed to list user plans", e);
        }
    }
}