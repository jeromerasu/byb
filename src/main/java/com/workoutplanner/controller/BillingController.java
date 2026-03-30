package com.workoutplanner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workoutplanner.dto.BillingStatusDto;
import com.workoutplanner.dto.BillingUsageDto;
import com.workoutplanner.dto.LinkCustomerRequestDto;
import com.workoutplanner.dto.RevenueCatWebhookDto;
import com.workoutplanner.model.BillingEntitlement;
import com.workoutplanner.service.BillingEntitlementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

    private static final Logger logger = LoggerFactory.getLogger(BillingController.class);

    private final BillingEntitlementService billingEntitlementService;
    private final ObjectMapper objectMapper;

    @Value("${revenuecat.webhook.secret:}")
    private String webhookSecret;

    public BillingController(BillingEntitlementService billingEntitlementService, ObjectMapper objectMapper) {
        this.billingEntitlementService = billingEntitlementService;
        this.objectMapper = objectMapper;
    }

    // -------------------------------------------------------------------------
    // New endpoints — TASK-BILLING-001
    // -------------------------------------------------------------------------

    /**
     * GET /api/v1/billing/status — returns subscription info for the authenticated user.
     */
    @GetMapping("/status")
    public ResponseEntity<?> getBillingStatus(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userId = authentication.getName();
        logger.info("billing.status.requested user_id={}", userId);

        try {
            BillingStatusDto status = billingEntitlementService.getBillingStatus(userId);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("billing.status.error user_id={} error={}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve billing status"));
        }
    }

    /**
     * POST /api/v1/billing/link-customer — associates a RevenueCat customer ID with the authenticated user.
     */
    @PostMapping("/link-customer")
    public ResponseEntity<?> linkCustomer(@RequestBody LinkCustomerRequestDto request,
                                          Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userId = authentication.getName();

        if (request.getRevenueCatCustomerId() == null || request.getRevenueCatCustomerId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "revenueCatCustomerId is required"));
        }

        logger.info("billing.link_customer.requested user_id={} provider_customer_id={}",
                userId, request.getRevenueCatCustomerId());

        try {
            billingEntitlementService.linkProviderCustomerId(userId, request.getRevenueCatCustomerId());
            return ResponseEntity.ok(Map.of("status", "success",
                    "message", "Customer ID linked successfully"));
        } catch (Exception e) {
            logger.error("billing.link_customer.error user_id={} error={}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to link customer ID"));
        }
    }

    /**
     * GET /api/v1/billing/usage — returns plan generation usage for the current billing period.
     */
    @GetMapping("/usage")
    public ResponseEntity<?> getBillingUsage(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userId = authentication.getName();
        logger.info("billing.usage.requested user_id={}", userId);

        try {
            BillingUsageDto usage = billingEntitlementService.getUsageForCurrentPeriod(userId);
            return ResponseEntity.ok(usage);
        } catch (Exception e) {
            logger.error("billing.usage.error user_id={} error={}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve billing usage"));
        }
    }

    // -------------------------------------------------------------------------
    // Webhook endpoint
    // -------------------------------------------------------------------------

    @PostMapping("/webhooks/revenuecat")
    public ResponseEntity<Map<String, String>> handleRevenueCatWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestHeader(value = "X-RevenueCat-Signature", required = false) String signature) {

        Map<String, String> response = new HashMap<>();

        // Validate webhook secret header — reject with 401 if missing or invalid
        if (webhookSecret != null && !webhookSecret.isEmpty()) {
            if (!verifyWebhookSignature(payload, signature)) {
                logger.warn("billing.webhook.unauthorized — invalid or missing signature");
                response.put("error", "Invalid or missing webhook signature");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
        }

        try {
            RevenueCatWebhookDto webhookDto = parseWebhookPayload(payload);
            if (webhookDto == null) {
                logger.error("billing.webhook.parse_error payload_length={}", payload.length());
                response.put("error", "Invalid payload");
                return ResponseEntity.badRequest().body(response);
            }

            billingEntitlementService.processWebhookEvent(webhookDto, payload);

            response.put("status", "success");
            response.put("message", "Webhook processed successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("billing.webhook.error error={}", e.getMessage(), e);
            response.put("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // -------------------------------------------------------------------------
    // Existing endpoints (unchanged)
    // -------------------------------------------------------------------------

    @PostMapping("/entitlements/sync")
    public ResponseEntity<Map<String, Object>> syncEntitlements(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userId = authentication.getName();
        Map<String, Object> response = new HashMap<>();

        try {
            BillingEntitlement entitlement = billingEntitlementService.syncEntitlementForUser(userId);

            response.put("status", "success");
            response.put("entitlement", createEntitlementResponse(entitlement));
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("billing.sync.error user_id={} error={}", userId, e.getMessage(), e);
            response.put("error", "Failed to sync entitlements");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/entitlements/me")
    public ResponseEntity<Map<String, Object>> getCurrentUserEntitlements(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userId = authentication.getName();
        Map<String, Object> response = new HashMap<>();

        try {
            Optional<BillingEntitlement> entitlementOpt = billingEntitlementService.getActiveEntitlementForUser(userId);

            if (entitlementOpt.isPresent()) {
                response.put("status", "success");
                response.put("entitlement", createEntitlementResponse(entitlementOpt.get()));
            } else {
                response.put("status", "success");
                response.put("entitlement", createFreeEntitlementResponse(userId));
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("billing.entitlements.me.error user_id={} error={}", userId, e.getMessage(), e);
            response.put("error", "Failed to fetch entitlements");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/entitlements/check/{feature}")
    public ResponseEntity<Map<String, Object>> checkFeatureAccess(
            @PathVariable String feature,
            Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userId = authentication.getName();
        Map<String, Object> response = new HashMap<>();

        try {
            boolean hasAccess = checkFeatureAccessByName(userId, feature);

            response.put("status", "success");
            response.put("feature", feature);
            response.put("hasAccess", hasAccess);
            response.put("userId", userId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("billing.feature_check.error user_id={} feature={} error={}", userId, feature, e.getMessage(), e);
            response.put("error", "Failed to check feature access");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private boolean verifyWebhookSignature(String payload, String signature) {
        if (signature == null || webhookSecret == null || webhookSecret.isEmpty()) {
            return false;
        }

        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);

            byte[] hash = sha256Hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString().equals(signature);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("billing.webhook.signature_verify_error error={}", e.getMessage(), e);
            return false;
        }
    }

    private RevenueCatWebhookDto parseWebhookPayload(String payload) {
        try {
            return objectMapper.readValue(payload, RevenueCatWebhookDto.class);
        } catch (Exception e) {
            logger.error("billing.webhook.json_parse_error error={}", e.getMessage(), e);
            return null;
        }
    }

    private Map<String, Object> createEntitlementResponse(BillingEntitlement entitlement) {
        Map<String, Object> response = new HashMap<>();
        response.put("planTier", entitlement.getPlanTier().toString());
        response.put("subscriptionStatus", entitlement.getSubscriptionStatus().toString());
        response.put("entitlementActive", entitlement.isEntitlementActive());
        response.put("hasActivePremiumEntitlement", entitlement.hasActivePremiumEntitlement());
        response.put("currentPeriodEnd", entitlement.getCurrentPeriodEnd());
        response.put("isSubscriptionExpired", entitlement.isSubscriptionExpired());
        response.put("isInGracePeriod", entitlement.isInGracePeriod());
        response.put("updatedAt", entitlement.getUpdatedAt());
        return response;
    }

    private Map<String, Object> createFreeEntitlementResponse(String userId) {
        Map<String, Object> response = new HashMap<>();
        response.put("planTier", "FREE");
        response.put("subscriptionStatus", "ACTIVE");
        response.put("entitlementActive", true);
        response.put("hasActivePremiumEntitlement", false);
        response.put("currentPeriodEnd", null);
        response.put("isSubscriptionExpired", false);
        response.put("isInGracePeriod", false);
        return response;
    }

    private boolean checkFeatureAccessByName(String userId, String feature) {
        boolean hasPremium = billingEntitlementService.hasActivePremiumEntitlement(userId);

        switch (feature.toLowerCase()) {
            case "unlimited_plans":
            case "custom_workouts":
            case "nutrition_tracking":
            case "progress_analytics":
                return hasPremium;
            case "basic_workouts":
            case "basic_nutrition":
                return true;
            default:
                return false;
        }
    }
}
