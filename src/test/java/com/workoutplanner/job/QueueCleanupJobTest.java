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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TASK-BE-016E: Unit tests for QueueCleanupJob.
 */
@ExtendWith(MockitoExtension.class)
class QueueCleanupJobTest {

    private static final Logger log = LoggerFactory.getLogger(QueueCleanupJobTest.class);

    @Mock
    private PlanGenerationQueueRepository queueRepository;

    private QueueCleanupJob cleanupJob;

    @BeforeEach
    void setUp() {
        cleanupJob = new QueueCleanupJob(queueRepository);
        ReflectionTestUtils.setField(cleanupJob, "retentionDays", 30);
        ReflectionTestUtils.setField(cleanupJob, "cleanupEnabled", true);
    }

    // -- cleanup() -------------------------------------------------------

    @Test
    void cleanup_WhenDisabled_ShouldSkipAllOperations() {
        ReflectionTestUtils.setField(cleanupJob, "cleanupEnabled", false);

        cleanupJob.cleanup();

        log.info("test.cleanup.disabled verified no repository calls");
        verify(queueRepository, never()).findByStatusInAndUpdatedAtBefore(any(), any());
        verify(queueRepository, never()).deleteAll(any());
    }

    @Test
    void cleanup_NothingExpired_ShouldNotDelete() {
        when(queueRepository.findByStatusInAndUpdatedAtBefore(any(), any()))
                .thenReturn(Collections.emptyList());

        cleanupJob.cleanup();

        log.info("test.cleanup.nothingExpired verified deleteAll not called");
        verify(queueRepository, never()).deleteAll(any());
    }

    @Test
    void cleanup_WithExpiredRows_ShouldDeleteThem() {
        PlanGenerationQueue expired1 = new PlanGenerationQueue();
        expired1.setId("exp-1");
        expired1.setStatus(QueueStatus.COMPLETED);

        PlanGenerationQueue expired2 = new PlanGenerationQueue();
        expired2.setId("exp-2");
        expired2.setStatus(QueueStatus.FAILED);

        when(queueRepository.findByStatusInAndUpdatedAtBefore(any(), any()))
                .thenReturn(List.of(expired1, expired2));

        cleanupJob.cleanup();

        log.info("test.cleanup.withExpired verified deleteAll called with 2 entries");
        verify(queueRepository).deleteAll(List.of(expired1, expired2));
    }

    @Test
    void cleanup_ShouldQueryWithCorrectTerminalStatuses() {
        when(queueRepository.findByStatusInAndUpdatedAtBefore(any(), any()))
                .thenReturn(Collections.emptyList());

        cleanupJob.cleanup();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<QueueStatus>> statusCaptor = ArgumentCaptor.forClass(List.class);
        verify(queueRepository).findByStatusInAndUpdatedAtBefore(statusCaptor.capture(), any());

        List<QueueStatus> statuses = statusCaptor.getValue();
        log.info("test.cleanup.statuses statuses={}", statuses);
        assertTrue(statuses.contains(QueueStatus.COMPLETED));
        assertTrue(statuses.contains(QueueStatus.FAILED));
        assertFalse(statuses.contains(QueueStatus.PENDING));
        assertFalse(statuses.contains(QueueStatus.CLAIMED));
    }

    @Test
    void cleanup_ShouldUseCutoffBasedOnRetentionDays() {
        when(queueRepository.findByStatusInAndUpdatedAtBefore(any(), any()))
                .thenReturn(Collections.emptyList());

        LocalDateTime before = LocalDateTime.now().minusDays(30);
        cleanupJob.cleanup();
        LocalDateTime after = LocalDateTime.now().minusDays(30);

        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(queueRepository).findByStatusInAndUpdatedAtBefore(any(), cutoffCaptor.capture());

        LocalDateTime cutoff = cutoffCaptor.getValue();
        log.info("test.cleanup.cutoff cutoff={}", cutoff);
        // cutoff should be between 30 days ago (within a few seconds)
        assertFalse(cutoff.isBefore(before.minusSeconds(5)));
        assertFalse(cutoff.isAfter(after.plusSeconds(5)));
    }

    @Test
    void cleanup_CustomRetentionDays_ShouldUseDifferentCutoff() {
        ReflectionTestUtils.setField(cleanupJob, "retentionDays", 7);
        when(queueRepository.findByStatusInAndUpdatedAtBefore(any(), any()))
                .thenReturn(Collections.emptyList());

        cleanupJob.cleanup();

        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(queueRepository).findByStatusInAndUpdatedAtBefore(any(), cutoffCaptor.capture());

        LocalDateTime cutoff = cutoffCaptor.getValue();
        LocalDateTime expectedCutoff = LocalDateTime.now().minusDays(7);
        long diffSeconds = Math.abs(java.time.Duration.between(cutoff, expectedCutoff).getSeconds());
        log.info("test.cleanup.customRetention cutoff={} diffSeconds={}", cutoff, diffSeconds);
        assertTrue(diffSeconds < 5);
    }

    @Test
    void cleanup_SingleExpiredRow_ShouldDeleteJustThatOne() {
        PlanGenerationQueue entry = new PlanGenerationQueue();
        entry.setId("single-expired");
        entry.setStatus(QueueStatus.COMPLETED);
        when(queueRepository.findByStatusInAndUpdatedAtBefore(any(), any()))
                .thenReturn(List.of(entry));

        cleanupJob.cleanup();

        ArgumentCaptor<List<PlanGenerationQueue>> deleteCaptor = ArgumentCaptor.forClass(List.class);
        verify(queueRepository).deleteAll(deleteCaptor.capture());
        assertEquals(1, deleteCaptor.getValue().size());
        assertEquals("single-expired", deleteCaptor.getValue().get(0).getId());
    }
}
