package com.workoutplanner.service;

import com.workoutplanner.model.PlanGenerationQueue;
import com.workoutplanner.model.QueueStatus;
import com.workoutplanner.repository.DietProfileRepository;
import com.workoutplanner.repository.PlanGenerationQueueRepository;
import com.workoutplanner.repository.UserRepository;
import com.workoutplanner.repository.WorkoutProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlanScanJobService {

    private static final Logger logger = LoggerFactory.getLogger(PlanScanJobService.class);

    private final UserRepository userRepository;
    private final WorkoutProfileRepository workoutProfileRepository;
    private final DietProfileRepository dietProfileRepository;
    private final PlanGenerationQueueRepository planGenerationQueueRepository;

    @Value("${plan.scan.max-age-days:7}")
    private int maxAgeDays;

    @Value("${plan.scan.enabled:true}")
    private boolean scanEnabled;

    @Autowired
    public PlanScanJobService(UserRepository userRepository,
                               WorkoutProfileRepository workoutProfileRepository,
                               DietProfileRepository dietProfileRepository,
                               PlanGenerationQueueRepository planGenerationQueueRepository) {
        this.userRepository = userRepository;
        this.workoutProfileRepository = workoutProfileRepository;
        this.dietProfileRepository = dietProfileRepository;
        this.planGenerationQueueRepository = planGenerationQueueRepository;
    }

    @Scheduled(fixedDelayString = "${plan.scan.interval-ms:300000}")
    public void runScan() {
        executeScan();
    }

    public Map<String, Object> triggerManualScan() {
        return executeScan();
    }

    private Map<String, Object> executeScan() {
        if (!scanEnabled) {
            logger.warn("Plan scan job is disabled (plan.scan.enabled=false). Skipping scan.");
            Map<String, Object> result = new HashMap<>();
            result.put("users_scanned", 0);
            result.put("skipped_no_profiles", 0);
            result.put("skipped_already_queued", 0);
            result.put("enqueued", 0);
            return result;
        }

        logger.info("Plan scan job started");

        var users = userRepository.findAll();

        int found = users.size();
        int skippedNoProfiles = 0;
        int skippedAlreadyQueued = 0;
        int enqueued = 0;

        for (var user : users) {
            String userId = user.getId();

            var workoutProfileOpt = workoutProfileRepository.findByUserId(userId);
            var dietProfileOpt = dietProfileRepository.findByUserId(userId);

            if (workoutProfileOpt.isEmpty() || dietProfileOpt.isEmpty()) {
                skippedNoProfiles++;
                continue;
            }

            var workoutProfile = workoutProfileOpt.get();
            var dietProfile = dietProfileOpt.get();

            boolean needsPlan = workoutProfile.getCurrentPlanStorageKey() == null
                    || dietProfile.getCurrentPlanStorageKey() == null
                    || (workoutProfile.getCurrentPlanCreatedAt() != null
                        && workoutProfile.getCurrentPlanCreatedAt().isBefore(LocalDateTime.now().minusDays(maxAgeDays)));

            if (!needsPlan) {
                continue;
            }

            List<PlanGenerationQueue> existing = planGenerationQueueRepository.findByUserIdAndStatusIn(
                    userId, List.of(QueueStatus.PENDING, QueueStatus.CLAIMED));

            if (!existing.isEmpty()) {
                skippedAlreadyQueued++;
                continue;
            }

            LocalDateTime now = LocalDateTime.now();
            PlanGenerationQueue queueEntry = new PlanGenerationQueue();
            queueEntry.setUserId(userId);
            queueEntry.setStatus(QueueStatus.PENDING);
            queueEntry.setScheduledAt(now);
            queueEntry.setCreatedAt(now);
            queueEntry.setUpdatedAt(now);

            planGenerationQueueRepository.save(queueEntry);
            enqueued++;
        }

        logger.info("Plan scan job complete: found={}, skipped_no_profiles={}, skipped_already_queued={}, enqueued={}",
                found, skippedNoProfiles, skippedAlreadyQueued, enqueued);

        Map<String, Object> result = new HashMap<>();
        result.put("users_scanned", found);
        result.put("skipped_no_profiles", skippedNoProfiles);
        result.put("skipped_already_queued", skippedAlreadyQueued);
        result.put("enqueued", enqueued);
        return result;
    }
}
