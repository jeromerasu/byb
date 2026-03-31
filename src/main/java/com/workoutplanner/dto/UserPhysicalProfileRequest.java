package com.workoutplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.workoutplanner.model.WorkoutProfile;

import java.math.BigDecimal;

public class UserPhysicalProfileRequest {

    @JsonProperty("height_cm")
    private Integer heightCm;

    @JsonProperty("weight_kg")
    private BigDecimal weightKg;

    private Integer age;

    private WorkoutProfile.Gender gender;

    @JsonProperty("activity_level")
    private WorkoutProfile.ActivityLevel activityLevel;

    public Integer getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(Integer heightCm) {
        this.heightCm = heightCm;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public WorkoutProfile.Gender getGender() {
        return gender;
    }

    public void setGender(WorkoutProfile.Gender gender) {
        this.gender = gender;
    }

    public WorkoutProfile.ActivityLevel getActivityLevel() {
        return activityLevel;
    }

    public void setActivityLevel(WorkoutProfile.ActivityLevel activityLevel) {
        this.activityLevel = activityLevel;
    }
}
