package com.workoutplanner.repository;

import com.workoutplanner.model.PlanGenerationQueue;
import com.workoutplanner.model.QueueStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TASK-BE-014: Integration tests for PlanGenerationQueueRepository.
 * Uses @DataJpaTest with H2 in-memory database.
 */
@DataJpaTest
@ActiveProfiles("test")
class PlanGenerationQueueRepositoryTest {

    private static final Logger log = LoggerFactory.getLogger(PlanGenerationQueueRepositoryTest.class);

    @Autowired
    private PlanGenerationQueueRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    // -- Helper ----------------------------------------------------------

    private PlanGenerationQueue buildEntry(String userId, QueueStatus status) {
        PlanGenerationQueue q = new PlanGenerationQueue();
        q.setUserId(userId);
        q.setStatus(status);
        q.setScheduledAt(LocalDateTime.now().minusMinutes(1));
        q.setCreatedAt(LocalDateTime.now());
        q.setUpdatedAt(LocalDateTime.now());
        return repository.save(q);
    }

    // -- findByUserIdAndStatusIn -----------------------------------------

    @Test
    void findByUserIdAndStatusIn_ShouldReturnMatchingRows() {
        buildEntry("user-1", QueueStatus.PENDING);
        buildEntry("user-1", QueueStatus.COMPLETED);

        List<PlanGenerationQueue> active = repository.findByUserIdAndStatusIn(
                "user-1", List.of(QueueStatus.PENDING, QueueStatus.CLAIMED));

        log.info("test.findByUserIdAndStatusIn active_count={}", active.size());
        assertEquals(1, active.size());
        assertEquals(QueueStatus.PENDING, active.get(0).getStatus());
    }

    @Test
    void findByUserIdAndStatusIn_NoMatch_ShouldReturnEmpty() {
        buildEntry("user-2", QueueStatus.COMPLETED);

        List<PlanGenerationQueue> result = repository.findByUserIdAndStatusIn(
                "user-2", List.of(QueueStatus.PENDING));

        assertTrue(result.isEmpty());
    }

    @Test
    void findByUserIdAndStatusIn_DifferentUser_ShouldNotReturn() {
        buildEntry("user-A", QueueStatus.PENDING);

        List<PlanGenerationQueue> result = repository.findByUserIdAndStatusIn(
                "user-B", List.of(QueueStatus.PENDING));

        assertTrue(result.isEmpty());
    }

    // -- findByStatusAndAttemptCountLessThanAndScheduledAtBefore --------

    @Test
    void findClaimableRows_ShouldReturnPendingRowsPastScheduledAt() {
        PlanGenerationQueue q = buildEntry("user-3", QueueStatus.PENDING);
        q.setAttemptCount(0);
        q.setScheduledAt(LocalDateTime.now().minusMinutes(5));
        repository.save(q);

        List<PlanGenerationQueue> claimable = repository
                .findByStatusAndAttemptCountLessThanAndScheduledAtBeforeOrderByScheduledAtAsc(
                        QueueStatus.PENDING, Integer.MAX_VALUE, LocalDateTime.now(), PageRequest.of(0, 10));

        log.info("test.findClaimableRows count={}", claimable.size());
        assertFalse(claimable.isEmpty());
    }

    @Test
    void findClaimableRows_FutureScheduledAt_ShouldNotReturn() {
        PlanGenerationQueue q = new PlanGenerationQueue();
        q.setUserId("user-future");
        q.setStatus(QueueStatus.PENDING);
        q.setScheduledAt(LocalDateTime.now().plusHours(1));
        q.setCreatedAt(LocalDateTime.now());
        q.setUpdatedAt(LocalDateTime.now());
        repository.save(q);

        List<PlanGenerationQueue> claimable = repository
                .findByStatusAndAttemptCountLessThanAndScheduledAtBeforeOrderByScheduledAtAsc(
                        QueueStatus.PENDING, Integer.MAX_VALUE, LocalDateTime.now(), PageRequest.of(0, 10));

        assertTrue(claimable.isEmpty());
    }

