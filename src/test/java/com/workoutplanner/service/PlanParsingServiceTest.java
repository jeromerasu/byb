package com.workoutplanner.service;

import com.workoutplanner.dto.CurrentWeekResponseDto;
import com.workoutplanner.repository.ExerciseCatalogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PlanParsingServiceTest {

    @Mock
    private ExerciseCatalogRepository exerciseCatalogRepository;

    private PlanParsingService planParsingService;

    @BeforeEach
    void setUp() {
        planParsingService = new PlanParsingService(exerciseCatalogRepository);
    }

    /**
     * Build a minimal diet plan with explicit field names for each meal macro.
     */
    private Map<String, Object> buildStoredDietPlan(Map<String, Object> mealFields) {
        Map<String, Object> meal = new LinkedHashMap<>(mealFields);
        meal.putIfAbsent("name", "Test Meal");
        meal.putIfAbsent("calories", 500);

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

    private CurrentWeekResponseDto.MealDto parseSingleMeal(Map<String, Object> mealFields) {
        Map<String, Object> dietPlan = buildStoredDietPlan(mealFields);
        CurrentWeekResponseDto result = planParsingService.extractCurrentWeek(null, dietPlan, 1);
        return result.getDietWeek().getDays().get("monday").getMeals().get(0);
    }

    // ─── meal_type variants ────────────────────────────────────────────────────

    @Test
    void parseMeals_mealType_fromMeal_type() {
        CurrentWeekResponseDto.MealDto meal = parseSingleMeal(Map.of(
                "meal_type", "breakfast", "proteins", 20, "carbs", 30, "fats", 10));
        assertThat(meal.getMealType()).isEqualTo("breakfast");
    }

    @Test
    void parseMeals_mealType_fromMealType_camelCase() {
        CurrentWeekResponseDto.MealDto meal = parseSingleMeal(Map.of(
                "mealType", "lunch", "proteins", 20, "carbs", 30, "fats", 10));
        assertThat(meal.getMealType()).isEqualTo("lunch");
    }

    @Test
    void parseMeals_mealType_fromType() {
        CurrentWeekResponseDto.MealDto meal = parseSingleMeal(Map.of(
                "type", "dinner", "proteins", 20, "carbs", 30, "fats", 10));
        assertThat(meal.getMealType()).isEqualTo("dinner");
    }

    // ─── protein variants ─────────────────────────────────────────────────────

    @Test
    void parseMeals_protein_fromProteins() {
        CurrentWeekResponseDto.MealDto meal = parseSingleMeal(Map.of(
                "meal_type", "breakfast", "proteins", 35, "carbs", 50, "fats", 15));
        assertThat(meal.getProteinGrams()).isEqualTo(35);
    }

    @Test
    void parseMeals_protein_fromProtein_singular() {
        CurrentWeekResponseDto.MealDto meal = parseSingleMeal(Map.of(
                "meal_type", "breakfast", "protein", 40, "carbs", 50, "fats", 15));
        assertThat(meal.getProteinGrams()).isEqualTo(40);
    }

    @Test
    void parseMeals_protein_fromProtein_grams() {
        CurrentWeekResponseDto.MealDto meal = parseSingleMeal(Map.of(
                "meal_type", "breakfast", "protein_grams", 45, "carbs", 50, "fats", 15));
        assertThat(meal.getProteinGrams()).isEqualTo(45);
    }

    @Test
    void parseMeals_protein_fromProteinGrams_camelCase() {
        CurrentWeekResponseDto.MealDto meal = parseSingleMeal(Map.of(
                "meal_type", "breakfast", "proteinGrams", 50, "carbs", 50, "fats", 15));
        assertThat(meal.getProteinGrams()).isEqualTo(50);
    }

    // ─── carbs variants ───────────────────────────────────────────────────────

    @Test
    void parseMeals_carbs_fromCarbs() {
        CurrentWeekResponseDto.MealDto meal = parseSingleMeal(Map.of(
                "meal_type", "lunch", "proteins", 30, "carbs", 60, "fats", 20));
        assertThat(meal.getCarbsGrams()).isEqualTo(60);
    }

    @Test
    void parseMeals_carbs_fromCarbohydrates() {
        CurrentWeekResponseDto.MealDto meal = parseSingleMeal(Map.of(
                "meal_type", "lunch", "proteins", 30, "carbohydrates", 65, "fats", 20));
        assertThat(meal.getCarbsGrams()).isEqualTo(65);
    }

    @Test
    void parseMeals_carbs_fromCarbs_grams() {
        CurrentWeekResponseDto.MealDto meal = parseSingleMeal(Map.of(
                "meal_type", "lunch", "proteins", 30, "carbs_grams", 70, "fats", 20));
        assertThat(meal.getCarbsGrams()).isEqualTo(70);
    }

    @Test
    void parseMeals_carbs_fromCarbsGrams_camelCase() {
        CurrentWeekResponseDto.MealDto meal = parseSingleMeal(Map.of(
                "meal_type", "lunch", "proteins", 30, "carbsGrams", 75, "fats", 20));
        assertThat(meal.getCarbsGrams()).isEqualTo(75);
    }

    // ─── fat variants ─────────────────────────────────────────────────────────

    @Test
    void parseMeals_fat_fromFats() {
        CurrentWeekResponseDto.MealDto meal = parseSingleMeal(Map.of(
                "meal_type", "dinner", "proteins", 25, "carbs", 45, "fats", 18));
        assertThat(meal.getFatGrams()).isEqualTo(18);
    }

    @Test
    void parseMeals_fat_fromFat_singular() {
        CurrentWeekResponseDto.MealDto meal = parseSingleMeal(Map.of(
                "meal_type", "dinner", "proteins", 25, "carbs", 45, "fat", 22));
        assertThat(meal.getFatGrams()).isEqualTo(22);
    }

    @Test
    void parseMeals_fat_fromFat_grams() {
        CurrentWeekResponseDto.MealDto meal = parseSingleMeal(Map.of(
                "meal_type", "dinner", "proteins", 25, "carbs", 45, "fat_grams", 25));
        assertThat(meal.getFatGrams()).isEqualTo(25);
    }

    @Test
    void parseMeals_fat_fromFatGrams_camelCase() {
        CurrentWeekResponseDto.MealDto meal = parseSingleMeal(Map.of(
                "meal_type", "dinner", "proteins", 25, "carbs", 45, "fatGrams", 28));
        assertThat(meal.getFatGrams()).isEqualTo(28);
    }

    // ─── original happy-path tests (regression guard) ─────────────────────────

    @Test
    void parseMeals_shouldMapProteinCarbsFatCorrectly() {
        CurrentWeekResponseDto.MealDto meal = parseSingleMeal(Map.of(
                "meal_type", "breakfast", "proteins", 35, "carbs", 50, "fats", 15, "calories", 500));
        assertThat(meal.getProteinGrams()).isEqualTo(35);
        assertThat(meal.getCarbsGrams()).isEqualTo(50);
        assertThat(meal.getFatGrams()).isEqualTo(15);
        assertThat(meal.getCalories()).isEqualTo(500);
    }

    @Test
    void parseMeals_shouldNotDefaultToSnackWhenMealTypePresent() {
        CurrentWeekResponseDto.MealDto meal = parseSingleMeal(Map.of(
                "meal_type", "dinner", "proteins", 50, "carbs", 70, "fats", 25));
        assertThat(meal.getMealType()).isNotEqualTo("snack");
        assertThat(meal.getMealType()).isEqualTo("dinner");
    }

    @Test
    void parseMeals_macrosShouldNotBeZeroWhenDataPresent() {
        CurrentWeekResponseDto.MealDto meal = parseSingleMeal(Map.of(
                "meal_type", "breakfast", "proteins", 35, "carbs", 50, "fats", 15));
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

    // ─── 7-day multi-day parsing ───────────────────────────────────────────────

    /**
     * Builds a full 7-day diet plan using the structured weeks format that OpenAI returns.
     * Each day has 3 meals (breakfast, lunch, dinner) with non-zero macros.
     */
    private Map<String, Object> buildSevenDayDietPlan() {
        String[] dayKeys = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};
        String[] mealTypes = {"breakfast", "lunch", "dinner"};

        Map<String, Object> week = new LinkedHashMap<>();
        week.put("done", false);

        for (int d = 0; d < dayKeys.length; d++) {
            List<Map<String, Object>> meals = new ArrayList<>();
            for (int m = 0; m < mealTypes.length; m++) {
                Map<String, Object> meal = new LinkedHashMap<>();
                meal.put("meal_type", mealTypes[m]);
                meal.put("name", dayKeys[d] + " " + mealTypes[m]);
                meal.put("calories", 400 + d * 10 + m * 5);
                meal.put("proteins", 30 + d + m);
                meal.put("carbs", 40 + d + m);
                meal.put("fats", 15 + d + m);
                meals.add(meal);
            }
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("done", false);
            day.put("meals", meals);
            week.put(dayKeys[d], day);
        }

        Map<String, Object> weeks = new LinkedHashMap<>();
        weeks.put("week_1", week);

        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("weeks", weeks);
        return plan;
    }

    @Test
    void allSevenDays_shouldParseWithCorrectMealTypesAndNonZeroMacros() {
        Map<String, Object> dietPlan = buildSevenDayDietPlan();
        CurrentWeekResponseDto result = planParsingService.extractCurrentWeek(null, dietPlan, 1);

        Map<String, CurrentWeekResponseDto.DietDayDto> days = result.getDietWeek().getDays();
        String[] dayKeys = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};

        for (String dayKey : dayKeys) {
            assertThat(days).containsKey(dayKey);
            List<CurrentWeekResponseDto.MealDto> meals = days.get(dayKey).getMeals();
            assertThat(meals).as("meals for " + dayKey).hasSize(3);

            for (CurrentWeekResponseDto.MealDto meal : meals) {
                assertThat(meal.getName()).as(dayKey + " meal name").doesNotContain("Unknown Food");
                assertThat(meal.getMealType()).as(dayKey + " meal_type").isIn("breakfast", "lunch", "dinner");
                assertThat(meal.getProteinGrams()).as(dayKey + " protein").isGreaterThan(0);
                assertThat(meal.getCarbsGrams()).as(dayKey + " carbs").isGreaterThan(0);
                assertThat(meal.getFatGrams()).as(dayKey + " fat").isGreaterThan(0);
                assertThat(meal.getCalories()).as(dayKey + " calories").isGreaterThan(0);
            }
        }
    }

    @Test
    void allSevenDays_firstDayMealsDoNotLeakToOtherDays() {
        Map<String, Object> dietPlan = buildSevenDayDietPlan();
        CurrentWeekResponseDto result = planParsingService.extractCurrentWeek(null, dietPlan, 1);

        Map<String, CurrentWeekResponseDto.DietDayDto> days = result.getDietWeek().getDays();

        // Each day's meals should be named after that specific day, not monday
        String[] dayKeys = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};
        for (String dayKey : dayKeys) {
            List<CurrentWeekResponseDto.MealDto> meals = days.get(dayKey).getMeals();
            for (CurrentWeekResponseDto.MealDto meal : meals) {
                assertThat(meal.getName()).as("meal for " + dayKey + " should not be from monday")
                        .startsWith(dayKey);
            }
        }
    }
}
