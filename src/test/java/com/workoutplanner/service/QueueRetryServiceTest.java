package com.workoutplanner.service;

import com.workoutplanner.model.PlanGenerationQueue;
import com.workoutplanner.model.QueueStatus;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TASK-BE-016D: Unit tests for QueueRetryService.
 * Covers retry/backoff/failure policy.
 */
@ExtendWith(MockitoExtension.class)
class QueueRetryServiceTest {

    private static final Logger log = LoggerFactory.getLogger(QueueRetryServiceTest.class);

    @Mock
    private QueueClaimService claimService;

    private QueueRetryService retryService;

    @BeforeEach
    void setUp() {
        retryService = new QueueRetryService(claimService);
        ReflectionTestUtils.setField(retryService, "baseDelaySeconds", 60L);
        ReflectionTestUtils.setField(retryService, "maxDelaySeconds", 3600L);
    }

    private PlanGenerationQueue makeEntry(int attemptCount, int maxAttempts) {
        PlanGenerationQueue q = new PlanGenerationQueue();
        q.setId("entry-test");
        q.setUserId("user-test");
        q.setStatus(QueueStatus.CLAIMED);
        q.setAttemptCount(attemptCount);
        q.setMaxAttempts(maxAttempts);
        return q;
    }

    private PlanGenerationExecutorService.PlanGenerationException fatalEx() {
        return new PlanGenerationExecutorService.PlanGenerationException("Fatal error", true);
    }

    private PlanGenerationExecutorService.PlanGenerationException transientEx() {
        return new PlanGenerationExecutorService.PlanGenerationException("Transient", false);
    }

    // -- handleFailure() - fatal -----------------------------------------

    @Test
    void handleFailure_FatalException_ShouldMarkFailed() {
        PlanGenerationQueue entry = makeEntry(1, 3);

        retryService.handleFailure(entry, fatalEx());

        log.info("test.handleFailure.fatal verified markFailed called");
        verify(claimService).markFailed(eq(entry), contains("FATAL:"));
        verify(claimService, never()).resetForRetry(any(), any());
    }

    @Test
    void handleFailure_FatalException_ShouldNeverRetry_RegardlessOfAttemptCount() {
        PlanGenerationQueue entry = makeEntry(0, 3);

        retryService.handleFailure(entry, fatalEx());

        verify(claimService).markFailed(any(), any());
        verify(claimService, never()).resetForRetry(any(), any());
    }

    // -- handleFailure() - exhausted retries -----------------------------

    @Test
    void handleFailure_MaxAttemptsReached_ShouldMarkFailed() {
        PlanGenerationQueue entry = makeEntry(3, 3); // at max

        retryService.handleFailure(entry, transientEx());

        log.info("test.handleFailure.exhausted verified markFailed called");
        verify(claimService).markFailed(eq(entry), contains("Max attempts reached"));
        verify(claimService, never()).resetForRetry(any(), any());
    }

    @Test
    void handleFailure_BeyondMaxAttempts_ShouldMarkFailed() {
        PlanGenerationQueue entry = makeEntry(5, 3); // beyond max

        retryService.handleFailure(entry, transientEx());

        verify(claimService).markFailed(any(), any());
    }

    // -- handleFailure() - retry with backoff ----------------------------

    @Test
    void handleFailure_TransientWithRemainingAttempts_ShouldResetForRetry() {
        PlanGenerationQueue entry = makeEntry(1, 3);

        retryService.handleFailure(entry, transientEx());

        log.info("test.handleFailure.retry verified resetForRetry called");
        verify(claimService).resetForRetry(eq(entry), any(LocalDateTime.class));
        verify(claimService, never()).markFailed(any(), any());
    }

    @Test
    void handleFailure_Attempt1_ShouldScheduleWithBaseDelay() {
        PlanGenerationQueue entry = makeEntry(1, 3);
        LocalDateTime before = LocalDateTime.now();

        retryService.handleFailure(entry, transientEx());

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(claimService).resetForRetry(eq(entry), captor.capture());
        LocalDateTime nextAt = captor.getValue();

        // delay should be 60s (base * 2^0)
        long secondsDiff = java.time.Duration.between(before.plusSeconds(55), nextAt).getSeconds();
        log.info("test.handleFailure.attempt1 nextAt={} secondsDiff={}", nextAt, secondsDiff);
        assertTrue(secondsDiff >= 0 && secondsDiff <= 10, "Expected ~60s delay but was off");
    }

    @Test
    void handleFailure_Attempt2_ShouldScheduleWithDoubleDelay() {
        PlanGenerationQueue entry = makeEntry(2, 3);
        LocalDateTime before = LocalDateTime.now();

        retryService.handleFailure(entry, transientEx());

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(claimService).resetForRetry(eq(entry), captor.capture());
        LocalDateTime nextAt = captor.getValue();

        // delay should be 120s (60 * 2^1)
        long secondsDiff = java.time.Duration.between(before.plusSeconds(115), nextAt).getSeconds();
        log.info("test.handleFailure.attempt2 nextAt={} secondsDiff={}", nextAt, secondsDiff);
        assertTrue(secondsDiff >= 0 && secondsDiff <= 10);
    }

    // -- computeBackoffSeconds() -----------------------------------------

    @Test
    void computeBackoffSeconds_Attempt1_ShouldReturn60() {
        long delay = retryService.computeBackoffSeconds(1);
        log.info("test.backoff attempt=1 delay={}", delay);
        assertEquals(60L, delay);
    }

    @Test
    void computeBackoffSeconds_Attempt2_ShouldReturn120() {
        long delay = retryService.computeBackoffSeconds(2);
        log.info("test.backoff attempt=2 delay={}", delay);
        assertEquals(120L, delay);
    }

    @Test
    void computeBackoffSeconds_Attempt3_ShouldReturn240() {
        long delay = retryService.computeBackoffSeconds(3);
        log.info("test.backoff attempt=3 delay={}", delay);
        assertEquals(240L, delay);
    }

    @Test
    void computeBackoffSeconds_LargeAttempt_ShouldCapAtMaxDelay() {
        long delay = retryService.computeBackoffSeconds(100);
        log.info("test.backoff attempt=100 delay={}", delay);
        assertEquals(3600L, delay);
    }

    @Test
    void computeBackoffSeconds_Attempt0_ShouldReturnBaseDelay() {
        long delay = retryService.computeBackoffSeconds(0);
        log.info("test.backoff attempt=0 delay={}", delay);
        assertEquals(60L, delay);
    }

    @Test
    void computeBackoffSeconds_CustomBase_ShouldComputeCorrectly() {
        ReflectionTestUtils.setField(retryService, "baseDelaySeconds", 30L);
        ReflectionTestUtils.setField(retryService, "maxDelaySeconds", 900L);

        assertEquals(30L, retryService.computeBackoffSeconds(1));
        assertEquals(60L, retryService.computeBackoffSeconds(2));
        assertEquals(120L, retryService.computeBackoffSeconds(3));
    }

    @Test
    void computeBackoffSeconds_MaxDelayCapApplied() {
        ReflectionTestUtils.setField(retryService, "baseDelaySeconds", 60L);
        ReflectionTestUtils.setField(retryService, "maxDelaySeconds", 100L);

        long delay = retryService.computeBackoffSeconds(5);
        assertEquals(100L, delay);
    }
}
