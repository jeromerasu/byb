package com.workoutplanner.dto;

import com.workoutplanner.model.MealLog;
import com.workoutplanner.model.MealRating;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class MealLogResponse {

    private String id;
    private String userId;
    private String mealName;
    private BigDecimal calories;
    private BigDecimal proteins;
    private BigDecimal fats;
    private BigDecimal carbs;
    private LocalDate date;
    private Long foodCatalogId;
    private MealRating rating;
    private String feedbackComment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MealLogResponse from(MealLog entity) {
        MealLogResponse dto = new MealLogResponse();
        dto.id = entity.getId();
        dto.userId = entity.getUserId();
        dto.mealName = entity.getMealName();
        dto.calories = entity.getCalories();
        dto.proteins = entity.getProteins();
        dto.fats = entity.getFats();
        dto.carbs = entity.getCarbs();
        dto.date = entity.getDate();
        dto.foodCatalogId = entity.getFoodCatalogId();
        dto.rating = entity.getRating();
        dto.feedbackComment = entity.getFeedbackComment();
        dto.createdAt = entity.getCreatedAt();
        dto.updatedAt = entity.getUpdatedAt();
        return dto;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getMealName() { return mealName; }
    public BigDecimal getCalories() { return calories; }
    public BigDecimal getProteins() { return proteins; }
    public BigDecimal getFats() { return fats; }
    public BigDecimal getCarbs() { return carbs; }
    public LocalDate getDate() { return date; }
    public Long getFoodCatalogId() { return foodCatalogId; }
    public MealRating getRating() { return rating; }
    public String getFeedbackComment() { return feedbackComment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
