package com.workoutplanner.model;

import com.workoutplanner.model.converter.StringListConverter;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "workout_feedback")
public class WorkoutFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "workout_date", nullable = false)
    private LocalDate workoutDate;

    @Column
    private Integer rating;

    @Convert(converter = StringListConverter.class)
    @Column(name = "session_comments", columnDefinition = "TEXT")
    private List<String> sessionComments;

    @Convert(converter = StringListConverter.class)
    @Column(name = "flagged_exercises", columnDefinition = "TEXT")
    private List<String> flaggedExercises;

    @Column(name = "free_form_note", columnDefinition = "TEXT")
    private String freeFormNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public WorkoutFeedback() {
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public LocalDate getWorkoutDate() { return workoutDate; }
    public void setWorkoutDate(LocalDate workoutDate) { this.workoutDate = workoutDate; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public List<String> getSessionComments() { return sessionComments; }
    public void setSessionComments(List<String> sessionComments) { this.sessionComments = sessionComments; }

    public List<String> getFlaggedExercises() { return flaggedExercises; }
    public void setFlaggedExercises(List<String> flaggedExercises) { this.flaggedExercises = flaggedExercises; }

    public String getFreeFormNote() { return freeFormNote; }
    public void setFreeFormNote(String freeFormNote) { this.freeFormNote = freeFormNote; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
