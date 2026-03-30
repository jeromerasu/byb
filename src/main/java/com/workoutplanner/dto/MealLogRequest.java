package com.workoutplanner.dto;

import com.workoutplanner.model.MealRating;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public class MealLogRequest {

    @NotBlank(message = "Meal name is required")
    private String mealName;

    @PositiveOrZero(message = "Calories must be positive or zero")
    private BigDecimal calories;

    @PositiveOrZero(message = "Proteins must be positive or zero")
    private BigDecimal proteins;

    @PositiveOrZero(message = "Fats must be positive or zero")
    private BigDecimal fats;

    @PositiveOrZero(message = "Carbs must be positive or zero")
    private BigDecimal carbs;

    @NotNull(message = "Date is required")
    private LocalDate date;

    private Long foodCatalogId;

    private MealRating rating;

    private String feedbackComment;

    public String getMealName() { return mealName; }
    public void setMealName(String mealName) { this.mealName = mealName; }

    public BigDecimal getCalories() { return calories; }
    public void setCalories(BigDecimal calories) { this.calories = calories; }

    public BigDecimal getProteins() { return proteins; }
    public void setProteins(BigDecimal proteins) { this.proteins = proteins; }

    public BigDecimal getFats() { return fats; }
    public void setFats(BigDecimal fats) { this.fats = fats; }

    public BigDecimal getCarbs() { return carbs; }
    public void setCarbs(BigDecimal carbs) { this.carbs = carbs; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public Long getFoodCatalogId() { return foodCatalogId; }
    public void setFoodCatalogId(Long foodCatalogId) { this.foodCatalogId = foodCatalogId; }

    public MealRating getRating() { return rating; }
    public void setRating(MealRating rating) { this.rating = rating; }

    public String getFeedbackComment() { return feedbackComment; }
    public void setFeedbackComment(String feedbackComment) { this.feedbackComment = feedbackComment; }
}
