package com.workoutplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public class BillingUsageDto {

    @JsonProperty("plansGeneratedThisPeriod")
    private int plansGeneratedThisPeriod;

    @JsonProperty("billingPeriodStart")
    private LocalDate billingPeriodStart;

    @JsonProperty("billingPeriodEnd")
    private LocalDate billingPeriodEnd;

    public BillingUsageDto() {}

    public BillingUsageDto(int plansGeneratedThisPeriod, LocalDate billingPeriodStart, LocalDate billingPeriodEnd) {
        this.plansGeneratedThisPeriod = plansGeneratedThisPeriod;
        this.billingPeriodStart = billingPeriodStart;
        this.billingPeriodEnd = billingPeriodEnd;
    }

    public int getPlansGeneratedThisPeriod() { return plansGeneratedThisPeriod; }
    public void setPlansGeneratedThisPeriod(int plansGeneratedThisPeriod) { this.plansGeneratedThisPeriod = plansGeneratedThisPeriod; }

    public LocalDate getBillingPeriodStart() { return billingPeriodStart; }
    public void setBillingPeriodStart(LocalDate billingPeriodStart) { this.billingPeriodStart = billingPeriodStart; }

    public LocalDate getBillingPeriodEnd() { return billingPeriodEnd; }
    public void setBillingPeriodEnd(LocalDate billingPeriodEnd) { this.billingPeriodEnd = billingPeriodEnd; }
}
