package com.workoutplanner.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class DietDay {

    @JsonProperty("day_of_week")
    private String dayOfWeek;

    @JsonProperty("meals")
    private List<Meal> meals;

    @JsonProperty("daily_notes")
    private String dailyNotes;

    @JsonProperty("hydration_goals")
    private String hydrationGoals;

    @JsonProperty("supplement_schedule")
    private List<String> supplementSchedule;

    @JsonProperty("daily_calories")
    private Integer dailyCalories;

    @JsonProperty("daily_protein")
    private Double dailyProtein;

    @JsonProperty("daily_carbs")
    private Double dailyCarbs;

    @JsonProperty("daily_fats")
    private Double dailyFats;

    @JsonProperty("meal_timing")
    private List<String> mealTiming;

    public DietDay() {}

    public DietDay(String dayOfWeek, List<Meal> meals) {
        this.dayOfWeek = dayOfWeek;
        this.meals = meals;
    }

    // Getters and setters
    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public List<Meal> getMeals() {
        return meals;
    }

    public void setMeals(List<Meal> meals) {
        this.meals = meals;
    }

    public String getDailyNotes() {
        return dailyNotes;
    }

    public void setDailyNotes(String dailyNotes) {
        this.dailyNotes = dailyNotes;
    }

    public String getHydrationGoals() {
        return hydrationGoals;
    }

    public void setHydrationGoals(String hydrationGoals) {
        this.hydrationGoals = hydrationGoals;
    }

    public List<String> getSupplementSchedule() {
        return supplementSchedule;
    }

    public void setSupplementSchedule(List<String> supplementSchedule) {
        this.supplementSchedule = supplementSchedule;
    }

    public Integer getDailyCalories() {
        return dailyCalories;
    }

    public void setDailyCalories(Integer dailyCalories) {
        this.dailyCalories = dailyCalories;
    }

    public Double getDailyProtein() {
        return dailyProtein;
    }

    public void setDailyProtein(Double dailyProtein) {
        this.dailyProtein = dailyProtein;
    }

    public Double getDailyCarbs() {
        return dailyCarbs;
    }

    public void setDailyCarbs(Double dailyCarbs) {
        this.dailyCarbs = dailyCarbs;
    }

    public Double getDailyFats() {
        return dailyFats;
    }

    public void setDailyFats(Double dailyFats) {
        this.dailyFats = dailyFats;
    }

    public List<String> getMealTiming() {
        return mealTiming;
    }

    public void setMealTiming(List<String> mealTiming) {
        this.mealTiming = mealTiming;
    }
}