package com.workoutplanner.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class DietProfile {

    @NotNull(message = "Preferred proteins cannot be null")
    @JsonProperty("preferred_proteins")
    private List<String> preferredProteins;

    @NotNull(message = "Preferred carbs cannot be null")
    @JsonProperty("preferred_carbs")
    private List<String> preferredCarbs;

    @NotNull(message = "Preferred fats cannot be null")
    @JsonProperty("preferred_fats")
    private List<String> preferredFats;

    @JsonProperty("allergies")
    private List<String> allergies;

    @JsonProperty("foods_to_avoid")
    private List<String> foodsToAvoid;

    @JsonProperty("favorite_foods")
    private List<String> favoriteFoods;

    @NotNull(message = "Diet goals cannot be null")
    @JsonProperty("diet_goals")
    private String dietGoals;

    @Min(value = 1, message = "Must have at least 1 meal per day")
    @Max(value = 8, message = "Cannot have more than 8 meals per day")
    @JsonProperty("meals_per_day")
    private int mealsPerDay;

    @JsonProperty("favorite_cheat_meal")
    private String favoriteCheatMeal;

    @JsonProperty("dietary_restrictions")
    private List<String> dietaryRestrictions;

    @JsonProperty("cuisine_preferences")
    private List<String> cuisinePreferences;

    @JsonProperty("cooking_skill_level")
    private String cookingSkillLevel;

    @Min(value = 800, message = "Daily calories must be at least 800")
    @Max(value = 5000, message = "Daily calories cannot exceed 5000")
    @JsonProperty("target_calories")
    private Integer targetCalories;

    @JsonProperty("meal_prep_preference")
    private String mealPrepPreference;

    @JsonProperty("budget_range")
    private String budgetRange;

    @JsonProperty("water_intake_goal")
    private String waterIntakeGoal;

    @JsonProperty("supplement_preferences")
    private List<String> supplementPreferences;

    public DietProfile() {}

    public DietProfile(List<String> preferredProteins, List<String> preferredCarbs,
                      List<String> preferredFats, String dietGoals, int mealsPerDay) {
        this.preferredProteins = preferredProteins;
        this.preferredCarbs = preferredCarbs;
        this.preferredFats = preferredFats;
        this.dietGoals = dietGoals;
        this.mealsPerDay = mealsPerDay;
    }

    // Getters and setters
    public List<String> getPreferredProteins() {
        return preferredProteins;
    }

    public void setPreferredProteins(List<String> preferredProteins) {
        this.preferredProteins = preferredProteins;
    }

    public List<String> getPreferredCarbs() {
        return preferredCarbs;
    }

    public void setPreferredCarbs(List<String> preferredCarbs) {
        this.preferredCarbs = preferredCarbs;
    }

    public List<String> getPreferredFats() {
        return preferredFats;
    }

    public void setPreferredFats(List<String> preferredFats) {
        this.preferredFats = preferredFats;
    }

    public List<String> getAllergies() {
        return allergies;
    }

    public void setAllergies(List<String> allergies) {
        this.allergies = allergies;
    }

    public List<String> getFoodsToAvoid() {
        return foodsToAvoid;
    }

    public void setFoodsToAvoid(List<String> foodsToAvoid) {
        this.foodsToAvoid = foodsToAvoid;
    }

    public List<String> getFavoriteFoods() {
        return favoriteFoods;
    }

    public void setFavoriteFoods(List<String> favoriteFoods) {
        this.favoriteFoods = favoriteFoods;
    }

    public String getDietGoals() {
        return dietGoals;
    }

    public void setDietGoals(String dietGoals) {
        this.dietGoals = dietGoals;
    }

    public int getMealsPerDay() {
        return mealsPerDay;
    }

    public void setMealsPerDay(int mealsPerDay) {
        this.mealsPerDay = mealsPerDay;
    }

    public String getFavoriteCheatMeal() {
        return favoriteCheatMeal;
    }

    public void setFavoriteCheatMeal(String favoriteCheatMeal) {
        this.favoriteCheatMeal = favoriteCheatMeal;
    }

    public List<String> getDietaryRestrictions() {
        return dietaryRestrictions;
    }

    public void setDietaryRestrictions(List<String> dietaryRestrictions) {
        this.dietaryRestrictions = dietaryRestrictions;
    }

    public List<String> getCuisinePreferences() {
        return cuisinePreferences;
    }

    public void setCuisinePreferences(List<String> cuisinePreferences) {
        this.cuisinePreferences = cuisinePreferences;
    }

    public String getCookingSkillLevel() {
        return cookingSkillLevel;
    }

    public void setCookingSkillLevel(String cookingSkillLevel) {
        this.cookingSkillLevel = cookingSkillLevel;
    }

    public Integer getTargetCalories() {
        return targetCalories;
    }

    public void setTargetCalories(Integer targetCalories) {
        this.targetCalories = targetCalories;
    }

    public String getMealPrepPreference() {
        return mealPrepPreference;
    }

    public void setMealPrepPreference(String mealPrepPreference) {
        this.mealPrepPreference = mealPrepPreference;
    }

    public String getBudgetRange() {
        return budgetRange;
    }

    public void setBudgetRange(String budgetRange) {
        this.budgetRange = budgetRange;
    }

    public String getWaterIntakeGoal() {
        return waterIntakeGoal;
    }

    public void setWaterIntakeGoal(String waterIntakeGoal) {
        this.waterIntakeGoal = waterIntakeGoal;
    }

    public List<String> getSupplementPreferences() {
        return supplementPreferences;
    }

    public void setSupplementPreferences(List<String> supplementPreferences) {
        this.supplementPreferences = supplementPreferences;
    }
}