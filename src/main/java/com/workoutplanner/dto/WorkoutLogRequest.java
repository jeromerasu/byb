package com.workoutplanner.dto;

import com.workoutplanner.model.ExerciseType;
import com.workoutplanner.model.WorkoutRating;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public class WorkoutLogRequest {

    @NotBlank(message = "Exercise is required")
    private String exercise;

    @PositiveOrZero(message = "Weight must be positive or zero")
    private BigDecimal weight;

    private Integer sets;

    private Integer reps;

    private Integer durationMinutes;

    private ExerciseType exerciseType;

    private String notes;

    private WorkoutRating rating;

    private String feedbackComment;

    private boolean painFlag = false;

    private boolean substitutionRequested = false;

    @NotNull(message = "Date is required")
    private LocalDate date;

    private Long exerciseCatalogId;

    public String getExercise() { return exercise; }
    public void setExercise(String exercise) { this.exercise = exercise; }

    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }

    public Integer getSets() { return sets; }
    public void setSets(Integer sets) { this.sets = sets; }

    public Integer getReps() { return reps; }
    public void setReps(Integer reps) { this.reps = reps; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public ExerciseType getExerciseType() { return exerciseType; }
    public void setExerciseType(ExerciseType exerciseType) { this.exerciseType = exerciseType; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public WorkoutRating getRating() { return rating; }
    public void setRating(WorkoutRating rating) { this.rating = rating; }

    public String getFeedbackComment() { return feedbackComment; }
    public void setFeedbackComment(String feedbackComment) { this.feedbackComment = feedbackComment; }

    public boolean isPainFlag() { return painFlag; }
    public void setPainFlag(boolean painFlag) { this.painFlag = painFlag; }

    public boolean isSubstitutionRequested() { return substitutionRequested; }
    public void setSubstitutionRequested(boolean substitutionRequested) { this.substitutionRequested = substitutionRequested; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public Long getExerciseCatalogId() { return exerciseCatalogId; }
    public void setExerciseCatalogId(Long exerciseCatalogId) { this.exerciseCatalogId = exerciseCatalogId; }
}
