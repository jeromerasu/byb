package com.workoutplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.workoutplanner.model.CoachPromptTemplate;

import java.time.LocalDateTime;

public class CoachPromptTemplateResponse {

    @JsonProperty("template_id")
    private String templateId;

    @JsonProperty("coach_id")
    private String coachId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("prompt_content")
    private String promptContent;

    @JsonProperty("is_coach_default")
    private boolean isCoachDefault;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    public CoachPromptTemplateResponse() {}

    public static CoachPromptTemplateResponse from(CoachPromptTemplate template) {
        CoachPromptTemplateResponse r = new CoachPromptTemplateResponse();
        r.templateId = template.getId();
        r.coachId = template.getCoachId();
        r.userId = template.getUserId();
        r.promptContent = template.getPromptContent();
        r.isCoachDefault = template.isCoachDefault();
        r.updatedAt = template.getUpdatedAt();
        return r;
    }

    public String getTemplateId() { return templateId; }
    public String getCoachId() { return coachId; }
    public String getUserId() { return userId; }
    public String getPromptContent() { return promptContent; }
    public boolean isCoachDefault() { return isCoachDefault; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
