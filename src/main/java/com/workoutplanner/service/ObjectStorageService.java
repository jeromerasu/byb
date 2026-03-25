package com.workoutplanner.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@ConditionalOnProperty(name = "storage.use-local", havingValue = "false", matchIfMissing = true)
public class ObjectStorageService {

    private static final Logger logger = LoggerFactory.getLogger(ObjectStorageService.class);

    private final S3Client s3Client;
    private final ObjectMapper objectMapper;
    private final ObjectMapper prettyObjectMapper;
    private final boolean autoCreateBucket;

    public ObjectStorageService(
            S3Client s3Client,
            @Value("${minio.auto-create-bucket:true}") boolean autoCreateBucket,
            ObjectMapper objectMapper) {

        this.s3Client = s3Client;
        this.objectMapper = objectMapper;
        this.autoCreateBucket = autoCreateBucket;

        // Create a pretty-printing ObjectMapper for JSON storage
        this.prettyObjectMapper = objectMapper.copy();
        this.prettyObjectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.prettyObjectMapper.registerModule(new JavaTimeModule());
        this.prettyObjectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Debug MinIO connection initialization
        logger.info("ObjectStorageService initialized with autoCreateBucket: {}", autoCreateBucket);
        logger.info("S3Client type: {}", s3Client.getClass().getSimpleName());
    }

    private void ensureBucketExists(String bucketName) {
        if (!autoCreateBucket) {
            logger.debug("Skipping bucket creation (autoCreateBucket=false) for: {}", bucketName);
            return;
        }

        logger.debug("Checking if MinIO bucket exists: {}", bucketName);
        try {
            // Check if bucket exists
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();

            s3Client.headBucket(headBucketRequest);
            logger.debug("MinIO bucket exists: {}", bucketName);
        } catch (NoSuchBucketException e) {
            // Bucket doesn't exist, create it
            logger.info("Creating MinIO bucket: {}", bucketName);
            try {
                CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                        .bucket(bucketName)
                        .build();

                s3Client.createBucket(createBucketRequest);
                logger.info("Created MinIO bucket: {}", bucketName);
            } catch (Exception createError) {
                logger.error("Failed to create MinIO bucket: {} - {}", bucketName, createError.getMessage(), createError);
            }
        } catch (Exception e) {
            logger.error("Failed to check MinIO bucket: {} - {}", bucketName, e.getMessage(), e);
        }
    }

    /**
     * Store workout plan in structured object storage
     */
    public String storeWorkoutPlan(String bucketName, String userId, String planTitle, Object workoutPlan) {
        if (workoutPlan == null) {
            throw new IllegalArgumentException("Workout plan cannot be null");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }

        try {
            // Ensure bucket exists
            ensureBucketExists(bucketName);

            // Determine current month number
            String monthNumber = "month-" + getCurrentMonthNumber();

            // Convert plan to Map for processing
            Map<String, Object> planMap = convertToMap(workoutPlan);

            // Store main plan with new structure: {user}/month-{n}/workout-plan.json
            String planKey = String.format("%s/%s/workout-plan.json", userId, monthNumber);

            // Store the complete plan (no need to separate exercises for 30-day plans)
            storeObject(bucketName, planKey, planMap, "workout-plan");

            // Return the plan key for future retrieval
            logger.info("Stored workout plan: {}", planKey);
            return planKey;

        } catch (Exception e) {
            System.err.println("❌ Failed to store workout plan in MinIO:");
            System.err.println("   Bucket: " + bucketName);
            System.err.println("   User ID: " + userId);
            System.err.println("   Error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to store workout plan: " + e.getMessage(), e);
        }
    }

    /**
     * Store diet plan in structured object storage
     */
    public String storeDietPlan(String bucketName, String userId, String planTitle, Object dietPlan) {
        if (dietPlan == null) {
            throw new IllegalArgumentException("Diet plan cannot be null");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }

        try {
            // Ensure bucket exists
            ensureBucketExists(bucketName);

            // Determine current month number
            String monthNumber = "month-" + getCurrentMonthNumber();

            // Convert plan to Map for processing
            Map<String, Object> planMap = convertToMap(dietPlan);

            // Store main plan with new structure: {user}/month-{n}/diet-plan.json
            String planKey = String.format("%s/%s/diet-plan.json", userId, monthNumber);

            // Store the complete plan (no need to separate meals for 30-day plans)
            storeObject(bucketName, planKey, planMap, "diet-plan");

            // Return the plan key for future retrieval
            logger.info("Stored diet plan: {}", planKey);
            return planKey;

        } catch (Exception e) {
            throw new RuntimeException("Failed to store diet plan", e);
        }
    }

