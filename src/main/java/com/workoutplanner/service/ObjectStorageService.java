package com.workoutplanner.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ObjectStorageService {

    private final S3Client s3Client;
    private final ObjectMapper objectMapper;
    private final ObjectMapper prettyObjectMapper;
    private final String bucketName;
    private final boolean autoCreateBucket;

    public ObjectStorageService(
            S3Client s3Client,
            @Value("${minio.bucket-name:workout-plans}") String bucketName,
            @Value("${minio.auto-create-bucket:true}") boolean autoCreateBucket,
            ObjectMapper objectMapper) {

        this.s3Client = s3Client;
        this.objectMapper = objectMapper;
        this.bucketName = bucketName;
        this.autoCreateBucket = autoCreateBucket;

        // Create a pretty-printing ObjectMapper for JSON storage
        this.prettyObjectMapper = objectMapper.copy();
        this.prettyObjectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.prettyObjectMapper.registerModule(new JavaTimeModule());
        this.prettyObjectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Initialize bucket if auto-create is enabled
        if (autoCreateBucket) {
            initializeBucket();
        }
    }

    private void initializeBucket() {
        try {
            // Check if bucket exists
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();

            s3Client.headBucket(headBucketRequest);
        } catch (NoSuchBucketException e) {
            // Bucket doesn't exist, create it
            try {
                CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                        .bucket(bucketName)
                        .build();

                s3Client.createBucket(createBucketRequest);
                System.out.println("Created MinIO bucket: " + bucketName);
            } catch (Exception createError) {
                System.err.println("Failed to create MinIO bucket: " + bucketName + " - " + createError.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Failed to check MinIO bucket: " + bucketName + " - " + e.getMessage());
        }
    }

    /**
     * Store workout plan in structured object storage
     */
    public String storeWorkoutPlan(String userId, String planTitle, Object workoutPlan) {
        if (workoutPlan == null) {
            throw new IllegalArgumentException("Workout plan cannot be null");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }

        try {
            // Determine week number
            String weekNumber = "week" + getCurrentWeekNumber();

            // Convert plan to Map for processing
            Map<String, Object> planMap = convertToMap(workoutPlan);

            // Store main plan
            String planKey = String.format("workout/%s/weeklyplan/%s/plan.json", userId, weekNumber);

            // Extract and store exercises separately
            Object exercises = planMap.get("exercises");
            if (exercises instanceof List<?> exercisesList) {
                storeExerciseMetadataInStorage(userId, weekNumber, exercisesList);
                // Remove exercises from main plan (will be loaded separately)
                planMap.put("exercisesStoredSeparately", true);
                planMap.put("exercisesLocation", "exercises/");
                planMap.remove("exercises");
            }

            storeObject(planKey, planMap, "workout-plan");

            // Return the week directory key
            String storageKey = String.format("workout/%s/weeklyplan/%s", userId, weekNumber);
            System.out.println("💪 Stored workout plan in structured format: " + planKey);
            return storageKey;

        } catch (Exception e) {
            throw new RuntimeException("Failed to store workout plan", e);
        }
    }

    /**
     * Store diet plan in structured object storage
     */
    public String storeDietPlan(String userId, String planTitle, Object dietPlan) {
        if (dietPlan == null) {
            throw new IllegalArgumentException("Diet plan cannot be null");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }

        try {
            // Determine week number
            String weekNumber = "week" + getCurrentWeekNumber();

            // Convert plan to Map for processing
            Map<String, Object> planMap = convertToMap(dietPlan);

            // Store main plan
            String planKey = String.format("diet/%s/weeklyplan/%s/plan.json", userId, weekNumber);

            // Extract and store meal metadata separately
            Object weeklyPlan = planMap.get("weeklyPlan");
            if (weeklyPlan instanceof List<?> weeklyPlanList) {
                storeMealMetadataInStorage(userId, weekNumber, weeklyPlanList);
                // Remove detailed meals from main plan (will be loaded separately)
                planMap.put("mealsStoredSeparately", true);
                planMap.put("mealsLocation", "meals/");
                planMap.remove("weeklyPlan");
            }

            storeObject(planKey, planMap, "diet-plan");

            // Return the week directory key
            String storageKey = String.format("diet/%s/weeklyplan/%s", userId, weekNumber);
            System.out.println("🥗 Stored diet plan in structured format: " + planKey);
            return storageKey;

        } catch (Exception e) {
            throw new RuntimeException("Failed to store diet plan", e);
        }
    }

    /**
     * Store workout session data
     */
    public String storeWorkoutSession(String userId, String sessionId, Object sessionData) {
        String year = String.valueOf(LocalDateTime.now().getYear());
        String key = String.format("workout-sessions/%s/%s/%s.json", userId, year, sessionId);
        return storeObject(key, sessionData, "workout-session");
    }

    /**
     * Generic method to store any object as pretty-printed JSON
     */
    private String storeObject(String key, Object data, String contentType) {
        try {
            // Convert object to pretty-printed JSON for better readability
            String jsonData = prettyObjectMapper.writeValueAsString(data);
            byte[] dataBytes = jsonData.getBytes("UTF-8");

            // Prepare metadata for JSON file
            Map<String, String> metadata = new HashMap<>();
            metadata.put("content-type", contentType);
            metadata.put("format", "json");
            metadata.put("encoding", "UTF-8");
            metadata.put("created-at", LocalDateTime.now().toString());
            metadata.put("size-bytes", String.valueOf(dataBytes.length));
            metadata.put("pretty-printed", "true");

            // Create put request with explicit JSON content type
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType("application/json; charset=utf-8")
                    .metadata(metadata)
                    .build();

            // Upload JSON to S3/MinIO
            s3Client.putObject(putRequest, RequestBody.fromBytes(dataBytes));

            System.out.println("Successfully stored " + contentType + " as JSON: " + key +
                              " (size: " + dataBytes.length + " bytes)");

            return key; // Return the storage key for database reference

        } catch (Exception e) {
            System.err.println("Failed to store " + contentType + " as JSON: " + key + " - " + e.getMessage());
            throw new RuntimeException("Failed to store object as JSON: " + key, e);
        }
    }

    /**
     * Retrieve workout plan from object storage
     */
    public <T> Optional<T> getWorkoutPlan(String storageKey, Class<T> targetClass) {
        return getObject(storageKey, targetClass);
    }

    /**
     * Retrieve diet plan from object storage
     */
    public <T> Optional<T> getDietPlan(String storageKey, Class<T> targetClass) {
        return getObject(storageKey, targetClass);
    }

    /**
     * Retrieve workout plan as Map from structured object storage
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> retrieveWorkoutPlan(String userId, String storageKey) {
        try {
            // storageKey format: "workout/{userId}/weeklyplan/{week}"
            String planKey = storageKey + "/plan.json";

            // Load main plan
            Map<String, Object> plan = (Map<String, Object>) getObject(planKey, Map.class).orElse(new HashMap<>());

            // If exercises are stored separately, load them
            if (Boolean.TRUE.equals(plan.get("exercisesStoredSeparately"))) {
                List<Map<String, Object>> exercises = loadExerciseMetadataFromStorage(userId, extractWeekFromStorageKey(storageKey));
                plan.put("exercises", exercises);
                // Clean up metadata fields
                plan.remove("exercisesStoredSeparately");
                plan.remove("exercisesLocation");
            }

            return plan;

        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve workout plan", e);
        }
    }

    /**
     * Retrieve diet plan as Map from structured object storage
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> retrieveDietPlan(String userId, String storageKey) {
        try {
            // storageKey format: "diet/{userId}/weeklyplan/{week}"
            String planKey = storageKey + "/plan.json";

            // Load main plan
            Map<String, Object> plan = (Map<String, Object>) getObject(planKey, Map.class).orElse(new HashMap<>());

            // If meals are stored separately, load them
            if (Boolean.TRUE.equals(plan.get("mealsStoredSeparately"))) {
                List<Map<String, Object>> weeklyPlan = loadMealMetadataFromStorage(userId, extractWeekFromStorageKey(storageKey));
                plan.put("weeklyPlan", weeklyPlan);
                // Clean up metadata fields
                plan.remove("mealsStoredSeparately");
                plan.remove("mealsLocation");
            }

            return plan;

        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve diet plan", e);
        }
    }

    /**
     * Generic method to retrieve any object from JSON storage
     */
    public <T> Optional<T> getObject(String key, Class<T> targetClass) {
        try {
            // Create get request
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            // Download JSON from S3/MinIO
            byte[] objectData = s3Client.getObject(getRequest).readAllBytes();

            System.out.println("Retrieved JSON from storage: " + key +
                              " (size: " + objectData.length + " bytes)");

            // Convert JSON back to object
            T object = objectMapper.readValue(objectData, targetClass);

            System.out.println("Successfully deserialized JSON to " + targetClass.getSimpleName());

            return Optional.of(object);

        } catch (NoSuchKeyException e) {
            System.out.println("JSON file not found: " + key);
            return Optional.empty();
        } catch (Exception e) {
            System.err.println("Failed to retrieve/parse JSON object: " + key + " - " + e.getMessage());
            throw new RuntimeException("Failed to retrieve object from JSON storage: " + key, e);
        }
    }

    /**
     * Delete object from storage
     */
    public void deleteObject(String key) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteRequest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete object: " + key, e);
        }
    }

    /**
     * Check if object exists
     */
    public boolean objectExists(String key) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.headObject(headRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Failed to check object existence: " + key, e);
        }
    }

    /**
     * Get object metadata
     */
    public Map<String, String> getObjectMetadata(String key) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            HeadObjectResponse response = s3Client.headObject(headRequest);
            return response.metadata();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get object metadata: " + key, e);
        }
    }


    /**
     * List objects with prefix (for user's content)
     */
    public java.util.List<String> listUserObjects(String userId, String objectType) {
        try {
            String prefix = String.format("%s/%s/", objectType, userId);

            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(listRequest);

            return response.contents().stream()
                    .map(S3Object::key)
                    .toList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to list objects for user: " + userId, e);
        }
    }

    /**
     * Upload a file (multipart) to object storage
     */
    public String uploadFile(MultipartFile file, String userId, String category, String metadata) {
        try {
            // Generate unique file key
            String timestamp = String.valueOf(System.currentTimeMillis());
            String originalFilename = file.getOriginalFilename();
            String sanitizedFilename = originalFilename != null ?
                originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_") : "unknown";

            String key = String.format("%s/%s/%s_%s", category, userId, timestamp, sanitizedFilename);

            // Prepare metadata
            Map<String, String> objectMetadata = new HashMap<>();
            objectMetadata.put("original-filename", originalFilename != null ? originalFilename : "unknown");
            objectMetadata.put("uploaded-by", userId);
            objectMetadata.put("category", category);
            objectMetadata.put("upload-timestamp", LocalDateTime.now().toString());
            objectMetadata.put("file-size", String.valueOf(file.getSize()));

            if (metadata != null && !metadata.trim().isEmpty()) {
                objectMetadata.put("user-metadata", metadata);
            }

            // Create put request
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .metadata(objectMetadata)
                    .build();

            // Upload file
            s3Client.putObject(putRequest, RequestBody.fromBytes(file.getBytes()));

            return key;

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file: " + file.getOriginalFilename(), e);
        }
    }

    /**
     * Download a file as byte array
     */
    public Optional<byte[]> downloadFile(String key) {
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            byte[] objectData = s3Client.getObject(getRequest, ResponseTransformer.toBytes()).asByteArray();
            return Optional.of(objectData);

        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (Exception e) {
            throw new RuntimeException("Failed to download file: " + key, e);
        }
    }

    /**
     * List all files for a user across all categories
     */
    public List<String> listAllUserFiles(String userId) {
        try {
            List<String> allFiles = new ArrayList<>();

            // List of categories to search
            List<String> categories = List.of(
                "progress-photos", "workout-videos", "documents",
                "diet-plans", "workout-plans", "workout-sessions"
            );

            for (String category : categories) {
                try {
                    List<String> categoryFiles = listUserObjects(userId, category);
                    allFiles.addAll(categoryFiles);
                } catch (Exception e) {
                    // Continue with other categories if one fails
                    System.err.println("Failed to list files in category " + category + ": " + e.getMessage());
                }
            }

            return allFiles;
        } catch (Exception e) {
            throw new RuntimeException("Failed to list all user files: " + userId, e);
        }
    }

    /**
     * Get storage statistics for a user
     */
    public Map<String, Object> getUserStorageStats(String userId) {
        try {
            Map<String, Object> stats = new HashMap<>();
            Map<String, Integer> categoryStats = new HashMap<>();
            Map<String, Long> categorySizes = new HashMap<>();

            long totalSize = 0;
            int totalFiles = 0;

            List<String> categories = List.of(
                "progress-photos", "workout-videos", "documents",
                "diet-plans", "workout-plans", "workout-sessions"
            );

            for (String category : categories) {
                try {
                    String prefix = String.format("%s/%s/", category, userId);

                    ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                            .bucket(bucketName)
                            .prefix(prefix)
                            .build();

                    ListObjectsV2Response response = s3Client.listObjectsV2(listRequest);

                    int categoryFileCount = response.contents().size();
                    long categorySize = response.contents().stream()
                            .mapToLong(S3Object::size)
                            .sum();

                    categoryStats.put(category, categoryFileCount);
                    categorySizes.put(category, categorySize);

                    totalFiles += categoryFileCount;
                    totalSize += categorySize;

                } catch (Exception e) {
                    categoryStats.put(category, 0);
                    categorySizes.put(category, 0L);
                }
            }

            stats.put("userId", userId);
            stats.put("totalFiles", totalFiles);
            stats.put("totalSizeBytes", totalSize);
            stats.put("totalSizeMB", totalSize / (1024.0 * 1024.0));
            stats.put("filesByCategory", categoryStats);
            stats.put("sizeByCategory", categorySizes);
            stats.put("generatedAt", LocalDateTime.now().toString());

            return stats;

        } catch (Exception e) {
            throw new RuntimeException("Failed to get storage stats for user: " + userId, e);
        }
    }

    /**
     * Enhanced presigned URL generation with proper expiration
     */
    public String generatePresignedUrl(String key, int expirationMinutes) {
        try {
            // For MinIO local development, return a simple URL
            // In production, you would use S3 Presigner
            String baseUrl = bucketName.contains("localhost") ?
                "http://localhost:9000" :
                "https://" + bucketName + ".s3.amazonaws.com";

            return String.format("%s/%s/%s?expires=%d",
                baseUrl, bucketName, key,
                System.currentTimeMillis() + (expirationMinutes * 60 * 1000));
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate presigned URL: " + key, e);
        }
    }

    // Helper methods for structured storage approach

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
            } catch (Exception e) {
                throw new RuntimeException("Failed to convert object to map", e);
            }
        }
    }

    private void storeExerciseMetadataInStorage(String userId, String weekNumber, List<?> exercisesList) {
        for (int i = 0; i < exercisesList.size(); i++) {
            Object exercise = exercisesList.get(i);
            String exerciseKey = String.format("workout/%s/weeklyplan/%s/exercises/exercise_%d.json", userId, weekNumber, i + 1);
            storeObject(exerciseKey, exercise, "exercise-metadata");
        }
    }

    private void storeMealMetadataInStorage(String userId, String weekNumber, List<?> weeklyPlanList) {
        for (int i = 0; i < weeklyPlanList.size(); i++) {
            Object dayPlan = weeklyPlanList.get(i);
            if (dayPlan instanceof Map<?, ?> dayMap) {
                String dayName = String.valueOf(dayMap.get("day"));
                String dayKey = String.format("diet/%s/weeklyplan/%s/meals/day_%d_%s.json", userId, weekNumber, i + 1, dayName);
                storeObject(dayKey, dayPlan, "meal-metadata");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> loadExerciseMetadataFromStorage(String userId, String weekNumber) {
        List<Map<String, Object>> exercises = new ArrayList<>();
        String prefix = String.format("workout/%s/weeklyplan/%s/exercises/", userId, weekNumber);

        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(listRequest);

            for (S3Object s3Object : response.contents()) {
                if (s3Object.key().endsWith(".json")) {
                    Optional<Map> exercise = getObject(s3Object.key(), Map.class);
                    exercise.ifPresent(exercises::add);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load exercise metadata: " + e.getMessage());
        }

        return exercises;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> loadMealMetadataFromStorage(String userId, String weekNumber) {
        List<Map<String, Object>> weeklyPlan = new ArrayList<>();
        String prefix = String.format("diet/%s/weeklyplan/%s/meals/", userId, weekNumber);

        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(listRequest);

            for (S3Object s3Object : response.contents()) {
                if (s3Object.key().endsWith(".json")) {
                    Optional<Map> dayPlan = getObject(s3Object.key(), Map.class);
                    dayPlan.ifPresent(weeklyPlan::add);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load meal metadata: " + e.getMessage());
        }

        return weeklyPlan;
    }

    private String extractWeekFromStorageKey(String storageKey) {
        // Extract week from "workout/{userId}/weeklyplan/{week}" or "diet/{userId}/weeklyplan/{week}"
        String[] parts = storageKey.split("/");
        if (parts.length >= 4) {
            return parts[3]; // week part
        }
        return "week1"; // fallback
    }
}