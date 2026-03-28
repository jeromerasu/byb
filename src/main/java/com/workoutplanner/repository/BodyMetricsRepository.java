package com.workoutplanner.repository;

import com.workoutplanner.model.BodyMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BodyMetricsRepository extends JpaRepository<BodyMetrics, Long> {

    // Chronological queries for trend analysis
    List<BodyMetrics> findByUserIdOrderByRecordedAtAsc(String userId);

    List<BodyMetrics> findByUserIdOrderByRecordedAtDesc(String userId);

    List<BodyMetrics> findByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(String userId,
                                                                            LocalDate from,
                                                                            LocalDate to);

    // Latest entry — used to display current metrics
    Optional<BodyMetrics> findFirstByUserIdOrderByRecordedAtDesc(String userId);

    // Trend aggregates — min/max weight for progress summary
    @Query("SELECT MIN(b.weightKg) FROM BodyMetrics b WHERE b.userId = :userId")
    Optional<BigDecimal> findMinWeightByUserId(@Param("userId") String userId);

    @Query("SELECT MAX(b.weightKg) FROM BodyMetrics b WHERE b.userId = :userId")
    Optional<BigDecimal> findMaxWeightByUserId(@Param("userId") String userId);

    // Count entries for a user (useful for requiring minimum data points before showing trends)
    long countByUserId(String userId);
}
