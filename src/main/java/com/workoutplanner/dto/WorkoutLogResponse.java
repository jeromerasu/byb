package com.workoutplanner.dto;

import com.workoutplanner.model.ExerciseType;
import com.workoutplanner.model.WorkoutLog;
import com.workoutplanner.model.WorkoutRating;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class WorkoutLogResponse {

    private String id;
    private String userId;
    private String exercise;
    private BigDecimal weight;
    private Integer sets;
    private Integer reps;
    private Integer durationMinutes;
    private ExerciseType exerciseType;
    private String notes;
    private WorkoutRating rating;
    private String feedbackComment;
    private boolean painFlag;
    private boolean substitutionRequested;
    private LocalDate date;
    private Long exerciseCatalogId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static WorkoutLogResponse from(WorkoutLog entity) {
        WorkoutLogResponse dto = new WorkoutLogResponse();
        dto.id = entity.getId();
        dto.userId = entity.getUserId();
        dto.exercise = entity.getExercise();
        dto.weight = entity.getWeight();
        dto.sets = entity.getSets();
        dto.reps = entity.getReps();
        dto.durationMinutes = entity.getDurationMinutes();
        dto.exerciseType = entity.getExerciseType();
        dto.notes = entity.getNotes();
        dto.rating = entity.getRating();
        dto.feedbackComment = entity.getFeedbackComment();
        dto.painFlag = entity.isPainFlag();
        dto.substitutionRequested = entity.isSubstitutionRequested();
        dto.date = entity.getDate();
        dto.exerciseCatalogId = entity.getExerciseCatalogId();
        dto.createdAt = entity.getCreatedAt();
        dto.updatedAt = entity.getUpdatedAt();
        return dto;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getExercise() { return exercise; }
    public BigDecimal getWeight() { return weight; }
    public Integer getSets() { return sets; }
    public Integer getReps() { return reps; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public ExerciseType getExerciseType() { return exerciseType; }
    public String getNotes() { return notes; }
    public WorkoutRating getRating() { return rating; }
    public String getFeedbackComment() { return feedbackComment; }
    public boolean isPainFlag() { return painFlag; }
    public boolean isSubstitutionRequested() { return substitutionRequested; }
    public LocalDate getDate() { return date; }
    public Long getExerciseCatalogId() { return exerciseCatalogId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
