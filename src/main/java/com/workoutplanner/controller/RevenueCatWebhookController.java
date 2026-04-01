package com.workoutplanner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workoutplanner.dto.RevenueCatWebhookDto;
import com.workoutplanner.service.RevenueCatWebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * RevenueCat webhook receiver for subscription tier sync.
 * Maps entitlement identifiers → SubscriptionTier on User.
 *
 * Endpoint: POST /api/webhooks/revenuecat
 *
 * Auth: webhook secret validated via X-RevenueCat-Webhook-Secret or Authorization header.
 * Requests with missing/invalid secret are rejected with 401.
 *
 * All events are persisted to webhook_event_log BEFORE processing.
 */
@RestController
@RequestMapping("/api/webhooks")
public class RevenueCatWebhookController {

    private static final Logger log = LoggerFactory.getLogger(RevenueCatWebhookController.class);

    private final RevenueCatWebhookService webhookService;
    private final ObjectMapper objectMapper;

    @Value("${revenuecat.webhook.secret:}")
    private String webhookSecret;

    @Value("${revenuecat.webhook.validation.skip:false}")
    private boolean skipWebhookValidation;

    public RevenueCatWebhookController(RevenueCatWebhookService webhookService, ObjectMapper objectMapper) {
        this.webhookService = webhookService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/revenuecat")
    public ResponseEntity<Map<String, String>> handleRevenueCatWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestHeader(value = "X-RevenueCat-Webhook-Secret", required = false) String webhookSecretHeader) {

        // Validate webhook secret (acceptance criterion 9)
        if (!skipWebhookValidation && webhookSecret != null && !webhookSecret.isEmpty()) {
            String provided = resolveSecret(authorizationHeader, webhookSecretHeader);
            if (!webhookSecret.equals(provided)) {
                log.warn("revenuecat.webhook.unauthorized — invalid or missing secret");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Unauthorized: invalid or missing webhook secret"));
            }
        }

        try {
            RevenueCatWebhookDto dto = objectMapper.readValue(payload, RevenueCatWebhookDto.class);
            if (dto == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid payload"));
            }
            webhookService.processWebhookEvent(dto, payload);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Webhook processed"));
        } catch (Exception e) {
            log.error("revenuecat.webhook.error error={}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    private String resolveSecret(String authorizationHeader, String webhookSecretHeader) {
        if (webhookSecretHeader != null && !webhookSecretHeader.isBlank()) return webhookSecretHeader;
        if (authorizationHeader != null) {
            return authorizationHeader.startsWith("Bearer ")
                    ? authorizationHeader.substring(7)
                    : authorizationHeader;
        }
        return null;
    }
}
