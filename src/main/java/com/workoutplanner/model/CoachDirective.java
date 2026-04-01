package com.workoutplanner.model;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

/**
 * A coaching instruction for a specific client, injected into the plan generation prompt.
 * Soft-delete is modelled via active=false (hard delete also supported via DELETE endpoint).
 */
@Entity
@Table(name = "coach_directive")
public class CoachDirective {

    @Id
    @UuidGenerator
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "coach_id", nullable = false, length = 36)
    private String coachId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "directive_type", nullable = false, length = 50)
    private DirectiveType directiveType;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "active", nullable = false)
    private boolean active = true;

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

    public CoachDirective() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCoachId() { return coachId; }
    public void setCoachId(String coachId) { this.coachId = coachId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public DirectiveType getDirectiveType() { return directiveType; }
    public void setDirectiveType(DirectiveType directiveType) { this.directiveType = directiveType; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
