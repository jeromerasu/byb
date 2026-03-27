package com.workoutplanner.job;

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
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TASK-BE-015: Unit tests for QueueScannerJob.
 */
@ExtendWith(MockitoExtension.class)
class QueueScannerJobTest {

    private static final Logger log = LoggerFactory.getLogger(QueueScannerJobTest.class);

    @Mock
    private PlanGenerationQueueRepository queueRepository;

    private QueueScannerJob scannerJob;

    @BeforeEach
    void setUp() {
        scannerJob = new QueueScannerJob(queueRepository);
        ReflectionTestUtils.setField(scannerJob, "batchSize", 5);
        ReflectionTestUtils.setField(scannerJob, "lockTimeoutMinutes", 10);
        ReflectionTestUtils.setField(scannerJob, "scannerEnabled", true);
    }

    // -- scan() ----------------------------------------------------------

    @Test
    void scan_WhenEnabled_ShouldQueryRepositoryAndLogMetrics() {
        when(queueRepository.countByStatus(QueueStatus.PENDING)).thenReturn(3L);
        when(queueRepository.countByStatus(QueueStatus.CLAIMED)).thenReturn(1L);
        when(queueRepository.findByStatusAndAttemptCountLessThanAndScheduledAtBeforeOrderByScheduledAtAsc(
                any(), anyInt(), any(), any())).thenReturn(Collections.emptyList());
        when(queueRepository.findByStatusAndLockedAtBefore(any(), any())).thenReturn(Collections.emptyList());

        scannerJob.scan();

        log.info("test.scan.basic verified counts queried");
        verify(queueRepository).countByStatus(QueueStatus.PENDING);
        verify(queueRepository).countByStatus(QueueStatus.CLAIMED);
    }

    @Test
    void scan_WhenDisabled_ShouldSkipRepositoryCalls() {
        ReflectionTestUtils.setField(scannerJob, "scannerEnabled", false);

        scannerJob.scan();

        log.info("test.scan.disabled verified no repository calls");
        verify(queueRepository, never()).countByStatus(any());
        verify(queueRepository, never()).findByStatusAndAttemptCountLessThanAndScheduledAtBeforeOrderByScheduledAtAsc(
                any(), anyInt(), any(), any());
    }

    @Test
    void scan_WithStaleLocks_ShouldLogWarnings() {
        PlanGenerationQueue staleEntry = new PlanGenerationQueue();
        staleEntry.setId("stale-id");
        staleEntry.setUserId("user-1");
        staleEntry.setLockedBy("worker-1");
        staleEntry.setLockedAt(LocalDateTime.now().minusMinutes(20));

        when(queueRepository.countByStatus(QueueStatus.PENDING)).thenReturn(0L);
        when(queueRepository.countByStatus(QueueStatus.CLAIMED)).thenReturn(1L);
        when(queueRepository.findByStatusAndAttemptCountLessThanAndScheduledAtBeforeOrderByScheduledAtAsc(
                any(), anyInt(), any(), any())).thenReturn(Collections.emptyList());
        when(queueRepository.findByStatusAndLockedAtBefore(any(), any())).thenReturn(List.of(staleEntry));

        scannerJob.scan();

        log.info("test.scan.staleLock verified stale entry logged");
        verify(queueRepository).findByStatusAndLockedAtBefore(eq(QueueStatus.CLAIMED), any(LocalDateTime.class));
    }

