package com.workoutplanner.dto;

import com.workoutplanner.model.BodyMetrics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class BodyMetricsResponse {

    private Long id;
    private String userId;
    private BigDecimal weightKg;
    private BigDecimal bodyFatPct;
    private BigDecimal muscleMassKg;
    private BigDecimal waistCm;
    private LocalDate recordedAt;
    private String notes;
    private LocalDateTime createdAt;

    public static BodyMetricsResponse from(BodyMetrics entity) {
        BodyMetricsResponse dto = new BodyMetricsResponse();
        dto.id = entity.getId();
        dto.userId = entity.getUserId();
        dto.weightKg = entity.getWeightKg();
        dto.bodyFatPct = entity.getBodyFatPct();
        dto.muscleMassKg = entity.getMuscleMassKg();
        dto.waistCm = entity.getWaistCm();
        dto.recordedAt = entity.getRecordedAt();
        dto.notes = entity.getNotes();
        dto.createdAt = entity.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public BigDecimal getWeightKg() { return weightKg; }
    public BigDecimal getBodyFatPct() { return bodyFatPct; }
    public BigDecimal getMuscleMassKg() { return muscleMassKg; }
    public BigDecimal getWaistCm() { return waistCm; }
    public LocalDate getRecordedAt() { return recordedAt; }
    public String getNotes() { return notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
