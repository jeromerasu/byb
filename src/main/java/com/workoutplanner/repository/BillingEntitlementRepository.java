package com.workoutplanner.repository;

import com.workoutplanner.model.BillingEntitlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillingEntitlementRepository extends JpaRepository<BillingEntitlement, String> {

    Optional<BillingEntitlement> findByUserId(String userId);

    Optional<BillingEntitlement> findByProviderSubscriptionId(String providerSubscriptionId);

    Optional<BillingEntitlement> findByProviderCustomerId(String providerCustomerId);

    @Query("SELECT be FROM BillingEntitlement be WHERE be.userId = :userId AND be.entitlementActive = true")
    Optional<BillingEntitlement> findActiveEntitlementByUserId(@Param("userId") String userId);

    @Query("SELECT be FROM BillingEntitlement be WHERE be.entitlementActive = true AND be.currentPeriodEnd < :now")
    List<BillingEntitlement> findExpiredActiveEntitlements(@Param("now") LocalDateTime now);

    @Query("SELECT be FROM BillingEntitlement be WHERE be.subscriptionStatus = 'GRACE_PERIOD' AND be.currentPeriodEnd < :now")
    List<BillingEntitlement> findExpiredGracePeriodEntitlements(@Param("now") LocalDateTime now);

    @Query("SELECT be FROM BillingEntitlement be WHERE be.userId = :userId ORDER BY be.updatedAt DESC")
    List<BillingEntitlement> findAllByUserIdOrderByUpdatedAtDesc(@Param("userId") String userId);

    boolean existsByUserIdAndEntitlementActive(String userId, boolean entitlementActive);
}