package com.workoutplanner.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminResetService {

    private static final Logger logger = LoggerFactory.getLogger(AdminResetService.class);

    // Fixed test coach credentials — matches V032 seed
    private static final String TEST_COACH_ID       = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
    private static final String TEST_COACH_NAME     = "Test Coach";
    private static final String TEST_COACH_EMAIL    = "coach@byb.app";
    private static final String TEST_COACH_BIO      = "Test coach for BYB development";
    // bcrypt hash of "coach123"
    private static final String TEST_COACH_PASSWORD_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectStorageService objectStorageService;
    private final StorageService storageService;

    @Autowired
    public AdminResetService(JdbcTemplate jdbcTemplate,
                             ObjectStorageService objectStorageService,
                             StorageService storageService) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectStorageService = objectStorageService;
        this.storageService = storageService;
    }

    /**
     * Wipes all user-generated data, clears MinIO object storage, and re-seeds the test coach.
     * Preserves exercise_catalog and food_catalog (reference data).
     */
    @Transactional
    public Map<String, Object> resetAll() {
        Map<String, Object> result = new HashMap<>();
        List<String> steps = new ArrayList<>();

        // Step 1: Wipe all user-generated database tables
        logger.warn("ADMIN RESET: Wiping all user-generated database tables");
        jdbcTemplate.execute(
                "TRUNCATE TABLE " +
                "coach_directive, coach_prompt_template, " +
                "billing_entitlements, webhook_event_log, " +
                "plan_usage_tracker, user_week_plan, " +
                "workout_log, meal_log, body_metrics, " +
                "plan_generation_queue, " +
                "workout_feedback, diet_feedback, " +
                "workout_profile, diet_profile, " +
                "users, coaches " +
                "CASCADE"
        );
        steps.add("Wiped all user-generated database tables (exercise_catalog and food_catalog preserved)");
        logger.info("ADMIN RESET: Database wipe complete");

        // Step 2: Re-seed test coach
        jdbcTemplate.update(
                "INSERT INTO coaches (id, name, email, hashed_password, bio, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                TEST_COACH_ID, TEST_COACH_NAME, TEST_COACH_EMAIL, TEST_COACH_PASSWORD_HASH, TEST_COACH_BIO
        );
        steps.add("Re-seeded test coach: " + TEST_COACH_EMAIL + " (password: coach123)");
        logger.info("ADMIN RESET: Test coach re-seeded");

        // Step 3: Clear MinIO object storage (skipped in local storage mode)
        if (storageService.isUsingLocalStorage()) {
            steps.add("Skipped MinIO clear (local storage mode active — clear ./test-storage manually if needed)");
            logger.info("ADMIN RESET: Skipping MinIO clear — using local storage");
        } else {
            Map<String, Object> storageResult = clearAllMinioObjects();
            int deleted = (int) storageResult.get("totalDeleted");
            List<?> cleared = (List<?>) storageResult.get("bucketsCleared");
            steps.add("Cleared MinIO: " + deleted + " objects deleted across " + cleared.size() + " buckets");
            result.put("storageDetail", storageResult);
            logger.info("ADMIN RESET: MinIO clear complete — {} objects deleted", deleted);
        }

        result.put("steps", steps);
        result.put("status", "ok");
        logger.warn("ADMIN RESET: Full reset complete");
        return result;
    }

    private Map<String, Object> clearAllMinioObjects() {
        int totalDeleted = 0;
        List<String> bucketsCleared = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try {
            List<String> buckets = objectStorageService.listAllBuckets();
            for (String bucket : buckets) {
                try {
                    List<Map<String, Object>> objects = objectStorageService.listAllObjectsInBucket(bucket);
                    for (Map<String, Object> obj : objects) {
                        String key = (String) obj.get("key");
                        objectStorageService.deleteObject(bucket, key);
                        totalDeleted++;
                    }
                    bucketsCleared.add(bucket + " (" + objects.size() + " objects deleted)");
                    logger.info("ADMIN RESET: Cleared {} objects from bucket '{}'", objects.size(), bucket);
                } catch (Exception e) {
                    logger.error("ADMIN RESET: Failed to clear bucket '{}': {}", bucket, e.getMessage());
                    errors.add(bucket + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("ADMIN RESET: Failed to list MinIO buckets: {}", e.getMessage());
            errors.add("listAllBuckets failed: " + e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalDeleted", totalDeleted);
        result.put("bucketsCleared", bucketsCleared);
        if (!errors.isEmpty()) {
            result.put("errors", errors);
        }
        return result;
    }
}
