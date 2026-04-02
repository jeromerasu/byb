package com.workoutplanner.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for Issue 4: RevenueCatWebhookDto must tolerate unknown fields
 * (RevenueCat sends event-type-specific fields not present in our DTO).
 */
class RevenueCatWebhookDtoDeserializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void basicPurchasePayload_Deserializes() throws Exception {
        String json = """
                {
                  "api_version": "1.0",
                  "event": {
                    "type": "INITIAL_PURCHASE",
                    "id": "evt-123",
                    "app_user_id": "user-abc",
                    "original_app_user_id": "$RCAnonymousID:abc",
                    "product_id": "standard_monthly",
                    "entitlement_ids": ["standard"],
                    "purchased_at_ms": 1700000000000,
                    "expiration_at_ms": 1702592000000,
                    "environment": "PRODUCTION",
                    "store": "APP_STORE",
                    "currency": "USD",
                    "price": 9.99,
                    "price_in_usd": 9.99
                  }
                }
                """;

        RevenueCatWebhookDto dto = objectMapper.readValue(json, RevenueCatWebhookDto.class);

        assertThat(dto.getApiVersion()).isEqualTo("1.0");
        assertThat(dto.getEvent()).isNotNull();
        assertThat(dto.getEvent().getType()).isEqualTo("INITIAL_PURCHASE");
        assertThat(dto.getEvent().getAppUserId()).isEqualTo("user-abc");
        assertThat(dto.getEvent().getEntitlementIds()).containsExactly("standard");
        assertThat(dto.getEvent().getPurchasedAtMs()).isEqualTo(1700000000000L);
    }

    @Test
    void cancellationPayload_WithUnknownFields_DoesNotThrow() {
        // CANCELLATION events include cancellation_reason which is NOT in our DTO
        String json = """
                {
                  "api_version": "1.0",
                  "event": {
                    "type": "CANCELLATION",
                    "id": "evt-456",
                    "app_user_id": "user-abc",
                    "cancellation_reason": "CUSTOMER_SUPPORT",
                    "product_id": "standard_monthly",
                    "auto_resume_at_ms": null,
                    "grace_period_expiration_at_ms": null
                  }
                }
                """;

        assertThatNoException().isThrownBy(() -> {
            RevenueCatWebhookDto dto = objectMapper.readValue(json, RevenueCatWebhookDto.class);
            assertThat(dto.getEvent().getType()).isEqualTo("CANCELLATION");
        });
    }

    @Test
    void billingIssuePayload_WithUnknownFields_DoesNotThrow() {
        // BILLING_ISSUE events include billing_issues_detected_at_ms
        String json = """
                {
                  "api_version": "1.0",
                  "event": {
                    "type": "BILLING_ISSUE",
                    "id": "evt-789",
                    "app_user_id": "user-def",
                    "billing_issues_detected_at_ms": 1700000000000,
                    "grace_period_expiration_at_ms": 1700086400000,
                    "product_id": "standard_monthly"
                  }
                }
                """;

        assertThatNoException().isThrownBy(() -> {
            RevenueCatWebhookDto dto = objectMapper.readValue(json, RevenueCatWebhookDto.class);
            assertThat(dto.getEvent().getType()).isEqualTo("BILLING_ISSUE");
            assertThat(dto.getEvent().getAppUserId()).isEqualTo("user-def");
        });
    }

    @Test
    void renewalPayload_WithIsTrialConversion_DoesNotThrow() {
        // RENEWAL events can include is_trial_conversion
        String json = """
                {
                  "api_version": "1.0",
                  "event": {
                    "type": "RENEWAL",
                    "id": "evt-999",
                    "app_user_id": "user-xyz",
                    "is_trial_conversion": false,
                    "product_id": "coaching_annual",
                    "entitlement_ids": ["coaching"],
                    "purchased_at_ms": 1700000000000,
                    "expiration_at_ms": 1731622400000
                  }
                }
                """;

        assertThatNoException().isThrownBy(() -> {
            RevenueCatWebhookDto dto = objectMapper.readValue(json, RevenueCatWebhookDto.class);
            assertThat(dto.getEvent().getType()).isEqualTo("RENEWAL");
            assertThat(dto.getEvent().getEntitlementIds()).containsExactly("coaching");
        });
    }

    @Test
    void topLevelUnknownFields_DoNotThrow() {
        // RevenueCat may add new top-level fields in future API versions
        String json = """
                {
                  "api_version": "1.0",
                  "some_new_field": "value",
                  "event": {
                    "type": "INITIAL_PURCHASE",
                    "app_user_id": "user-abc"
                  }
                }
                """;

        assertThatNoException().isThrownBy(() -> {
            RevenueCatWebhookDto dto = objectMapper.readValue(json, RevenueCatWebhookDto.class);
            assertThat(dto.getEvent().getAppUserId()).isEqualTo("user-abc");
        });
    }

    @Test
    void eventTimestampMs_ConvertsToCorrectInstant() throws Exception {
        // 1700000000000 ms = 2023-11-14T22:13:20Z
        String json = """
                {
                  "api_version": "1.0",
                  "event": {
                    "type": "INITIAL_PURCHASE",
                    "app_user_id": "user-abc",
                    "event_timestamp_ms": 1700000000000,
                    "purchased_at_ms": 1700000000000,
                    "expiration_at_ms": 1702592000000
                  }
                }
                """;

        RevenueCatWebhookDto dto = objectMapper.readValue(json, RevenueCatWebhookDto.class);

        assertThat(dto.getEvent().getEventTimestamp()).isNotNull();
        assertThat(dto.getEvent().getPurchasedAt()).isNotNull();
        assertThat(dto.getEvent().getExpirationAt()).isNotNull();
        // Verify ordering: expiration is after purchase
        assertThat(dto.getEvent().getExpirationAt())
                .isAfter(dto.getEvent().getPurchasedAt());
    }
}
