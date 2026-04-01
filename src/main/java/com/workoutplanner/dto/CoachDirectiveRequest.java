package com.workoutplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.workoutplanner.model.DirectiveType;

public class CoachDirectiveRequest {

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("directive_type")
    private DirectiveType directiveType;

    private String content;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public DirectiveType getDirectiveType() { return directiveType; }
    public void setDirectiveType(DirectiveType directiveType) { this.directiveType = directiveType; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
