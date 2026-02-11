package com.workoutplanner.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "diet_profile")
public class DietProfile {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    @JsonProperty("user_id")
    private String userId;

    // Diet preferences
    @Enumerated(EnumType.STRING)
    @Column(name = "diet_type")
    @JsonProperty("diet_type")
    private DietType dietType;

    @Column(name = "daily_calorie_goal")
    @JsonProperty("daily_calorie_goal")
    private Integer dailyCalorieGoal;

    @Column(name = "meals_per_day")
    @JsonProperty("meals_per_day")
    private Integer mealsPerDay = 3;

    @Column(name = "dietary_restrictions")
    @JsonProperty("dietary_restrictions")
    private String[] dietaryRestrictions;

    @Column(name = "disliked_foods")
    @JsonProperty("disliked_foods")
    private String[] dislikedFoods;

    @Column(name = "preferred_cuisines")
    @JsonProperty("preferred_cuisines")
    private String[] preferredCuisines;

    // Nutritional goals
    @Column(name = "protein_goal_grams")
    @JsonProperty("protein_goal_grams")
    private Integer proteinGoalGrams;

    @Column(name = "carb_goal_grams")
    @JsonProperty("carb_goal_grams")
    private Integer carbGoalGrams;

    @Column(name = "fat_goal_grams")
    @JsonProperty("fat_goal_grams")
    private Integer fatGoalGrams;

    @Column(name = "fiber_goal_grams")
    @JsonProperty("fiber_goal_grams")
    private Integer fiberGoalGrams;

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
    private WorkoutProfile.Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_level")
    @JsonProperty("activity_level")
    private WorkoutProfile.ActivityLevel activityLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "weight_goal")
    @JsonProperty("weight_goal")
    private WeightGoal weightGoal;

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

    @Column(name = "last_meal_logged")
    @JsonProperty("last_meal_logged")
    private LocalDateTime lastMealLogged;

    @Column(name = "total_meals_logged")
    @JsonProperty("total_meals_logged")
    private Integer totalMealsLogged = 0;

    // Lazy-loaded user relationship
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @JsonIgnore
    private User user;

    // Enums
    public enum DietType {
        OMNIVORE,
        VEGETARIAN,
        VEGAN,
        KETO,
        PALEO,
        MEDITERRANEAN,
        LOW_CARB,
        LOW_FAT,
        DIABETIC,
        HEART_HEALTHY,
        GLUTEN_FREE,
        DAIRY_FREE,
        INTERMITTENT_FASTING
    }

    public enum WeightGoal {
        LOSE,
        MAINTAIN,
        GAIN
    }

    // Constructors
    public DietProfile() {}

    public DietProfile(String id, String userId) {
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

    public DietType getDietType() {
        return dietType;
    }

    public void setDietType(DietType dietType) {
        this.dietType = dietType;
    }

    public Integer getDailyCalorieGoal() {
        return dailyCalorieGoal;
    }

    public void setDailyCalorieGoal(Integer dailyCalorieGoal) {
        this.dailyCalorieGoal = dailyCalorieGoal;
    }

    public Integer getMealsPerDay() {
        return mealsPerDay;
    }

    public void setMealsPerDay(Integer mealsPerDay) {
        this.mealsPerDay = mealsPerDay;
    }

    public String[] getDietaryRestrictions() {
        return dietaryRestrictions;
    }

    public void setDietaryRestrictions(String[] dietaryRestrictions) {
        this.dietaryRestrictions = dietaryRestrictions;
    }

    public String[] getDislikedFoods() {
        return dislikedFoods;
    }

    public void setDislikedFoods(String[] dislikedFoods) {
        this.dislikedFoods = dislikedFoods;
    }

    public String[] getPreferredCuisines() {
        return preferredCuisines;
    }

    public void setPreferredCuisines(String[] preferredCuisines) {
        this.preferredCuisines = preferredCuisines;
    }

    public Integer getProteinGoalGrams() {
        return proteinGoalGrams;
    }

    public void setProteinGoalGrams(Integer proteinGoalGrams) {
        this.proteinGoalGrams = proteinGoalGrams;
    }

    public Integer getCarbGoalGrams() {
        return carbGoalGrams;
    }

    public void setCarbGoalGrams(Integer carbGoalGrams) {
        this.carbGoalGrams = carbGoalGrams;
    }

    public Integer getFatGoalGrams() {
        return fatGoalGrams;
    }

    public void setFatGoalGrams(Integer fatGoalGrams) {
        this.fatGoalGrams = fatGoalGrams;
    }

    public Integer getFiberGoalGrams() {
        return fiberGoalGrams;
    }

    public void setFiberGoalGrams(Integer fiberGoalGrams) {
        this.fiberGoalGrams = fiberGoalGrams;
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

    public WorkoutProfile.Gender getGender() {
        return gender;
    }

    public void setGender(WorkoutProfile.Gender gender) {
        this.gender = gender;
    }

    public WorkoutProfile.ActivityLevel getActivityLevel() {
        return activityLevel;
    }

    public void setActivityLevel(WorkoutProfile.ActivityLevel activityLevel) {
        this.activityLevel = activityLevel;
    }

    public WeightGoal getWeightGoal() {
        return weightGoal;
    }

    public void setWeightGoal(WeightGoal weightGoal) {
        this.weightGoal = weightGoal;
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

    public LocalDateTime getLastMealLogged() {
        return lastMealLogged;
    }

    public void setLastMealLogged(LocalDateTime lastMealLogged) {
        this.lastMealLogged = lastMealLogged;
    }

    public Integer getTotalMealsLogged() {
        return totalMealsLogged;
    }

    public void setTotalMealsLogged(Integer totalMealsLogged) {
        this.totalMealsLogged = totalMealsLogged;
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

    public void incrementMealCount() {
        this.totalMealsLogged = (this.totalMealsLogged == null ? 0 : this.totalMealsLogged) + 1;
        this.lastMealLogged = LocalDateTime.now();
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

    public double calculateBMR() {
        if (weightKg == null || heightCm == null || age == null || gender == null) {
            return 0.0;
        }

        double weight = weightKg.doubleValue();

        // Mifflin-St Jeor Equation
        if (gender == WorkoutProfile.Gender.MALE) {
            return (10 * weight) + (6.25 * heightCm) - (5 * age) + 5;
        } else {
            return (10 * weight) + (6.25 * heightCm) - (5 * age) - 161;
        }
    }

    public double calculateTDEE() {
        double bmr = calculateBMR();
        if (bmr == 0.0 || activityLevel == null) {
            return 0.0;
        }

        return switch (activityLevel) {
            case SEDENTARY -> bmr * 1.2;
            case LIGHTLY_ACTIVE -> bmr * 1.375;
            case MODERATELY_ACTIVE -> bmr * 1.55;
            case VERY_ACTIVE -> bmr * 1.725;
            case EXTREMELY_ACTIVE -> bmr * 1.9;
        };
    }
}