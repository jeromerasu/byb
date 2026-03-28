package com.workoutplanner.repository;

import com.workoutplanner.model.BodyMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Repository integration tests for BodyMetrics entity and trend query methods.
 */
@DataJpaTest
@ActiveProfiles("test")
class BodyMetricsRepositoryTest {

    private static final Logger log = LoggerFactory.getLogger(BodyMetricsRepositoryTest.class);

    @Autowired
    private BodyMetricsRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    // -- Helpers ---------------------------------------------------------

    private BodyMetrics buildMetrics(String userId, BigDecimal weightKg, LocalDate recordedAt) {
        BodyMetrics m = new BodyMetrics(userId, weightKg, recordedAt);
        return repository.save(m);
    }

    private BodyMetrics buildFullMetrics(String userId, BigDecimal weightKg, BigDecimal bodyFatPct,
                                          BigDecimal muscleMassKg, BigDecimal waistCm, LocalDate recordedAt) {
        BodyMetrics m = new BodyMetrics(userId, weightKg, recordedAt);
        m.setBodyFatPct(bodyFatPct);
        m.setMuscleMassKg(muscleMassKg);
        m.setWaistCm(waistCm);
        return repository.save(m);
    }

    // -- Persistence basics ---------------------------------------------

    @Test
    void save_RequiredFields_ShouldPersistAndAssignId() {
        BodyMetrics saved = buildMetrics("u1", new BigDecimal("75.50"), LocalDate.now());

        log.info("test.save_RequiredFields id={}", saved.getId());
        assertNotNull(saved.getId());
        assertEquals("u1", saved.getUserId());
        assertEquals(0, new BigDecimal("75.50").compareTo(saved.getWeightKg()));
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void save_AllOptionalFields_ShouldPersist() {
        BodyMetrics saved = buildFullMetrics("u2",
                new BigDecimal("80.00"), new BigDecimal("18.5"),
                new BigDecimal("35.00"), new BigDecimal("85.0"),
                LocalDate.now());
        saved.setNotes("Monthly check-in");
        repository.save(saved);

        BodyMetrics found = repository.findById(saved.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("18.5").compareTo(found.getBodyFatPct()));
        assertEquals(0, new BigDecimal("35.00").compareTo(found.getMuscleMassKg()));
        assertEquals(0, new BigDecimal("85.0").compareTo(found.getWaistCm()));
        assertEquals("Monthly check-in", found.getNotes());
    }

    @Test
    void save_NullableFields_ShouldBeNull() {
        BodyMetrics saved = buildMetrics("u3", new BigDecimal("70.00"), LocalDate.now());

        assertNull(saved.getBodyFatPct());
        assertNull(saved.getMuscleMassKg());
        assertNull(saved.getWaistCm());
        assertNull(saved.getNotes());
    }

    @Test
    void delete_ShouldRemoveEntry() {
        BodyMetrics saved = buildMetrics("del-user", new BigDecimal("65.00"), LocalDate.now());
        Long id = saved.getId();
        repository.delete(saved);
        assertTrue(repository.findById(id).isEmpty());
    }

    // -- findByUserId (ordered) -----------------------------------------

    @Test
    void findByUserIdOrderByRecordedAtAsc_ShouldReturnChronologicalOrder() {
        LocalDate base = LocalDate.now().minusDays(10);
        buildMetrics("trend-user", new BigDecimal("82.00"), base.plusDays(2));
        buildMetrics("trend-user", new BigDecimal("80.00"), base);
        buildMetrics("trend-user", new BigDecimal("81.00"), base.plusDays(1));

        List<BodyMetrics> result = repository.findByUserIdOrderByRecordedAtAsc("trend-user");

        log.info("test.findByUserIdAsc count={}", result.size());
        assertEquals(3, result.size());
        assertEquals(0, new BigDecimal("80.00").compareTo(result.get(0).getWeightKg()));
        assertEquals(0, new BigDecimal("81.00").compareTo(result.get(1).getWeightKg()));
        assertEquals(0, new BigDecimal("82.00").compareTo(result.get(2).getWeightKg()));
    }

    @Test
    void findByUserIdOrderByRecordedAtDesc_ShouldReturnReverseChronologicalOrder() {
        LocalDate base = LocalDate.now().minusDays(5);
        buildMetrics("desc-user", new BigDecimal("78.00"), base);
        buildMetrics("desc-user", new BigDecimal("77.00"), base.plusDays(3));

        List<BodyMetrics> result = repository.findByUserIdOrderByRecordedAtDesc("desc-user");

        assertEquals(2, result.size());
        assertEquals(0, new BigDecimal("77.00").compareTo(result.get(0).getWeightKg()));
    }

    @Test
    void findByUserId_DifferentUser_ShouldNotReturn() {
        buildMetrics("user-A", new BigDecimal("70.00"), LocalDate.now());

        List<BodyMetrics> result = repository.findByUserIdOrderByRecordedAtAsc("user-B");
        assertTrue(result.isEmpty());
    }

    // -- findByUserIdAndRecordedAtBetween -------------------------------

    @Test
    void findByUserIdAndRecordedAtBetween_ShouldReturnOnlyInRange() {
        LocalDate base = LocalDate.now().minusDays(30);
        buildMetrics("range-user", new BigDecimal("85.00"), base.minusDays(5));  // before range
        buildMetrics("range-user", new BigDecimal("84.00"), base);               // in range
        buildMetrics("range-user", new BigDecimal("83.00"), base.plusDays(15)); // in range
        buildMetrics("range-user", new BigDecimal("82.00"), base.plusDays(40)); // after range

        List<BodyMetrics> result = repository.findByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(
                "range-user", base, base.plusDays(20));

        log.info("test.findByDateRange count={}", result.size());
        assertEquals(2, result.size());
        assertEquals(0, new BigDecimal("84.00").compareTo(result.get(0).getWeightKg()));
        assertEquals(0, new BigDecimal("83.00").compareTo(result.get(1).getWeightKg()));
    }

    // -- findFirstByUserIdOrderByRecordedAtDesc -------------------------

    @Test
    void findFirstByUserIdOrderByRecordedAtDesc_ShouldReturnLatestEntry() {
        LocalDate base = LocalDate.now().minusDays(10);
        buildMetrics("latest-user", new BigDecimal("88.00"), base);
        buildMetrics("latest-user", new BigDecimal("86.00"), base.plusDays(5));
        buildMetrics("latest-user", new BigDecimal("85.00"), base.plusDays(10));

        Optional<BodyMetrics> latest = repository.findFirstByUserIdOrderByRecordedAtDesc("latest-user");

        log.info("test.findLatest found={}", latest.isPresent());
        assertTrue(latest.isPresent());
        assertEquals(0, new BigDecimal("85.00").compareTo(latest.get().getWeightKg()));
    }

    @Test
    void findFirstByUserIdOrderByRecordedAtDesc_NoEntries_ShouldReturnEmpty() {
        Optional<BodyMetrics> result = repository.findFirstByUserIdOrderByRecordedAtDesc("no-user");
        assertTrue(result.isEmpty());
    }

    // -- Trend aggregates -----------------------------------------------

    @Test
    void findMinWeightByUserId_ShouldReturnLowest() {
        buildMetrics("min-user", new BigDecimal("90.00"), LocalDate.now().minusDays(10));
        buildMetrics("min-user", new BigDecimal("85.00"), LocalDate.now().minusDays(5));
        buildMetrics("min-user", new BigDecimal("88.00"), LocalDate.now());

        Optional<BigDecimal> min = repository.findMinWeightByUserId("min-user");

        log.info("test.findMinWeight min={}", min.orElse(null));
        assertTrue(min.isPresent());
        assertEquals(0, new BigDecimal("85.00").compareTo(min.get()));
    }

    @Test
    void findMaxWeightByUserId_ShouldReturnHighest() {
        buildMetrics("max-user", new BigDecimal("75.00"), LocalDate.now().minusDays(10));
        buildMetrics("max-user", new BigDecimal("92.00"), LocalDate.now().minusDays(5));
        buildMetrics("max-user", new BigDecimal("80.00"), LocalDate.now());

        Optional<BigDecimal> max = repository.findMaxWeightByUserId("max-user");

        log.info("test.findMaxWeight max={}", max.orElse(null));
        assertTrue(max.isPresent());
        assertEquals(0, new BigDecimal("92.00").compareTo(max.get()));
    }

    @Test
    void findMinMaxWeight_NoEntries_ShouldReturnEmpty() {
        Optional<BigDecimal> min = repository.findMinWeightByUserId("empty-user");
        Optional<BigDecimal> max = repository.findMaxWeightByUserId("empty-user");
        assertTrue(min.isEmpty());
        assertTrue(max.isEmpty());
    }

    // -- countByUserId --------------------------------------------------

    @Test
    void countByUserId_ShouldReturnCorrectCount() {
        buildMetrics("count-user", new BigDecimal("70"), LocalDate.now().minusDays(2));
        buildMetrics("count-user", new BigDecimal("71"), LocalDate.now().minusDays(1));
        buildMetrics("count-user", new BigDecimal("72"), LocalDate.now());
        buildMetrics("other-user", new BigDecimal("80"), LocalDate.now());

        long count = repository.countByUserId("count-user");

        log.info("test.countByUserId count={}", count);
        assertEquals(3, count);
    }

    @Test
    void countByUserId_NoEntries_ShouldReturnZero() {
        assertEquals(0, repository.countByUserId("nonexistent-user"));
    }
}