    @Test
    void scan_WithClaimableRows_ShouldLogClaimableEntries() {
        PlanGenerationQueue claimable = new PlanGenerationQueue();
        claimable.setId("claim-id");
        claimable.setUserId("user-2");
        claimable.setAttemptCount(0);
        claimable.setScheduledAt(LocalDateTime.now().minusMinutes(5));

        when(queueRepository.countByStatus(QueueStatus.PENDING)).thenReturn(1L);
        when(queueRepository.countByStatus(QueueStatus.CLAIMED)).thenReturn(0L);
        when(queueRepository.findByStatusAndAttemptCountLessThanAndScheduledAtBeforeOrderByScheduledAtAsc(
                any(), anyInt(), any(), any())).thenReturn(List.of(claimable));
        when(queueRepository.findByStatusAndLockedAtBefore(any(), any())).thenReturn(Collections.emptyList());

        scannerJob.scan();

        log.info("test.scan.claimable verified claimable logged");
        verify(queueRepository).findByStatusAndAttemptCountLessThanAndScheduledAtBeforeOrderByScheduledAtAsc(
                eq(QueueStatus.PENDING), eq(Integer.MAX_VALUE), any(LocalDateTime.class), any(Pageable.class));
    }

    // -- findClaimableRows() ---------------------------------------------

    @Test
    void findClaimableRows_ShouldUsePendingStatusAndBatchSize() {
        when(queueRepository.findByStatusAndAttemptCountLessThanAndScheduledAtBeforeOrderByScheduledAtAsc(
                any(), anyInt(), any(), any())).thenReturn(Collections.emptyList());

        List<PlanGenerationQueue> result = scannerJob.findClaimableRows();

        log.info("test.findClaimableRows result_count={}", result.size());
        ArgumentCaptor<Pageable> pageCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(queueRepository).findByStatusAndAttemptCountLessThanAndScheduledAtBeforeOrderByScheduledAtAsc(
                eq(QueueStatus.PENDING), eq(Integer.MAX_VALUE), any(LocalDateTime.class), pageCaptor.capture());
        assertEquals(5, pageCaptor.getValue().getPageSize());
    }

    @Test
    void findClaimableRows_ShouldReturnRepositoryResult() {
        PlanGenerationQueue entry = new PlanGenerationQueue();
        entry.setUserId("u1");
        when(queueRepository.findByStatusAndAttemptCountLessThanAndScheduledAtBeforeOrderByScheduledAtAsc(
                any(), anyInt(), any(), any())).thenReturn(List.of(entry));

        List<PlanGenerationQueue> result = scannerJob.findClaimableRows();

        assertEquals(1, result.size());
        assertEquals("u1", result.get(0).getUserId());
    }

    // -- findStaleClaimedRows() ------------------------------------------

    @Test
    void findStaleClaimedRows_ShouldQueryWithLockExpiryTime() {
        when(queueRepository.findByStatusAndLockedAtBefore(any(), any())).thenReturn(Collections.emptyList());

        List<PlanGenerationQueue> result = scannerJob.findStaleClaimedRows();

        log.info("test.findStaleClaimedRows result_count={}", result.size());
        ArgumentCaptor<LocalDateTime> timeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(queueRepository).findByStatusAndLockedAtBefore(eq(QueueStatus.CLAIMED), timeCaptor.capture());

        // The cutoff should be approximately now minus lockTimeoutMinutes
        LocalDateTime captured = timeCaptor.getValue();
        LocalDateTime expected = LocalDateTime.now().minusMinutes(10);
        // within 5 seconds of expected
        assertTrue(Math.abs(java.time.Duration.between(captured, expected).getSeconds()) < 5);
    }

    @Test
    void findStaleClaimedRows_ShouldReturnStaleEntries() {
        PlanGenerationQueue stale = new PlanGenerationQueue();
        stale.setUserId("stale-user");
        when(queueRepository.findByStatusAndLockedAtBefore(any(), any())).thenReturn(List.of(stale));

        List<PlanGenerationQueue> result = scannerJob.findStaleClaimedRows();

        assertEquals(1, result.size());
        assertEquals("stale-user", result.get(0).getUserId());
    }

    // -- Configuration accessors -----------------------------------------

    @Test
    void getBatchSize_ShouldReturnConfiguredValue() {
        assertEquals(5, scannerJob.getBatchSize());
    }

    @Test
    void getLockTimeoutMinutes_ShouldReturnConfiguredValue() {
        assertEquals(10, scannerJob.getLockTimeoutMinutes());
    }
}
