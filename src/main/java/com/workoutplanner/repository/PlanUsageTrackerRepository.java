package com.workoutplanner.repository;

import com.workoutplanner.model.PlanUsageTracker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlanUsageTrackerRepository extends JpaRepository<PlanUsageTracker, Long> {

    List<PlanUsageTracker> findByUserId(String userId);

    // Find the active tracker for a user given a date falling within the billing period
    @Query("SELECT p FROM PlanUsageTracker p WHERE p.userId = :userId " +
           "AND p.billingPeriodStart <= :date AND p.billingPeriodEnd >= :date")
    Optional<PlanUsageTracker> findActiveByUserIdAndDate(@Param("userId") String userId,
                                                          @Param("date") LocalDate date);

    // Find all trackers for a user where the user still has remaining capacity
    @Query("SELECT p FROM PlanUsageTracker p WHERE p.userId = :userId " +
           "AND p.plansGenerated < p.maxPlansAllowed ORDER BY p.billingPeriodStart DESC")
    List<PlanUsageTracker> findWithRemainingCapacityByUserId(@Param("userId") String userId);

    // Find trackers where the period overlaps a given range (for duplicate-period detection)
    @Query("SELECT p FROM PlanUsageTracker p WHERE p.userId = :userId " +
           "AND p.billingPeriodStart <= :periodEnd AND p.billingPeriodEnd >= :periodStart")
    List<PlanUsageTracker> findOverlappingByUserIdAndPeriod(@Param("userId") String userId,
                                                             @Param("periodStart") LocalDate periodStart,
                                                             @Param("periodEnd") LocalDate periodEnd);
}
