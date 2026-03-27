package com.workoutplanner.service;

import com.workoutplanner.model.PlanGenerationQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * TASK-BE-016D: Retry / backoff / failure policy for queue entries.
 *
 * Decision matrix for a failed execution:
 * 1. fatal=true  → immediately mark FAILED (no retry, regardless of attempts)
 * 2. attemptCount >= maxAttempts → mark FAILED (exhausted retries)
 * 3. otherwise   → reset to PENDING with exponential back-off on scheduledAt
 *
 * Back-off formula: baseDelaySeconds * 2^(attempt - 1), capped at maxDelaySeconds.
 */
@Service
public class QueueRetryService {

    private static final Logger log = LoggerFactory.getLogger(QueueRetryService.class);

    private final QueueClaimService claimService;

    @Value("${queue.retry.base-delay-seconds:60}")
    private long baseDelaySeconds;

    @Value("${queue.retry.max-delay-seconds:3600}")
    private long maxDelaySeconds;

    public QueueRetryService(QueueClaimService claimService) {
        this.claimService = claimService;
    }

    /**
     * Apply retry or failure policy after a generation error.
     *
     * @param entry     the CLAIMED queue entry that failed
     * @param exception the failure cause
     */
    public void handleFailure(PlanGenerationQueue entry,
                              PlanGenerationExecutorService.PlanGenerationException exception) {

        if (exception.isFatal()) {
            log.error("queue.retry.fatal id={} userId={} attempt={} reason=fatal error={}",
                    entry.getId(), entry.getUserId(), entry.getAttemptCount(), exception.getMessage());
            claimService.markFailed(entry, "FATAL: " + exception.getMessage());
            return;
        }

        if (entry.getAttemptCount() >= entry.getMaxAttempts()) {
            log.error("queue.retry.exhausted id={} userId={} attempt={} maxAttempts={}",
                    entry.getId(), entry.getUserId(), entry.getAttemptCount(), entry.getMaxAttempts());
            claimService.markFailed(entry, "Max attempts reached. Last error: " + exception.getMessage());
            return;
        }

        // Schedule retry with exponential back-off
        long delaySeconds = computeBackoffSeconds(entry.getAttemptCount());
        LocalDateTime nextScheduledAt = LocalDateTime.now().plusSeconds(delaySeconds);

        log.warn("queue.retry.scheduled id={} userId={} attempt={} maxAttempts={} delaySeconds={} nextScheduledAt={}",
                entry.getId(), entry.getUserId(), entry.getAttemptCount(),
                entry.getMaxAttempts(), delaySeconds, nextScheduledAt);

        claimService.resetForRetry(entry, nextScheduledAt);
    }

    /**
     * Compute back-off delay: baseDelay * 2^(attempt-1), capped at maxDelay.
     */
    long computeBackoffSeconds(int attemptCount) {
        // attemptCount is already incremented before execution, so attempt 1 → delay 60s, 2 → 120s, 3 → 240s
        long delay = baseDelaySeconds * (1L << Math.max(0, attemptCount - 1));
        return Math.min(delay, maxDelaySeconds);
    }
}
