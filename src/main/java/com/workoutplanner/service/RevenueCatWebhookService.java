package com.workoutplanner.service;

import com.workoutplanner.dto.RevenueCatWebhookDto;
import com.workoutplanner.model.SubscriptionTier;
import com.workoutplanner.model.User;
import com.workoutplanner.model.WebhookEventLog;
import com.workoutplanner.repository.UserRepository;
import com.workoutplanner.repository.WebhookEventLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * TASK-COACHING-001: Maps RevenueCat entitlement identifiers → SubscriptionTier on User.
 *
 * This service is the ONLY code path that writes subscription_tier on User.
 * No other service or endpoint may directly set this field.
 *
 * Entitlement mapping:
 *   No active entitlement → FREE
 *   "standard"            → STANDARD
 *   "coaching"            → COACHING
 *
 * CANCELLATION semantics: retain current tier until currentPeriodEnd (no immediate downgrade).
 * EXPIRATION: downgrade to FREE.
 *
 * All events are persisted to webhook_event_log BEFORE processing.
 */
@Service
@Transactional
public class RevenueCatWebhookService {

    private static final Logger log = LoggerFactory.getLogger(RevenueCatWebhookService.class);

    private final UserRepository userRepository;
    private final WebhookEventLogRepository webhookEventLogRepository;

    public RevenueCatWebhookService(UserRepository userRepository,
                                    WebhookEventLogRepository webhookEventLogRepository) {
        this.userRepository = userRepository;
        this.webhookEventLogRepository = webhookEventLogRepository;
    }

    /**
     * Process a RevenueCat webhook event and sync SubscriptionTier on the User.
     * Persists to webhook_event_log before processing as per acceptance criteria.
     */
    public void processWebhookEvent(RevenueCatWebhookDto webhookDto, String rawPayload) {
        if (webhookDto.getEvent() == null || webhookDto.getEvent().getAppUserId() == null) {
            log.warn("revenuecat.tier_sync.invalid_payload — missing event or app_user_id");
            return;
        }

        RevenueCatWebhookDto.Event event = webhookDto.getEvent();
        String userId = event.getAppUserId();
        String providerCustomerId = event.getOriginalAppUserId() != null ? event.getOriginalAppUserId() : userId;
        String eventType = event.getType();

        log.info("revenuecat.tier_sync.received event_type={} userId={}", eventType, userId);

        // Persist log entry BEFORE processing (acceptance criterion 12)
        WebhookEventLog logEntry = new WebhookEventLog(userId, providerCustomerId, eventType, rawPayload);
        webhookEventLogRepository.save(logEntry);

        try {
            switch (eventType) {
                case "INITIAL_PURCHASE", "RENEWAL" -> handlePurchaseOrRenewal(event);
                case "CANCELLATION"                -> handleCancellation(event);
                case "EXPIRATION"                  -> handleExpiration(event);
                case "PRODUCT_CHANGE"              -> handleProductChange(event);
                default -> log.info("revenuecat.tier_sync.unhandled event_type={}", eventType);
            }
            logEntry.setProcessedSuccessfully(true);
            webhookEventLogRepository.save(logEntry);
            log.info("revenuecat.tier_sync.processed_ok event_type={} userId={}", eventType, userId);

        } catch (Exception ex) {
            logEntry.setProcessedSuccessfully(false);
            logEntry.setErrorMessage(ex.getMessage());
            webhookEventLogRepository.save(logEntry);
            log.error("revenuecat.tier_sync.error event_type={} userId={} error={}", eventType, userId, ex.getMessage());
            throw ex;
        }
    }

    // -------------------------------------------------------------------------
    // Event handlers
    // -------------------------------------------------------------------------

    private void handlePurchaseOrRenewal(RevenueCatWebhookDto.Event event) {
        SubscriptionTier tier = resolveEntitlementTier(event);
        updateUserTier(event.getAppUserId(), tier, event.getType());
    }

    private void handleCancellation(RevenueCatWebhookDto.Event event) {
        // CANCELLATION: retain current tier until currentPeriodEnd — do NOT downgrade immediately
        // The EXPIRATION event that follows will downgrade when the period ends.
        log.info("revenuecat.tier_sync.cancellation userId={} — tier retained until period end",
                event.getAppUserId());
    }

    private void handleExpiration(RevenueCatWebhookDto.Event event) {
        updateUserTier(event.getAppUserId(), SubscriptionTier.FREE, event.getType());
    }

    private void handleProductChange(RevenueCatWebhookDto.Event event) {
        SubscriptionTier tier = resolveEntitlementTier(event);
        updateUserTier(event.getAppUserId(), tier, event.getType());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Maps RevenueCat entitlement identifiers to SubscriptionTier.
     * Checks entitlement_ids array first (preferred), then entitlement_id, then product_id.
     */
    SubscriptionTier resolveEntitlementTier(RevenueCatWebhookDto.Event event) {
        // Check entitlement_ids array (RevenueCat can send multiple)
        if (event.getEntitlementIds() != null) {
            for (String id : event.getEntitlementIds()) {
                SubscriptionTier tier = mapEntitlementId(id);
                if (tier != SubscriptionTier.FREE) return tier;
            }
        }
        // Fallback: single entitlement_id
        if (event.getEntitlementId() != null) {
            SubscriptionTier tier = mapEntitlementId(event.getEntitlementId());
            if (tier != SubscriptionTier.FREE) return tier;
        }
        // Fallback: product_id pattern matching
        return resolveFromProductId(event.getProductId());
    }

    private SubscriptionTier mapEntitlementId(String entitlementId) {
        if (entitlementId == null) return SubscriptionTier.FREE;
        return switch (entitlementId.toLowerCase()) {
            case "coaching" -> SubscriptionTier.COACHING;
            case "standard" -> SubscriptionTier.STANDARD;
            default         -> SubscriptionTier.FREE;
        };
    }

    private SubscriptionTier resolveFromProductId(String productId) {
        if (productId == null) return SubscriptionTier.FREE;
        String lower = productId.toLowerCase();
        if (lower.contains("coaching")) return SubscriptionTier.COACHING;
        if (lower.contains("standard")) return SubscriptionTier.STANDARD;
        return SubscriptionTier.FREE;
    }

    private void updateUserTier(String userId, SubscriptionTier tier, String eventType) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.warn("revenuecat.tier_sync.user_not_found userId={} event={}", userId, eventType);
            return;
        }
        User user = userOpt.get();
        SubscriptionTier previous = user.getSubscriptionTier();
        user.setSubscriptionTier(tier);
        userRepository.save(user);
        log.info("revenuecat.tier_sync.tier_updated userId={} event={} prev={} new={}",
                userId, eventType, previous, tier);
    }
}
