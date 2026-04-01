package com.workoutplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CoachPromptTemplateRequest {

    @JsonProperty("user_id")
    private String userId;  // null → sets coach default template

    @JsonProperty("prompt_content")
    private String promptContent;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPromptContent() { return promptContent; }
    public void setPromptContent(String promptContent) { this.promptContent = promptContent; }
}
