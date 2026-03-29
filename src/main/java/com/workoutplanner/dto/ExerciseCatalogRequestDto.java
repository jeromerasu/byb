package com.workoutplanner.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Request DTO for creating or updating an exercise catalog entry.
 */
public class ExerciseCatalogRequestDto {

    @NotBlank(message = "Exercise name is required")
    private String name;

    /** STRENGTH | CARDIO | FLEXIBILITY | PLYOMETRIC */
    private String exerciseType;

    private List<String> muscleGroups;

    private List<String> equipmentRequired;

    /** BEGINNER | INTERMEDIATE | ADVANCED */
    private String difficultyLevel;

    private String videoUrl;

    private String thumbnailUrl;

    private String instructions;

    // Getters and setters
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
}
