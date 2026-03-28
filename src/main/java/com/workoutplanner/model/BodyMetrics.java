package com.workoutplanner.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "body_metrics")
public class BodyMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "User ID is required")
    @Column(name = "user_id", nullable = false)
    @JsonProperty("user_id")
    private String userId;

    @NotNull(message = "Weight is required")
    @PositiveOrZero(message = "Weight must be positive or zero")
    @JsonProperty("weight_kg")
    @Column(name = "weight_kg", nullable = false, precision = 5, scale = 2)
    private BigDecimal weightKg;

    @PositiveOrZero(message = "Body fat percentage must be positive or zero")
    @JsonProperty("body_fat_pct")
    @Column(name = "body_fat_pct", precision = 4, scale = 1)
    private BigDecimal bodyFatPct;

    @PositiveOrZero(message = "Muscle mass must be positive or zero")
    @JsonProperty("muscle_mass_kg")
    @Column(name = "muscle_mass_kg", precision = 5, scale = 2)
    private BigDecimal muscleMassKg;

    @PositiveOrZero(message = "Waist circumference must be positive or zero")
    @JsonProperty("waist_cm")
    @Column(name = "waist_cm", precision = 5, scale = 1)
    private BigDecimal waistCm;

    @NotNull(message = "Recorded date is required")
    @JsonProperty("recorded_at")
    @Column(name = "recorded_at", nullable = false)
    private LocalDate recordedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @JsonProperty("created_at")
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public BodyMetrics() {
        this.createdAt = LocalDateTime.now();
    }

    public BodyMetrics(String userId, BigDecimal weightKg, LocalDate recordedAt) {
        this();
        this.userId = userId;
        this.weightKg = weightKg;
        this.recordedAt = recordedAt;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
