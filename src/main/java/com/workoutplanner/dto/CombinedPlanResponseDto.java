package com.workoutplanner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CombinedPlanResponseDto {
    private String message;
    private PlanMetaDto planMeta;
    private WorkoutPlanResponseDto workout;
    private DietPlanResponseDto diet;

    public static class PlanMetaDto {
        private String duration;
        private LocalDateTime generatedAt;
        private String version;
        private String userId;
        private String workoutStorageKey;
        private String dietStorageKey;

        public PlanMetaDto() {}

        public PlanMetaDto(String duration, LocalDateTime generatedAt, String version, String userId, String workoutStorageKey, String dietStorageKey) {
            this.duration = duration;
            this.generatedAt = generatedAt;
            this.version = version;
            this.userId = userId;
            this.workoutStorageKey = workoutStorageKey;
            this.dietStorageKey = dietStorageKey;
        }

        public String getDuration() { return duration; }
        public void setDuration(String duration) { this.duration = duration; }

        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getWorkoutStorageKey() { return workoutStorageKey; }
        public void setWorkoutStorageKey(String workoutStorageKey) { this.workoutStorageKey = workoutStorageKey; }

        public String getDietStorageKey() { return dietStorageKey; }
        public void setDietStorageKey(String dietStorageKey) { this.dietStorageKey = dietStorageKey; }
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public PlanMetaDto getPlanMeta() { return planMeta; }
    public void setPlanMeta(PlanMetaDto planMeta) { this.planMeta = planMeta; }

    public WorkoutPlanResponseDto getWorkout() { return workout; }
    public void setWorkout(WorkoutPlanResponseDto workout) { this.workout = workout; }

    public DietPlanResponseDto getDiet() { return diet; }
    public void setDiet(DietPlanResponseDto diet) { this.diet = diet; }
}