package com.workoutplanner.service;

import com.workoutplanner.dto.RevenueCatWebhookDto;
import com.workoutplanner.model.BillingEntitlement;
import com.workoutplanner.repository.BillingEntitlementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class BillingEntitlementService {

    private static final Logger logger = LoggerFactory.getLogger(BillingEntitlementService.class);

    private final BillingEntitlementRepository billingEntitlementRepository;

    public BillingEntitlementService(BillingEntitlementRepository billingEntitlementRepository) {
        this.billingEntitlementRepository = billingEntitlementRepository;
    }

    public void processWebhookEvent(RevenueCatWebhookDto webhookDto) {
        if (webhookDto.getEvent() == null || webhookDto.getEvent().getAppUserId() == null) {
            logger.warn("Received webhook with missing event or app_user_id");
            return;
        }

        RevenueCatWebhookDto.Event event = webhookDto.getEvent();
        String userId = event.getAppUserId();
        String eventType = event.getType();

        logger.info("Processing RevenueCat webhook event: {} for user: {}", eventType, userId);

        switch (eventType) {
            case "INITIAL_PURCHASE":
            case "RENEWAL":
                handlePurchaseOrRenewal(event);
                break;
            case "CANCELLATION":
                handleCancellation(event);
                break;
            case "EXPIRATION":
                handleExpiration(event);
                break;
            case "BILLING_ISSUE":
                handleBillingIssue(event);
                break;
            case "PRODUCT_CHANGE":
                handleProductChange(event);
                break;
            case "SUBSCRIBER_ALIAS":
                handleSubscriberAlias(event);
                break;
            default:
                logger.info("Unhandled webhook event type: {}", eventType);
        }
    }

    private void handlePurchaseOrRenewal(RevenueCatWebhookDto.Event event) {
        String userId = event.getAppUserId();

        BillingEntitlement entitlement = billingEntitlementRepository.findByUserId(userId)
            .orElse(new BillingEntitlement(userId, determinePlanTier(event.getProductId()), BillingEntitlement.SubscriptionStatus.ACTIVE));

        entitlement.setProviderCustomerId(event.getOriginalAppUserId());
        entitlement.setProviderSubscriptionId(event.getOriginalTransactionId());
        entitlement.setPlanTier(determinePlanTier(event.getProductId()));
        entitlement.setSubscriptionStatus(BillingEntitlement.SubscriptionStatus.ACTIVE);
        entitlement.setCurrentPeriodEnd(event.getExpirationAt());
        entitlement.setLastWebhookEvent(event.getType());
        entitlement.setWebhookEventTimestamp(event.getEventTimestamp());

        billingEntitlementRepository.save(entitlement);

        logger.info("Updated entitlement for user {} - Status: ACTIVE, Plan: {}", userId, entitlement.getPlanTier());
    }

    private void handleCancellation(RevenueCatWebhookDto.Event event) {
        String userId = event.getAppUserId();

        billingEntitlementRepository.findByUserId(userId).ifPresent(entitlement -> {
            entitlement.setSubscriptionStatus(BillingEntitlement.SubscriptionStatus.CANCELLED);
            entitlement.setLastWebhookEvent(event.getType());
            entitlement.setWebhookEventTimestamp(event.getEventTimestamp());

            billingEntitlementRepository.save(entitlement);
            logger.info("Marked subscription as cancelled for user: {}", userId);
        });
    }

    private void handleExpiration(RevenueCatWebhookDto.Event event) {
        String userId = event.getAppUserId();

        billingEntitlementRepository.findByUserId(userId).ifPresent(entitlement -> {
            entitlement.setSubscriptionStatus(BillingEntitlement.SubscriptionStatus.EXPIRED);
            entitlement.setLastWebhookEvent(event.getType());
            entitlement.setWebhookEventTimestamp(event.getEventTimestamp());

            billingEntitlementRepository.save(entitlement);
            logger.info("Marked subscription as expired for user: {}", userId);
        });
    }

    private void handleBillingIssue(RevenueCatWebhookDto.Event event) {
        String userId = event.getAppUserId();

        billingEntitlementRepository.findByUserId(userId).ifPresent(entitlement -> {
            entitlement.setSubscriptionStatus(BillingEntitlement.SubscriptionStatus.BILLING_ISSUE);
            entitlement.setLastWebhookEvent(event.getType());
            entitlement.setWebhookEventTimestamp(event.getEventTimestamp());

            billingEntitlementRepository.save(entitlement);
            logger.info("Marked subscription as billing issue for user: {}", userId);
        });
    }

    private void handleProductChange(RevenueCatWebhookDto.Event event) {
        String userId = event.getAppUserId();

        billingEntitlementRepository.findByUserId(userId).ifPresent(entitlement -> {
            entitlement.setPlanTier(determinePlanTier(event.getProductId()));
            entitlement.setCurrentPeriodEnd(event.getExpirationAt());
            entitlement.setLastWebhookEvent(event.getType());
            entitlement.setWebhookEventTimestamp(event.getEventTimestamp());

            billingEntitlementRepository.save(entitlement);
            logger.info("Updated plan tier to {} for user: {}", entitlement.getPlanTier(), userId);
        });
    }

    private void handleSubscriberAlias(RevenueCatWebhookDto.Event event) {
        logger.info("Subscriber alias event for user: {} -> {}", event.getOriginalAppUserId(), event.getAppUserId());
    }

    private BillingEntitlement.PlanTier determinePlanTier(String productId) {
        if (productId == null) return BillingEntitlement.PlanTier.FREE;

        String lowerCaseProductId = productId.toLowerCase();
        if (lowerCaseProductId.contains("premium") || lowerCaseProductId.contains("plus")) {
            return BillingEntitlement.PlanTier.PREMIUM;
        } else if (lowerCaseProductId.contains("pro") || lowerCaseProductId.contains("unlimited")) {
            return BillingEntitlement.PlanTier.PRO;
        }
        return BillingEntitlement.PlanTier.FREE;
    }

    @Transactional(readOnly = true)
    public Optional<BillingEntitlement> getActiveEntitlementForUser(String userId) {
        return billingEntitlementRepository.findActiveEntitlementByUserId(userId);
    }

    @Transactional(readOnly = true)
    public boolean hasActivePremiumEntitlement(String userId) {
        return billingEntitlementRepository.findActiveEntitlementByUserId(userId)
            .map(BillingEntitlement::hasActivePremiumEntitlement)
            .orElse(false);
    }

    public BillingEntitlement syncEntitlementForUser(String userId) {
        // This method would typically call RevenueCat API to fetch current entitlement
        // For now, we'll return the stored entitlement
        Optional<BillingEntitlement> existing = billingEntitlementRepository.findByUserId(userId);

        if (existing.isPresent()) {
            BillingEntitlement entitlement = existing.get();

            // Check if subscription has expired based on current_period_end
            if (entitlement.isSubscriptionExpired() && entitlement.isEntitlementActive()) {
                entitlement.updateSubscriptionStatus(BillingEntitlement.SubscriptionStatus.EXPIRED);
                billingEntitlementRepository.save(entitlement);
                logger.info("Marked expired subscription for user: {}", userId);
            }

            return entitlement;
        } else {
            // Create free tier entitlement for users without subscription records
            BillingEntitlement freeEntitlement = new BillingEntitlement(userId,
                BillingEntitlement.PlanTier.FREE, BillingEntitlement.SubscriptionStatus.ACTIVE);
            return billingEntitlementRepository.save(freeEntitlement);
        }
    }

    public void cleanupExpiredEntitlements() {
        LocalDateTime now = LocalDateTime.now();

        // Mark expired active entitlements
        billingEntitlementRepository.findExpiredActiveEntitlements(now)
            .forEach(entitlement -> {
                entitlement.updateSubscriptionStatus(BillingEntitlement.SubscriptionStatus.EXPIRED);
                billingEntitlementRepository.save(entitlement);
                logger.info("Marked expired entitlement for user: {}", entitlement.getUserId());
            });

        // Handle expired grace period entitlements
        billingEntitlementRepository.findExpiredGracePeriodEntitlements(now)
            .forEach(entitlement -> {
                entitlement.updateSubscriptionStatus(BillingEntitlement.SubscriptionStatus.EXPIRED);
                billingEntitlementRepository.save(entitlement);
                logger.info("Expired grace period for user: {}", entitlement.getUserId());
            });
    }
}