    /**
     * Store workout session data
     */
    public String storeWorkoutSession(String bucketName, String userId, String sessionId, Object sessionData) {
        String year = String.valueOf(LocalDateTime.now().getYear());
        String key = String.format("workout-sessions/%s/%s/%s.json", userId, year, sessionId);
        return storeObject(bucketName, key, sessionData, "workout-session");
    }

    /**
     * Generic method to store any object as pretty-printed JSON
     */
    private String storeObject(String bucketName, String key, Object data, String contentType) {
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
    public <T> Optional<T> getWorkoutPlan(String bucketName, String storageKey, Class<T> targetClass) {
        return getObject(bucketName, storageKey, targetClass);
    }

    /**
     * Retrieve diet plan from object storage
     */
    public <T> Optional<T> getDietPlan(String bucketName, String storageKey, Class<T> targetClass) {
        return getObject(bucketName, storageKey, targetClass);
    }

    /**
     * Retrieve workout plan as Map from structured object storage
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> retrieveWorkoutPlan(String bucketName, String userId, String storageKey) {
        try {
            // storageKey is now the direct path: {userId}/month-{n}/workout-plan.json
            Map<String, Object> plan = (Map<String, Object>) getObject(bucketName, storageKey, Map.class).orElse(new HashMap<>());
            return plan;

        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve workout plan", e);
        }
    }

    /**
     * Retrieve diet plan as Map from structured object storage
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> retrieveDietPlan(String bucketName, String userId, String storageKey) {
        try {
            // storageKey is now the direct path: {userId}/month-{n}/diet-plan.json
            Map<String, Object> plan = (Map<String, Object>) getObject(bucketName, storageKey, Map.class).orElse(new HashMap<>());
            return plan;

        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve diet plan", e);
        }
    }

    /**
     * Generic method to retrieve any object from JSON storage
     */
    public <T> Optional<T> getObject(String bucketName, String key, Class<T> targetClass) {
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
    public void deleteObject(String bucketName, String key) {
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
    public boolean objectExists(String bucketName, String key) {
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
    public Map<String, String> getObjectMetadata(String bucketName, String key) {
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
    public java.util.List<String> listUserObjects(String bucketName, String userId, String objectType) {
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
    public String uploadFile(String bucketName, MultipartFile file, String userId, String category, String metadata) {
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
    public Optional<byte[]> downloadFile(String bucketName, String key) {
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
    public List<String> listAllUserFiles(String bucketName, String userId) {
        try {
            List<String> allFiles = new ArrayList<>();

            // List of categories to search
            List<String> categories = List.of(
                "progress-photos", "workout-videos", "documents",
                "diet-plans", "workout-plans", "workout-sessions"
            );

            for (String category : categories) {
                try {
                    List<String> categoryFiles = listUserObjects(bucketName, userId, category);
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
    public Map<String, Object> getUserStorageStats(String bucketName, String userId) {
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
    public String generatePresignedUrl(String bucketName, String key, int expirationMinutes) {
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

    private String getCurrentMonthNumber() {
        // Return current month number (1-12)
        LocalDateTime now = LocalDateTime.now();
        return String.valueOf(now.getMonthValue());
    }

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

    private void storeExerciseMetadataInStorage(String bucketName, String userId, List<?> exercisesList) {
        String weekNumber = "week" + getCurrentWeekNumber();
        for (int i = 0; i < exercisesList.size(); i++) {
            Object exercise = exercisesList.get(i);
            String exerciseKey = String.format("workout/%s/weeklyplan/%s/exercises/exercise_%d.json", userId, weekNumber, i + 1);
            storeObject(bucketName, exerciseKey, exercise, "exercise-metadata");
        }
    }

    private void storeMealMetadataInStorage(String bucketName, String userId, List<?> weeklyPlanList) {
        String weekNumber = "week" + getCurrentWeekNumber();
        for (int i = 0; i < weeklyPlanList.size(); i++) {
            Object dayPlan = weeklyPlanList.get(i);
            if (dayPlan instanceof Map<?, ?> dayMap) {
                String dayName = String.valueOf(dayMap.get("day"));
                String dayKey = String.format("diet/%s/weeklyplan/%s/meals/day_%d_%s.json", userId, weekNumber, i + 1, dayName);
                storeObject(bucketName, dayKey, dayPlan, "meal-metadata");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> loadExerciseMetadataFromStorage(String bucketName, String userId, String weekNumber) {
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
                    Optional<Map> exercise = getObject(bucketName, s3Object.key(), Map.class);
                    exercise.ifPresent(exercises::add);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load exercise metadata: " + e.getMessage());
        }

        return exercises;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> loadMealMetadataFromStorage(String bucketName, String userId, String weekNumber) {
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
                    Optional<Map> dayPlan = getObject(bucketName, s3Object.key(), Map.class);
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

    /**
     * Debug method to list all buckets in MinIO
     */
    public List<String> listAllBuckets() {
        try {
            System.out.println("🔍 Listing all MinIO buckets...");
            ListBucketsResponse response = s3Client.listBuckets();

            List<String> bucketNames = response.buckets().stream()
                    .map(bucket -> bucket.name())
                    .toList();

            System.out.println("📦 Found " + bucketNames.size() + " buckets: " + bucketNames);
            return bucketNames;

        } catch (Exception e) {
            System.err.println("❌ Failed to list MinIO buckets: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to list buckets: " + e.getMessage(), e);
        }
    }

    /**
     * Debug method to list all objects in a specific bucket
     */
    public List<Map<String, Object>> listAllObjectsInBucket(String bucketName) {
        try {
            System.out.println("🔍 Listing all objects in bucket: " + bucketName);

            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(listRequest);

            List<Map<String, Object>> objects = new ArrayList<>();
            for (S3Object s3Object : response.contents()) {
                Map<String, Object> objectInfo = new HashMap<>();
                objectInfo.put("key", s3Object.key());
                objectInfo.put("size", s3Object.size());
                objectInfo.put("lastModified", s3Object.lastModified().toString());
                objectInfo.put("storageClass", s3Object.storageClassAsString());
                objects.add(objectInfo);
            }

            System.out.println("📁 Found " + objects.size() + " objects in bucket " + bucketName);
            return objects;

        } catch (NoSuchBucketException e) {
            System.err.println("❌ Bucket not found: " + bucketName);
            return new ArrayList<>();
        } catch (Exception e) {
            System.err.println("❌ Failed to list objects in bucket " + bucketName + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to list objects in bucket: " + bucketName, e);
        }
    }

    /**
     * Debug method to get detailed storage information
     */
    public Map<String, Object> getStorageDebugInfo() {
        try {
            Map<String, Object> debugInfo = new HashMap<>();

            // Get all buckets
            List<String> buckets = listAllBuckets();
            debugInfo.put("totalBuckets", buckets.size());
            debugInfo.put("bucketNames", buckets);

            // Get object counts for each bucket
            Map<String, Integer> bucketObjectCounts = new HashMap<>();
            Map<String, List<Map<String, Object>>> bucketObjects = new HashMap<>();

            for (String bucketName : buckets) {
                try {
                    List<Map<String, Object>> objects = listAllObjectsInBucket(bucketName);
                    bucketObjectCounts.put(bucketName, objects.size());
                    bucketObjects.put(bucketName, objects);
                } catch (Exception e) {
                    bucketObjectCounts.put(bucketName, -1); // Error indicator
                    bucketObjects.put(bucketName, new ArrayList<>());
                }
            }

            debugInfo.put("bucketObjectCounts", bucketObjectCounts);
            debugInfo.put("bucketObjects", bucketObjects);
            debugInfo.put("autoCreateBucket", autoCreateBucket);
            debugInfo.put("s3ClientType", s3Client.getClass().getSimpleName());
            debugInfo.put("generatedAt", LocalDateTime.now().toString());

            return debugInfo;

        } catch (Exception e) {
            System.err.println("❌ Failed to get storage debug info: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("error", e.getMessage());
            errorInfo.put("errorType", e.getClass().getSimpleName());
            return errorInfo;
        }
    }

    /**
     * Debug method to test connectivity and configuration
     */
    public Map<String, Object> testConnectivity() {
        Map<String, Object> result = new HashMap<>();
        try {
            System.out.println("🔌 Testing MinIO connectivity and configuration...");

            // Test 1: Basic S3Client initialization
            result.put("s3ClientInitialized", s3Client != null);
            result.put("s3ClientType", s3Client != null ? s3Client.getClass().getSimpleName() : "null");

            // Test 2: Try to get service configuration (this tests basic connectivity)
            try {
                System.out.println("📞 Attempting to contact MinIO service...");
                ListBucketsResponse response = s3Client.listBuckets();
                result.put("connectivityTest", "SUCCESS");
                result.put("bucketCount", response.buckets().size());
                result.put("owner", response.owner() != null ? response.owner().displayName() : "unknown");
            } catch (Exception connectError) {
                result.put("connectivityTest", "FAILED");
                result.put("connectivityError", connectError.getMessage());
                result.put("connectivityErrorType", connectError.getClass().getSimpleName());
                System.err.println("❌ Connectivity test failed: " + connectError.getMessage());

                // Additional error analysis
                if (connectError.getMessage() != null) {
                    String errorMsg = connectError.getMessage().toLowerCase();
                    if (errorMsg.contains("timeout") || errorMsg.contains("connection")) {
                        result.put("likelyIssue", "Network connectivity - MinIO endpoint unreachable from Render");
                    } else if (errorMsg.contains("access") || errorMsg.contains("auth") || errorMsg.contains("credential")) {
                        result.put("likelyIssue", "Authentication - Invalid MinIO credentials");
                    } else if (errorMsg.contains("ssl") || errorMsg.contains("certificate")) {
                        result.put("likelyIssue", "SSL/TLS certificate issues");
                    } else {
                        result.put("likelyIssue", "Unknown MinIO service error");
                    }
                }
            }

            // Test 3: Configuration validation
            result.put("autoCreateBucket", autoCreateBucket);

            return result;

        } catch (Exception e) {
            System.err.println("❌ Connectivity test completely failed: " + e.getMessage());
            e.printStackTrace();
            result.put("testResult", "COMPLETE_FAILURE");
            result.put("error", e.getMessage());
            result.put("errorType", e.getClass().getSimpleName());
            return result;
        }
    }

    /**
     * Debug method to test bucket creation
     */
    public Map<String, Object> testBucketCreation(String bucketName) {
        Map<String, Object> result = new HashMap<>();
        try {
            System.out.println("🧪 Testing bucket creation for: " + bucketName);

            // Check if bucket exists first
            boolean existsBefore = false;
            try {
                HeadBucketRequest headRequest = HeadBucketRequest.builder()
                        .bucket(bucketName)
                        .build();
                s3Client.headBucket(headRequest);
                existsBefore = true;
                System.out.println("✅ Bucket already exists: " + bucketName);
            } catch (NoSuchBucketException e) {
                System.out.println("📦 Bucket does not exist, will create: " + bucketName);
            }

            result.put("bucketName", bucketName);
            result.put("existedBefore", existsBefore);

            // Try to create bucket
            if (!existsBefore) {
                ensureBucketExists(bucketName);

                // Check if it exists now
                try {
                    HeadBucketRequest headRequest = HeadBucketRequest.builder()
                            .bucket(bucketName)
                            .build();
                    s3Client.headBucket(headRequest);
                    result.put("createdSuccessfully", true);
                    System.out.println("✅ Bucket created successfully: " + bucketName);
                } catch (NoSuchBucketException e) {
                    result.put("createdSuccessfully", false);
                    result.put("error", "Bucket creation appeared to succeed but bucket still not found");
                    System.err.println("❌ Bucket creation failed - bucket still not found: " + bucketName);
                }
            } else {
                result.put("createdSuccessfully", true);
                result.put("alreadyExisted", true);
            }

        } catch (Exception e) {
            result.put("createdSuccessfully", false);
            result.put("error", e.getMessage());
            result.put("errorType", e.getClass().getSimpleName());
            System.err.println("❌ Bucket creation test failed for " + bucketName + ": " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }
}