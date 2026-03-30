package com.workoutplanner.service;

import com.workoutplanner.dto.BillingStatusDto;
import com.workoutplanner.dto.BillingUsageDto;
import com.workoutplanner.dto.RevenueCatWebhookDto;
import com.workoutplanner.model.BillingEntitlement;
import com.workoutplanner.model.PlanUsageTracker;
import com.workoutplanner.model.WebhookEventLog;
import com.workoutplanner.repository.BillingEntitlementRepository;
import com.workoutplanner.repository.PlanUsageTrackerRepository;
import com.workoutplanner.repository.WebhookEventLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class BillingEntitlementService {

    private static final Logger logger = LoggerFactory.getLogger(BillingEntitlementService.class);

    private static final int DEFAULT_MAX_PLANS_PREMIUM = 10;
    private static final int DEFAULT_MAX_PLANS_FREE = 0;

    private final BillingEntitlementRepository billingEntitlementRepository;
    private final WebhookEventLogRepository webhookEventLogRepository;
    private final PlanUsageTrackerRepository planUsageTrackerRepository;

    @Value("${billing.enforcement.enabled:false}")
    private boolean enforcementEnabled;

    public BillingEntitlementService(BillingEntitlementRepository billingEntitlementRepository,
                                     WebhookEventLogRepository webhookEventLogRepository,
                                     PlanUsageTrackerRepository planUsageTrackerRepository) {
        this.billingEntitlementRepository = billingEntitlementRepository;
        this.webhookEventLogRepository = webhookEventLogRepository;
        this.planUsageTrackerRepository = planUsageTrackerRepository;
    }

    /**
     * Process a RevenueCat webhook event: logs to webhook_event_log before processing,
     * updates the log entry with outcome after processing.
     */
    public void processWebhookEvent(RevenueCatWebhookDto webhookDto, String rawPayload) {
        if (webhookDto.getEvent() == null || webhookDto.getEvent().getAppUserId() == null) {
            logger.warn("billing.webhook.invalid_payload — missing event or app_user_id");
            return;
        }

        RevenueCatWebhookDto.Event event = webhookDto.getEvent();
        String userId = event.getAppUserId();
        String providerCustomerId = event.getOriginalAppUserId() != null ? event.getOriginalAppUserId() : userId;
        String eventType = event.getType();

        logger.info("billing.webhook.received event_type={} user_id={} provider_customer_id={}",
                eventType, userId, providerCustomerId);

        // Persist log entry BEFORE processing
        WebhookEventLog logEntry = new WebhookEventLog(userId, providerCustomerId, eventType, rawPayload);
        webhookEventLogRepository.save(logEntry);

        try {
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
                    logger.info("billing.webhook.unhandled event_type={}", eventType);
            }

            logEntry.setProcessedSuccessfully(true);
            webhookEventLogRepository.save(logEntry);
            logger.info("billing.webhook.processed_ok event_type={} user_id={}", eventType, userId);

        } catch (Exception ex) {
            logEntry.setProcessedSuccessfully(false);
            logEntry.setErrorMessage(ex.getMessage());
            webhookEventLogRepository.save(logEntry);
            logger.error("billing.webhook.processing_error event_type={} user_id={} error={}",
                    eventType, userId, ex.getMessage(), ex);
            throw ex;
        }
    }

    /** Legacy overload without raw payload — used by existing code paths. */
    public void processWebhookEvent(RevenueCatWebhookDto webhookDto) {
        processWebhookEvent(webhookDto, "{}");
    }

    private void handlePurchaseOrRenewal(RevenueCatWebhookDto.Event event) {
        String userId = event.getAppUserId();

        BillingEntitlement entitlement = billingEntitlementRepository.findByUserId(userId)
            .orElse(new BillingEntitlement(userId, determinePlanTier(event.getProductId()),
                    BillingEntitlement.SubscriptionStatus.ACTIVE));

        entitlement.setProviderCustomerId(event.getOriginalAppUserId());
        entitlement.setProviderSubscriptionId(event.getOriginalTransactionId());
        entitlement.setPlanTier(determinePlanTier(event.getProductId()));
        entitlement.setSubscriptionStatus(BillingEntitlement.SubscriptionStatus.ACTIVE);
        entitlement.setEntitlementActive(true);
        entitlement.setCurrentPeriodEnd(event.getExpirationAt());
        entitlement.setLastWebhookEvent(event.getType());
        entitlement.setWebhookEventTimestamp(event.getEventTimestamp());

        billingEntitlementRepository.save(entitlement);
        logger.info("billing.entitlement.updated user_id={} event_type={} status=ACTIVE plan={}",
                userId, event.getType(), entitlement.getPlanTier());
    }

    private void handleCancellation(RevenueCatWebhookDto.Event event) {
        String userId = event.getAppUserId();

        billingEntitlementRepository.findByUserId(userId).ifPresent(entitlement -> {
            // Set status to CANCELLED but keep entitlementActive=true — user retains access until currentPeriodEnd
            entitlement.setSubscriptionStatus(BillingEntitlement.SubscriptionStatus.CANCELLED);
            entitlement.setEntitlementActive(true);  // access continues until period end
            entitlement.setLastWebhookEvent(event.getType());
            entitlement.setWebhookEventTimestamp(event.getEventTimestamp());

            billingEntitlementRepository.save(entitlement);
            logger.info("billing.entitlement.cancelled user_id={} period_end={} — access retained until period end",
                    userId, entitlement.getCurrentPeriodEnd());
        });
    }

    private void handleExpiration(RevenueCatWebhookDto.Event event) {
        String userId = event.getAppUserId();

        billingEntitlementRepository.findByUserId(userId).ifPresent(entitlement -> {
            entitlement.setSubscriptionStatus(BillingEntitlement.SubscriptionStatus.EXPIRED);
            entitlement.setEntitlementActive(false);
            entitlement.setLastWebhookEvent(event.getType());
            entitlement.setWebhookEventTimestamp(event.getEventTimestamp());

            billingEntitlementRepository.save(entitlement);
            logger.info("billing.entitlement.expired user_id={} — access revoked", userId);
        });
    }

    private void handleBillingIssue(RevenueCatWebhookDto.Event event) {
        String userId = event.getAppUserId();

        billingEntitlementRepository.findByUserId(userId).ifPresent(entitlement -> {
            // Keep entitlementActive=true during grace period
            entitlement.setSubscriptionStatus(BillingEntitlement.SubscriptionStatus.BILLING_ISSUE);
            entitlement.setEntitlementActive(true);  // grace period — access retained
            entitlement.setLastWebhookEvent(event.getType());
            entitlement.setWebhookEventTimestamp(event.getEventTimestamp());

            billingEntitlementRepository.save(entitlement);
            logger.info("billing.entitlement.billing_issue user_id={} — grace period active", userId);
        });
    }

    private void handleProductChange(RevenueCatWebhookDto.Event event) {
        String userId = event.getAppUserId();

        billingEntitlementRepository.findByUserId(userId).ifPresent(entitlement -> {
            BillingEntitlement.PlanTier newTier = determinePlanTier(event.getProductId());
            entitlement.setPlanTier(newTier);
            entitlement.setCurrentPeriodEnd(event.getExpirationAt());
            entitlement.setLastWebhookEvent(event.getType());
            entitlement.setWebhookEventTimestamp(event.getEventTimestamp());

            billingEntitlementRepository.save(entitlement);
            logger.info("billing.entitlement.product_change user_id={} new_tier={}", userId, newTier);
        });
    }

    private void handleSubscriberAlias(RevenueCatWebhookDto.Event event) {
        logger.info("billing.webhook.subscriber_alias original_user={} alias_user={}",
                event.getOriginalAppUserId(), event.getAppUserId());
    }

    private BillingEntitlement.PlanTier determinePlanTier(String productId) {
        if (productId == null) return BillingEntitlement.PlanTier.FREE;

        String lower = productId.toLowerCase();
        if (lower.contains("premium") || lower.contains("plus")) {
            return BillingEntitlement.PlanTier.PREMIUM;
        } else if (lower.contains("pro") || lower.contains("unlimited")) {
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
        Optional<BillingEntitlement> existing = billingEntitlementRepository.findByUserId(userId);

        if (existing.isPresent()) {
            BillingEntitlement entitlement = existing.get();

            if (entitlement.isSubscriptionExpired() && entitlement.isEntitlementActive()) {
                entitlement.updateSubscriptionStatus(BillingEntitlement.SubscriptionStatus.EXPIRED);
                billingEntitlementRepository.save(entitlement);
                logger.info("billing.entitlement.auto_expired user_id={}", userId);
            }

            return entitlement;
        } else {
            BillingEntitlement freeEntitlement = new BillingEntitlement(userId,
                BillingEntitlement.PlanTier.FREE, BillingEntitlement.SubscriptionStatus.ACTIVE);
            return billingEntitlementRepository.save(freeEntitlement);
        }
    }

    /**
     * Links a RevenueCat customer ID to the user's billing entitlement.
     * Creates a FREE entitlement if none exists.
     */
    public void linkProviderCustomerId(String userId, String providerCustomerId) {
        BillingEntitlement entitlement = billingEntitlementRepository.findByUserId(userId)
                .orElseGet(() -> {
                    BillingEntitlement newEnt = new BillingEntitlement(userId,
                            BillingEntitlement.PlanTier.FREE, BillingEntitlement.SubscriptionStatus.ACTIVE);
                    return newEnt;
                });

        entitlement.setProviderCustomerId(providerCustomerId);
        billingEntitlementRepository.save(entitlement);
        logger.info("billing.customer_linked user_id={} provider_customer_id={}", userId, providerCustomerId);
    }

    /**
     * Builds BillingStatusDto for the given user.
     */
    @Transactional(readOnly = true)
    public BillingStatusDto getBillingStatus(String userId) {
        Optional<BillingEntitlement> entOpt = billingEntitlementRepository.findByUserId(userId);

        if (entOpt.isEmpty()) {
            boolean canGenerate = !enforcementEnabled;
            return new BillingStatusDto("FREE", "ACTIVE", false, null, canGenerate);
        }

        BillingEntitlement ent = entOpt.get();
        boolean canGenerate = !enforcementEnabled || ent.hasActivePremiumEntitlement();

        return new BillingStatusDto(
                ent.getPlanTier().name(),
                ent.getSubscriptionStatus().name(),
                ent.isEntitlementActive(),
                ent.getCurrentPeriodEnd(),
                canGenerate
        );
    }

    /**
     * Increments plan usage for the current billing period.
     * Creates a tracker record if none exists for this period.
     */
    public void incrementPlanUsage(String userId) {
        LocalDate today = LocalDate.now();
        LocalDate periodStart = today.withDayOfMonth(1);
        LocalDate periodEnd = today.withDayOfMonth(today.lengthOfMonth());

        PlanUsageTracker tracker = planUsageTrackerRepository
                .findActiveByUserIdAndDate(userId, today)
                .orElseGet(() -> {
                    int maxPlans = hasActivePremiumEntitlement(userId)
                            ? DEFAULT_MAX_PLANS_PREMIUM : DEFAULT_MAX_PLANS_FREE;
                    PlanUsageTracker newTracker = new PlanUsageTracker(userId, periodStart, periodEnd, maxPlans);
                    return planUsageTrackerRepository.save(newTracker);
                });

        tracker.setPlansGenerated(tracker.getPlansGenerated() + 1);
        planUsageTrackerRepository.save(tracker);
        logger.info("billing.usage.incremented user_id={} plans_generated={} period={}/{}",
                userId, tracker.getPlansGenerated(), periodStart, periodEnd);
    }

    /**
     * Returns usage DTO for the current billing period.
     */
    @Transactional(readOnly = true)
    public BillingUsageDto getUsageForCurrentPeriod(String userId) {
        LocalDate today = LocalDate.now();
        LocalDate periodStart = today.withDayOfMonth(1);
        LocalDate periodEnd = today.withDayOfMonth(today.lengthOfMonth());

        return planUsageTrackerRepository.findActiveByUserIdAndDate(userId, today)
                .map(t -> new BillingUsageDto(t.getPlansGenerated(), t.getBillingPeriodStart(), t.getBillingPeriodEnd()))
                .orElse(new BillingUsageDto(0, periodStart, periodEnd));
    }

    public void cleanupExpiredEntitlements() {
        LocalDateTime now = LocalDateTime.now();

        billingEntitlementRepository.findExpiredActiveEntitlements(now)
            .forEach(entitlement -> {
                entitlement.updateSubscriptionStatus(BillingEntitlement.SubscriptionStatus.EXPIRED);
                billingEntitlementRepository.save(entitlement);
                logger.info("billing.entitlement.cleanup_expired user_id={}", entitlement.getUserId());
            });

        billingEntitlementRepository.findExpiredGracePeriodEntitlements(now)
            .forEach(entitlement -> {
                entitlement.updateSubscriptionStatus(BillingEntitlement.SubscriptionStatus.EXPIRED);
                billingEntitlementRepository.save(entitlement);
                logger.info("billing.entitlement.grace_period_expired user_id={}", entitlement.getUserId());
            });
    }
}
