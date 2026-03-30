package com.workoutplanner.dto;

import com.workoutplanner.model.WorkoutLog;
import com.workoutplanner.model.WorkoutRating;

import java.math.BigDecimal;
import java.time.LocalDate;

public class WorkoutFeedbackResponse {

    private String logId;
    private String exerciseName;
    private LocalDate date;
    private Integer sets;
    private Integer reps;
    private BigDecimal weight;
    private WorkoutRating rating;
    private String feedbackComment;
    private boolean painFlag;
    private boolean substitutionRequested;

    public static WorkoutFeedbackResponse from(WorkoutLog entity) {
        WorkoutFeedbackResponse dto = new WorkoutFeedbackResponse();
        dto.logId = entity.getId();
        dto.exerciseName = entity.getExercise();
        dto.date = entity.getDate();
        dto.sets = entity.getSets();
        dto.reps = entity.getReps();
        dto.weight = entity.getWeight();
        dto.rating = entity.getRating();
        dto.feedbackComment = entity.getFeedbackComment();
        dto.painFlag = entity.isPainFlag();
        dto.substitutionRequested = entity.isSubstitutionRequested();
        return dto;
    }

    public String getLogId() { return logId; }
    public String getExerciseName() { return exerciseName; }
    public LocalDate getDate() { return date; }
    public Integer getSets() { return sets; }
    public Integer getReps() { return reps; }
    public BigDecimal getWeight() { return weight; }
    public WorkoutRating getRating() { return rating; }
    public String getFeedbackComment() { return feedbackComment; }
    public boolean isPainFlag() { return painFlag; }
    public boolean isSubstitutionRequested() { return substitutionRequested; }
}
