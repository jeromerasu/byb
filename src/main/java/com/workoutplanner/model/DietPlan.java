package com.workoutplanner.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "diet_plans")
public class DietPlan {

    @Id
    private String id;

    @JsonProperty("diet_profile")
    @Column(name = "diet_profile", columnDefinition = "TEXT")
    private String dietProfileJson;

    @JsonProperty("weekly_schedule")
    @Column(name = "weekly_schedule", columnDefinition = "TEXT")
    private String weeklyScheduleJson;

    @JsonProperty("generated_at")
    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @JsonProperty("ai_response")
    @Column(name = "ai_response", columnDefinition = "TEXT")
    private String aiResponse;

    @Column(name = "title")
    private String title;

    @JsonProperty("weekly_nutrition_summary")
    @Column(name = "weekly_nutrition_summary", columnDefinition = "TEXT")
    private String weeklyNutritionSummaryJson;

    @JsonProperty("shopping_list")
    @Column(name = "shopping_list", columnDefinition = "TEXT")
    private String shoppingListJson;

    @JsonProperty("meal_prep_notes")
    @Column(name = "meal_prep_notes", columnDefinition = "TEXT")
    private String mealPrepNotes;

    @Transient
    private DietProfile dietProfile;

    @Transient
    private List<DietDay> weeklySchedule;

    @Transient
    private NutritionalInfo weeklyNutritionSummary;

    @Transient
    private List<String> shoppingList;

    @Transient
    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public DietPlan() {
        this.generatedAt = LocalDateTime.now();
    }

    public DietPlan(DietProfile dietProfile, List<DietDay> weeklySchedule, String aiResponse) {
        this.id = java.util.UUID.randomUUID().toString();
        this.dietProfile = dietProfile;
        this.weeklySchedule = weeklySchedule;
        this.aiResponse = aiResponse;
        this.generatedAt = LocalDateTime.now();
        this.title = "Diet Plan - " + LocalDateTime.now().toLocalDate();

        try {
            this.dietProfileJson = objectMapper.writeValueAsString(dietProfile);
            this.weeklyScheduleJson = objectMapper.writeValueAsString(weeklySchedule);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing diet plan data", e);
        }
    }

    @PostLoad
    @PostPersist
    @PostUpdate
    private void deserializeFields() {
        try {
            if (dietProfileJson != null) {
                this.dietProfile = objectMapper.readValue(dietProfileJson, DietProfile.class);
            }
            if (weeklyScheduleJson != null) {
                this.weeklySchedule = objectMapper.readValue(weeklyScheduleJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, DietDay.class));
            }
            if (weeklyNutritionSummaryJson != null) {
                this.weeklyNutritionSummary = objectMapper.readValue(weeklyNutritionSummaryJson, NutritionalInfo.class);
            }
            if (shoppingListJson != null) {
                this.shoppingList = objectMapper.readValue(shoppingListJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializing diet plan data", e);
        }
    }

    @PrePersist
    @PreUpdate
    private void serializeFields() {
        try {
            if (dietProfile != null) {
                this.dietProfileJson = objectMapper.writeValueAsString(dietProfile);
            }
            if (weeklySchedule != null) {
                this.weeklyScheduleJson = objectMapper.writeValueAsString(weeklySchedule);
            }
            if (weeklyNutritionSummary != null) {
                this.weeklyNutritionSummaryJson = objectMapper.writeValueAsString(weeklyNutritionSummary);
            }
            if (shoppingList != null) {
                this.shoppingListJson = objectMapper.writeValueAsString(shoppingList);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing diet plan data", e);
        }
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public DietProfile getDietProfile() {
        return dietProfile;
    }

    public void setDietProfile(DietProfile dietProfile) {
        this.dietProfile = dietProfile;
    }

    public List<DietDay> getWeeklySchedule() {
        return weeklySchedule;
    }

    public void setWeeklySchedule(List<DietDay> weeklySchedule) {
        this.weeklySchedule = weeklySchedule;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getAiResponse() {
        return aiResponse;
    }

    public void setAiResponse(String aiResponse) {
        this.aiResponse = aiResponse;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public NutritionalInfo getWeeklyNutritionSummary() {
        return weeklyNutritionSummary;
    }

    public void setWeeklyNutritionSummary(NutritionalInfo weeklyNutritionSummary) {
        this.weeklyNutritionSummary = weeklyNutritionSummary;
    }

    public List<String> getShoppingList() {
        return shoppingList;
    }

    public void setShoppingList(List<String> shoppingList) {
        this.shoppingList = shoppingList;
    }

    public String getMealPrepNotes() {
        return mealPrepNotes;
    }

    public void setMealPrepNotes(String mealPrepNotes) {
        this.mealPrepNotes = mealPrepNotes;
    }

    public String getDietProfileJson() {
        return dietProfileJson;
    }

    public void setDietProfileJson(String dietProfileJson) {
        this.dietProfileJson = dietProfileJson;
    }

    public String getWeeklyScheduleJson() {
        return weeklyScheduleJson;
    }

    public void setWeeklyScheduleJson(String weeklyScheduleJson) {
        this.weeklyScheduleJson = weeklyScheduleJson;
    }

    public String getWeeklyNutritionSummaryJson() {
        return weeklyNutritionSummaryJson;
    }

    public void setWeeklyNutritionSummaryJson(String weeklyNutritionSummaryJson) {
        this.weeklyNutritionSummaryJson = weeklyNutritionSummaryJson;
    }

    public String getShoppingListJson() {
        return shoppingListJson;
    }

    public void setShoppingListJson(String shoppingListJson) {
        this.shoppingListJson = shoppingListJson;
    }
}