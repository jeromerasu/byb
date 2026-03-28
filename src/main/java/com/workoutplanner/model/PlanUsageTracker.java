package com.workoutplanner.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "plan_usage_tracker")
public class PlanUsageTracker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "User ID is required")
    @Column(name = "user_id", nullable = false)
    @JsonProperty("user_id")
    private String userId;

    @NotNull(message = "Billing period start is required")
    @JsonProperty("billing_period_start")
    @Column(name = "billing_period_start", nullable = false)
    private LocalDate billingPeriodStart;

    @NotNull(message = "Billing period end is required")
    @JsonProperty("billing_period_end")
    @Column(name = "billing_period_end", nullable = false)
    private LocalDate billingPeriodEnd;

    @PositiveOrZero(message = "Plans generated must be zero or positive")
    @JsonProperty("plans_generated")
    @Column(name = "plans_generated", nullable = false)
    private int plansGenerated = 0;

    @NotNull(message = "Max plans allowed is required")
    @JsonProperty("max_plans_allowed")
    @Column(name = "max_plans_allowed", nullable = false)
    private int maxPlansAllowed;

    @JsonProperty("created_at")
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public PlanUsageTracker() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public PlanUsageTracker(String userId, LocalDate billingPeriodStart, LocalDate billingPeriodEnd,
                             int maxPlansAllowed) {
        this();
        this.userId = userId;
        this.billingPeriodStart = billingPeriodStart;
        this.billingPeriodEnd = billingPeriodEnd;
        this.maxPlansAllowed = maxPlansAllowed;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public LocalDate getBillingPeriodStart() { return billingPeriodStart; }
    public void setBillingPeriodStart(LocalDate billingPeriodStart) { this.billingPeriodStart = billingPeriodStart; }

    public LocalDate getBillingPeriodEnd() { return billingPeriodEnd; }
    public void setBillingPeriodEnd(LocalDate billingPeriodEnd) { this.billingPeriodEnd = billingPeriodEnd; }

    public int getPlansGenerated() { return plansGenerated; }
    public void setPlansGenerated(int plansGenerated) { this.plansGenerated = plansGenerated; }

    public int getMaxPlansAllowed() { return maxPlansAllowed; }
    public void setMaxPlansAllowed(int maxPlansAllowed) { this.maxPlansAllowed = maxPlansAllowed; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
