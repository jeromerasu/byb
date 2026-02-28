package com.workoutplanner.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "workout_profile")
public class WorkoutProfile {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    @JsonProperty("user_id")
    private String userId;

    // Profile preferences
    @Enumerated(EnumType.STRING)
    @Column(name = "fitness_level")
    @JsonProperty("fitness_level")
    private FitnessLevel fitnessLevel;

    @Column(name = "workout_frequency")
    @JsonProperty("workout_frequency")
    private Integer workoutFrequency; // per week

    @Column(name = "session_duration")
    @JsonProperty("session_duration")
    private Integer sessionDuration; // minutes

    @Column(name = "preferred_workout_types")
    @JsonProperty("preferred_workout_types")
    private String[] preferredWorkoutTypes;

    @Column(name = "available_equipment")
    @JsonProperty("available_equipment")
    private String[] availableEquipment;

    @Column(name = "target_goals")
    @JsonProperty("target_goals")
    private String[] targetGoals;

    // Physical information
    @Column(name = "height_cm")
    @JsonProperty("height_cm")
    private Integer heightCm;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    @JsonProperty("weight_kg")
    private BigDecimal weightKg;

    @Column(name = "age")
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_level")
    @JsonProperty("activity_level")
    private ActivityLevel activityLevel;

    // Current plan object storage
    @Column(name = "current_plan_storage_key")
    @JsonProperty("current_plan_storage_key")
    private String currentPlanStorageKey;

    @Column(name = "current_plan_title")
    @JsonProperty("current_plan_title")
    private String currentPlanTitle;

    @Column(name = "current_plan_created_at")
    @JsonProperty("current_plan_created_at")
    private LocalDateTime currentPlanCreatedAt;

    @Column(name = "current_plan_file_size")
    @JsonProperty("current_plan_file_size")
    private Long currentPlanFileSize;

    // Tracking
    @CreationTimestamp
    @Column(name = "created_at")
    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_workout")
    @JsonProperty("last_workout")
    private LocalDateTime lastWorkout;

    @Column(name = "total_workouts_completed")
    @JsonProperty("total_workouts_completed")
    private Integer totalWorkoutsCompleted = 0;

    // Lazy-loaded user relationship
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @JsonIgnore
    private User user;

    // Enums
    public enum FitnessLevel {
        BEGINNER,
        INTERMEDIATE,
        ADVANCED,
        EXPERT
    }

    public enum Gender {
        MALE,
        FEMALE,
        OTHER,
        PREFER_NOT_TO_SAY
    }

    public enum ActivityLevel {
        SEDENTARY,
        LIGHTLY_ACTIVE,
        MODERATELY_ACTIVE,
        VERY_ACTIVE,
        EXTREMELY_ACTIVE
    }

    // Constructors
    public WorkoutProfile() {}

    public WorkoutProfile(String id, String userId) {
        this.id = id;
        this.userId = userId;
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

    public FitnessLevel getFitnessLevel() {
        return fitnessLevel;
    }

    public void setFitnessLevel(FitnessLevel fitnessLevel) {
        this.fitnessLevel = fitnessLevel;
    }

    public Integer getWorkoutFrequency() {
        return workoutFrequency;
    }

    public void setWorkoutFrequency(Integer workoutFrequency) {
        this.workoutFrequency = workoutFrequency;
    }

    public Integer getSessionDuration() {
        return sessionDuration;
    }

    public void setSessionDuration(Integer sessionDuration) {
        this.sessionDuration = sessionDuration;
    }

    public String[] getPreferredWorkoutTypes() {
        return preferredWorkoutTypes;
    }

    public void setPreferredWorkoutTypes(String[] preferredWorkoutTypes) {
        this.preferredWorkoutTypes = preferredWorkoutTypes;
    }

    public String[] getAvailableEquipment() {
        return availableEquipment;
    }

    public void setAvailableEquipment(String[] availableEquipment) {
        this.availableEquipment = availableEquipment;
    }

    public String[] getTargetGoals() {
        return targetGoals;
    }

    public void setTargetGoals(String[] targetGoals) {
        this.targetGoals = targetGoals;
    }

    public Integer getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(Integer heightCm) {
        this.heightCm = heightCm;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public ActivityLevel getActivityLevel() {
        return activityLevel;
    }

    public void setActivityLevel(ActivityLevel activityLevel) {
        this.activityLevel = activityLevel;
    }

    public String getCurrentPlanStorageKey() {
        return currentPlanStorageKey;
    }

    public void setCurrentPlanStorageKey(String currentPlanStorageKey) {
        this.currentPlanStorageKey = currentPlanStorageKey;
    }

    public String getCurrentPlanTitle() {
        return currentPlanTitle;
    }

    public void setCurrentPlanTitle(String currentPlanTitle) {
        this.currentPlanTitle = currentPlanTitle;
    }

    public LocalDateTime getCurrentPlanCreatedAt() {
        return currentPlanCreatedAt;
    }

    public void setCurrentPlanCreatedAt(LocalDateTime currentPlanCreatedAt) {
        this.currentPlanCreatedAt = currentPlanCreatedAt;
    }

    public Long getCurrentPlanFileSize() {
        return currentPlanFileSize;
    }

    public void setCurrentPlanFileSize(Long currentPlanFileSize) {
        this.currentPlanFileSize = currentPlanFileSize;
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

    public LocalDateTime getLastWorkout() {
        return lastWorkout;
    }

    public void setLastWorkout(LocalDateTime lastWorkout) {
        this.lastWorkout = lastWorkout;
    }

    public Integer getTotalWorkoutsCompleted() {
        return totalWorkoutsCompleted;
    }

    public void setTotalWorkoutsCompleted(Integer totalWorkoutsCompleted) {
        this.totalWorkoutsCompleted = totalWorkoutsCompleted;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    // Helper methods
    public void updateCurrentPlan(String storageKey, String title, Long fileSize) {
        this.currentPlanStorageKey = storageKey;
        this.currentPlanTitle = title;
        this.currentPlanFileSize = fileSize;
        this.currentPlanCreatedAt = LocalDateTime.now();
    }

    public void incrementWorkoutCount() {
        this.totalWorkoutsCompleted = (this.totalWorkoutsCompleted == null ? 0 : this.totalWorkoutsCompleted) + 1;
        this.lastWorkout = LocalDateTime.now();
    }

    public boolean hasCurrentPlan() {
        return currentPlanStorageKey != null && !currentPlanStorageKey.trim().isEmpty();
    }

    public double getBMI() {
        if (heightCm != null && weightKg != null && heightCm > 0) {
            double heightM = heightCm / 100.0;
            return weightKg.doubleValue() / (heightM * heightM);
        }
        return 0.0;
    }
}