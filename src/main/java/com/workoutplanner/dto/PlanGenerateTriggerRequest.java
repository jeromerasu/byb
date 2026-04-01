package com.workoutplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PlanGenerateTriggerRequest {

    @JsonProperty("user_id")
    private String userId;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}
