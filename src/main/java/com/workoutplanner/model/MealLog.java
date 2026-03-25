package com.workoutplanner.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "meal_log")
public class MealLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @NotNull(message = "User ID is required")
    @Column(name = "user_id", nullable = false)
    @JsonProperty("user_id")
    private String userId;

    @NotBlank(message = "Meal name is required")
    @Column(name = "meal_name", nullable = false)
    @JsonProperty("meal_name")
    private String mealName;

    @PositiveOrZero(message = "Calories must be positive or zero")
    @Column(precision = 7, scale = 2)
    private BigDecimal calories;

    @PositiveOrZero(message = "Proteins must be positive or zero")
    @Column(precision = 6, scale = 2)
    private BigDecimal proteins;

    @PositiveOrZero(message = "Fats must be positive or zero")
    @Column(precision = 6, scale = 2)
    private BigDecimal fats;

    @PositiveOrZero(message = "Carbs must be positive or zero")
    @Column(precision = 6, scale = 2)
    private BigDecimal carbs;

    @NotNull(message = "Date is required")
    @Column(nullable = false)
    private LocalDate date;

    @JsonProperty("created_at")
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public MealLog() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public MealLog(String userId, String mealName, BigDecimal calories,
                   BigDecimal proteins, BigDecimal fats, BigDecimal carbs, LocalDate date) {
        this();
        this.userId = userId;
        this.mealName = mealName;
        this.calories = calories;
        this.proteins = proteins;
        this.fats = fats;
        this.carbs = carbs;
        this.date = date;
    }

    @PreUpdate
    public void preUpdate() {
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

    public String getMealName() {
        return mealName;
    }

    public void setMealName(String mealName) {
        this.mealName = mealName;
    }

    public BigDecimal getCalories() {
        return calories;
    }

    public void setCalories(BigDecimal calories) {
        this.calories = calories;
    }

    public BigDecimal getProteins() {
        return proteins;
    }

    public void setProteins(BigDecimal proteins) {
        this.proteins = proteins;
    }

    public BigDecimal getFats() {
        return fats;
    }

    public void setFats(BigDecimal fats) {
        this.fats = fats;
    }

    public BigDecimal getCarbs() {
        return carbs;
    }

    public void setCarbs(BigDecimal carbs) {
        this.carbs = carbs;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
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
}