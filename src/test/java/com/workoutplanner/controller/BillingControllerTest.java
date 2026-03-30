package com.workoutplanner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workoutplanner.dto.BillingStatusDto;
import com.workoutplanner.dto.BillingUsageDto;
import com.workoutplanner.dto.LinkCustomerRequestDto;
import com.workoutplanner.service.BillingEntitlementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TASK-BILLING-001: Unit tests for BillingController.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BillingControllerTest {

    private static final Logger log = LoggerFactory.getLogger(BillingControllerTest.class);

    @Mock
    private BillingEntitlementService billingEntitlementService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Authentication authentication;

    private BillingController controller;

    @BeforeEach
    void setUp() {
        controller = new BillingController(billingEntitlementService, objectMapper);
        ReflectionTestUtils.setField(controller, "webhookSecret", "");
        when(authentication.getName()).thenReturn("test-user");
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/billing/status
    // -------------------------------------------------------------------------

    @Test
    void getBillingStatus_WhenUnauthenticated_Returns401() {
        ResponseEntity<?> response = controller.getBillingStatus(null);

        log.info("test.billingStatus.unauth status={}", response.getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void getBillingStatus_WhenAuthenticated_ReturnsStatusDto() {
        BillingStatusDto expected = new BillingStatusDto("PREMIUM", "ACTIVE", true,
                LocalDateTime.of(2026, 4, 28, 23, 59), true);
        when(billingEntitlementService.getBillingStatus("test-user")).thenReturn(expected);

        ResponseEntity<?> response = controller.getBillingStatus(authentication);

        log.info("test.billingStatus.ok status={}", response.getStatusCode());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }

    @Test
    void getBillingStatus_WhenServiceThrows_Returns500() {
        when(billingEntitlementService.getBillingStatus(anyString()))
                .thenThrow(new RuntimeException("DB error"));

        ResponseEntity<?> response = controller.getBillingStatus(authentication);

        log.info("test.billingStatus.error status={}", response.getStatusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/billing/link-customer
    // -------------------------------------------------------------------------

    @Test
    void linkCustomer_WhenUnauthenticated_Returns401() {
        LinkCustomerRequestDto req = new LinkCustomerRequestDto("rc_123");

        ResponseEntity<?> response = controller.linkCustomer(req, null);

        log.info("test.linkCustomer.unauth status={}", response.getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void linkCustomer_WhenValidRequest_Returns200() {
        LinkCustomerRequestDto req = new LinkCustomerRequestDto("rc_customer_abc123");
        doNothing().when(billingEntitlementService).linkProviderCustomerId("test-user", "rc_customer_abc123");

        ResponseEntity<?> response = controller.linkCustomer(req, authentication);

        log.info("test.linkCustomer.ok status={}", response.getStatusCode());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(billingEntitlementService).linkProviderCustomerId("test-user", "rc_customer_abc123");
    }

    @Test
    void linkCustomer_WhenMissingCustomerId_Returns400() {
        LinkCustomerRequestDto req = new LinkCustomerRequestDto(null);

        ResponseEntity<?> response = controller.linkCustomer(req, authentication);

        log.info("test.linkCustomer.missingId status={}", response.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(billingEntitlementService);
    }

    @Test
    void linkCustomer_WhenBlankCustomerId_Returns400() {
        LinkCustomerRequestDto req = new LinkCustomerRequestDto("  ");

        ResponseEntity<?> response = controller.linkCustomer(req, authentication);

        log.info("test.linkCustomer.blankId status={}", response.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void linkCustomer_WhenServiceThrows_Returns500() {
        LinkCustomerRequestDto req = new LinkCustomerRequestDto("rc_123");
        doThrow(new RuntimeException("DB error"))
                .when(billingEntitlementService).linkProviderCustomerId(anyString(), anyString());

        ResponseEntity<?> response = controller.linkCustomer(req, authentication);

        log.info("test.linkCustomer.error status={}", response.getStatusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/billing/usage
    // -------------------------------------------------------------------------

    @Test
    void getBillingUsage_WhenUnauthenticated_Returns401() {
        ResponseEntity<?> response = controller.getBillingUsage(null);

        log.info("test.billingUsage.unauth status={}", response.getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void getBillingUsage_WhenAuthenticated_ReturnsUsageDto() {
        BillingUsageDto expected = new BillingUsageDto(3, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
        when(billingEntitlementService.getUsageForCurrentPeriod("test-user")).thenReturn(expected);

        ResponseEntity<?> response = controller.getBillingUsage(authentication);

        log.info("test.billingUsage.ok status={} plans={}", response.getStatusCode(),
                ((BillingUsageDto) response.getBody()).getPlansGeneratedThisPeriod());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }

    @Test
    void getBillingUsage_WhenServiceThrows_Returns500() {
        when(billingEntitlementService.getUsageForCurrentPeriod(anyString()))
                .thenThrow(new RuntimeException("Query failed"));

        ResponseEntity<?> response = controller.getBillingUsage(authentication);

        log.info("test.billingUsage.error status={}", response.getStatusCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/billing/webhooks/revenuecat — secret validation
    // -------------------------------------------------------------------------

    @Test
    void handleWebhook_WhenSecretConfiguredAndMissingHeader_Returns401() {
        ReflectionTestUtils.setField(controller, "webhookSecret", "my-secret");

        ResponseEntity<?> response = controller.handleRevenueCatWebhook(
                "{\"event\":{\"type\":\"INITIAL_PURCHASE\",\"app_user_id\":\"user1\"}}",
                null,
                null  // missing signature
        );

        log.info("test.webhook.missingSecret status={}", response.getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void handleWebhook_WhenSecretConfiguredAndWrongSignature_Returns401() {
        ReflectionTestUtils.setField(controller, "webhookSecret", "my-secret");

        ResponseEntity<?> response = controller.handleRevenueCatWebhook(
                "{\"event\":{\"type\":\"INITIAL_PURCHASE\"}}",
                null,
                "wrong-signature"
        );

        log.info("test.webhook.wrongSecret status={}", response.getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void handleWebhook_WhenNoSecretConfigured_ParsesPayload() throws Exception {
        ReflectionTestUtils.setField(controller, "webhookSecret", "");

        // Simulate parse error returning null
        when(objectMapper.readValue(anyString(), eq(com.workoutplanner.dto.RevenueCatWebhookDto.class)))
                .thenReturn(null);

        ResponseEntity<java.util.Map<String, String>> response = controller.handleRevenueCatWebhook(
                "invalid",
                null,
                null
        );

        log.info("test.webhook.noSecret.nullDto status={}", response.getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