    @Test
    void findClaimableRows_BatchLimitRespected() {
        for (int i = 0; i < 5; i++) {
            PlanGenerationQueue q = new PlanGenerationQueue();
            q.setUserId("batch-user-" + i);
            q.setStatus(QueueStatus.PENDING);
            q.setScheduledAt(LocalDateTime.now().minusMinutes(1));
            q.setCreatedAt(LocalDateTime.now());
            q.setUpdatedAt(LocalDateTime.now());
            repository.save(q);
        }

        List<PlanGenerationQueue> claimable = repository
                .findByStatusAndAttemptCountLessThanAndScheduledAtBeforeOrderByScheduledAtAsc(
                        QueueStatus.PENDING, Integer.MAX_VALUE, LocalDateTime.now(), PageRequest.of(0, 3));

        log.info("test.batchLimit returned={}", claimable.size());
        assertEquals(3, claimable.size());
    }

    // -- findByStatusAndLockedAtBefore -----------------------------------

    @Test
    void findStaleClaimedRows_ShouldReturnLockedBeforeExpiry() {
        PlanGenerationQueue q = buildEntry("user-stale", QueueStatus.CLAIMED);
        q.setLockedAt(LocalDateTime.now().minusMinutes(15));
        repository.save(q);

        List<PlanGenerationQueue> stale = repository.findByStatusAndLockedAtBefore(
                QueueStatus.CLAIMED, LocalDateTime.now().minusMinutes(10));

        log.info("test.findStaleClaimedRows count={}", stale.size());
        assertEquals(1, stale.size());
    }

    @Test
    void findStaleClaimedRows_RecentLock_ShouldNotReturn() {
        PlanGenerationQueue q = buildEntry("user-fresh", QueueStatus.CLAIMED);
        q.setLockedAt(LocalDateTime.now().minusMinutes(2));
        repository.save(q);

        List<PlanGenerationQueue> stale = repository.findByStatusAndLockedAtBefore(
                QueueStatus.CLAIMED, LocalDateTime.now().minusMinutes(10));

        assertTrue(stale.isEmpty());
    }

    // -- countByStatus ---------------------------------------------------

    @Test
    void countByStatus_ShouldCountCorrectly() {
        buildEntry("u1", QueueStatus.PENDING);
        buildEntry("u2", QueueStatus.PENDING);
        buildEntry("u3", QueueStatus.COMPLETED);

        long pending = repository.countByStatus(QueueStatus.PENDING);
        long completed = repository.countByStatus(QueueStatus.COMPLETED);
        long failed = repository.countByStatus(QueueStatus.FAILED);

        log.info("test.countByStatus pending={} completed={} failed={}", pending, completed, failed);
        assertEquals(2, pending);
        assertEquals(1, completed);
        assertEquals(0, failed);
    }

    // -- countByStatusAndCreatedAtAfter ----------------------------------

    @Test
    void countByStatusAndCreatedAtAfter_ShouldReturnRecentEntries() {
        PlanGenerationQueue recent = buildEntry("user-recent", QueueStatus.COMPLETED);
        recent.setCreatedAt(LocalDateTime.now().minusHours(1));
        repository.save(recent);

        PlanGenerationQueue old = new PlanGenerationQueue();
        old.setUserId("user-old");
        old.setStatus(QueueStatus.COMPLETED);
        old.setScheduledAt(LocalDateTime.now().minusDays(2));
        old.setCreatedAt(LocalDateTime.now().minusDays(2));
        old.setUpdatedAt(LocalDateTime.now().minusDays(2));
        repository.save(old);

        long count = repository.countByStatusAndCreatedAtAfter(
                QueueStatus.COMPLETED, LocalDateTime.now().minusHours(24));

        log.info("test.countByStatusAndCreatedAtAfter count={}", count);
        assertEquals(1, count);
    }

    // -- findFirstByStatusOrderByScheduledAtAsc --------------------------

