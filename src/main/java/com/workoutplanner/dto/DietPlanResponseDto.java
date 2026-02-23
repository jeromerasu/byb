package com.workoutplanner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DietPlanResponseDto {
    private String message;
    private String planTitle;
    private String storageKey;
    private LocalDateTime createdAt;

    // Stable frontend contract fields
    private String title;
    private String phaseLabel;
    private Integer calories;
    private Integer mealsPerDay;
    private String dietType;
    private Map<String, Object> summary;

    // Backward compatibility payload
    private Map<String, Object> plan;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getPlanTitle() { return planTitle; }
    public void setPlanTitle(String planTitle) { this.planTitle = planTitle; }

    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPhaseLabel() { return phaseLabel; }
    public void setPhaseLabel(String phaseLabel) { this.phaseLabel = phaseLabel; }

    public Integer getCalories() { return calories; }
    public void setCalories(Integer calories) { this.calories = calories; }

    public Integer getMealsPerDay() { return mealsPerDay; }
    public void setMealsPerDay(Integer mealsPerDay) { this.mealsPerDay = mealsPerDay; }

    public String getDietType() { return dietType; }
    public void setDietType(String dietType) { this.dietType = dietType; }

    public Map<String, Object> getSummary() { return summary; }
    public void setSummary(Map<String, Object> summary) { this.summary = summary; }

    public Map<String, Object> getPlan() { return plan; }
    public void setPlan(Map<String, Object> plan) { this.plan = plan; }
}
