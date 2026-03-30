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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TASK-BILLING-001: Unit tests for BillingEntitlementService.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BillingEntitlementServiceTest {

    private static final Logger log = LoggerFactory.getLogger(BillingEntitlementServiceTest.class);

    @Mock
    private BillingEntitlementRepository billingEntitlementRepository;
    @Mock
    private WebhookEventLogRepository webhookEventLogRepository;
    @Mock
    private PlanUsageTrackerRepository planUsageTrackerRepository;

    private BillingEntitlementService service;

    @BeforeEach
    void setUp() {
        service = new BillingEntitlementService(
                billingEntitlementRepository,
                webhookEventLogRepository,
                planUsageTrackerRepository);
        ReflectionTestUtils.setField(service, "enforcementEnabled", false);
        when(webhookEventLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(billingEntitlementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // -------------------------------------------------------------------------
    // hasActivePremiumEntitlement
    // -------------------------------------------------------------------------

    @Test
    void hasActivePremiumEntitlement_WhenActiveAndPremium_ReturnsTrue() {
        BillingEntitlement ent = premiumEntitlement("user-1");
        when(billingEntitlementRepository.findActiveEntitlementByUserId("user-1"))
                .thenReturn(Optional.of(ent));

        boolean result = service.hasActivePremiumEntitlement("user-1");

        log.info("test.hasActivePremium result={}", result);
        assertTrue(result);
    }

    @Test
    void hasActivePremiumEntitlement_WhenFreeUser_ReturnsFalse() {
        when(billingEntitlementRepository.findActiveEntitlementByUserId("free-user"))
                .thenReturn(Optional.empty());

        boolean result = service.hasActivePremiumEntitlement("free-user");

        log.info("test.hasActivePremium freeUser result={}", result);
        assertFalse(result);
    }

    @Test
    void hasActivePremiumEntitlement_WhenEntitlementInactive_ReturnsFalse() {
        BillingEntitlement ent = premiumEntitlement("user-inactive");
        ent.setEntitlementActive(false);
        when(billingEntitlementRepository.findActiveEntitlementByUserId("user-inactive"))
                .thenReturn(Optional.empty()); // findActive returns empty when not active

        boolean result = service.hasActivePremiumEntitlement("user-inactive");

        log.info("test.hasActivePremium inactive result={}", result);
        assertFalse(result);
    }

    // -------------------------------------------------------------------------
    // getBillingStatus
    // -------------------------------------------------------------------------

    @Test
    void getBillingStatus_EnforcementDisabled_CanGeneratePlansAlwaysTrue() {
        ReflectionTestUtils.setField(service, "enforcementEnabled", false);
        when(billingEntitlementRepository.findByUserId("user-free"))
                .thenReturn(Optional.empty());

        BillingStatusDto status = service.getBillingStatus("user-free");

        log.info("test.billingStatus.enforcementOff planTier={} canGenerate={}", status.getPlanTier(), status.isCanGeneratePlans());
        assertEquals("FREE", status.getPlanTier());
        assertTrue(status.isCanGeneratePlans(), "canGeneratePlans should be true when enforcement disabled");
    }

    @Test
    void getBillingStatus_EnforcementEnabled_FreeUserCannotGenerate() {
        ReflectionTestUtils.setField(service, "enforcementEnabled", true);
        BillingEntitlement ent = new BillingEntitlement("user-free",
                BillingEntitlement.PlanTier.FREE, BillingEntitlement.SubscriptionStatus.ACTIVE);
        when(billingEntitlementRepository.findByUserId("user-free"))
                .thenReturn(Optional.of(ent));

        BillingStatusDto status = service.getBillingStatus("user-free");

        log.info("test.billingStatus.enforced freeUser canGenerate={}", status.isCanGeneratePlans());
        assertEquals("FREE", status.getPlanTier());
        assertFalse(status.isCanGeneratePlans(), "Free user cannot generate plans when enforcement enabled");
    }

    @Test
    void getBillingStatus_EnforcementEnabled_PremiumUserCanGenerate() {
        ReflectionTestUtils.setField(service, "enforcementEnabled", true);
        BillingEntitlement ent = premiumEntitlement("user-premium");
        when(billingEntitlementRepository.findByUserId("user-premium"))
                .thenReturn(Optional.of(ent));

        BillingStatusDto status = service.getBillingStatus("user-premium");

        log.info("test.billingStatus.enforced premiumUser canGenerate={}", status.isCanGeneratePlans());
        assertEquals("PREMIUM", status.getPlanTier());
        assertTrue(status.isCanGeneratePlans(), "Premium user can generate plans");
        assertTrue(status.isEntitlementActive());
    }

    @Test
    void getBillingStatus_PremiumUser_ReturnsCorrectShape() {
        BillingEntitlement ent = premiumEntitlement("user-shape");
        ent.setCurrentPeriodEnd(LocalDateTime.of(2026, 4, 28, 23, 59, 59));
        when(billingEntitlementRepository.findByUserId("user-shape"))
                .thenReturn(Optional.of(ent));

        BillingStatusDto status = service.getBillingStatus("user-shape");

        log.info("test.billingStatus.shape planTier={} status={} active={} periodEnd={}",
                status.getPlanTier(), status.getSubscriptionStatus(),
                status.isEntitlementActive(), status.getCurrentPeriodEnd());
        assertEquals("PREMIUM", status.getPlanTier());
        assertEquals("ACTIVE", status.getSubscriptionStatus());
        assertTrue(status.isEntitlementActive());
        assertNotNull(status.getCurrentPeriodEnd());
    }

    // -------------------------------------------------------------------------
    // linkProviderCustomerId
    // -------------------------------------------------------------------------

    @Test
    void linkProviderCustomerId_WhenEntitlementExists_UpdatesCustomerId() {
        BillingEntitlement ent = premiumEntitlement("user-link");
        when(billingEntitlementRepository.findByUserId("user-link"))
                .thenReturn(Optional.of(ent));

        service.linkProviderCustomerId("user-link", "rc_cust_123");

        ArgumentCaptor<BillingEntitlement> captor = ArgumentCaptor.forClass(BillingEntitlement.class);
        verify(billingEntitlementRepository).save(captor.capture());
        assertEquals("rc_cust_123", captor.getValue().getProviderCustomerId());
        log.info("test.linkCustomer customerId={}", captor.getValue().getProviderCustomerId());
    }

    @Test
    void linkProviderCustomerId_WhenNoEntitlement_CreatesAndLinks() {
        when(billingEntitlementRepository.findByUserId("new-user"))
                .thenReturn(Optional.empty());

        service.linkProviderCustomerId("new-user", "rc_new_456");

        ArgumentCaptor<BillingEntitlement> captor = ArgumentCaptor.forClass(BillingEntitlement.class);
        verify(billingEntitlementRepository).save(captor.capture());
        assertEquals("rc_new_456", captor.getValue().getProviderCustomerId());
        assertEquals("new-user", captor.getValue().getUserId());
        log.info("test.linkCustomer.new userId={} customerId={}", captor.getValue().getUserId(), captor.getValue().getProviderCustomerId());
    }

    // -------------------------------------------------------------------------
    // Webhook event handling — INITIAL_PURCHASE / RENEWAL
    // -------------------------------------------------------------------------

    @Test
    void processWebhookEvent_InitialPurchase_SetsActivePremiumEntitlement() {
        when(billingEntitlementRepository.findByUserId("rc-user"))
                .thenReturn(Optional.empty());

        RevenueCatWebhookDto dto = buildWebhook("INITIAL_PURCHASE", "rc-user", "rc_premium_monthly");
        service.processWebhookEvent(dto, "{\"event\":{\"type\":\"INITIAL_PURCHASE\"}}");

        ArgumentCaptor<BillingEntitlement> captor = ArgumentCaptor.forClass(BillingEntitlement.class);
        verify(billingEntitlementRepository).save(captor.capture());
        BillingEntitlement saved = captor.getValue();
        log.info("test.webhook.initialPurchase planTier={} active={}", saved.getPlanTier(), saved.isEntitlementActive());
        assertEquals(BillingEntitlement.PlanTier.PREMIUM, saved.getPlanTier());
        assertTrue(saved.isEntitlementActive());
        assertEquals(BillingEntitlement.SubscriptionStatus.ACTIVE, saved.getSubscriptionStatus());
    }

    @Test
    void processWebhookEvent_Renewal_ExtendsEntitlement() {
        BillingEntitlement existing = premiumEntitlement("renew-user");
        when(billingEntitlementRepository.findByUserId("renew-user"))
                .thenReturn(Optional.of(existing));

        RevenueCatWebhookDto dto = buildWebhook("RENEWAL", "renew-user", "rc_premium_monthly");
        service.processWebhookEvent(dto, "{}");

        ArgumentCaptor<BillingEntitlement> captor = ArgumentCaptor.forClass(BillingEntitlement.class);
        verify(billingEntitlementRepository).save(captor.capture());
        BillingEntitlement saved = captor.getValue();
        log.info("test.webhook.renewal status={} active={}", saved.getSubscriptionStatus(), saved.isEntitlementActive());
        assertEquals(BillingEntitlement.SubscriptionStatus.ACTIVE, saved.getSubscriptionStatus());
        assertTrue(saved.isEntitlementActive());
    }

    // -------------------------------------------------------------------------
    // Webhook event handling — CANCELLATION (access retained until period end)
    // -------------------------------------------------------------------------

    @Test
    void processWebhookEvent_Cancellation_KeepsEntitlementActiveUntilPeriodEnd() {
        BillingEntitlement existing = premiumEntitlement("cancel-user");
        existing.setCurrentPeriodEnd(LocalDateTime.now().plusDays(15)); // 15 days left
        when(billingEntitlementRepository.findByUserId("cancel-user"))
                .thenReturn(Optional.of(existing));

        RevenueCatWebhookDto dto = buildWebhook("CANCELLATION", "cancel-user", null);
        service.processWebhookEvent(dto, "{}");

        ArgumentCaptor<BillingEntitlement> captor = ArgumentCaptor.forClass(BillingEntitlement.class);
        verify(billingEntitlementRepository).save(captor.capture());
        BillingEntitlement saved = captor.getValue();
        log.info("test.webhook.cancellation status={} active={}", saved.getSubscriptionStatus(), saved.isEntitlementActive());
        assertEquals(BillingEntitlement.SubscriptionStatus.CANCELLED, saved.getSubscriptionStatus());
        assertTrue(saved.isEntitlementActive(), "Cancellation must NOT revoke access immediately — user retains access until period end");
    }

    // -------------------------------------------------------------------------
    // Webhook event handling — EXPIRATION
    // -------------------------------------------------------------------------

    @Test
    void processWebhookEvent_Expiration_RevokesAccess() {
        BillingEntitlement existing = premiumEntitlement("expire-user");
        when(billingEntitlementRepository.findByUserId("expire-user"))
                .thenReturn(Optional.of(existing));

        RevenueCatWebhookDto dto = buildWebhook("EXPIRATION", "expire-user", null);
        service.processWebhookEvent(dto, "{}");

        ArgumentCaptor<BillingEntitlement> captor = ArgumentCaptor.forClass(BillingEntitlement.class);
        verify(billingEntitlementRepository).save(captor.capture());
        BillingEntitlement saved = captor.getValue();
        log.info("test.webhook.expiration status={} active={}", saved.getSubscriptionStatus(), saved.isEntitlementActive());
        assertEquals(BillingEntitlement.SubscriptionStatus.EXPIRED, saved.getSubscriptionStatus());
        assertFalse(saved.isEntitlementActive(), "EXPIRATION must set entitlementActive=false");
    }

    // -------------------------------------------------------------------------
    // Webhook event handling — BILLING_ISSUE (grace period — access retained)
    // -------------------------------------------------------------------------

    @Test
    void processWebhookEvent_BillingIssue_KeepsAccessDuringGracePeriod() {
        BillingEntitlement existing = premiumEntitlement("billing-issue-user");
        when(billingEntitlementRepository.findByUserId("billing-issue-user"))
                .thenReturn(Optional.of(existing));

        RevenueCatWebhookDto dto = buildWebhook("BILLING_ISSUE", "billing-issue-user", null);
        service.processWebhookEvent(dto, "{}");

        ArgumentCaptor<BillingEntitlement> captor = ArgumentCaptor.forClass(BillingEntitlement.class);
        verify(billingEntitlementRepository).save(captor.capture());
        BillingEntitlement saved = captor.getValue();
        log.info("test.webhook.billingIssue status={} active={}", saved.getSubscriptionStatus(), saved.isEntitlementActive());
        assertEquals(BillingEntitlement.SubscriptionStatus.BILLING_ISSUE, saved.getSubscriptionStatus());
        assertTrue(saved.isEntitlementActive(), "BILLING_ISSUE must keep entitlementActive=true during grace period");
    }

    // -------------------------------------------------------------------------
    // Webhook event handling — PRODUCT_CHANGE
    // -------------------------------------------------------------------------

    @Test
    void processWebhookEvent_ProductChange_UpdatesPlanTier() {
        BillingEntitlement existing = premiumEntitlement("product-change-user");
        when(billingEntitlementRepository.findByUserId("product-change-user"))
                .thenReturn(Optional.of(existing));

        RevenueCatWebhookDto dto = buildWebhook("PRODUCT_CHANGE", "product-change-user", "rc_pro_annual");
        service.processWebhookEvent(dto, "{}");

        ArgumentCaptor<BillingEntitlement> captor = ArgumentCaptor.forClass(BillingEntitlement.class);
        verify(billingEntitlementRepository).save(captor.capture());
        BillingEntitlement saved = captor.getValue();
        log.info("test.webhook.productChange newTier={}", saved.getPlanTier());
        assertEquals(BillingEntitlement.PlanTier.PRO, saved.getPlanTier());
    }

    // -------------------------------------------------------------------------
    // Webhook event log persistence
    // -------------------------------------------------------------------------

    @Test
    void processWebhookEvent_LogEntryPersistedBeforeProcessing() {
        when(billingEntitlementRepository.findByUserId(anyString())).thenReturn(Optional.empty());

        RevenueCatWebhookDto dto = buildWebhook("INITIAL_PURCHASE", "log-user", "rc_premium");
        service.processWebhookEvent(dto, "{\"raw\":\"payload\"}");

        // Should have saved log entry (at least once for initial, once for success)
        ArgumentCaptor<WebhookEventLog> logCaptor = ArgumentCaptor.forClass(WebhookEventLog.class);
        verify(webhookEventLogRepository, atLeastOnce()).save(logCaptor.capture());

        WebhookEventLog firstLog = logCaptor.getAllValues().get(0);
        log.info("test.webhookLog eventType={} providerCustomerId={}", firstLog.getEventType(), firstLog.getProviderCustomerId());
        assertEquals("INITIAL_PURCHASE", firstLog.getEventType());
        assertEquals("{\"raw\":\"payload\"}", firstLog.getEventPayload());
    }

    @Test
    void processWebhookEvent_OnSuccess_SetsProcessedSuccessfullyTrue() {
        when(billingEntitlementRepository.findByUserId(anyString())).thenReturn(Optional.empty());

        RevenueCatWebhookDto dto = buildWebhook("INITIAL_PURCHASE", "success-log-user", "rc_premium");
        service.processWebhookEvent(dto, "{}");

        ArgumentCaptor<WebhookEventLog> logCaptor = ArgumentCaptor.forClass(WebhookEventLog.class);
        verify(webhookEventLogRepository, atLeastOnce()).save(logCaptor.capture());

        // Last save should have processedSuccessfully=true
        WebhookEventLog lastLog = logCaptor.getAllValues().get(logCaptor.getAllValues().size() - 1);
        log.info("test.webhookLog.success processedSuccessfully={}", lastLog.isProcessedSuccessfully());
        assertTrue(lastLog.isProcessedSuccessfully());
        assertNull(lastLog.getErrorMessage());
    }

    @Test
    void processWebhookEvent_OnFailure_SetsProcessedSuccessfullyFalseAndErrorMessage() {
        // Simulate processing failure
        when(billingEntitlementRepository.findByUserId(anyString()))
                .thenThrow(new RuntimeException("DB connection failed"));

        RevenueCatWebhookDto dto = buildWebhook("RENEWAL", "fail-log-user", "rc_premium");

        assertThrows(RuntimeException.class, () -> service.processWebhookEvent(dto, "{}"));

        ArgumentCaptor<WebhookEventLog> logCaptor = ArgumentCaptor.forClass(WebhookEventLog.class);
        verify(webhookEventLogRepository, atLeastOnce()).save(logCaptor.capture());

        // Last save should have processedSuccessfully=false and errorMessage set
        WebhookEventLog lastLog = logCaptor.getAllValues().get(logCaptor.getAllValues().size() - 1);
        log.info("test.webhookLog.failure processedSuccessfully={} errorMessage={}",
                lastLog.isProcessedSuccessfully(), lastLog.getErrorMessage());
        assertFalse(lastLog.isProcessedSuccessfully());
        assertNotNull(lastLog.getErrorMessage());
        assertEquals("DB connection failed", lastLog.getErrorMessage());
    }

    // -------------------------------------------------------------------------
    // Usage tracking
    // -------------------------------------------------------------------------

    @Test
    void incrementPlanUsage_WhenTrackerExists_IncrementsCount() {
        PlanUsageTracker tracker = new PlanUsageTracker("usage-user",
                LocalDate.now().withDayOfMonth(1),
                LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()),
                10);
        tracker.setPlansGenerated(2);

        when(planUsageTrackerRepository.findActiveByUserIdAndDate(eq("usage-user"), any()))
                .thenReturn(Optional.of(tracker));

        service.incrementPlanUsage("usage-user");

        ArgumentCaptor<PlanUsageTracker> captor = ArgumentCaptor.forClass(PlanUsageTracker.class);
        verify(planUsageTrackerRepository).save(captor.capture());
        log.info("test.usage.increment plansGenerated={}", captor.getValue().getPlansGenerated());
        assertEquals(3, captor.getValue().getPlansGenerated());
    }

    @Test
    void incrementPlanUsage_WhenNoTrackerExists_CreatesAndIncrements() {
        when(planUsageTrackerRepository.findActiveByUserIdAndDate(eq("new-usage-user"), any()))
                .thenReturn(Optional.empty());
        when(planUsageTrackerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(billingEntitlementRepository.findActiveEntitlementByUserId("new-usage-user"))
                .thenReturn(Optional.empty());

        service.incrementPlanUsage("new-usage-user");

        verify(planUsageTrackerRepository, times(2)).save(any()); // once to create, once to increment
        log.info("test.usage.createAndIncrement verified save called twice");
    }

    @Test
    void getUsageForCurrentPeriod_WhenTrackerExists_ReturnsCorrectDto() {
        PlanUsageTracker tracker = new PlanUsageTracker("usage-query-user",
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                10);
        tracker.setPlansGenerated(5);

        when(planUsageTrackerRepository.findActiveByUserIdAndDate(eq("usage-query-user"), any()))
                .thenReturn(Optional.of(tracker));

        BillingUsageDto dto = service.getUsageForCurrentPeriod("usage-query-user");

        log.info("test.usage.query plans={} start={} end={}",
                dto.getPlansGeneratedThisPeriod(), dto.getBillingPeriodStart(), dto.getBillingPeriodEnd());
        assertEquals(5, dto.getPlansGeneratedThisPeriod());
        assertEquals(LocalDate.of(2026, 3, 1), dto.getBillingPeriodStart());
        assertEquals(LocalDate.of(2026, 3, 31), dto.getBillingPeriodEnd());
    }

    @Test
    void getUsageForCurrentPeriod_WhenNoTracker_ReturnsZero() {
        when(planUsageTrackerRepository.findActiveByUserIdAndDate(eq("zero-usage-user"), any()))
                .thenReturn(Optional.empty());

        BillingUsageDto dto = service.getUsageForCurrentPeriod("zero-usage-user");

        log.info("test.usage.zero plans={}", dto.getPlansGeneratedThisPeriod());
        assertEquals(0, dto.getPlansGeneratedThisPeriod());
        assertNotNull(dto.getBillingPeriodStart());
        assertNotNull(dto.getBillingPeriodEnd());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private BillingEntitlement premiumEntitlement(String userId) {
        BillingEntitlement ent = new BillingEntitlement(userId,
                BillingEntitlement.PlanTier.PREMIUM,
                BillingEntitlement.SubscriptionStatus.ACTIVE);
        ent.setEntitlementActive(true);
        return ent;
    }

    private RevenueCatWebhookDto buildWebhook(String eventType, String appUserId, String productId) {
        RevenueCatWebhookDto dto = new RevenueCatWebhookDto();
        RevenueCatWebhookDto.Event event = new RevenueCatWebhookDto.Event();
        event.setType(eventType);
        event.setAppUserId(appUserId);
        event.setOriginalAppUserId(appUserId);
        event.setProductId(productId);
        event.setEventTimestampMs(System.currentTimeMillis());
        event.setExpirationAtMs(System.currentTimeMillis() + 30L * 24 * 3600 * 1000); // +30 days
        dto.setEvent(event);
        return dto;
    }
}
