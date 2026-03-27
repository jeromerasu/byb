package com.workoutplanner.controller;

import com.workoutplanner.model.PlanGenerationQueue;
import com.workoutplanner.model.QueueStatus;
import com.workoutplanner.repository.PlanGenerationQueueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TASK-BE-016E: Unit tests for QueueMetricsController.
 */
@ExtendWith(MockitoExtension.class)
class QueueMetricsControllerTest {

    private static final Logger log = LoggerFactory.getLogger(QueueMetricsControllerTest.class);

    @Mock
    private PlanGenerationQueueRepository queueRepository;

    @InjectMocks
    private QueueMetricsController controller;

    // -- getMetrics() ----------------------------------------------------

    @Test
    void getMetrics_ShouldReturn200WithCounts() {
        when(queueRepository.countByStatus(QueueStatus.PENDING)).thenReturn(3L);
        when(queueRepository.countByStatus(QueueStatus.CLAIMED)).thenReturn(1L);
        when(queueRepository.countByStatus(QueueStatus.COMPLETED)).thenReturn(10L);
        when(queueRepository.countByStatus(QueueStatus.FAILED)).thenReturn(2L);
        when(queueRepository.countByStatusAndCreatedAtAfter(eq(QueueStatus.COMPLETED), any())).thenReturn(5L);
        when(queueRepository.findFirstByStatusOrderByScheduledAtAsc(QueueStatus.PENDING)).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = controller.getMetrics();

        log.info("test.getMetrics.basic status={}", response.getStatusCode());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void getMetrics_ShouldContainCountsMap() {
        when(queueRepository.countByStatus(QueueStatus.PENDING)).thenReturn(4L);
        when(queueRepository.countByStatus(QueueStatus.CLAIMED)).thenReturn(2L);
        when(queueRepository.countByStatus(QueueStatus.COMPLETED)).thenReturn(20L);
        when(queueRepository.countByStatus(QueueStatus.FAILED)).thenReturn(3L);
        when(queueRepository.countByStatusAndCreatedAtAfter(any(), any())).thenReturn(7L);
        when(queueRepository.findFirstByStatusOrderByScheduledAtAsc(any())).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = controller.getMetrics();
        Map<String, Object> body = response.getBody();
        Map<?, ?> counts = (Map<?, ?>) body.get("counts");

        log.info("test.getMetrics.counts pending={} claimed={} completed={} failed={}",
                counts.get("pending"), counts.get("claimed"), counts.get("completed"), counts.get("failed"));

        assertEquals(4L, counts.get("pending"));
        assertEquals(2L, counts.get("claimed"));
        assertEquals(20L, counts.get("completed"));
        assertEquals(3L, counts.get("failed"));
        assertEquals(29L, counts.get("total"));
    }

    @Test
    void getMetrics_ShouldContainCompletedLast24h() {
        when(queueRepository.countByStatus(any())).thenReturn(0L);
        when(queueRepository.countByStatusAndCreatedAtAfter(eq(QueueStatus.COMPLETED), any())).thenReturn(12L);
        when(queueRepository.findFirstByStatusOrderByScheduledAtAsc(any())).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = controller.getMetrics();
        Map<String, Object> body = response.getBody();

        log.info("test.getMetrics.last24h completedLast24h={}", body.get("completedLast24h"));
        assertEquals(12L, body.get("completedLast24h"));
    }

    @Test
    void getMetrics_WithOldestPending_ShouldIncludeOldestInfo() {
        PlanGenerationQueue oldest = new PlanGenerationQueue();
        oldest.setId("oldest-id");
        oldest.setUserId("user-oldest");
        oldest.setScheduledAt(LocalDateTime.now().minusHours(3));
        oldest.setAttemptCount(0);

        when(queueRepository.countByStatus(any())).thenReturn(0L);
        when(queueRepository.countByStatusAndCreatedAtAfter(any(), any())).thenReturn(0L);
        when(queueRepository.findFirstByStatusOrderByScheduledAtAsc(QueueStatus.PENDING))
                .thenReturn(Optional.of(oldest));

        ResponseEntity<Map<String, Object>> response = controller.getMetrics();
        Map<String, Object> body = response.getBody();
        Map<?, ?> oldestInfo = (Map<?, ?>) body.get("oldestPending");

        log.info("test.getMetrics.oldestPending id={} userId={}", oldestInfo.get("id"), oldestInfo.get("userId"));
        assertNotNull(oldestInfo);
        assertEquals("oldest-id", oldestInfo.get("id"));
        assertEquals("user-oldest", oldestInfo.get("userId"));
        assertEquals(0, oldestInfo.get("attemptCount"));
        assertNotNull(oldestInfo.get("scheduledAt"));
    }

    @Test
    void getMetrics_NoPendingEntries_ShouldHaveNullOldestPending() {
        when(queueRepository.countByStatus(any())).thenReturn(0L);
        when(queueRepository.countByStatusAndCreatedAtAfter(any(), any())).thenReturn(0L);
        when(queueRepository.findFirstByStatusOrderByScheduledAtAsc(any())).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = controller.getMetrics();
        Map<String, Object> body = response.getBody();

        log.info("test.getMetrics.noOldestPending oldestPending={}", body.get("oldestPending"));
        assertNull(body.get("oldestPending"));
    }

    @Test
    void getMetrics_ShouldContainGeneratedAtTimestamp() {
        when(queueRepository.countByStatus(any())).thenReturn(0L);
        when(queueRepository.countByStatusAndCreatedAtAfter(any(), any())).thenReturn(0L);
        when(queueRepository.findFirstByStatusOrderByScheduledAtAsc(any())).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = controller.getMetrics();
        Map<String, Object> body = response.getBody();

        log.info("test.getMetrics.generatedAt={}", body.get("generatedAt"));
        assertNotNull(body.get("generatedAt"));
        assertTrue(body.get("generatedAt") instanceof String);
    }

    @Test
    void getMetrics_TotalCount_ShouldBeSumOfAllStatuses() {
        when(queueRepository.countByStatus(QueueStatus.PENDING)).thenReturn(1L);
        when(queueRepository.countByStatus(QueueStatus.CLAIMED)).thenReturn(2L);
        when(queueRepository.countByStatus(QueueStatus.COMPLETED)).thenReturn(3L);
        when(queueRepository.countByStatus(QueueStatus.FAILED)).thenReturn(4L);
        when(queueRepository.countByStatusAndCreatedAtAfter(any(), any())).thenReturn(0L);
        when(queueRepository.findFirstByStatusOrderByScheduledAtAsc(any())).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = controller.getMetrics();
        Map<?, ?> counts = (Map<?, ?>) response.getBody().get("counts");

        assertEquals(10L, counts.get("total"));
    }

    @Test
    void getMetrics_OldestPendingWithNullScheduledAt_ShouldHandleGracefully() {
        PlanGenerationQueue oldest = new PlanGenerationQueue();
        oldest.setId("no-scheduled");
        oldest.setUserId("user-x");
        oldest.setScheduledAt(null);
        oldest.setAttemptCount(0);

        when(queueRepository.countByStatus(any())).thenReturn(0L);
        when(queueRepository.countByStatusAndCreatedAtAfter(any(), any())).thenReturn(0L);
        when(queueRepository.findFirstByStatusOrderByScheduledAtAsc(QueueStatus.PENDING))
                .thenReturn(Optional.of(oldest));

        ResponseEntity<Map<String, Object>> response = controller.getMetrics();
        Map<?, ?> oldestInfo = (Map<?, ?>) response.getBody().get("oldestPending");

        log.info("test.getMetrics.nullScheduledAt scheduledAt={}", oldestInfo.get("scheduledAt"));
        assertNull(oldestInfo.get("scheduledAt"));
    }
}
