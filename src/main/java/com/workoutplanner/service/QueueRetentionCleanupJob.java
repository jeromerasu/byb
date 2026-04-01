package com.workoutplanner.service;

import com.workoutplanner.model.QueueStatus;
import com.workoutplanner.repository.PlanGenerationQueueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * TASK-COACHING-001: Closes TASK-BE-016E gap with per-status configurable retention.
 *
 * Different retention windows per status because:
 *   - COMPLETED rows are low-value after 30 days (plan already delivered)
 *   - FAILED rows should be kept longer (90 days) for incident investigation
 *
 * Configurable via application properties:
 *   queue.retention.completed-days (default 30)
 *   queue.retention.failed-days    (default 90)
 */
@Component
public class QueueRetentionCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(QueueRetentionCleanupJob.class);

    private final PlanGenerationQueueRepository queueRepository;

    @Value("${queue.retention.completed-days:30}")
    private int completedRetentionDays;

    @Value("${queue.retention.failed-days:90}")
    private int failedRetentionDays;

    public QueueRetentionCleanupJob(PlanGenerationQueueRepository queueRepository) {
        this.queueRepository = queueRepository;
    }

    /**
     * Run daily at 01:00 UTC (offset from QueueCleanupJob midnight run to avoid lock contention).
     */
    @Scheduled(cron = "${queue.retention.cron:0 0 1 * * *}")
    public void cleanup() {
        LocalDateTime now = LocalDateTime.now();
        log.info("queue.retention.start ts={} completed_cutoff_days={} failed_cutoff_days={}",
                now, completedRetentionDays, failedRetentionDays);

        int completedDeleted = deleteByStatus(QueueStatus.COMPLETED, completedRetentionDays, now);
        int failedDeleted = deleteByStatus(QueueStatus.FAILED, failedRetentionDays, now);

        log.info("queue.retention.done ts={} completed_deleted={} failed_deleted={}",
                now, completedDeleted, failedDeleted);
    }

    private int deleteByStatus(QueueStatus status, int retentionDays, LocalDateTime now) {
        LocalDateTime cutoff = now.minusDays(retentionDays);
        var rows = queueRepository.findByStatusInAndUpdatedAtBefore(List.of(status), cutoff);
        if (!rows.isEmpty()) {
            queueRepository.deleteAll(rows);
            log.info("queue.retention.deleted status={} count={} cutoff={}", status, rows.size(), cutoff);
        }
        return rows.size();
    }
}
