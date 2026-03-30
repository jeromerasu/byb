package com.workoutplanner.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class WorkoutHeatmapResponse {

    private LocalDate date;
    private Integer workoutCount;
    private Integer totalSets;
    private Integer totalDuration;
    private BigDecimal totalVolume;

    public WorkoutHeatmapResponse() {}

    public WorkoutHeatmapResponse(LocalDate date, Integer workoutCount, Integer totalSets,
                                   Integer totalDuration, BigDecimal totalVolume) {
        this.date = date;
        this.workoutCount = workoutCount;
        this.totalSets = totalSets;
        this.totalDuration = totalDuration;
        this.totalVolume = totalVolume;
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public Integer getWorkoutCount() { return workoutCount; }
    public void setWorkoutCount(Integer workoutCount) { this.workoutCount = workoutCount; }

    public Integer getTotalSets() { return totalSets; }
    public void setTotalSets(Integer totalSets) { this.totalSets = totalSets; }

    public Integer getTotalDuration() { return totalDuration; }
    public void setTotalDuration(Integer totalDuration) { this.totalDuration = totalDuration; }

    public BigDecimal getTotalVolume() { return totalVolume; }
    public void setTotalVolume(BigDecimal totalVolume) { this.totalVolume = totalVolume; }
}
