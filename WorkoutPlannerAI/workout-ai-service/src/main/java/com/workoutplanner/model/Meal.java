package com.workoutplanner.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Meal {

    @JsonProperty("meal_name")
    private String mealName;

    @JsonProperty("meal_type")
    private String mealType; // breakfast, lunch, dinner, snack

    @JsonProperty("description")
    private String description;

    @JsonProperty("ingredients")
    private List<String> ingredients;

    @JsonProperty("cooking_instructions")
    private String cookingInstructions;

    @JsonProperty("prep_time_minutes")
    private Integer prepTimeMinutes;

    @JsonProperty("cook_time_minutes")
    private Integer cookTimeMinutes;

    @JsonProperty("servings")
    private Integer servings;

    @JsonProperty("nutritional_info")
    private NutritionalInfo nutritionalInfo;

    @JsonProperty("difficulty_level")
    private String difficultyLevel; // easy, medium, hard

    @JsonProperty("cuisine_type")
    private String cuisineType;

    @JsonProperty("dietary_tags")
    private List<String> dietaryTags; // vegetarian, vegan, gluten-free, etc.

    @JsonProperty("cost_estimate")
    private String costEstimate;

    public Meal() {}

    public Meal(String mealName, String mealType, String description, List<String> ingredients) {
        this.mealName = mealName;
        this.mealType = mealType;
        this.description = description;
        this.ingredients = ingredients;
    }

    // Getters and setters
    public String getMealName() {
        return mealName;
    }

    public void setMealName(String mealName) {
        this.mealName = mealName;
    }

    public String getMealType() {
        return mealType;
    }

    public void setMealType(String mealType) {
        this.mealType = mealType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<String> ingredients) {
        this.ingredients = ingredients;
    }

    public String getCookingInstructions() {
        return cookingInstructions;
    }

    public void setCookingInstructions(String cookingInstructions) {
        this.cookingInstructions = cookingInstructions;
    }

    public Integer getPrepTimeMinutes() {
        return prepTimeMinutes;
    }

    public void setPrepTimeMinutes(Integer prepTimeMinutes) {
        this.prepTimeMinutes = prepTimeMinutes;
    }

    public Integer getCookTimeMinutes() {
        return cookTimeMinutes;
    }

    public void setCookTimeMinutes(Integer cookTimeMinutes) {
        this.cookTimeMinutes = cookTimeMinutes;
    }

    public Integer getServings() {
        return servings;
    }

    public void setServings(Integer servings) {
        this.servings = servings;
    }

    public NutritionalInfo getNutritionalInfo() {
        return nutritionalInfo;
    }

    public void setNutritionalInfo(NutritionalInfo nutritionalInfo) {
        this.nutritionalInfo = nutritionalInfo;
    }

    public String getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(String difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public String getCuisineType() {
        return cuisineType;
    }

    public void setCuisineType(String cuisineType) {
        this.cuisineType = cuisineType;
    }

    public List<String> getDietaryTags() {
        return dietaryTags;
    }

    public void setDietaryTags(List<String> dietaryTags) {
        this.dietaryTags = dietaryTags;
    }

    public String getCostEstimate() {
        return costEstimate;
    }

    public void setCostEstimate(String costEstimate) {
        this.costEstimate = costEstimate;
    }
}