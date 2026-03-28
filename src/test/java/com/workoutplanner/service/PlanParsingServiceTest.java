package com.workoutplanner.service;

import com.workoutplanner.dto.CurrentWeekResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class PlanParsingServiceTest {

    private PlanParsingService planParsingService;

    @BeforeEach
    void setUp() {
        planParsingService = new PlanParsingService();
    }

    /**
     * Build a minimal diet plan in the format the OpenAI service generates
     * (snake_case field names: meal_type, proteins, carbs, fats).
     */
    private Map<String, Object> buildStoredDietPlan(String mealType, int proteins, int carbs, int fats) {
        Map<String, Object> meal = new LinkedHashMap<>();
        meal.put("meal_type", mealType);
        meal.put("name", "Test Meal");
        meal.put("calories", 500);
        meal.put("proteins", proteins);
        meal.put("carbs", carbs);
        meal.put("fats", fats);
        meal.put("ingredients", List.of("ingredient1"));
        meal.put("instructions", "Cook it");

        Map<String, Object> day = new LinkedHashMap<>();
        day.put("done", false);
        day.put("meals", List.of(meal));

        Map<String, Object> week = new LinkedHashMap<>();
        week.put("done", false);
        week.put("monday", day);

        Map<String, Object> weeks = new LinkedHashMap<>();
        weeks.put("week_1", week);

        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("weeks", weeks);
        return plan;
    }

    @Test
    void parseMeals_shouldMapProteinCarbsFatCorrectly() {
        Map<String, Object> dietPlan = buildStoredDietPlan("breakfast", 35, 50, 15);

        CurrentWeekResponseDto result = planParsingService.extractCurrentWeek(null, dietPlan, 1);

        CurrentWeekResponseDto.DietWeekDto dietWeek = result.getDietWeek();
        assertThat(dietWeek).isNotNull();

        CurrentWeekResponseDto.DietDayDto monday = dietWeek.getDays().get("monday");
        assertThat(monday).isNotNull();
        assertThat(monday.getMeals()).hasSize(1);

        CurrentWeekResponseDto.MealDto meal = monday.getMeals().get(0);
        assertThat(meal.getProteinGrams()).isEqualTo(35);
        assertThat(meal.getCarbsGrams()).isEqualTo(50);
        assertThat(meal.getFatGrams()).isEqualTo(15);
        assertThat(meal.getCalories()).isEqualTo(500);
    }

    @Test
    void parseMeals_shouldMapMealTypeCorrectly() {
        Map<String, Object> dietPlan = buildStoredDietPlan("breakfast", 20, 30, 10);

        CurrentWeekResponseDto result = planParsingService.extractCurrentWeek(null, dietPlan, 1);

        CurrentWeekResponseDto.MealDto meal = result.getDietWeek().getDays().get("monday").getMeals().get(0);
        assertThat(meal.getMealType()).isEqualTo("breakfast");
    }

    @Test
    void parseMeals_shouldMapLunchMealType() {
        Map<String, Object> dietPlan = buildStoredDietPlan("lunch", 40, 60, 20);

        CurrentWeekResponseDto result = planParsingService.extractCurrentWeek(null, dietPlan, 1);

        CurrentWeekResponseDto.MealDto meal = result.getDietWeek().getDays().get("monday").getMeals().get(0);
        assertThat(meal.getMealType()).isEqualTo("lunch");
        assertThat(meal.getProteinGrams()).isEqualTo(40);
        assertThat(meal.getCarbsGrams()).isEqualTo(60);
        assertThat(meal.getFatGrams()).isEqualTo(20);
    }

    @Test
    void parseMeals_shouldNotDefaultToSnackWhenMealTypePresent() {
        Map<String, Object> dietPlan = buildStoredDietPlan("dinner", 50, 70, 25);

        CurrentWeekResponseDto result = planParsingService.extractCurrentWeek(null, dietPlan, 1);

        CurrentWeekResponseDto.MealDto meal = result.getDietWeek().getDays().get("monday").getMeals().get(0);
        assertThat(meal.getMealType()).isNotEqualTo("snack");
        assertThat(meal.getMealType()).isEqualTo("dinner");
    }

    @Test
    void parseMeals_macrosShouldNotBeZeroWhenDataPresent() {
        Map<String, Object> dietPlan = buildStoredDietPlan("breakfast", 35, 50, 15);

        CurrentWeekResponseDto result = planParsingService.extractCurrentWeek(null, dietPlan, 1);

        CurrentWeekResponseDto.MealDto meal = result.getDietWeek().getDays().get("monday").getMeals().get(0);
        assertThat(meal.getProteinGrams()).isNotEqualTo(0);
        assertThat(meal.getCarbsGrams()).isNotEqualTo(0);
        assertThat(meal.getFatGrams()).isNotEqualTo(0);
    }

    @Test
    void parseMeals_shouldHandleNullDietPlan() {
        CurrentWeekResponseDto result = planParsingService.extractCurrentWeek(null, null, 1);

        assertThat(result.getDietWeek()).isNotNull();
        assertThat(result.getDietWeek().getDays()).isEmpty();
    }
}
