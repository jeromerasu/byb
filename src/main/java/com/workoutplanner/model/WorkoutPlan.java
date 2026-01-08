package com.workoutplanner.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "workout_plans")
public class WorkoutPlan {

    @Id
    private String id;

    @JsonProperty("user_profile")
    @Column(name = "user_profile", columnDefinition = "TEXT")
    private String userProfileJson;

    @JsonProperty("weekly_schedule")
    @Column(name = "weekly_schedule", columnDefinition = "TEXT")
    private String weeklyScheduleJson;

    @JsonProperty("generated_at")
    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @JsonProperty("ai_response")
    @Column(name = "ai_response", columnDefinition = "TEXT")
    private String aiResponse;

    @Column(name = "title")
    private String title;

    @Transient
    private UserProfile userProfile;

    @Transient
    private List<WorkoutDay> weeklySchedule;

    @Transient
    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public WorkoutPlan() {
        this.generatedAt = LocalDateTime.now();
    }

    public WorkoutPlan(UserProfile userProfile, List<WorkoutDay> weeklySchedule, String aiResponse) {
        this.id = java.util.UUID.randomUUID().toString();
        this.userProfile = userProfile;
        this.weeklySchedule = weeklySchedule;
        this.aiResponse = aiResponse;
        this.generatedAt = LocalDateTime.now();
        this.title = "Workout Plan - " + LocalDateTime.now().toLocalDate();

        try {
            this.userProfileJson = objectMapper.writeValueAsString(userProfile);
            this.weeklyScheduleJson = objectMapper.writeValueAsString(weeklySchedule);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing workout plan data", e);
        }
    }

    @PostLoad
    @PostPersist
    @PostUpdate
    private void deserializeFields() {
        try {
            if (userProfileJson != null) {
                this.userProfile = objectMapper.readValue(userProfileJson, UserProfile.class);
            }
            if (weeklyScheduleJson != null) {
                this.weeklySchedule = objectMapper.readValue(weeklyScheduleJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, WorkoutDay.class));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializing workout plan data", e);
        }
    }

    @PrePersist
    @PreUpdate
    private void serializeFields() {
        try {
            if (userProfile != null) {
                this.userProfileJson = objectMapper.writeValueAsString(userProfile);
            }
            if (weeklySchedule != null) {
                this.weeklyScheduleJson = objectMapper.writeValueAsString(weeklySchedule);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing workout plan data", e);
        }
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonIgnore
    public UserProfile getUserProfile() {
        return userProfile;
    }

    public void setUserProfile(UserProfile userProfile) {
        this.userProfile = userProfile;
    }

    @JsonIgnore
    public List<WorkoutDay> getWeeklySchedule() {
        return weeklySchedule;
    }

    public void setWeeklySchedule(List<WorkoutDay> weeklySchedule) {
        this.weeklySchedule = weeklySchedule;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getAiResponse() {
        return aiResponse;
    }

    public void setAiResponse(String aiResponse) {
        this.aiResponse = aiResponse;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUserProfileJson() {
        return userProfileJson;
    }

    public void setUserProfileJson(String userProfileJson) {
        this.userProfileJson = userProfileJson;
    }

    public String getWeeklyScheduleJson() {
        return weeklyScheduleJson;
    }

    public void setWeeklyScheduleJson(String weeklyScheduleJson) {
        this.weeklyScheduleJson = weeklyScheduleJson;
    }
}