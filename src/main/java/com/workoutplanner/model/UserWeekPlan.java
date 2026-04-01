package com.workoutplanner.model;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Registry row tracking which storage keys hold the plan for a given (user, week).
 * Closes TASK-BE-016C gap.
 *
 * Unique constraint on (user_id, week_start) enforces upsert semantics:
 * a second generation for the same week overwrites the storage keys.
 */
@Entity
@Table(
    name = "user_week_plan",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_user_week_plan_user_week",
        columnNames = {"user_id", "week_start"}
    )
)
public class UserWeekPlan {

    @Id
    @UuidGenerator
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "workout_storage_key", length = 1000)
    private String workoutStorageKey;

    @Column(name = "diet_storage_key", length = 1000)
    private String dietStorageKey;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "generated_by", nullable = false, length = 50)
    private GeneratedBy generatedBy = GeneratedBy.CRON;

    @PrePersist
    protected void onCreate() {
        if (generatedAt == null) generatedAt = LocalDateTime.now();
    }

    public UserWeekPlan() {}

    public UserWeekPlan(String userId, LocalDate weekStart, String workoutStorageKey,
                        String dietStorageKey, GeneratedBy generatedBy) {
        this.userId = userId;
        this.weekStart = weekStart;
        this.workoutStorageKey = workoutStorageKey;
        this.dietStorageKey = dietStorageKey;
        this.generatedBy = generatedBy;
        this.generatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public LocalDate getWeekStart() { return weekStart; }
    public void setWeekStart(LocalDate weekStart) { this.weekStart = weekStart; }

    public String getWorkoutStorageKey() { return workoutStorageKey; }
    public void setWorkoutStorageKey(String workoutStorageKey) { this.workoutStorageKey = workoutStorageKey; }

    public String getDietStorageKey() { return dietStorageKey; }
    public void setDietStorageKey(String dietStorageKey) { this.dietStorageKey = dietStorageKey; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public GeneratedBy getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(GeneratedBy generatedBy) { this.generatedBy = generatedBy; }
}
