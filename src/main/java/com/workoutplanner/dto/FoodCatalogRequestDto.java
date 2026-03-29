package com.workoutplanner.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public class FoodCatalogRequestDto {

    @NotBlank(message = "Food name is required")
    private String name;

    private String category;
    private String servingSize;
    private Integer calories;
    private BigDecimal proteinGrams;
    private BigDecimal carbsGrams;
    private BigDecimal fatGrams;
    private BigDecimal fiberGrams;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getServingSize() { return servingSize; }
    public void setServingSize(String servingSize) { this.servingSize = servingSize; }

    public Integer getCalories() { return calories; }
    public void setCalories(Integer calories) { this.calories = calories; }

    public BigDecimal getProteinGrams() { return proteinGrams; }
    public void setProteinGrams(BigDecimal proteinGrams) { this.proteinGrams = proteinGrams; }

    public BigDecimal getCarbsGrams() { return carbsGrams; }
    public void setCarbsGrams(BigDecimal carbsGrams) { this.carbsGrams = carbsGrams; }

    public BigDecimal getFatGrams() { return fatGrams; }
    public void setFatGrams(BigDecimal fatGrams) { this.fatGrams = fatGrams; }

    public BigDecimal getFiberGrams() { return fiberGrams; }
    public void setFiberGrams(BigDecimal fiberGrams) { this.fiberGrams = fiberGrams; }
}
