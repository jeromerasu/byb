package com.workoutplanner.dto;

import java.time.LocalDate;

public class NutritionAdherenceResponse {

    private LocalDate date;
    private double caloriesConsumed;
    private int calorieTarget;
    private double proteinConsumed;
    private int proteinTarget;
    private double carbsConsumed;
    private int carbsTarget;
    private double fatConsumed;
    private int fatTarget;
    private double adherenceScore;

    public NutritionAdherenceResponse() {}

    public NutritionAdherenceResponse(LocalDate date, double caloriesConsumed, int calorieTarget,
                                       double proteinConsumed, int proteinTarget,
                                       double carbsConsumed, int carbsTarget,
                                       double fatConsumed, int fatTarget,
                                       double adherenceScore) {
        this.date = date;
        this.caloriesConsumed = caloriesConsumed;
        this.calorieTarget = calorieTarget;
        this.proteinConsumed = proteinConsumed;
        this.proteinTarget = proteinTarget;
        this.carbsConsumed = carbsConsumed;
        this.carbsTarget = carbsTarget;
        this.fatConsumed = fatConsumed;
        this.fatTarget = fatTarget;
        this.adherenceScore = adherenceScore;
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public double getCaloriesConsumed() { return caloriesConsumed; }
    public void setCaloriesConsumed(double caloriesConsumed) { this.caloriesConsumed = caloriesConsumed; }

    public int getCalorieTarget() { return calorieTarget; }
    public void setCalorieTarget(int calorieTarget) { this.calorieTarget = calorieTarget; }

    public double getProteinConsumed() { return proteinConsumed; }
    public void setProteinConsumed(double proteinConsumed) { this.proteinConsumed = proteinConsumed; }

    public int getProteinTarget() { return proteinTarget; }
    public void setProteinTarget(int proteinTarget) { this.proteinTarget = proteinTarget; }

    public double getCarbsConsumed() { return carbsConsumed; }
    public void setCarbsConsumed(double carbsConsumed) { this.carbsConsumed = carbsConsumed; }

    public int getCarbsTarget() { return carbsTarget; }
    public void setCarbsTarget(int carbsTarget) { this.carbsTarget = carbsTarget; }

    public double getFatConsumed() { return fatConsumed; }
    public void setFatConsumed(double fatConsumed) { this.fatConsumed = fatConsumed; }

    public int getFatTarget() { return fatTarget; }
    public void setFatTarget(int fatTarget) { this.fatTarget = fatTarget; }

    public double getAdherenceScore() { return adherenceScore; }
    public void setAdherenceScore(double adherenceScore) { this.adherenceScore = adherenceScore; }
}
