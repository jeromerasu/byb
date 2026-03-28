package com.workoutplanner.repository;

import com.workoutplanner.model.PlanUsageTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Repository integration tests for PlanUsageTracker entity and billing-period query methods.
 */
@DataJpaTest
@ActiveProfiles("test")
class PlanUsageTrackerRepositoryTest {

    private static final Logger log = LoggerFactory.getLogger(PlanUsageTrackerRepositoryTest.class);

    @Autowired
    private PlanUsageTrackerRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    // -- Helpers ---------------------------------------------------------

    private PlanUsageTracker buildTracker(String userId, LocalDate start, LocalDate end,
                                           int plansGenerated, int maxAllowed) {
        PlanUsageTracker tracker = new PlanUsageTracker(userId, start, end, maxAllowed);
        tracker.setPlansGenerated(plansGenerated);
        return repository.save(tracker);
    }

    // -- Persistence basics ---------------------------------------------

    @Test
    void save_ShouldPersistAndAssignId() {
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        LocalDate end = start.plusMonths(1).minusDays(1);

        PlanUsageTracker saved = new PlanUsageTracker("u1", start, end, 5);
        saved = repository.save(saved);

        log.info("test.save_ShouldPersistAndAssignId id={}", saved.getId());
        assertNotNull(saved.getId());
        assertEquals("u1", saved.getUserId());
        assertEquals(0, saved.getPlansGenerated());
        assertEquals(5, saved.getMaxPlansAllowed());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void save_DefaultPlansGenerated_ShouldBeZero() {
        PlanUsageTracker tracker = new PlanUsageTracker("u2", LocalDate.now(), LocalDate.now().plusDays(30), 10);
        PlanUsageTracker saved = repository.save(tracker);

        assertEquals(0, saved.getPlansGenerated());
    }

    @Test
    void delete_ShouldRemoveEntry() {
        PlanUsageTracker saved = buildTracker("del-user", LocalDate.now(), LocalDate.now().plusDays(30), 0, 3);
        Long id = saved.getId();
        repository.delete(saved);
        assertTrue(repository.findById(id).isEmpty());
    }

    // -- findByUserId ---------------------------------------------------

    @Test
    void findByUserId_ShouldReturnAllTrackersForUser() {
        LocalDate now = LocalDate.now();
        buildTracker("user-A", now.minusMonths(2), now.minusMonths(1), 2, 5);
        buildTracker("user-A", now.minusMonths(1), now, 3, 5);
        buildTracker("user-B", now.minusMonths(1), now, 1, 5);

        List<PlanUsageTracker> result = repository.findByUserId("user-A");

        log.info("test.findByUserId count={}", result.size());
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(t -> "user-A".equals(t.getUserId())));
    }

    @Test
    void findByUserId_NoTrackers_ShouldReturnEmpty() {
        assertTrue(repository.findByUserId("no-user").isEmpty());
    }

    // -- findActiveByUserIdAndDate ---------------------------------------

    @Test
    void findActiveByUserIdAndDate_ShouldReturnMatchingPeriod() {
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        buildTracker("active-user", start, end, 1, 5);

        Optional<PlanUsageTracker> result = repository.findActiveByUserIdAndDate(
                "active-user", start.plusDays(10));

        log.info("test.findActive found={}", result.isPresent());
        assertTrue(result.isPresent());
        assertEquals("active-user", result.get().getUserId());
    }

    @Test
    void findActiveByUserIdAndDate_StartDate_ShouldBeInclusive() {
        LocalDate start = LocalDate.now();
        buildTracker("incl-user", start, start.plusDays(30), 0, 5);

        Optional<PlanUsageTracker> result = repository.findActiveByUserIdAndDate("incl-user", start);
        assertTrue(result.isPresent());
    }

    @Test
    void findActiveByUserIdAndDate_EndDate_ShouldBeInclusive() {
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(30);
        buildTracker("incl-end-user", start, end, 0, 5);

        Optional<PlanUsageTracker> result = repository.findActiveByUserIdAndDate("incl-end-user", end);
        assertTrue(result.isPresent());
    }

