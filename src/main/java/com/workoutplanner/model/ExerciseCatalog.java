package com.workoutplanner.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.workoutplanner.model.converter.StringListConverter;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Catalog entry for a single exercise.
 * <p>
 * System entries (is_system=true) are created by admins and visible to all users.
 * Custom entries (is_system=false) are created by individual users and visible only
 * to the owning user.
 */
@Entity
@Table(
    name = "exercise_catalog",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_exercise_catalog_name_user",
        columnNames = {"name", "created_by_user_id"}
    )
)
public class ExerciseCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Exercise name is required")
    @Column(nullable = false)
    private String name;

    @Column(name = "exercise_type", length = 50)
    @JsonProperty("exerciseType")
    private String exerciseType;

    @Convert(converter = StringListConverter.class)
    @Column(name = "muscle_groups", columnDefinition = "TEXT")
    @JsonProperty("muscleGroups")
    private List<String> muscleGroups;

    @Convert(converter = StringListConverter.class)
    @Column(name = "equipment_required", columnDefinition = "TEXT")
    @JsonProperty("equipmentRequired")
    private List<String> equipmentRequired;

    @Column(name = "difficulty_level", length = 50)
    @JsonProperty("difficultyLevel")
    private String difficultyLevel;

    @Column(name = "video_url", length = 500)
    @JsonProperty("videoUrl")
    private String videoUrl;

    @Column(name = "thumbnail_url", length = 500)
    @JsonProperty("thumbnailUrl")
    private String thumbnailUrl;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "is_system", nullable = false)
    @JsonProperty("isSystem")
    private boolean isSystem = true;

    @Column(name = "created_by_user_id")
    @JsonProperty("createdByUserId")
    private String createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;

    public ExerciseCatalog() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getExerciseType() { return exerciseType; }
    public void setExerciseType(String exerciseType) { this.exerciseType = exerciseType; }

    public List<String> getMuscleGroups() { return muscleGroups; }
    public void setMuscleGroups(List<String> muscleGroups) { this.muscleGroups = muscleGroups; }

    public List<String> getEquipmentRequired() { return equipmentRequired; }
    public void setEquipmentRequired(List<String> equipmentRequired) { this.equipmentRequired = equipmentRequired; }

    public String getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(String difficultyLevel) { this.difficultyLevel = difficultyLevel; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public boolean isSystem() { return isSystem; }
    public void setSystem(boolean system) { isSystem = system; }

    public String getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(String createdByUserId) { this.createdByUserId = createdByUserId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
