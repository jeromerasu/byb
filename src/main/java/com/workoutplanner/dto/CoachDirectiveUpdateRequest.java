package com.workoutplanner.dto;

public class CoachDirectiveUpdateRequest {

    private String content;
    private Boolean active;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
