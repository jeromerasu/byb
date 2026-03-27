package com.workoutplanner.job;

import com.workoutplanner.model.PlanGenerationQueue;
import com.workoutplanner.service.QueueClaimService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * TASK-BE-016A: Orchestrator that wires QueueScannerJob (discover) →
 * QueueClaimService (claim) into a single scheduled pipeline.
 *
 * Runs on the same cadence as the scanner.  Generation execution is
 * delegated to PlanGenerationExecutorService (016B).
 */
@Component
public class QueueOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(QueueOrchestrator.class);

    private final QueueScannerJob scannerJob;
    private final QueueClaimService claimService;

    @Value("${queue.scanner.batch-size:5}")
    private int batchSize;

    @Value("${queue.scanner.enabled:true}")
    private boolean enabled;

    public QueueOrchestrator(QueueScannerJob scannerJob, QueueClaimService claimService) {
        this.scannerJob = scannerJob;
        this.claimService = claimService;
    }

    /**
     * Scheduled orchestration tick:
     * 1. Recover stale locks
     * 2. Find claimable rows
     * 3. Claim a batch
     * 4. Dispatch claimed entries for execution (wired in 016B)
     */
    @Scheduled(fixedDelayString = "${queue.scanner.fixed-delay-ms:30000}")
    public void orchestrate() {
        if (!enabled) {
            return;
        }

        // Step 1: recover stale locks so they re-enter PENDING
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

        // Step 4: dispatch (execution wired in 016B via PlanGenerationExecutorService)
        for (PlanGenerationQueue entry : claimed) {
            log.info("queue.orchestrate.dispatching id={} userId={}", entry.getId(), entry.getUserId());
            dispatchForExecution(entry);
        }
    }

    /**
     * Dispatch hook — overridden/extended in 016B when PlanGenerationExecutorService
     * is wired. Default: no-op with log.
     */
    protected void dispatchForExecution(PlanGenerationQueue entry) {
        log.info("queue.orchestrate.dispatch_pending id={} userId={} (executor not yet wired)",
                entry.getId(), entry.getUserId());
    }
}
