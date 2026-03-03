package com.workoutplanner.controller;

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

    @Value("${revenuecat.webhook.secret:}")
    private String webhookSecret;

    public BillingController(BillingEntitlementService billingEntitlementService) {
        this.billingEntitlementService = billingEntitlementService;
    }

    @PostMapping("/webhooks/revenuecat")
    public ResponseEntity<Map<String, String>> handleRevenueCatWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestHeader(value = "X-RevenueCat-Signature", required = false) String signature) {

        Map<String, String> response = new HashMap<>();

        try {
            // Verify webhook signature if secret is configured
            if (webhookSecret != null && !webhookSecret.isEmpty()) {
                if (!verifyWebhookSignature(payload, signature)) {
                    logger.warn("Invalid webhook signature received");
                    response.put("error", "Invalid signature");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
                }
            }

            // Parse webhook payload
            RevenueCatWebhookDto webhookDto = parseWebhookPayload(payload);
            if (webhookDto == null) {
                logger.error("Failed to parse webhook payload");
                response.put("error", "Invalid payload");
                return ResponseEntity.badRequest().body(response);
            }

            // Process the webhook event
            billingEntitlementService.processWebhookEvent(webhookDto);

            response.put("status", "success");
            response.put("message", "Webhook processed successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error processing RevenueCat webhook", e);
            response.put("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

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
            logger.error("Error syncing entitlements for user: " + userId, e);
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
                BillingEntitlement entitlement = entitlementOpt.get();
                response.put("status", "success");
                response.put("entitlement", createEntitlementResponse(entitlement));
            } else {
                // Return free tier entitlement for users without subscriptions
                response.put("status", "success");
                response.put("entitlement", createFreeEntitlementResponse(userId));
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error fetching entitlements for user: " + userId, e);
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
            boolean hasAccess = checkFeatureAccess(userId, feature);

            response.put("status", "success");
            response.put("feature", feature);
            response.put("hasAccess", hasAccess);
            response.put("userId", userId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error checking feature access for user: " + userId + ", feature: " + feature, e);
            response.put("error", "Failed to check feature access");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

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
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            String expectedSignature = hexString.toString();
            return signature.equals(expectedSignature);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Error verifying webhook signature", e);
            return false;
        }
    }

    private RevenueCatWebhookDto parseWebhookPayload(String payload) {
        try {
            // Use a JSON parser to convert payload to RevenueCatWebhookDto
            // For now, returning null as a placeholder
            // In real implementation, use ObjectMapper or similar
            logger.info("Parsing webhook payload: {}", payload);
            return null; // TODO: Implement JSON parsing
        } catch (Exception e) {
            logger.error("Error parsing webhook payload", e);
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

    private boolean checkFeatureAccess(String userId, String feature) {
        // Define feature access rules based on plan tier
        boolean hasPremiumEntitlement = billingEntitlementService.hasActivePremiumEntitlement(userId);

        switch (feature.toLowerCase()) {
            case "unlimited_plans":
            case "custom_workouts":
            case "nutrition_tracking":
            case "progress_analytics":
                return hasPremiumEntitlement;
            case "basic_workouts":
            case "basic_nutrition":
                return true; // Available to all users
            default:
                return false; // Unknown features default to no access
        }
    }
}