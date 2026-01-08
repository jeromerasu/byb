package com.workoutplanner.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class WorkoutDay {

    @JsonProperty("day_number")
    private Integer dayNumber;

    @JsonProperty("day_name")
    private String dayName;

    private List<Exercise> exercises;

    @JsonProperty("estimated_duration")
    private Integer estimatedDuration;

    private String notes;

    public WorkoutDay() {}

    public WorkoutDay(Integer dayNumber, String dayName, List<Exercise> exercises,
                     Integer estimatedDuration, String notes) {
        this.dayNumber = dayNumber;
        this.dayName = dayName;
        this.exercises = exercises;
        this.estimatedDuration = estimatedDuration;
        this.notes = notes;
    }

    // Getters and setters
    public Integer getDayNumber() {
        return dayNumber;
    }

    public void setDayNumber(Integer dayNumber) {
        this.dayNumber = dayNumber;
    }

    public String getDayName() {
        return dayName;
    }

    public void setDayName(String dayName) {
        this.dayName = dayName;
    }

    public List<Exercise> getExercises() {
        return exercises;
    }

    public void setExercises(List<Exercise> exercises) {
        this.exercises = exercises;
    }

    public Integer getEstimatedDuration() {
        return estimatedDuration;
    }

    public void setEstimatedDuration(Integer estimatedDuration) {
        this.estimatedDuration = estimatedDuration;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}