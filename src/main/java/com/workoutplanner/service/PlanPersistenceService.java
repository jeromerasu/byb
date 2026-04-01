package com.workoutplanner.service;

import com.workoutplanner.model.DietProfile;
import com.workoutplanner.model.GeneratedBy;
import com.workoutplanner.model.PlanGenerationQueue;
import com.workoutplanner.model.WorkoutProfile;
import com.workoutplanner.repository.DietProfileRepository;
import com.workoutplanner.repository.WorkoutProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * TASK-BE-016C: Persist generated plans to storage and update the
 * workout/diet profile week registry (currentPlanStorageKey).
 *
 * Responsibilities:
 * - Store workout and diet plan maps via StorageService
 * - Update WorkoutProfile.currentPlanStorageKey / DietProfile.currentPlanStorageKey
 * - Return storage keys so QueueClaimService.markCompleted() can stamp them on the queue entry
 */
@Service
public class PlanPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(PlanPersistenceService.class);

    private final StorageService storageService;
    private final WorkoutProfileRepository workoutProfileRepository;
    private final DietProfileRepository dietProfileRepository;
    private final UserWeekPlanService userWeekPlanService;

    @Value("${beta.mode:false}")
    private boolean betaMode;

    public PlanPersistenceService(StorageService storageService,
                                  WorkoutProfileRepository workoutProfileRepository,
                                  DietProfileRepository dietProfileRepository,
                                  UserWeekPlanService userWeekPlanService) {
        this.storageService = storageService;
        this.workoutProfileRepository = workoutProfileRepository;
        this.dietProfileRepository = dietProfileRepository;
        this.userWeekPlanService = userWeekPlanService;
    }

    /**
     * Persist both plans and update profile week registry.
     *
     * @param entry       the CLAIMED queue entry (for userId)
     * @param workoutPlan raw workout plan map from OpenAI
     * @param dietPlan    raw diet plan map from OpenAI
     * @return PersistenceResult with both storage keys
     */
    @Transactional
    public PersistenceResult persist(PlanGenerationQueue entry,
                                     Map<String, Object> workoutPlan,
                                     Map<String, Object> dietPlan) {
        String userId = entry.getUserId();
        LocalDateTime now = LocalDateTime.now();
        String workoutBucket = betaMode ? "workoutbeta" : "workout";
        String dietBucket = betaMode ? "dietbeta" : "diet";
        String planTitle = "AI Plan - " + now.toLocalDate();

        log.info("queue.persist.start id={} userId={} workoutBucket={} dietBucket={}",
                entry.getId(), userId, workoutBucket, dietBucket);

        String workoutKey = storeWorkoutPlan(workoutBucket, userId, planTitle, workoutPlan, entry.getId());
        String dietKey = storeDietPlan(dietBucket, userId, planTitle, dietPlan, entry.getId());

        updateWorkoutProfile(userId, workoutKey, planTitle, now, entry.getId());
        updateDietProfile(userId, dietKey, planTitle, now, entry.getId());

        // Write/upsert user_week_plan registry row (TASK-COACHING-001 / closes 016C gap)
        GeneratedBy generatedBy = entry.getGeneratedBy() != null ? entry.getGeneratedBy() : GeneratedBy.MANUAL;
        userWeekPlanService.upsert(userId, now.toLocalDate(), workoutKey, dietKey, generatedBy);

        log.info("queue.persist.done id={} userId={} workoutKey={} dietKey={}",
                entry.getId(), userId, workoutKey, dietKey);

        return new PersistenceResult(workoutKey, dietKey);
    }

    private String storeWorkoutPlan(String bucket, String userId, String title,
                                    Map<String, Object> plan, String entryId) {
        try {
            String key = storageService.storeWorkoutPlan(bucket, userId, title, plan);
            log.info("queue.persist.workout_stored id={} key={}", entryId, key);
            return key;
        } catch (Exception ex) {
            log.error("queue.persist.workout_error id={} error={}", entryId, ex.getMessage());
            throw new RuntimeException("Failed to store workout plan: " + ex.getMessage(), ex);
        }
    }

    private String storeDietPlan(String bucket, String userId, String title,
                                 Map<String, Object> plan, String entryId) {
        try {
            String key = storageService.storeDietPlan(bucket, userId, title, plan);
            log.info("queue.persist.diet_stored id={} key={}", entryId, key);
            return key;
        } catch (Exception ex) {
            log.error("queue.persist.diet_error id={} error={}", entryId, ex.getMessage());
            throw new RuntimeException("Failed to store diet plan: " + ex.getMessage(), ex);
        }
    }

    private void updateWorkoutProfile(String userId, String storageKey, String planTitle,
                                      LocalDateTime now, String entryId) {
        Optional<WorkoutProfile> opt = workoutProfileRepository.findByUserId(userId);
        if (opt.isEmpty()) {
            log.warn("queue.persist.no_workout_profile id={} userId={}", entryId, userId);
            return;
        }
        WorkoutProfile profile = opt.get();
        profile.setCurrentPlanStorageKey(storageKey);
        profile.setCurrentPlanTitle(planTitle);
        profile.setCurrentPlanCreatedAt(now);
        profile.setUpdatedAt(now);
        workoutProfileRepository.save(profile);
        log.info("queue.persist.workout_profile_updated id={} userId={}", entryId, userId);
    }

    private void updateDietProfile(String userId, String storageKey, String planTitle,
                                   LocalDateTime now, String entryId) {
        Optional<DietProfile> opt = dietProfileRepository.findByUserId(userId);
        if (opt.isEmpty()) {
            log.warn("queue.persist.no_diet_profile id={} userId={}", entryId, userId);
            return;
        }
        DietProfile profile = opt.get();
        profile.setCurrentPlanStorageKey(storageKey);
        profile.setCurrentPlanTitle(planTitle);
        profile.setCurrentPlanCreatedAt(now);
        profile.setUpdatedAt(now);
        dietProfileRepository.save(profile);
        log.info("queue.persist.diet_profile_updated id={} userId={}", entryId, userId);
    }

    // -------------------------------------------------------------------------
    // Result type
    // -------------------------------------------------------------------------

    public static class PersistenceResult {
        private final String workoutStorageKey;
        private final String dietStorageKey;

        public PersistenceResult(String workoutStorageKey, String dietStorageKey) {
            this.workoutStorageKey = workoutStorageKey;
            this.dietStorageKey = dietStorageKey;
        }

        public String getWorkoutStorageKey() { return workoutStorageKey; }
        public String getDietStorageKey() { return dietStorageKey; }
    }
}
