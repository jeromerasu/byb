package com.workoutplanner.dto;

import java.math.BigDecimal;

public class WeeklyOverviewResponse {

    private int workoutsCompleted;
    private int workoutsPlanned;
    private double consistencyScore;
    private int activeStreak;
    private double nutritionAdherence;
    private BigDecimal currentWeight;
    private BigDecimal weightChange7d;

    public WeeklyOverviewResponse() {}

    public WeeklyOverviewResponse(int workoutsCompleted, int workoutsPlanned, double consistencyScore,
                                   int activeStreak, double nutritionAdherence,
                                   BigDecimal currentWeight, BigDecimal weightChange7d) {
        this.workoutsCompleted = workoutsCompleted;
        this.workoutsPlanned = workoutsPlanned;
        this.consistencyScore = consistencyScore;
        this.activeStreak = activeStreak;
        this.nutritionAdherence = nutritionAdherence;
        this.currentWeight = currentWeight;
        this.weightChange7d = weightChange7d;
    }

    public int getWorkoutsCompleted() { return workoutsCompleted; }
    public void setWorkoutsCompleted(int workoutsCompleted) { this.workoutsCompleted = workoutsCompleted; }

    public int getWorkoutsPlanned() { return workoutsPlanned; }
    public void setWorkoutsPlanned(int workoutsPlanned) { this.workoutsPlanned = workoutsPlanned; }

    public double getConsistencyScore() { return consistencyScore; }
    public void setConsistencyScore(double consistencyScore) { this.consistencyScore = consistencyScore; }

    public int getActiveStreak() { return activeStreak; }
    public void setActiveStreak(int activeStreak) { this.activeStreak = activeStreak; }

    public double getNutritionAdherence() { return nutritionAdherence; }
    public void setNutritionAdherence(double nutritionAdherence) { this.nutritionAdherence = nutritionAdherence; }

    public BigDecimal getCurrentWeight() { return currentWeight; }
    public void setCurrentWeight(BigDecimal currentWeight) { this.currentWeight = currentWeight; }

    public BigDecimal getWeightChange7d() { return weightChange7d; }
    public void setWeightChange7d(BigDecimal weightChange7d) { this.weightChange7d = weightChange7d; }
}
