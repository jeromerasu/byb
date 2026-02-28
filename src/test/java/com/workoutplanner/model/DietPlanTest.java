package com.workoutplanner.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.List;

public class DietPlanTest {

    @Test
    public void testDietPlanCreation() {
        DietProfile profile = new DietProfile(
            Arrays.asList("chicken"),
            Arrays.asList("rice"),
            Arrays.asList("olive oil"),
            "muscle gain",
            4
        );

        Meal meal = new Meal("Test Meal", "breakfast", "Test description", Arrays.asList("eggs", "toast"));
        DietDay day = new DietDay("Monday", Arrays.asList(meal));
        List<DietDay> weeklySchedule = Arrays.asList(day);

        DietPlan dietPlan = new DietPlan(profile, weeklySchedule, "AI generated response");

        assertNotNull(dietPlan.getId());
        assertEquals(profile, dietPlan.getDietProfile());
        assertEquals(weeklySchedule, dietPlan.getWeeklySchedule());
        assertEquals("AI generated response", dietPlan.getAiResponse());
        assertNotNull(dietPlan.getGeneratedAt());
        assertTrue(dietPlan.getTitle().contains("Diet Plan"));
    }

    @Test
    public void testDietPlanAdditionalFields() {
        DietPlan dietPlan = new DietPlan();
        dietPlan.setId("test-id");
        dietPlan.setTitle("Custom Diet Plan");
        dietPlan.setMealPrepNotes("Prep on Sundays");

        NutritionalInfo nutrition = new NutritionalInfo(1800, 120.0, 200.0, 60.0);
        dietPlan.setWeeklyNutritionSummary(nutrition);

        List<String> shoppingList = Arrays.asList("chicken", "rice", "broccoli");
        dietPlan.setShoppingList(shoppingList);

        assertEquals("test-id", dietPlan.getId());
        assertEquals("Custom Diet Plan", dietPlan.getTitle());
        assertEquals("Prep on Sundays", dietPlan.getMealPrepNotes());
        assertEquals(nutrition, dietPlan.getWeeklyNutritionSummary());
        assertEquals(shoppingList, dietPlan.getShoppingList());
    }
}