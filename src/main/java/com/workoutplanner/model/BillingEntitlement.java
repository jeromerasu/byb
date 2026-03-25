package com.workoutplanner.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "billing_entitlements")
public class BillingEntitlement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotBlank(message = "User ID is required")
    @Column(name = "user_id", nullable = false)
    @JsonProperty("user_id")
    private String userId;

    @Column(name = "provider_customer_id")
    @JsonProperty("provider_customer_id")
    private String providerCustomerId;

    @Column(name = "provider_subscription_id")
    @JsonProperty("provider_subscription_id")
    private String providerSubscriptionId;

    @NotNull(message = "Plan tier is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "plan_tier", nullable = false)
    @JsonProperty("plan_tier")
    private PlanTier planTier;

    @NotNull(message = "Subscription status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_status", nullable = false)
    @JsonProperty("subscription_status")
    private SubscriptionStatus subscriptionStatus;

    @Column(name = "current_period_end")
    @JsonProperty("current_period_end")
    private LocalDateTime currentPeriodEnd;

    @Column(name = "entitlement_active", nullable = false)
    @JsonProperty("entitlement_active")
    private boolean entitlementActive = false;

    @Column(name = "created_at", nullable = false)
    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_webhook_event")
    @JsonProperty("last_webhook_event")
    private String lastWebhookEvent;

    @Column(name = "webhook_event_timestamp")
    @JsonProperty("webhook_event_timestamp")
    private LocalDateTime webhookEventTimestamp;

    public enum PlanTier {
        FREE, PREMIUM, PRO
    }

    public enum SubscriptionStatus {
        ACTIVE,
        CANCELLED,
        EXPIRED,
        GRACE_PERIOD,
        BILLING_ISSUE,
        PAUSED,
        PENDING
    }

    public BillingEntitlement() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public BillingEntitlement(String userId, PlanTier planTier, SubscriptionStatus subscriptionStatus) {
        this();
        this.userId = userId;
        this.planTier = planTier;
        this.subscriptionStatus = subscriptionStatus;
        this.entitlementActive = isStatusActiveForEntitlement(subscriptionStatus);
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    private boolean isStatusActiveForEntitlement(SubscriptionStatus status) {
        return status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.GRACE_PERIOD;
    }

    public void updateSubscriptionStatus(SubscriptionStatus newStatus) {
        this.subscriptionStatus = newStatus;
        this.entitlementActive = isStatusActiveForEntitlement(newStatus);
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProviderCustomerId() {
        return providerCustomerId;
    }

    public void setProviderCustomerId(String providerCustomerId) {
        this.providerCustomerId = providerCustomerId;
    }

    public String getProviderSubscriptionId() {
        return providerSubscriptionId;
    }

    public void setProviderSubscriptionId(String providerSubscriptionId) {
        this.providerSubscriptionId = providerSubscriptionId;
    }

    public PlanTier getPlanTier() {
        return planTier;
    }

    public void setPlanTier(PlanTier planTier) {
        this.planTier = planTier;
    }

    public SubscriptionStatus getSubscriptionStatus() {
        return subscriptionStatus;
    }

    public void setSubscriptionStatus(SubscriptionStatus subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
        this.entitlementActive = isStatusActiveForEntitlement(subscriptionStatus);
    }

    public LocalDateTime getCurrentPeriodEnd() {
        return currentPeriodEnd;
    }

    public void setCurrentPeriodEnd(LocalDateTime currentPeriodEnd) {
        this.currentPeriodEnd = currentPeriodEnd;
    }

    public boolean isEntitlementActive() {
        return entitlementActive;
    }

    public void setEntitlementActive(boolean entitlementActive) {
        this.entitlementActive = entitlementActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getLastWebhookEvent() {
        return lastWebhookEvent;
    }

    public void setLastWebhookEvent(String lastWebhookEvent) {
        this.lastWebhookEvent = lastWebhookEvent;
    }

    public LocalDateTime getWebhookEventTimestamp() {
        return webhookEventTimestamp;
    }

    public void setWebhookEventTimestamp(LocalDateTime webhookEventTimestamp) {
        this.webhookEventTimestamp = webhookEventTimestamp;
    }

    // Helper methods
    public boolean hasActivePremiumEntitlement() {
        return entitlementActive && (planTier == PlanTier.PREMIUM || planTier == PlanTier.PRO);
    }

    public boolean isSubscriptionExpired() {
        return currentPeriodEnd != null && currentPeriodEnd.isBefore(LocalDateTime.now());
    }

    public boolean isInGracePeriod() {
        return subscriptionStatus == SubscriptionStatus.GRACE_PERIOD;
    }
}