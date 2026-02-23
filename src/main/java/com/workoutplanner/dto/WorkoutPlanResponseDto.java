package com.workoutplanner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkoutPlanResponseDto {
    private String message;
    private String planTitle;
    private String storageKey;
    private LocalDateTime createdAt;

    // Stable frontend contract fields
    private String title;
    private String phaseLabel;
    private Integer durationMin;
    private Integer calories;
    private List<ExerciseDto> exercises;

    // Backward compatibility payload
    private Map<String, Object> plan;

    public static class ExerciseDto {
        private String name;
        private String prescription;
        private String muscle;

        public ExerciseDto() {}

        public ExerciseDto(String name, String prescription, String muscle) {
            this.name = name;
            this.prescription = prescription;
            this.muscle = muscle;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getPrescription() { return prescription; }
        public void setPrescription(String prescription) { this.prescription = prescription; }

        public String getMuscle() { return muscle; }
        public void setMuscle(String muscle) { this.muscle = muscle; }
    }

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

    public Integer getDurationMin() { return durationMin; }
    public void setDurationMin(Integer durationMin) { this.durationMin = durationMin; }

    public Integer getCalories() { return calories; }
    public void setCalories(Integer calories) { this.calories = calories; }

    public List<ExerciseDto> getExercises() { return exercises; }
    public void setExercises(List<ExerciseDto> exercises) { this.exercises = exercises; }

    public Map<String, Object> getPlan() { return plan; }
    public void setPlan(Map<String, Object> plan) { this.plan = plan; }
}
