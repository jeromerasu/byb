package com.workoutplanner.model;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

/**
 * Prompt template authored by a coach.
 *
 * Resolution precedence at generation time:
 *   1. Per-client template (coach_id + user_id set)
 *   2. Coach default template (coach_id set, user_id = null)
 *   3. Base system prompt (hardcoded fallback in StandardPromptStrategy)
 *
 * The unique constraint (coach_id, user_id) is nullable on user_id — only one
 * default per coach is allowed and one per-client template per coach+user pair.
 */
@Entity
@Table(name = "coach_prompt_template")
public class CoachPromptTemplate {

    @Id
    @UuidGenerator
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "coach_id", nullable = false, length = 36)
    private String coachId;

    @Column(name = "user_id", length = 36)
    private String userId;  // null → coach default template

    @Column(name = "prompt_content", nullable = false, columnDefinition = "TEXT")
    private String promptContent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public CoachPromptTemplate() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCoachId() { return coachId; }
    public void setCoachId(String coachId) { this.coachId = coachId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getPromptContent() { return promptContent; }
    public void setPromptContent(String promptContent) { this.promptContent = promptContent; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isCoachDefault() { return userId == null; }
}
