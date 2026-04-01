package com.workoutplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.workoutplanner.model.CoachDirective;
import com.workoutplanner.model.DirectiveType;

import java.time.LocalDateTime;

public class CoachDirectiveResponse {

    private String id;

    @JsonProperty("directive_type")
    private DirectiveType directiveType;

    private String content;
    private boolean active;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    public CoachDirectiveResponse() {}

    public static CoachDirectiveResponse from(CoachDirective directive) {
        CoachDirectiveResponse r = new CoachDirectiveResponse();
        r.id = directive.getId();
        r.directiveType = directive.getDirectiveType();
        r.content = directive.getContent();
        r.active = directive.isActive();
        r.createdAt = directive.getCreatedAt();
        return r;
    }

    public String getId() { return id; }
    public DirectiveType getDirectiveType() { return directiveType; }
    public String getContent() { return content; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
