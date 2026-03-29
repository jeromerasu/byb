package com.workoutplanner.dto;

import com.workoutplanner.model.ExerciseCatalog;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for an exercise catalog entry.
 */
public class ExerciseCatalogResponseDto {

    private Long id;
    private String name;
    private String exerciseType;
    private List<String> muscleGroups;
    private List<String> equipmentRequired;
    private String difficultyLevel;
    private String videoUrl;
    private String thumbnailUrl;
    private String instructions;
    private boolean isSystem;
    private String createdByUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ExerciseCatalogResponseDto from(ExerciseCatalog entity) {
        ExerciseCatalogResponseDto dto = new ExerciseCatalogResponseDto();
        dto.id = entity.getId();
        dto.name = entity.getName();
        dto.exerciseType = entity.getExerciseType();
        dto.muscleGroups = entity.getMuscleGroups();
        dto.equipmentRequired = entity.getEquipmentRequired();
        dto.difficultyLevel = entity.getDifficultyLevel();
        dto.videoUrl = entity.getVideoUrl();
        dto.thumbnailUrl = entity.getThumbnailUrl();
        dto.instructions = entity.getInstructions();
        dto.isSystem = entity.isSystem();
        dto.createdByUserId = entity.getCreatedByUserId();
        dto.createdAt = entity.getCreatedAt();
        dto.updatedAt = entity.getUpdatedAt();
        return dto;
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
