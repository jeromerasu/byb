package com.workoutplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.workoutplanner.model.enums.ActivityLevel;
import com.workoutplanner.model.enums.Gender;

import java.math.BigDecimal;

public class UserPhysicalProfileRequest {

    @JsonProperty("height_cm")
    private Integer heightCm;

    @JsonProperty("weight_kg")
    private BigDecimal weightKg;

    @JsonProperty("date_of_birth")
    private String dateOfBirth;

    private Gender gender;

    @JsonProperty("activity_level")
    private ActivityLevel activityLevel;

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

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public ActivityLevel getActivityLevel() {
        return activityLevel;
    }

    public void setActivityLevel(ActivityLevel activityLevel) {
        this.activityLevel = activityLevel;
    }
}
