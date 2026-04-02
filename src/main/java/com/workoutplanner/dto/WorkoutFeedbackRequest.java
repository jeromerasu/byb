package com.workoutplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

public class WorkoutFeedbackRequest {

    @JsonProperty("workout_date")
    private LocalDate workoutDate;

    private Integer rating;

    @JsonProperty("session_comments")
    private List<String> sessionComments;

    @JsonProperty("flagged_exercises")
    private List<String> flaggedExercises;

    @JsonProperty("free_form_note")
    private String freeFormNote;

    public LocalDate getWorkoutDate() { return workoutDate; }
    public void setWorkoutDate(LocalDate workoutDate) { this.workoutDate = workoutDate; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public List<String> getSessionComments() { return sessionComments; }
    public void setSessionComments(List<String> sessionComments) { this.sessionComments = sessionComments; }

    public List<String> getFlaggedExercises() { return flaggedExercises; }
    public void setFlaggedExercises(List<String> flaggedExercises) { this.flaggedExercises = flaggedExercises; }

    public String getFreeFormNote() { return freeFormNote; }
    public void setFreeFormNote(String freeFormNote) { this.freeFormNote = freeFormNote; }
}
