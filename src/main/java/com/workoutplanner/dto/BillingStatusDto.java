package com.workoutplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class BillingStatusDto {

    @JsonProperty("planTier")
    private String planTier;

    @JsonProperty("subscriptionStatus")
    private String subscriptionStatus;

    @JsonProperty("entitlementActive")
    private boolean entitlementActive;

    @JsonProperty("currentPeriodEnd")
    private LocalDateTime currentPeriodEnd;

    @JsonProperty("canGeneratePlans")
    private boolean canGeneratePlans;

    public BillingStatusDto() {}

    public BillingStatusDto(String planTier, String subscriptionStatus, boolean entitlementActive,
                            LocalDateTime currentPeriodEnd, boolean canGeneratePlans) {
        this.planTier = planTier;
        this.subscriptionStatus = subscriptionStatus;
        this.entitlementActive = entitlementActive;
        this.currentPeriodEnd = currentPeriodEnd;
        this.canGeneratePlans = canGeneratePlans;
    }

    public String getPlanTier() { return planTier; }
    public void setPlanTier(String planTier) { this.planTier = planTier; }

    public String getSubscriptionStatus() { return subscriptionStatus; }
    public void setSubscriptionStatus(String subscriptionStatus) { this.subscriptionStatus = subscriptionStatus; }

    public boolean isEntitlementActive() { return entitlementActive; }
    public void setEntitlementActive(boolean entitlementActive) { this.entitlementActive = entitlementActive; }

    public LocalDateTime getCurrentPeriodEnd() { return currentPeriodEnd; }
    public void setCurrentPeriodEnd(LocalDateTime currentPeriodEnd) { this.currentPeriodEnd = currentPeriodEnd; }

    public boolean isCanGeneratePlans() { return canGeneratePlans; }
    public void setCanGeneratePlans(boolean canGeneratePlans) { this.canGeneratePlans = canGeneratePlans; }
}