    @Test
    void findOldestPending_ShouldReturnEarliestScheduledAt() {
        PlanGenerationQueue newer = new PlanGenerationQueue();
        newer.setUserId("user-newer");
        newer.setStatus(QueueStatus.PENDING);
        newer.setScheduledAt(LocalDateTime.now().minusMinutes(5));
        newer.setCreatedAt(LocalDateTime.now());
        newer.setUpdatedAt(LocalDateTime.now());
        repository.save(newer);

        PlanGenerationQueue oldest = new PlanGenerationQueue();
        oldest.setUserId("user-oldest");
        oldest.setStatus(QueueStatus.PENDING);
        oldest.setScheduledAt(LocalDateTime.now().minusHours(2));
        oldest.setCreatedAt(LocalDateTime.now());
        oldest.setUpdatedAt(LocalDateTime.now());
        repository.save(oldest);

        Optional<PlanGenerationQueue> result =
                repository.findFirstByStatusOrderByScheduledAtAsc(QueueStatus.PENDING);

        log.info("test.findOldestPending found={}", result.isPresent());
        assertTrue(result.isPresent());
        assertEquals("user-oldest", result.get().getUserId());
    }

    @Test
    void findOldestPending_EmptyQueue_ShouldReturnEmpty() {
        Optional<PlanGenerationQueue> result =
                repository.findFirstByStatusOrderByScheduledAtAsc(QueueStatus.PENDING);
        assertTrue(result.isEmpty());
    }

    // -- findByStatusInAndUpdatedAtBefore --------------------------------

    @Test
    void findExpiredRows_ShouldReturnTerminalRowsOlderThanCutoff() {
        PlanGenerationQueue completed = new PlanGenerationQueue();
        completed.setUserId("u-completed-old");
        completed.setStatus(QueueStatus.COMPLETED);
        completed.setScheduledAt(LocalDateTime.now().minusDays(40));
        completed.setCreatedAt(LocalDateTime.now().minusDays(40));
        completed.setUpdatedAt(LocalDateTime.now().minusDays(35));
        repository.save(completed);

        PlanGenerationQueue failed = new PlanGenerationQueue();
        failed.setUserId("u-failed-old");
        failed.setStatus(QueueStatus.FAILED);
        failed.setScheduledAt(LocalDateTime.now().minusDays(31));
        failed.setCreatedAt(LocalDateTime.now().minusDays(31));
        failed.setUpdatedAt(LocalDateTime.now().minusDays(31));
        repository.save(failed);

        PlanGenerationQueue recent = buildEntry("u-completed-recent", QueueStatus.COMPLETED);

        List<PlanGenerationQueue> expired = repository.findByStatusInAndUpdatedAtBefore(
                List.of(QueueStatus.COMPLETED, QueueStatus.FAILED),
                LocalDateTime.now().minusDays(30));

        log.info("test.findExpiredRows count={}", expired.size());
        assertEquals(2, expired.size());
    }

    @Test
    void findExpiredRows_PendingNotIncluded() {
        PlanGenerationQueue pending = new PlanGenerationQueue();
        pending.setUserId("u-pending-old");
        pending.setStatus(QueueStatus.PENDING);
        pending.setScheduledAt(LocalDateTime.now().minusDays(40));
        pending.setCreatedAt(LocalDateTime.now().minusDays(40));
        pending.setUpdatedAt(LocalDateTime.now().minusDays(40));
        repository.save(pending);

        List<PlanGenerationQueue> expired = repository.findByStatusInAndUpdatedAtBefore(
                List.of(QueueStatus.COMPLETED, QueueStatus.FAILED),
                LocalDateTime.now().minusDays(30));

        assertTrue(expired.isEmpty());
    }

    // -- Persistence basics ----------------------------------------------

    @Test
    void save_ShouldPersistAndReturnWithId() {
        PlanGenerationQueue q = new PlanGenerationQueue();
        q.setUserId("persist-user");
        q.setStatus(QueueStatus.PENDING);
        q.setScheduledAt(LocalDateTime.now());
        q.setCreatedAt(LocalDateTime.now());
        q.setUpdatedAt(LocalDateTime.now());

        PlanGenerationQueue saved = repository.save(q);

        assertNotNull(saved.getId());
        assertEquals("persist-user", saved.getUserId());
    }

    @Test
    void delete_ShouldRemoveFromRepository() {
        PlanGenerationQueue q = buildEntry("user-del", QueueStatus.COMPLETED);
        String id = q.getId();

        repository.delete(q);

        assertTrue(repository.findById(id).isEmpty());
    }
}
