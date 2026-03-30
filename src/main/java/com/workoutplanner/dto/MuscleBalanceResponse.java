package com.workoutplanner.dto;

import java.math.BigDecimal;

public class MuscleBalanceResponse {

    private String muscleGroup;
    private int workoutCount;
    private int totalSets;
    private BigDecimal totalVolume;

    public MuscleBalanceResponse() {}

    public MuscleBalanceResponse(String muscleGroup, int workoutCount, int totalSets, BigDecimal totalVolume) {
        this.muscleGroup = muscleGroup;
        this.workoutCount = workoutCount;
        this.totalSets = totalSets;
        this.totalVolume = totalVolume;
    }

    public String getMuscleGroup() { return muscleGroup; }
    public void setMuscleGroup(String muscleGroup) { this.muscleGroup = muscleGroup; }

    public int getWorkoutCount() { return workoutCount; }
    public void setWorkoutCount(int workoutCount) { this.workoutCount = workoutCount; }

    public int getTotalSets() { return totalSets; }
    public void setTotalSets(int totalSets) { this.totalSets = totalSets; }

    public BigDecimal getTotalVolume() { return totalVolume; }
    public void setTotalVolume(BigDecimal totalVolume) { this.totalVolume = totalVolume; }
}
