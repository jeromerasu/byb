package com.workoutplanner.job;

import com.workoutplanner.model.PlanGenerationQueue;
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
 * TASK-BE-016E: Periodic cleanup of terminal queue rows beyond the retention window.
 *
 * Deletes COMPLETED and FAILED rows whose updated_at is older than
 * queue.cleanup.retention-days (default 30).  Runs daily at midnight.
 */
@Component
public class QueueCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(QueueCleanupJob.class);

    private final PlanGenerationQueueRepository queueRepository;

    @Value("${queue.cleanup.retention-days:30}")
    private int retentionDays;

    @Value("${queue.cleanup.enabled:true}")
    private boolean cleanupEnabled;

    public QueueCleanupJob(PlanGenerationQueueRepository queueRepository) {
        this.queueRepository = queueRepository;
    }

    /**
     * Run cleanup at midnight every day.
     */
    @Scheduled(cron = "${queue.cleanup.cron:0 0 0 * * *}")
    public void cleanup() {
        if (!cleanupEnabled) {
            log.debug("queue.cleanup.skipped reason=disabled");
            return;
        }

        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        log.info("queue.cleanup.start retentionDays={} cutoff={}", retentionDays, cutoff);

        List<PlanGenerationQueue> expired = queueRepository.findByStatusInAndUpdatedAtBefore(
                List.of(QueueStatus.COMPLETED, QueueStatus.FAILED),
                cutoff);

        if (expired.isEmpty()) {
            log.info("queue.cleanup.nothing_to_delete");
            return;
        }

        log.info("queue.cleanup.deleting count={}", expired.size());
        queueRepository.deleteAll(expired);
        log.info("queue.cleanup.done deleted={}", expired.size());
    }
}
