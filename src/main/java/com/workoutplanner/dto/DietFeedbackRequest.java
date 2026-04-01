package com.workoutplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

public class DietFeedbackRequest {

    @JsonProperty("feedback_date")
    private LocalDate feedbackDate;

    private Integer rating;

    @JsonProperty("session_comments")
    private List<String> sessionComments;

    @JsonProperty("flagged_meals")
    private String flaggedMeals;

    @JsonProperty("free_form_note")
    private String freeFormNote;

    public LocalDate getFeedbackDate() { return feedbackDate; }
    public void setFeedbackDate(LocalDate feedbackDate) { this.feedbackDate = feedbackDate; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public List<String> getSessionComments() { return sessionComments; }
    public void setSessionComments(List<String> sessionComments) { this.sessionComments = sessionComments; }

    public String getFlaggedMeals() { return flaggedMeals; }
    public void setFlaggedMeals(String flaggedMeals) { this.flaggedMeals = flaggedMeals; }

    public String getFreeFormNote() { return freeFormNote; }
    public void setFreeFormNote(String freeFormNote) { this.freeFormNote = freeFormNote; }
}
