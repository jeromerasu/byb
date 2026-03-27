package com.workoutplanner.service;

import com.workoutplanner.model.PlanGenerationQueue;
import com.workoutplanner.model.QueueStatus;
import com.workoutplanner.repository.PlanGenerationQueueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TASK-BE-016A: Unit tests for QueueClaimService.
 */
@ExtendWith(MockitoExtension.class)
class QueueClaimServiceTest {

    private static final Logger log = LoggerFactory.getLogger(QueueClaimServiceTest.class);

    @Mock
    private PlanGenerationQueueRepository queueRepository;

    private QueueClaimService claimService;

    @BeforeEach
    void setUp() {
        claimService = new QueueClaimService(queueRepository);
        ReflectionTestUtils.setField(claimService, "workerId", "worker-test");
    }

    private PlanGenerationQueue makePending(String id, String userId) {
        PlanGenerationQueue q = new PlanGenerationQueue();
        q.setId(id);
        q.setUserId(userId);
        q.setStatus(QueueStatus.PENDING);
        q.setAttemptCount(0);
        q.setMaxAttempts(3);
        return q;
    }

    // -- claimEntry() ----------------------------------------------------

    @Test
    void claimEntry_PendingEntry_ShouldClaimSuccessfully() {
        PlanGenerationQueue entry = makePending("id-1", "user-1");
        when(queueRepository.findById("id-1")).thenReturn(Optional.of(entry));
        when(queueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Optional<PlanGenerationQueue> result = claimService.claimEntry("id-1");

        log.info("test.claimEntry.success present={}", result.isPresent());
        assertTrue(result.isPresent());
        assertEquals(QueueStatus.CLAIMED, result.get().getStatus());
        assertEquals(1, result.get().getAttemptCount());
        assertNotNull(result.get().getLockedBy());
        assertNotNull(result.get().getLockedAt());
    }

    @Test
    void claimEntry_NonExistentEntry_ShouldReturnEmpty() {
        when(queueRepository.findById("missing-id")).thenReturn(Optional.empty());

        Optional<PlanGenerationQueue> result = claimService.claimEntry("missing-id");

        log.info("test.claimEntry.notFound present={}", result.isPresent());
        assertFalse(result.isPresent());
        verify(queueRepository, never()).save(any());
    }

    @Test
    void claimEntry_AlreadyClaimedEntry_ShouldReturnEmpty() {
        PlanGenerationQueue entry = makePending("id-2", "user-2");
        entry.setStatus(QueueStatus.CLAIMED);
        when(queueRepository.findById("id-2")).thenReturn(Optional.of(entry));

        Optional<PlanGenerationQueue> result = claimService.claimEntry("id-2");

        assertFalse(result.isPresent());
        verify(queueRepository, never()).save(any());
    }

    @Test
    void claimEntry_CompletedEntry_ShouldReturnEmpty() {
        PlanGenerationQueue entry = makePending("id-3", "user-3");
        entry.setStatus(QueueStatus.COMPLETED);
        when(queueRepository.findById("id-3")).thenReturn(Optional.of(entry));

        Optional<PlanGenerationQueue> result = claimService.claimEntry("id-3");

        assertFalse(result.isPresent());
    }

    @Test
    void claimEntry_ShouldIncrementAttemptCount() {
        PlanGenerationQueue entry = makePending("id-4", "user-4");
        entry.setAttemptCount(1);
        when(queueRepository.findById("id-4")).thenReturn(Optional.of(entry));
        when(queueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Optional<PlanGenerationQueue> result = claimService.claimEntry("id-4");

        assertEquals(2, result.get().getAttemptCount());
    }

    @Test
    void claimEntry_LockedBy_ShouldContainWorkerId() {
        PlanGenerationQueue entry = makePending("id-5", "user-5");
        when(queueRepository.findById("id-5")).thenReturn(Optional.of(entry));
        when(queueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Optional<PlanGenerationQueue> result = claimService.claimEntry("id-5");

        assertTrue(result.get().getLockedBy().startsWith("worker-test:"));
    }

    // -- claimBatch() ----------------------------------------------------

    @Test
    void claimBatch_ShouldClaimUpToBatchSize() {
        // Stub only 3 entries since batchSize=3 stops after claiming 3
        for (int i = 1; i <= 3; i++) {
            PlanGenerationQueue q = makePending("id-" + i, "user-" + i);
            when(queueRepository.findById("id-" + i)).thenReturn(Optional.of(q));
        }
        when(queueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<String> ids = List.of("id-1", "id-2", "id-3", "id-4", "id-5");
        List<PlanGenerationQueue> claimed = claimService.claimBatch(ids, 3);

        log.info("test.claimBatch claimed={}", claimed.size());
        assertEquals(3, claimed.size());
    }

    @Test
    void claimBatch_EmptyList_ShouldReturnEmpty() {
        List<PlanGenerationQueue> claimed = claimService.claimBatch(List.of(), 5);
        assertTrue(claimed.isEmpty());
    }

    @Test
    void claimBatch_AllFail_ShouldReturnEmpty() {
        when(queueRepository.findById(any())).thenReturn(Optional.empty());

        List<PlanGenerationQueue> claimed = claimService.claimBatch(List.of("x", "y"), 5);
        assertTrue(claimed.isEmpty());
    }

    @Test
    void claimBatch_PartialClaims_ShouldReturnOnlySuccessful() {
        PlanGenerationQueue pending = makePending("good-id", "user-good");
        when(queueRepository.findById("good-id")).thenReturn(Optional.of(pending));
        when(queueRepository.findById("bad-id")).thenReturn(Optional.empty());
        when(queueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<PlanGenerationQueue> claimed = claimService.claimBatch(List.of("bad-id", "good-id"), 5);

        assertEquals(1, claimed.size());
        assertEquals("user-good", claimed.get(0).getUserId());
    }

    // -- recoverStaleLock() ----------------------------------------------

    @Test
    void recoverStaleLock_ClaimedEntry_ShouldResetToPending() {
        PlanGenerationQueue entry = makePending("stale-id", "user-stale");
        entry.setStatus(QueueStatus.CLAIMED);
        entry.setLockedBy("old-worker:lock-123");
        entry.setLockedAt(LocalDateTime.now().minusMinutes(15));
        when(queueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        claimService.recoverStaleLock(entry);

        log.info("test.recoverStaleLock verified reset to PENDING");
        assertEquals(QueueStatus.PENDING, entry.getStatus());
        assertNull(entry.getLockedBy());
        assertNull(entry.getLockedAt());
        verify(queueRepository).save(entry);
    }

    @Test
    void recoverStaleLock_NonClaimedEntry_ShouldNotSave() {
        PlanGenerationQueue entry = makePending("pending-id", "user-pending");
        entry.setStatus(QueueStatus.PENDING);

        claimService.recoverStaleLock(entry);

        verify(queueRepository, never()).save(any());
    }

    // -- markFailed() ----------------------------------------------------

    @Test
    void markFailed_ShouldSetFailedStatusAndErrorMessage() {
        PlanGenerationQueue entry = makePending("fail-id", "user-fail");
        entry.setStatus(QueueStatus.CLAIMED);
        when(queueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        claimService.markFailed(entry, "OpenAI quota exceeded");

        log.info("test.markFailed status={}", entry.getStatus());
        assertEquals(QueueStatus.FAILED, entry.getStatus());
        assertEquals("OpenAI quota exceeded", entry.getErrorMessage());
        assertNotNull(entry.getFailedAt());
        assertNull(entry.getLockedBy());
        assertNull(entry.getLockedAt());
        verify(queueRepository).save(entry);
    }

    // -- markCompleted() -------------------------------------------------

    @Test
    void markCompleted_ShouldSetCompletedStatusAndStorageKeys() {
        PlanGenerationQueue entry = makePending("comp-id", "user-comp");
        entry.setStatus(QueueStatus.CLAIMED);
        when(queueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        claimService.markCompleted(entry, "workout/key.json", "diet/key.json");

        log.info("test.markCompleted status={}", entry.getStatus());
        assertEquals(QueueStatus.COMPLETED, entry.getStatus());
        assertEquals("workout/key.json", entry.getWorkoutStorageKey());
        assertEquals("diet/key.json", entry.getDietStorageKey());
        assertNotNull(entry.getCompletedAt());
        assertNull(entry.getLockedBy());
        assertNull(entry.getLockedAt());
        verify(queueRepository).save(entry);
    }

    // -- resetForRetry() -------------------------------------------------

    @Test
    void resetForRetry_ShouldResetToPendingWithNewScheduledAt() {
        PlanGenerationQueue entry = makePending("retry-id", "user-retry");
        entry.setStatus(QueueStatus.CLAIMED);
        entry.setLockedBy("worker:lock");
        entry.setLockedAt(LocalDateTime.now().minusMinutes(1));
        when(queueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime next = LocalDateTime.now().plusMinutes(5);
        claimService.resetForRetry(entry, next);

        log.info("test.resetForRetry status={} scheduledAt={}", entry.getStatus(), entry.getScheduledAt());
        assertEquals(QueueStatus.PENDING, entry.getStatus());
        assertNull(entry.getLockedBy());
        assertNull(entry.getLockedAt());
        assertEquals(next, entry.getScheduledAt());
        verify(queueRepository).save(entry);
    }
}
