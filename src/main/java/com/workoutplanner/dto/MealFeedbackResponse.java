package com.workoutplanner.dto;

import com.workoutplanner.model.MealLog;
import com.workoutplanner.model.MealRating;

import java.time.LocalDate;

public class MealFeedbackResponse {

    private String logId;
    private String mealName;
    private LocalDate date;
    private MealRating rating;
    private String feedbackComment;

    public static MealFeedbackResponse from(MealLog entity) {
        MealFeedbackResponse dto = new MealFeedbackResponse();
        dto.logId = entity.getId();
        dto.mealName = entity.getMealName();
        dto.date = entity.getDate();
        dto.rating = entity.getRating();
        dto.feedbackComment = entity.getFeedbackComment();
        return dto;
    }

    public String getLogId() { return logId; }
    public String getMealName() { return mealName; }
    public LocalDate getDate() { return date; }
    public MealRating getRating() { return rating; }
    public String getFeedbackComment() { return feedbackComment; }
}
