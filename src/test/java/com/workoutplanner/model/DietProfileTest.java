package com.workoutplanner.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.List;

public class DietProfileTest {

    @Test
    public void testDietProfileCreation() {
        List<String> proteins = Arrays.asList("chicken", "salmon", "tofu");
        List<String> carbs = Arrays.asList("rice", "quinoa", "sweet potato");
        List<String> fats = Arrays.asList("avocado", "olive oil", "nuts");

        DietProfile profile = new DietProfile(proteins, carbs, fats, "weight loss", 3);

        assertEquals(proteins, profile.getPreferredProteins());
        assertEquals(carbs, profile.getPreferredCarbs());
        assertEquals(fats, profile.getPreferredFats());
        assertEquals("weight loss", profile.getDietGoals());
        assertEquals(3, profile.getMealsPerDay());
    }

    @Test
    public void testDietProfileWithAllergies() {
        DietProfile profile = new DietProfile();
        profile.setAllergies(Arrays.asList("peanuts", "shellfish"));
        profile.setFoodsToAvoid(Arrays.asList("processed foods", "sugar"));

        assertEquals(2, profile.getAllergies().size());
        assertEquals(2, profile.getFoodsToAvoid().size());
        assertTrue(profile.getAllergies().contains("peanuts"));
        assertTrue(profile.getFoodsToAvoid().contains("processed foods"));
    }

    @Test
    public void testDietProfileCalorieTarget() {
        DietProfile profile = new DietProfile();
        profile.setTargetCalories(2000);
        profile.setCookingSkillLevel("intermediate");
        profile.setBudgetRange("medium");

        assertEquals(Integer.valueOf(2000), profile.getTargetCalories());
        assertEquals("intermediate", profile.getCookingSkillLevel());
        assertEquals("medium", profile.getBudgetRange());
    }
}