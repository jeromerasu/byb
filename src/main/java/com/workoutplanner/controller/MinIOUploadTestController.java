package com.workoutplanner.controller;

import com.workoutplanner.service.ObjectStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/minio-test")
public class MinIOUploadTestController {

    private static final Logger logger = LoggerFactory.getLogger(MinIOUploadTestController.class);

    @Autowired(required = false)
    private ObjectStorageService objectStorageService;

    @Value("${storage.use-local:false}")
    private boolean useLocalStorage;

    @PostMapping("/upload-dummy")
    public ResponseEntity<Map<String, Object>> testMinIOUpload() {
        Map<String, Object> response = new HashMap<>();

        try {
            if (useLocalStorage || objectStorageService == null) {
                response.put("status", "skipped");
                response.put("message", "Local storage enabled or ObjectStorageService not available - MinIO test skipped");
                response.put("use_local_storage", useLocalStorage);
                response.put("object_storage_service_available", objectStorageService != null);
                return ResponseEntity.ok(response);
            }

            // Create test data
            Map<String, Object> testPlan = new HashMap<>();
            testPlan.put("title", "MinIO Upload Test Plan");
            testPlan.put("timestamp", LocalDateTime.now().toString());
            testPlan.put("test_credentials", "Using user provided MinIO credentials");
            testPlan.put("bucket_target", "workoutbeta");

            String testUserId = "test-minio-user-123";
            String bucketName = "workoutbeta";
            String planTitle = "test-upload-" + System.currentTimeMillis();

            logger.info("Testing MinIO upload to bucket: {} for user: {}", bucketName, testUserId);

            // Attempt to store the test plan
            String storageKey = objectStorageService.storeWorkoutPlan(bucketName, testUserId, planTitle, testPlan);

            response.put("status", "success");
            response.put("message", "MinIO upload test completed successfully");
            response.put("bucket_name", bucketName);
            response.put("user_id", testUserId);
            response.put("storage_key", storageKey);
            response.put("plan_title", planTitle);
            response.put("timestamp", LocalDateTime.now().toString());

            logger.info("✅ MinIO upload test successful: {}", storageKey);

        } catch (Exception e) {
            logger.error("❌ MinIO upload test failed: {}", e.getMessage(), e);
            response.put("status", "failed");
            response.put("error", e.getMessage());
            response.put("error_class", e.getClass().getSimpleName());
            return ResponseEntity.status(500).body(response);
        }

        return ResponseEntity.ok(response);
    }
}