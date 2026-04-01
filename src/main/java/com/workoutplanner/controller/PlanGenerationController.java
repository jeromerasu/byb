package com.workoutplanner.controller;

import com.workoutplanner.dto.PlanGenerateTriggerRequest;
import com.workoutplanner.model.GeneratedBy;
import com.workoutplanner.model.PlanGenerationQueue;
import com.workoutplanner.model.QueueStatus;
import com.workoutplanner.repository.PlanGenerationQueueRepository;
import com.workoutplanner.service.SubscriptionAccessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Worker trigger endpoint — closes TASK-BE-016A gap.
 *
 * POST /api/v1/plans/generate
 * Inserts a PENDING queue entry for immediate plan generation.
 *
 * Auth: coach (for their assigned clients) or the authenticated user themselves.
 *
 * TODO(PROD-HARDEN): Tighten coach auth check before production cutover.
 *   Currently the endpoint validates tier access but does not fully verify
 *   the coach ↔ user assignment relationship.
 *   Owner: backend team. Target: Phase 2 coach portal delivery.
 */
@RestController
@RequestMapping("/api/v1/plans")
public class PlanGenerationController {

    private static final Logger log = LoggerFactory.getLogger(PlanGenerationController.class);

    private final PlanGenerationQueueRepository queueRepository;
    private final SubscriptionAccessService subscriptionAccessService;

    public PlanGenerationController(PlanGenerationQueueRepository queueRepository,
                                    SubscriptionAccessService subscriptionAccessService) {
        this.queueRepository = queueRepository;
        this.subscriptionAccessService = subscriptionAccessService;
    }

    /**
     * POST /api/v1/plans/generate
     * Inserts a PENDING queue entry for the target user.
     *
     * During current testing phase the endpoint is broadly accessible.
     * TODO(PROD-HARDEN): add strict coach-to-client assignment check.
     */
    @PostMapping("/generate")
    public ResponseEntity<?> triggerGeneration(
            @RequestBody PlanGenerateTriggerRequest request,
            @RequestHeader(value = "X-Coach-Id", required = false) String coachId,
            Authentication authentication) {

        String targetUserId = request.getUserId();
        if (targetUserId == null || targetUserId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "user_id is required"));
        }

        // Determine if caller is a coach or the user themselves
        GeneratedBy generatedBy = (coachId != null && !coachId.isBlank()) ? GeneratedBy.COACH : GeneratedBy.MANUAL;

        log.info("plans.generate.trigger userId={} generatedBy={}", targetUserId, generatedBy);

        // Tier enforcement: FREE users may not trigger generation
        try {
            subscriptionAccessService.assertCanGeneratePlan(targetUserId);
        } catch (SubscriptionAccessService.AccessDeniedException e) {
            log.info("plans.generate.blocked userId={} reason=free_tier", targetUserId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }

        // Insert PENDING queue entry
        PlanGenerationQueue entry = new PlanGenerationQueue();
        entry.setUserId(targetUserId);
        entry.setStatus(QueueStatus.PENDING);
        entry.setGeneratedBy(generatedBy);

        PlanGenerationQueue saved = queueRepository.save(entry);
        log.info("plans.generate.queued queueId={} userId={} generatedBy={}", saved.getId(), targetUserId, generatedBy);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "status", "queued",
                "queue_id", saved.getId(),
                "user_id", targetUserId,
                "generated_by", generatedBy.name()
        ));
    }
}
