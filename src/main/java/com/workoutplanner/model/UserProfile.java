package com.workoutplanner.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class UserProfile {

    @NotNull
    @Min(16)
    @Max(80)
    private Integer age;

    @NotNull
    private Equipment equipment;

    @NotNull
    @Min(1)
    @Max(7)
    @JsonProperty("weekly_frequency")
    private Integer weeklyFrequency;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    public UserProfile() {
        this.createdAt = LocalDateTime.now();
    }

    public UserProfile(Integer age, Equipment equipment, Integer weeklyFrequency) {
        this.age = age;
        this.equipment = equipment;
        this.weeklyFrequency = weeklyFrequency;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and setters
    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public void setEquipment(Equipment equipment) {
        this.equipment = equipment;
    }

    public Integer getWeeklyFrequency() {
        return weeklyFrequency;
    }

    public void setWeeklyFrequency(Integer weeklyFrequency) {
        this.weeklyFrequency = weeklyFrequency;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}