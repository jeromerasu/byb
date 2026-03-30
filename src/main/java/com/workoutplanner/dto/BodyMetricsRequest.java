package com.workoutplanner.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BodyMetricsRequest {

    @NotNull(message = "Weight is required")
    @PositiveOrZero(message = "Weight must be positive or zero")
    private BigDecimal weightKg;

    @PositiveOrZero(message = "Body fat percentage must be positive or zero")
    private BigDecimal bodyFatPct;

    @PositiveOrZero(message = "Muscle mass must be positive or zero")
    private BigDecimal muscleMassKg;

    @PositiveOrZero(message = "Waist circumference must be positive or zero")
    private BigDecimal waistCm;

    @NotNull(message = "Recorded date is required")
    private LocalDate recordedAt;

    private String notes;

    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }

    public BigDecimal getBodyFatPct() { return bodyFatPct; }
    public void setBodyFatPct(BigDecimal bodyFatPct) { this.bodyFatPct = bodyFatPct; }

    public BigDecimal getMuscleMassKg() { return muscleMassKg; }
    public void setMuscleMassKg(BigDecimal muscleMassKg) { this.muscleMassKg = muscleMassKg; }

    public BigDecimal getWaistCm() { return waistCm; }
    public void setWaistCm(BigDecimal waistCm) { this.waistCm = waistCm; }

    public LocalDate getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDate recordedAt) { this.recordedAt = recordedAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
