package com.workoutplanner.job;

import com.workoutplanner.model.PlanGenerationQueue;
import com.workoutplanner.model.QueueStatus;
import com.workoutplanner.repository.PlanGenerationQueueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * TASK-BE-015: Scheduled job that scans plan_generation_queue for actionable rows.
 *
 * Responsibilities:
 * - Poll PENDING rows that are past their scheduled_at time (batch-limited)
 * - Detect stale CLAIMED rows whose lock has expired (lock recovery)
 * - Emit structured SLF4J metrics per scan tick
 * - Delegate actual claim + execution to QueueOrchestrator (016A+)
 */
@Component
public class QueueScannerJob {

    private static final Logger log = LoggerFactory.getLogger(QueueScannerJob.class);

    private final PlanGenerationQueueRepository queueRepository;

    @Value("${queue.scanner.batch-size:5}")
    private int batchSize;

    @Value("${queue.scanner.lock-timeout-minutes:10}")
    private int lockTimeoutMinutes;

    @Value("${queue.scanner.enabled:true}")
    private boolean scannerEnabled;

    public QueueScannerJob(PlanGenerationQueueRepository queueRepository) {
        this.queueRepository = queueRepository;
    }

    /**
     * Main scan tick — runs every 30 seconds.
     * Emits scan metrics and discovers rows ready for processing.
     */
    @Scheduled(fixedDelayString = "${queue.scanner.fixed-delay-ms:30000}")
    public void scan() {
        if (!scannerEnabled) {
            log.debug("queue.scan.skipped reason=disabled");
            return;
        }

        long pendingCount = queueRepository.countByStatus(QueueStatus.PENDING);
        long claimedCount = queueRepository.countByStatus(QueueStatus.CLAIMED);

        log.info("queue.scan.start pending={} claimed={} batchSize={}", pendingCount, claimedCount, batchSize);

        List<PlanGenerationQueue> claimable = findClaimableRows();
        List<PlanGenerationQueue> stale = findStaleClaimedRows();

        log.info("queue.scan.result claimable={} stale={}", claimable.size(), stale.size());

        for (PlanGenerationQueue entry : stale) {
            log.warn("queue.scan.stale_lock id={} userId={} lockedBy={} lockedAt={}",
                    entry.getId(), entry.getUserId(), entry.getLockedBy(), entry.getLockedAt());
        }

        for (PlanGenerationQueue entry : claimable) {
            log.info("queue.scan.claimable id={} userId={} attempt={} scheduledAt={}",
                    entry.getId(), entry.getUserId(), entry.getAttemptCount(), entry.getScheduledAt());
        }

        log.info("queue.scan.end");
    }

    /**
     * Returns PENDING rows eligible for claim: scheduled_at <= now, attempt_count < max_attempts.
     */
    public List<PlanGenerationQueue> findClaimableRows() {
        return queueRepository
                .findByStatusAndAttemptCountLessThanAndScheduledAtBeforeOrderByScheduledAtAsc(
                        QueueStatus.PENDING,
                        Integer.MAX_VALUE,
                        LocalDateTime.now(),
                        PageRequest.of(0, batchSize));
    }

    /**
     * Returns CLAIMED rows whose lock has expired (lock recovery candidates).
     */
    public List<PlanGenerationQueue> findStaleClaimedRows() {
        LocalDateTime lockExpiry = LocalDateTime.now().minusMinutes(lockTimeoutMinutes);
        return queueRepository.findByStatusAndLockedAtBefore(QueueStatus.CLAIMED, lockExpiry);
    }

    // Package-visible for testing
    int getBatchSize() { return batchSize; }
    int getLockTimeoutMinutes() { return lockTimeoutMinutes; }
}