    @Test
    void findActiveByUserIdAndDate_OutsidePeriod_ShouldReturnEmpty() {
        LocalDate start = LocalDate.now().plusDays(5);
        LocalDate end = start.plusDays(30);
        buildTracker("future-user", start, end, 0, 5);

        Optional<PlanUsageTracker> result = repository.findActiveByUserIdAndDate(
                "future-user", LocalDate.now());

        assertTrue(result.isEmpty());
    }

    @Test
    void findActiveByUserIdAndDate_DifferentUser_ShouldNotReturn() {
        LocalDate start = LocalDate.now();
        buildTracker("user-X", start, start.plusDays(30), 0, 5);

        Optional<PlanUsageTracker> result = repository.findActiveByUserIdAndDate("user-Y", start.plusDays(5));
        assertTrue(result.isEmpty());
    }

    // -- findWithRemainingCapacityByUserId ------------------------------

    @Test
    void findWithRemainingCapacity_ShouldReturnTrackersUnderLimit() {
        LocalDate now = LocalDate.now();
        buildTracker("cap-user", now.minusMonths(2), now.minusMonths(1), 5, 5); // full
        buildTracker("cap-user", now.minusMonths(1), now, 2, 5);                 // has capacity
        buildTracker("cap-user", now, now.plusMonths(1), 0, 3);                  // has capacity

        List<PlanUsageTracker> result = repository.findWithRemainingCapacityByUserId("cap-user");

        log.info("test.findWithRemainingCapacity count={}", result.size());
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(t -> t.getPlansGenerated() < t.getMaxPlansAllowed()));
    }

    @Test
    void findWithRemainingCapacity_AllFull_ShouldReturnEmpty() {
        LocalDate now = LocalDate.now();
        buildTracker("full-user", now, now.plusDays(30), 3, 3);

        List<PlanUsageTracker> result = repository.findWithRemainingCapacityByUserId("full-user");
        assertTrue(result.isEmpty());
    }

    // -- findOverlappingByUserIdAndPeriod -------------------------------

    @Test
    void findOverlapping_ExactOverlap_ShouldReturn() {
        LocalDate start = LocalDate.now().withDayOfMonth(1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        buildTracker("overlap-user", start, end, 0, 5);

        List<PlanUsageTracker> result = repository.findOverlappingByUserIdAndPeriod(
                "overlap-user", start, end);

        log.info("test.findOverlapping count={}", result.size());
        assertEquals(1, result.size());
    }

    @Test
    void findOverlapping_PartialOverlap_ShouldReturn() {
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(30);
        buildTracker("partial-user", start, end, 0, 5);

        // Query with a range that partially overlaps
        List<PlanUsageTracker> result = repository.findOverlappingByUserIdAndPeriod(
                "partial-user", start.plusDays(15), end.plusDays(15));

        assertEquals(1, result.size());
    }

    @Test
    void findOverlapping_NoOverlap_ShouldReturnEmpty() {
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(30);
        buildTracker("no-overlap-user", start, end, 0, 5);

        // Query with a range entirely after the existing period
        List<PlanUsageTracker> result = repository.findOverlappingByUserIdAndPeriod(
                "no-overlap-user", end.plusDays(1), end.plusDays(31));

        assertTrue(result.isEmpty());
    }

    // -- preUpdate hook -------------------------------------------------

    @Test
    void preUpdate_ShouldUpdateTimestamp() throws InterruptedException {
        PlanUsageTracker saved = buildTracker("update-user", LocalDate.now(), LocalDate.now().plusDays(30), 0, 5);
        var originalUpdatedAt = saved.getUpdatedAt();

        Thread.sleep(50);
        saved.setPlansGenerated(1);
        PlanUsageTracker updated = repository.saveAndFlush(saved);

        log.info("test.preUpdate original={} updated={}", originalUpdatedAt, updated.getUpdatedAt());
        assertFalse(updated.getUpdatedAt().isBefore(originalUpdatedAt));
    }
}
