package com.workoutplanner.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class VolumeTrendResponse {

    private LocalDate date;
    private BigDecimal totalVolume;
    private int totalSets;
    private int totalReps;

    public VolumeTrendResponse() {}

    public VolumeTrendResponse(LocalDate date, BigDecimal totalVolume, int totalSets, int totalReps) {
        this.date = date;
        this.totalVolume = totalVolume;
        this.totalSets = totalSets;
        this.totalReps = totalReps;
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public BigDecimal getTotalVolume() { return totalVolume; }
    public void setTotalVolume(BigDecimal totalVolume) { this.totalVolume = totalVolume; }

    public int getTotalSets() { return totalSets; }
    public void setTotalSets(int totalSets) { this.totalSets = totalSets; }

    public int getTotalReps() { return totalReps; }
    public void setTotalReps(int totalReps) { this.totalReps = totalReps; }
}
