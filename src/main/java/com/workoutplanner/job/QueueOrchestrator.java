package com.workoutplanner.job;

import com.workoutplanner.model.PlanGenerationQueue;
import com.workoutplanner.service.PlanGenerationExecutorService;
import com.workoutplanner.service.PlanPersistenceService;
import com.workoutplanner.service.QueueClaimService;
import com.workoutplanner.service.QueueRetryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * TASK-BE-016A/B/C/D: Orchestrator — scan → claim → execute → persist/retry pipeline.
 *
 * Each scheduled tick:
 * 1. Recover stale CLAIMED locks (016A)
 * 2. Find claimable PENDING rows (015)
 * 3. Claim a batch (016A)
 * 4. Execute each claimed entry (016B)
 * 5. On success: persist plans + mark COMPLETED (016C)
 * 6. On failure: apply retry/backoff policy (016D)
 */
@Component
public class QueueOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(QueueOrchestrator.class);

    private final QueueScannerJob scannerJob;
    private final QueueClaimService claimService;
    private final PlanGenerationExecutorService executorService;
    private final PlanPersistenceService persistenceService;
    private final QueueRetryService retryService;

    @Value("${queue.scanner.batch-size:5}")
    private int batchSize;

    @Value("${queue.scanner.enabled:true}")
    private boolean enabled;

    public QueueOrchestrator(QueueScannerJob scannerJob,
                             QueueClaimService claimService,
                             PlanGenerationExecutorService executorService,
                             PlanPersistenceService persistenceService,
                             QueueRetryService retryService) {
        this.scannerJob = scannerJob;
        this.claimService = claimService;
        this.executorService = executorService;
        this.persistenceService = persistenceService;
        this.retryService = retryService;
    }

    /**
     * Scheduled orchestration tick.
     */
    @Scheduled(fixedDelayString = "${queue.scanner.fixed-delay-ms:30000}")
    public void orchestrate() {
        if (!enabled) {
            return;
        }

        // Step 1: recover stale locks
        List<PlanGenerationQueue> stale = scannerJob.findStaleClaimedRows();
        for (PlanGenerationQueue entry : stale) {
            try {
                claimService.recoverStaleLock(entry);
            } catch (Exception ex) {
                log.error("queue.orchestrate.recover_error id={} error={}", entry.getId(), ex.getMessage(), ex);
            }
        }

        // Step 2: find claimable rows
        List<PlanGenerationQueue> claimable = scannerJob.findClaimableRows();
        if (claimable.isEmpty()) {
            log.debug("queue.orchestrate.idle");
            return;
        }

        // Step 3: claim batch
        List<String> ids = claimable.stream().map(PlanGenerationQueue::getId).collect(Collectors.toList());
        List<PlanGenerationQueue> claimed = claimService.claimBatch(ids, batchSize);

        // Step 4: execute each claimed entry
        for (PlanGenerationQueue entry : claimed) {
            log.info("queue.orchestrate.dispatching id={} userId={}", entry.getId(), entry.getUserId());
            dispatchForExecution(entry);
        }
    }

    /**
     * Dispatch: execute generation. Failure handling wired in 016D.
     */
    protected void dispatchForExecution(PlanGenerationQueue entry) {
        try {
            PlanGenerationExecutorService.GenerationResult result = executorService.execute(entry);
            onExecutionSuccess(entry, result);
        } catch (PlanGenerationExecutorService.PlanGenerationException ex) {
            onExecutionFailure(entry, ex);
        } catch (Exception ex) {
            log.error("queue.orchestrate.unexpected_error id={} error={}", entry.getId(), ex.getMessage(), ex);
            onExecutionFailure(entry, new PlanGenerationExecutorService.PlanGenerationException(
                    "Unexpected error: " + ex.getMessage(), ex));
        }
    }

    /**
     * Success hook — persists plans and marks entry COMPLETED with storage keys.
     */
    protected void onExecutionSuccess(PlanGenerationQueue entry,
                                      PlanGenerationExecutorService.GenerationResult result) {
        try {
            PlanPersistenceService.PersistenceResult persisted =
                    persistenceService.persist(entry, result.getWorkoutPlan(), result.getDietPlan());
            claimService.markCompleted(entry, persisted.getWorkoutStorageKey(), persisted.getDietStorageKey());
        } catch (Exception ex) {
            log.error("queue.orchestrate.persist_error id={} error={}", entry.getId(), ex.getMessage(), ex);
            onExecutionFailure(entry, new PlanGenerationExecutorService.PlanGenerationException(
                    "Persistence failed: " + ex.getMessage(), ex));
        }
    }

    /**
     * Failure hook — delegates to QueueRetryService (016D) for retry/backoff/fail decision.
     */
    protected void onExecutionFailure(PlanGenerationQueue entry,
                                      PlanGenerationExecutorService.PlanGenerationException ex) {
        log.error("queue.orchestrate.generation_fail id={} userId={} fatal={} attempt={} error={}",
                entry.getId(), entry.getUserId(), ex.isFatal(), entry.getAttemptCount(), ex.getMessage());
        retryService.handleFailure(entry, ex);
    }
}
