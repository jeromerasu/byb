package com.workoutplanner.job;

import com.workoutplanner.model.PlanGenerationQueue;
import com.workoutplanner.model.QueueStatus;
import com.workoutplanner.service.PlanGenerationExecutorService;
import com.workoutplanner.service.PlanPersistenceService;
import com.workoutplanner.service.QueueClaimService;
import com.workoutplanner.service.QueueRetryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TASK-BE-016A: Unit tests for QueueOrchestrator.
 */
@ExtendWith(MockitoExtension.class)
class QueueOrchestratorTest {

    private static final Logger log = LoggerFactory.getLogger(QueueOrchestratorTest.class);

    @Mock
    private QueueScannerJob scannerJob;
    @Mock
    private QueueClaimService claimService;
    @Mock
    private PlanGenerationExecutorService executorService;
    @Mock
    private PlanPersistenceService persistenceService;
    @Mock
    private QueueRetryService retryService;

    private QueueOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new QueueOrchestrator(scannerJob, claimService, executorService, persistenceService, retryService);
        ReflectionTestUtils.setField(orchestrator, "batchSize", 5);
        ReflectionTestUtils.setField(orchestrator, "enabled", true);
    }

    private PlanGenerationQueue makeEntry(String id, String userId) {
        PlanGenerationQueue q = new PlanGenerationQueue();
        q.setId(id);
        q.setUserId(userId);
        q.setStatus(QueueStatus.CLAIMED);
        q.setAttemptCount(1);
        q.setMaxAttempts(3);
        return q;
    }

    // -- orchestrate() ---------------------------------------------------

    @Test
    void orchestrate_WhenDisabled_ShouldDoNothing() {
        ReflectionTestUtils.setField(orchestrator, "enabled", false);

        orchestrator.orchestrate();

        log.info("test.orchestrate.disabled verified no interactions");
        verifyNoInteractions(scannerJob, claimService, executorService, persistenceService, retryService);
    }

    @Test
    void orchestrate_NoClaimableRows_ShouldDoNothingAfterStaleLockRecovery() {
        when(scannerJob.findStaleClaimedRows()).thenReturn(Collections.emptyList());
        when(scannerJob.findClaimableRows()).thenReturn(Collections.emptyList());

        orchestrator.orchestrate();

        log.info("test.orchestrate.idle verified no dispatch");
        verify(scannerJob).findStaleClaimedRows();
        verify(scannerJob).findClaimableRows();
        verify(claimService, never()).claimBatch(any(), anyInt());
        verifyNoInteractions(executorService);
    }

    @Test
    void orchestrate_WithStaleLocksAndNoClaimable_ShouldRecoverStaleLocks() {
        PlanGenerationQueue stale = makeEntry("stale-1", "user-stale");
        stale.setStatus(QueueStatus.CLAIMED);
        when(scannerJob.findStaleClaimedRows()).thenReturn(List.of(stale));
        when(scannerJob.findClaimableRows()).thenReturn(Collections.emptyList());

        orchestrator.orchestrate();

        log.info("test.orchestrate.staleLockRecovery verified recoverStaleLock called");
        verify(claimService).recoverStaleLock(stale);
    }

    @Test
    void orchestrate_StaleLockRecoverError_ShouldContinueProcessing() {
        PlanGenerationQueue stale = makeEntry("stale-err", "user-stale");
        when(scannerJob.findStaleClaimedRows()).thenReturn(List.of(stale));
        doThrow(new RuntimeException("DB error")).when(claimService).recoverStaleLock(stale);
        when(scannerJob.findClaimableRows()).thenReturn(Collections.emptyList());

        // Should not throw
        orchestrator.orchestrate();

        log.info("test.orchestrate.staleLockError verified continues despite error");
    }

    @Test
    void orchestrate_WithClaimableRows_ShouldClaimAndDispatch() {
        PlanGenerationQueue claimable = new PlanGenerationQueue();
        claimable.setId("claimable-1");
        claimable.setUserId("user-1");
        claimable.setStatus(QueueStatus.PENDING);

        PlanGenerationQueue claimed = makeEntry("claimable-1", "user-1");

        when(scannerJob.findStaleClaimedRows()).thenReturn(Collections.emptyList());
        when(scannerJob.findClaimableRows()).thenReturn(List.of(claimable));
        when(claimService.claimBatch(List.of("claimable-1"), 5)).thenReturn(List.of(claimed));

        PlanGenerationExecutorService.GenerationResult genResult =
                new PlanGenerationExecutorService.GenerationResult(Map.of(), Map.of());
        when(executorService.execute(claimed)).thenReturn(genResult);

        PlanPersistenceService.PersistenceResult persResult =
                new PlanPersistenceService.PersistenceResult("workout/key", "diet/key");
        when(persistenceService.persist(eq(claimed), any(), any())).thenReturn(persResult);

        orchestrator.orchestrate();

        log.info("test.orchestrate.claimAndDispatch verified full pipeline");
        verify(claimService).claimBatch(List.of("claimable-1"), 5);
        verify(executorService).execute(claimed);
        verify(persistenceService).persist(eq(claimed), any(), any());
        verify(claimService).markCompleted(claimed, "workout/key", "diet/key");
    }

    // -- dispatchForExecution() ------------------------------------------

    @Test
    void dispatchForExecution_Success_ShouldCallOnExecutionSuccess() {
        PlanGenerationQueue entry = makeEntry("e1", "u1");
        PlanGenerationExecutorService.GenerationResult genResult =
                new PlanGenerationExecutorService.GenerationResult(Map.of("key", "val"), Map.of("k", "v"));
        PlanPersistenceService.PersistenceResult persResult =
                new PlanPersistenceService.PersistenceResult("wk", "dk");

        when(executorService.execute(entry)).thenReturn(genResult);
        when(persistenceService.persist(eq(entry), any(), any())).thenReturn(persResult);

        orchestrator.dispatchForExecution(entry);

        log.info("test.dispatchForExecution.success verified completed marked");
        verify(claimService).markCompleted(entry, "wk", "dk");
        verifyNoInteractions(retryService);
    }

    @Test
    void dispatchForExecution_PlanGenerationException_ShouldCallRetryService() {
        PlanGenerationQueue entry = makeEntry("e2", "u2");
        PlanGenerationExecutorService.PlanGenerationException ex =
                new PlanGenerationExecutorService.PlanGenerationException("OpenAI failed", false);
        when(executorService.execute(entry)).thenThrow(ex);

        orchestrator.dispatchForExecution(entry);

        log.info("test.dispatchForExecution.genException verified retryService called");
        verify(retryService).handleFailure(eq(entry), any());
        verify(claimService, never()).markCompleted(any(), any(), any());
    }

    @Test
    void dispatchForExecution_UnexpectedException_ShouldCallRetryService() {
        PlanGenerationQueue entry = makeEntry("e3", "u3");
        when(executorService.execute(entry)).thenThrow(new RuntimeException("Unexpected"));

        orchestrator.dispatchForExecution(entry);

        log.info("test.dispatchForExecution.unexpected verified retryService called");
        verify(retryService).handleFailure(eq(entry), any());
    }

    @Test
    void dispatchForExecution_PersistError_ShouldCallRetryService() {
        PlanGenerationQueue entry = makeEntry("e4", "u4");
        PlanGenerationExecutorService.GenerationResult genResult =
                new PlanGenerationExecutorService.GenerationResult(Map.of(), Map.of());
        when(executorService.execute(entry)).thenReturn(genResult);
        when(persistenceService.persist(eq(entry), any(), any()))
                .thenThrow(new RuntimeException("Storage unavailable"));

        orchestrator.dispatchForExecution(entry);

        log.info("test.dispatchForExecution.persistError verified retryService called");
        verify(retryService).handleFailure(eq(entry), any());
        verify(claimService, never()).markCompleted(any(), any(), any());
    }
}
