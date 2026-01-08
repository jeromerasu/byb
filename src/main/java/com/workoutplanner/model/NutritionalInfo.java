package com.workoutplanner.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NutritionalInfo {

    @JsonProperty("calories")
    private Integer calories;

    @JsonProperty("protein_grams")
    private Double proteinGrams;

    @JsonProperty("carbs_grams")
    private Double carbsGrams;

    @JsonProperty("fat_grams")
    private Double fatGrams;

    @JsonProperty("fiber_grams")
    private Double fiberGrams;

    @JsonProperty("sugar_grams")
    private Double sugarGrams;

    @JsonProperty("sodium_mg")
    private Double sodiumMg;

    @JsonProperty("cholesterol_mg")
    private Double cholesterolMg;

    @JsonProperty("saturated_fat_grams")
    private Double saturatedFatGrams;

    @JsonProperty("unsaturated_fat_grams")
    private Double unsaturatedFatGrams;

    @JsonProperty("calcium_mg")
    private Double calciumMg;

    @JsonProperty("iron_mg")
    private Double ironMg;

    @JsonProperty("vitamin_c_mg")
    private Double vitaminCMg;

    @JsonProperty("vitamin_d_mcg")
    private Double vitaminDMcg;

    public NutritionalInfo() {}

    public NutritionalInfo(Integer calories, Double proteinGrams, Double carbsGrams, Double fatGrams) {
        this.calories = calories;
        this.proteinGrams = proteinGrams;
        this.carbsGrams = carbsGrams;
        this.fatGrams = fatGrams;
    }

    // Getters and setters
    public Integer getCalories() {
        return calories;
    }

    public void setCalories(Integer calories) {
        this.calories = calories;
    }

    public Double getProteinGrams() {
        return proteinGrams;
    }

    public void setProteinGrams(Double proteinGrams) {
        this.proteinGrams = proteinGrams;
    }

    public Double getCarbsGrams() {
        return carbsGrams;
    }

    public void setCarbsGrams(Double carbsGrams) {
        this.carbsGrams = carbsGrams;
    }

    public Double getFatGrams() {
        return fatGrams;
    }

    public void setFatGrams(Double fatGrams) {
        this.fatGrams = fatGrams;
    }

    public Double getFiberGrams() {
        return fiberGrams;
    }

    public void setFiberGrams(Double fiberGrams) {
        this.fiberGrams = fiberGrams;
    }

    public Double getSugarGrams() {
        return sugarGrams;
    }

    public void setSugarGrams(Double sugarGrams) {
        this.sugarGrams = sugarGrams;
    }

    public Double getSodiumMg() {
        return sodiumMg;
    }

    public void setSodiumMg(Double sodiumMg) {
        this.sodiumMg = sodiumMg;
    }

    public Double getCholesterolMg() {
        return cholesterolMg;
    }

    public void setCholesterolMg(Double cholesterolMg) {
        this.cholesterolMg = cholesterolMg;
    }

    public Double getSaturatedFatGrams() {
        return saturatedFatGrams;
    }

    public void setSaturatedFatGrams(Double saturatedFatGrams) {
        this.saturatedFatGrams = saturatedFatGrams;
    }

    public Double getUnsaturatedFatGrams() {
        return unsaturatedFatGrams;
    }

    public void setUnsaturatedFatGrams(Double unsaturatedFatGrams) {
        this.unsaturatedFatGrams = unsaturatedFatGrams;
    }

    public Double getCalciumMg() {
        return calciumMg;
    }

    public void setCalciumMg(Double calciumMg) {
        this.calciumMg = calciumMg;
    }

    public Double getIronMg() {
        return ironMg;
    }

    public void setIronMg(Double ironMg) {
        this.ironMg = ironMg;
    }

    public Double getVitaminCMg() {
        return vitaminCMg;
    }

    public void setVitaminCMg(Double vitaminCMg) {
        this.vitaminCMg = vitaminCMg;
    }

    public Double getVitaminDMcg() {
        return vitaminDMcg;
    }

    public void setVitaminDMcg(Double vitaminDMcg) {
        this.vitaminDMcg = vitaminDMcg;
    }
}