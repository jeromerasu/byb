package com.workoutplanner.service;

import com.workoutplanner.model.PlanGenerationQueue;
import com.workoutplanner.model.QueueStatus;
import com.workoutplanner.repository.PlanGenerationQueueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueueRetentionCleanupJobTest {

    @Mock
    private PlanGenerationQueueRepository repository;

    @InjectMocks
    private QueueRetentionCleanupJob job;

    @Test
    void cleanup_deletesCompletedRowsBeyondRetentionWindow() {
        ReflectionTestUtils.setField(job, "completedRetentionDays", 30);
        ReflectionTestUtils.setField(job, "failedRetentionDays", 90);

        PlanGenerationQueue staleCompleted = new PlanGenerationQueue();
        staleCompleted.setStatus(QueueStatus.COMPLETED);

        when(repository.findByStatusInAndUpdatedAtBefore(eq(List.of(QueueStatus.COMPLETED)), any()))
                .thenReturn(List.of(staleCompleted));
        when(repository.findByStatusInAndUpdatedAtBefore(eq(List.of(QueueStatus.FAILED)), any()))
                .thenReturn(List.of());

        job.cleanup();

        verify(repository).deleteAll(List.of(staleCompleted));
    }

    @Test
    void cleanup_deletesFailedRowsBeyondRetentionWindow() {
        ReflectionTestUtils.setField(job, "completedRetentionDays", 30);
        ReflectionTestUtils.setField(job, "failedRetentionDays", 90);

        PlanGenerationQueue staleFailed = new PlanGenerationQueue();
        staleFailed.setStatus(QueueStatus.FAILED);

        when(repository.findByStatusInAndUpdatedAtBefore(eq(List.of(QueueStatus.COMPLETED)), any()))
                .thenReturn(List.of());
        when(repository.findByStatusInAndUpdatedAtBefore(eq(List.of(QueueStatus.FAILED)), any()))
                .thenReturn(List.of(staleFailed));

        job.cleanup();

        verify(repository).deleteAll(List.of(staleFailed));
    }

    @Test
    void cleanup_cutoffDatesAreCorrect() {
        ReflectionTestUtils.setField(job, "completedRetentionDays", 30);
        ReflectionTestUtils.setField(job, "failedRetentionDays", 90);

        when(repository.findByStatusInAndUpdatedAtBefore(any(), any())).thenReturn(List.of());

        LocalDateTime before = LocalDateTime.now();
        job.cleanup();
        LocalDateTime after = LocalDateTime.now();

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repository, times(2)).findByStatusInAndUpdatedAtBefore(any(), captor.capture());

        List<LocalDateTime> cutoffs = captor.getAllValues();
        // First call is COMPLETED (30d), second is FAILED (90d)
        assertThat(cutoffs.get(0)).isBetween(before.minusDays(30).minusSeconds(2), after.minusDays(30).plusSeconds(2));
        assertThat(cutoffs.get(1)).isBetween(before.minusDays(90).minusSeconds(2), after.minusDays(90).plusSeconds(2));
    }

    @Test
    void cleanup_doesNotDelete_whenNothingStale() {
        ReflectionTestUtils.setField(job, "completedRetentionDays", 30);
        ReflectionTestUtils.setField(job, "failedRetentionDays", 90);

        when(repository.findByStatusInAndUpdatedAtBefore(any(), any())).thenReturn(List.of());

        job.cleanup();

        verify(repository, never()).deleteAll(anyCollection());
    }
}
