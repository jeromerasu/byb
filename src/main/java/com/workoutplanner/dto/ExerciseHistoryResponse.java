package com.workoutplanner.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ExerciseHistoryResponse {

    private String exerciseName;
    private LocalDate date;
    private Integer sets;
    private Integer reps;
    private BigDecimal weight;
    private String unit;
    private Boolean isPersonalRecord;

    public ExerciseHistoryResponse() {}

    public ExerciseHistoryResponse(String exerciseName, LocalDate date, Integer sets, Integer reps,
                                   BigDecimal weight, String unit, Boolean isPersonalRecord) {
        this.exerciseName = exerciseName;
        this.date = date;
        this.sets = sets;
        this.reps = reps;
        this.weight = weight;
        this.unit = unit;
        this.isPersonalRecord = isPersonalRecord;
    }

    public String getExerciseName() { return exerciseName; }
    public void setExerciseName(String exerciseName) { this.exerciseName = exerciseName; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public Integer getSets() { return sets; }
    public void setSets(Integer sets) { this.sets = sets; }

    public Integer getReps() { return reps; }
    public void setReps(Integer reps) { this.reps = reps; }

    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Boolean getIsPersonalRecord() { return isPersonalRecord; }
    public void setIsPersonalRecord(Boolean isPersonalRecord) { this.isPersonalRecord = isPersonalRecord; }
}
