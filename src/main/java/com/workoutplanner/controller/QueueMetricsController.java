package com.workoutplanner.controller;

import com.workoutplanner.model.PlanGenerationQueue;
import com.workoutplanner.model.QueueStatus;
import com.workoutplanner.repository.PlanGenerationQueueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * TASK-BE-016E: Ops metrics endpoint for the plan_generation_queue.
 *
 * GET /api/v1/queue/metrics
 * Returns counts per status, 24h completion rate, and oldest pending entry age.
 */
@RestController
@RequestMapping("/api/v1/queue")
public class QueueMetricsController {

    private static final Logger log = LoggerFactory.getLogger(QueueMetricsController.class);
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final PlanGenerationQueueRepository queueRepository;

    public QueueMetricsController(PlanGenerationQueueRepository queueRepository) {
        this.queueRepository = queueRepository;
    }

    /**
     * Return live queue metrics.
     *
     * @return JSON payload with pending/claimed/completed/failed counts,
     *         completions in last 24h, and oldest pending entry metadata.
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        log.info("queue.metrics.request");

        long pending   = queueRepository.countByStatus(QueueStatus.PENDING);
        long claimed   = queueRepository.countByStatus(QueueStatus.CLAIMED);
        long completed = queueRepository.countByStatus(QueueStatus.COMPLETED);
        long failed    = queueRepository.countByStatus(QueueStatus.FAILED);

        LocalDateTime window24h = LocalDateTime.now().minusHours(24);
        long completedLast24h = queueRepository.countByStatusAndCreatedAtAfter(QueueStatus.COMPLETED, window24h);

        Optional<PlanGenerationQueue> oldestPending =
                queueRepository.findFirstByStatusOrderByScheduledAtAsc(QueueStatus.PENDING);

        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("pending", pending);
        counts.put("claimed", claimed);
        counts.put("completed", completed);
        counts.put("failed", failed);
        counts.put("total", pending + claimed + completed + failed);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("generatedAt", LocalDateTime.now().format(ISO));
        response.put("counts", counts);
        response.put("completedLast24h", completedLast24h);

        if (oldestPending.isPresent()) {
            PlanGenerationQueue oldest = oldestPending.get();
            Map<String, Object> oldestInfo = new LinkedHashMap<>();
            oldestInfo.put("id", oldest.getId());
            oldestInfo.put("userId", oldest.getUserId());
            oldestInfo.put("scheduledAt", oldest.getScheduledAt() != null
                    ? oldest.getScheduledAt().format(ISO) : null);
            oldestInfo.put("attemptCount", oldest.getAttemptCount());
            response.put("oldestPending", oldestInfo);
        } else {
            response.put("oldestPending", null);
        }

        log.info("queue.metrics.response pending={} claimed={} completed={} failed={} completedLast24h={}",
                pending, claimed, completed, failed, completedLast24h);

        return ResponseEntity.ok(response);
    }
}
