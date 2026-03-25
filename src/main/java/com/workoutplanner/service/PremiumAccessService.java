package com.workoutplanner.service;

import com.workoutplanner.model.BillingEntitlement;
import com.workoutplanner.repository.BillingEntitlementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PremiumAccessService {

    private static final Logger logger = LoggerFactory.getLogger(PremiumAccessService.class);

    private final BillingEntitlementRepository billingEntitlementRepository;

    public PremiumAccessService(BillingEntitlementRepository billingEntitlementRepository) {
        this.billingEntitlementRepository = billingEntitlementRepository;
    }

    public boolean hasFeatureAccess(String userId, PremiumFeature feature) {
        if (userId == null || feature == null) {
            logger.debug("Null userId or feature provided");
            return false;
        }

        try {
            Optional<BillingEntitlement> entitlementOpt = billingEntitlementRepository.findActiveEntitlementByUserId(userId);

            if (entitlementOpt.isEmpty()) {
                logger.debug("No active entitlement found for user: {}, checking free tier access", userId);
                return feature.isAvailableInFreeTier();
            }

            BillingEntitlement entitlement = entitlementOpt.get();

            // Check if entitlement is still valid
            if (!entitlement.isEntitlementActive()) {
                logger.debug("Entitlement is not active for user: {}", userId);
                return feature.isAvailableInFreeTier();
            }

            // Check feature access based on plan tier
            return checkFeatureAccessByPlanTier(entitlement.getPlanTier(), feature);

        } catch (Exception e) {
            logger.error("Error checking feature access for user: {} and feature: {}", userId, feature, e);
            // Fail closed - no access if we can't determine entitlement
            return false;
        }
    }

    public boolean hasPremiumAccess(String userId) {
        return hasFeatureAccess(userId, PremiumFeature.PREMIUM_WORKOUTS);
    }

    public boolean hasProAccess(String userId) {
        return hasFeatureAccess(userId, PremiumFeature.UNLIMITED_PLANS);
    }

    public PremiumAccessResult checkAccessWithDetails(String userId, PremiumFeature feature) {
        if (userId == null || feature == null) {
            return new PremiumAccessResult(false, PremiumTier.FREE, "Invalid request parameters");
        }

        try {
            Optional<BillingEntitlement> entitlementOpt = billingEntitlementRepository.findActiveEntitlementByUserId(userId);

            if (entitlementOpt.isEmpty()) {
                return new PremiumAccessResult(
                    feature.isAvailableInFreeTier(),
                    PremiumTier.FREE,
                    feature.isAvailableInFreeTier() ? "Access granted for free tier" : "Premium subscription required"
                );
            }

            BillingEntitlement entitlement = entitlementOpt.get();

            if (!entitlement.isEntitlementActive()) {
                return new PremiumAccessResult(
                    feature.isAvailableInFreeTier(),
                    PremiumTier.FREE,
                    "Subscription expired or inactive"
                );
            }

            PremiumTier tier = mapPlanTierToPremiumTier(entitlement.getPlanTier());
            boolean hasAccess = checkFeatureAccessByPlanTier(entitlement.getPlanTier(), feature);

            return new PremiumAccessResult(
                hasAccess,
                tier,
                hasAccess ? "Access granted" : "Higher tier subscription required"
            );

        } catch (Exception e) {
            logger.error("Error checking access details for user: {} and feature: {}", userId, feature, e);
            return new PremiumAccessResult(false, PremiumTier.FREE, "Error checking access");
        }
    }

    private boolean checkFeatureAccessByPlanTier(BillingEntitlement.PlanTier planTier, PremiumFeature feature) {
        switch (planTier) {
            case PRO:
                return true; // Pro tier has access to all features
            case PREMIUM:
                return feature.isAvailableInPremiumTier();
            case FREE:
            default:
                return feature.isAvailableInFreeTier();
        }
    }

    private PremiumTier mapPlanTierToPremiumTier(BillingEntitlement.PlanTier planTier) {
        switch (planTier) {
            case PRO:
                return PremiumTier.PRO;
            case PREMIUM:
                return PremiumTier.PREMIUM;
            case FREE:
            default:
                return PremiumTier.FREE;
        }
    }

    public enum PremiumFeature {
        // Free tier features
        BASIC_WORKOUTS(true, true, true),
        BASIC_NUTRITION(true, true, true),
        PROFILE_CREATION(true, true, true),

        // Premium tier features
        PREMIUM_WORKOUTS(false, true, true),
        CUSTOM_NUTRITION_PLANS(false, true, true),
        PROGRESS_TRACKING(false, true, true),
        EXPORT_DATA(false, true, true),

        // Pro tier features
        UNLIMITED_PLANS(false, false, true),
        ADVANCED_ANALYTICS(false, false, true),
        PRIORITY_SUPPORT(false, false, true),
        EARLY_ACCESS_FEATURES(false, false, true);

        private final boolean availableInFreeTier;
        private final boolean availableInPremiumTier;
        private final boolean availableInProTier;

        PremiumFeature(boolean availableInFreeTier, boolean availableInPremiumTier, boolean availableInProTier) {
            this.availableInFreeTier = availableInFreeTier;
            this.availableInPremiumTier = availableInPremiumTier;
            this.availableInProTier = availableInProTier;
        }

        public boolean isAvailableInFreeTier() { return availableInFreeTier; }
        public boolean isAvailableInPremiumTier() { return availableInPremiumTier; }
        public boolean isAvailableInProTier() { return availableInProTier; }
    }

    public enum PremiumTier {
        FREE, PREMIUM, PRO
    }

    public static class PremiumAccessResult {
        private final boolean hasAccess;
        private final PremiumTier currentTier;
        private final String message;

        public PremiumAccessResult(boolean hasAccess, PremiumTier currentTier, String message) {
            this.hasAccess = hasAccess;
            this.currentTier = currentTier;
            this.message = message;
        }

        public boolean hasAccess() { return hasAccess; }
        public PremiumTier getCurrentTier() { return currentTier; }
        public String getMessage() { return message; }
    }
}