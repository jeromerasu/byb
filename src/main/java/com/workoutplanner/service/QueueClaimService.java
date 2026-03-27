package com.workoutplanner.service;

import com.workoutplanner.model.PlanGenerationQueue;
import com.workoutplanner.model.QueueStatus;
import com.workoutplanner.repository.PlanGenerationQueueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * TASK-BE-016A: Atomic queue claim, lock, and batching service.
 *
 * Responsibilities:
 * - Claim a single PENDING row atomically (PENDING → CLAIMED) using optimistic
 *   read + conditional update in a SERIALIZABLE transaction per entry.
 * - Batch-claim up to N rows from a candidate list.
 * - Recover stale CLAIMED locks (CLAIMED → PENDING reset for retry eligibility).
 * - All transitions are logged with structured SLF4J fields.
 */
@Service
public class QueueClaimService {

    private static final Logger log = LoggerFactory.getLogger(QueueClaimService.class);

    private final PlanGenerationQueueRepository queueRepository;

    @Value("${queue.worker.id:#{T(java.util.UUID).randomUUID().toString()}}")
    private String workerId;

    public QueueClaimService(PlanGenerationQueueRepository queueRepository) {
        this.queueRepository = queueRepository;
    }

    /**
     * Attempt to claim a single PENDING entry.
     * Uses SERIALIZABLE isolation to prevent double-claim under concurrent workers.
     *
     * @param entryId ID of the PENDING queue entry to claim
     * @return the claimed entry, or empty if already claimed/missing
     */
    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
    public Optional<PlanGenerationQueue> claimEntry(String entryId) {
        Optional<PlanGenerationQueue> opt = queueRepository.findById(entryId);
        if (opt.isEmpty()) {
            log.warn("queue.claim.not_found id={}", entryId);
            return Optional.empty();
        }

        PlanGenerationQueue entry = opt.get();

        if (entry.getStatus() != QueueStatus.PENDING) {
            log.info("queue.claim.skip id={} status={} reason=not_pending", entryId, entry.getStatus());
            return Optional.empty();
        }

        String lockId = workerId + ":" + UUID.randomUUID();
        entry.setStatus(QueueStatus.CLAIMED);
        entry.setLockedBy(lockId);
        entry.setLockedAt(LocalDateTime.now());
        entry.setAttemptCount(entry.getAttemptCount() + 1);

        PlanGenerationQueue saved = queueRepository.save(entry);
        log.info("queue.claim.success id={} userId={} lockId={} attempt={}",
                saved.getId(), saved.getUserId(), lockId, saved.getAttemptCount());
        return Optional.of(saved);
    }

    /**
     * Batch-claim up to batchSize entries from a list of candidate IDs.
     * Each claim is an independent REQUIRES_NEW transaction so one failure
     * does not roll back the others.
     *
     * @param candidateIds ordered list of PENDING entry IDs to attempt
     * @param batchSize    maximum claims per batch
     * @return list of successfully claimed entries
     */
    public List<PlanGenerationQueue> claimBatch(List<String> candidateIds, int batchSize) {
        List<PlanGenerationQueue> claimed = new ArrayList<>();
        for (String id : candidateIds) {
            if (claimed.size() >= batchSize) break;
            claimEntry(id).ifPresent(claimed::add);
        }
        log.info("queue.claim_batch.done attempted={} claimed={}", candidateIds.size(), claimed.size());
        return claimed;
    }

    /**
     * Recover a stale CLAIMED entry by resetting it to PENDING so the scanner
     * can re-discover and re-claim it on the next tick.
     *
     * @param entry the stale CLAIMED queue entry
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recoverStaleLock(PlanGenerationQueue entry) {
        if (entry.getStatus() != QueueStatus.CLAIMED) {
            log.warn("queue.recover.skip id={} status={} reason=not_claimed", entry.getId(), entry.getStatus());
            return;
        }

        String oldLock = entry.getLockedBy();
        entry.setStatus(QueueStatus.PENDING);
        entry.setLockedBy(null);
        entry.setLockedAt(null);
        // Do not decrement attemptCount — the attempt was consumed
        queueRepository.save(entry);

        log.warn("queue.recover.stale_lock id={} userId={} oldLock={} attempt={}",
                entry.getId(), entry.getUserId(), oldLock, entry.getAttemptCount());
    }

    /**
     * Mark a claimed entry as FAILED (terminal state, no more retries).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(PlanGenerationQueue entry, String errorMessage) {
        entry.setStatus(QueueStatus.FAILED);
        entry.setFailedAt(LocalDateTime.now());
        entry.setErrorMessage(errorMessage);
        entry.setLockedBy(null);
        entry.setLockedAt(null);
        queueRepository.save(entry);
        log.error("queue.failed id={} userId={} attempt={} error={}",
                entry.getId(), entry.getUserId(), entry.getAttemptCount(), errorMessage);
    }

    /**
     * Mark a claimed entry as COMPLETED (terminal success state).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(PlanGenerationQueue entry, String workoutKey, String dietKey) {
        entry.setStatus(QueueStatus.COMPLETED);
        entry.setCompletedAt(LocalDateTime.now());
        entry.setWorkoutStorageKey(workoutKey);
        entry.setDietStorageKey(dietKey);
        entry.setLockedBy(null);
        entry.setLockedAt(null);
        queueRepository.save(entry);
        log.info("queue.completed id={} userId={} workoutKey={} dietKey={}",
                entry.getId(), entry.getUserId(), workoutKey, dietKey);
    }

    /**
     * Reset a CLAIMED entry back to PENDING for retry with updated scheduledAt.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resetForRetry(PlanGenerationQueue entry, LocalDateTime nextScheduledAt) {
        entry.setStatus(QueueStatus.PENDING);
        entry.setLockedBy(null);
        entry.setLockedAt(null);
        entry.setScheduledAt(nextScheduledAt);
        queueRepository.save(entry);
        log.info("queue.retry.scheduled id={} userId={} nextScheduledAt={} attempt={}",
                entry.getId(), entry.getUserId(), nextScheduledAt, entry.getAttemptCount());
    }
}
