package com.workoutplanner.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.workoutplanner.dto.OpenAIRequest;
import com.workoutplanner.dto.OpenAIResponse;
import com.workoutplanner.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DietAIService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public DietAIService(WebClient webClient) {
        this.webClient = webClient;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public Mono<DietPlan> generateDietPlan(DietProfile dietProfile) {
        String prompt = buildDietPlanPrompt(dietProfile);

        OpenAIRequest request = new OpenAIRequest();
        request.setModel("gpt-4");
        request.setMessages(List.of(
            new OpenAIRequest.OpenAIMessage("system", getSystemPrompt()),
            new OpenAIRequest.OpenAIMessage("user", prompt)
        ));
        request.setMaxTokens(4000);
        request.setTemperature(0.7);

        return webClient.post()
                .uri("/v1/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OpenAIResponse.class)
                .map(response -> parseDietPlanResponse(response, dietProfile))
                .onErrorResume(error -> {
                    System.err.println("Error calling OpenAI API: " + error.getMessage());
                    return Mono.just(createFallbackDietPlan(dietProfile));
                });
    }

    private String getSystemPrompt() {
        return """
            You are a professional nutritionist and meal planning expert. Your task is to create comprehensive,
            personalized weekly diet plans based on individual dietary preferences, restrictions, and goals.

            IMPORTANT: Respond ONLY with valid JSON in the exact format specified. Do not include any explanatory
            text before or after the JSON.

            The response must be a JSON object with this exact structure:
            {
                "weekly_schedule": [
                    {
                        "day_of_week": "Monday",
                        "meals": [
                            {
                                "meal_name": "Protein-Packed Breakfast Bowl",
                                "meal_type": "breakfast",
                                "description": "A nutritious start to your day",
                                "ingredients": ["eggs", "spinach", "avocado", "whole grain toast"],
                                "cooking_instructions": "Step-by-step cooking instructions",
                                "prep_time_minutes": 15,
                                "cook_time_minutes": 10,
                                "servings": 1,
                                "nutritional_info": {
                                    "calories": 350,
                                    "protein_grams": 20.5,
                                    "carbs_grams": 25.0,
                                    "fat_grams": 18.0,
                                    "fiber_grams": 8.0,
                                    "sugar_grams": 3.0,
                                    "sodium_mg": 450.0
                                },
                                "difficulty_level": "easy",
                                "cuisine_type": "American",
                                "dietary_tags": ["high-protein", "low-carb"],
                                "cost_estimate": "low"
                            }
                        ],
                        "daily_notes": "Focus on hydration today",
                        "hydration_goals": "8 glasses of water",
                        "supplement_schedule": ["multivitamin morning"],
                        "daily_calories": 1800,
                        "daily_protein": 120.0,
                        "daily_carbs": 180.0,
                        "daily_fats": 65.0,
                        "meal_timing": ["7:00 AM", "12:00 PM", "3:00 PM", "7:00 PM"]
                    }
                ],
                "weekly_nutrition_summary": {
                    "calories": 12600,
                    "protein_grams": 840.0,
                    "carbs_grams": 1260.0,
                    "fat_grams": 455.0
                },
                "shopping_list": ["eggs", "spinach", "avocado", "chicken breast"],
                "meal_prep_notes": "Prep vegetables on Sunday. Cook grains in batches."
            }

            Guidelines:
            - Create 7 days of meal plans (Monday through Sunday)
            - Include the specified number of meals per day based on user preference
            - Respect all dietary restrictions and allergies
            - Focus on achieving the stated diet goals
            - Include detailed nutritional information for each meal
            - Provide practical cooking instructions
            - Consider user's cooking skill level and time constraints
            - Include cost estimates (low, medium, high)
            - Add helpful meal prep notes and shopping lists
            """;
    }

    private String buildDietPlanPrompt(DietProfile dietProfile) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Create a personalized weekly diet plan with the following specifications:\n\n");

        prompt.append("DIETARY PREFERENCES:\n");
        prompt.append("- Preferred Proteins: ").append(String.join(", ", dietProfile.getPreferredProteins())).append("\n");
        prompt.append("- Preferred Carbs: ").append(String.join(", ", dietProfile.getPreferredCarbs())).append("\n");
        prompt.append("- Preferred Fats: ").append(String.join(", ", dietProfile.getPreferredFats())).append("\n");

        if (dietProfile.getAllergies() != null && !dietProfile.getAllergies().isEmpty()) {
            prompt.append("- Allergies: ").append(String.join(", ", dietProfile.getAllergies())).append("\n");
        }

        if (dietProfile.getFoodsToAvoid() != null && !dietProfile.getFoodsToAvoid().isEmpty()) {
            prompt.append("- Foods to Avoid: ").append(String.join(", ", dietProfile.getFoodsToAvoid())).append("\n");
        }

        if (dietProfile.getFavoriteFoods() != null && !dietProfile.getFavoriteFoods().isEmpty()) {
            prompt.append("- Favorite Foods: ").append(String.join(", ", dietProfile.getFavoriteFoods())).append("\n");
        }

        prompt.append("\nGOALS & REQUIREMENTS:\n");
        prompt.append("- Diet Goals: ").append(dietProfile.getDietGoals()).append("\n");
        prompt.append("- Meals Per Day: ").append(dietProfile.getMealsPerDay()).append("\n");

        if (dietProfile.getTargetCalories() != null) {
            prompt.append("- Target Daily Calories: ").append(dietProfile.getTargetCalories()).append("\n");
        }

        if (dietProfile.getFavoriteCheatMeal() != null && !dietProfile.getFavoriteCheatMeal().isEmpty()) {
            prompt.append("- Favorite Cheat Meal: ").append(dietProfile.getFavoriteCheatMeal()).append(" (incorporate occasionally)\n");
        }

        if (dietProfile.getDietaryRestrictions() != null && !dietProfile.getDietaryRestrictions().isEmpty()) {
            prompt.append("- Dietary Restrictions: ").append(String.join(", ", dietProfile.getDietaryRestrictions())).append("\n");
        }

        if (dietProfile.getCuisinePreferences() != null && !dietProfile.getCuisinePreferences().isEmpty()) {
            prompt.append("- Cuisine Preferences: ").append(String.join(", ", dietProfile.getCuisinePreferences())).append("\n");
        }

        if (dietProfile.getCookingSkillLevel() != null && !dietProfile.getCookingSkillLevel().isEmpty()) {
            prompt.append("- Cooking Skill Level: ").append(dietProfile.getCookingSkillLevel()).append("\n");
        }

        if (dietProfile.getMealPrepPreference() != null && !dietProfile.getMealPrepPreference().isEmpty()) {
            prompt.append("- Meal Prep Preference: ").append(dietProfile.getMealPrepPreference()).append("\n");
        }

        if (dietProfile.getBudgetRange() != null && !dietProfile.getBudgetRange().isEmpty()) {
            prompt.append("- Budget Range: ").append(dietProfile.getBudgetRange()).append("\n");
        }

        if (dietProfile.getWaterIntakeGoal() != null && !dietProfile.getWaterIntakeGoal().isEmpty()) {
            prompt.append("- Water Intake Goal: ").append(dietProfile.getWaterIntakeGoal()).append("\n");
        }

        if (dietProfile.getSupplementPreferences() != null && !dietProfile.getSupplementPreferences().isEmpty()) {
            prompt.append("- Supplements: ").append(String.join(", ", dietProfile.getSupplementPreferences())).append("\n");
        }

        prompt.append("\nPlease create a complete weekly diet plan that addresses all these requirements and preferences.");

        return prompt.toString();
    }

    private DietPlan parseDietPlanResponse(OpenAIResponse response, DietProfile dietProfile) {
        try {
            String content = response.getChoices().get(0).getMessage().getContent();

            // Parse the JSON response
            Map<String, Object> planData = objectMapper.readValue(content, new TypeReference<Map<String, Object>>() {});

            // Extract weekly schedule
            List<DietDay> weeklySchedule = objectMapper.convertValue(
                planData.get("weekly_schedule"),
                new TypeReference<List<DietDay>>() {}
            );

            // Create diet plan
            DietPlan dietPlan = new DietPlan(dietProfile, weeklySchedule, content);

            // Set additional fields if they exist
            if (planData.containsKey("weekly_nutrition_summary")) {
                NutritionalInfo nutritionSummary = objectMapper.convertValue(
                    planData.get("weekly_nutrition_summary"),
                    NutritionalInfo.class
                );
                dietPlan.setWeeklyNutritionSummary(nutritionSummary);
            }

            if (planData.containsKey("shopping_list")) {
                List<String> shoppingList = objectMapper.convertValue(
                    planData.get("shopping_list"),
                    new TypeReference<List<String>>() {}
                );
                dietPlan.setShoppingList(shoppingList);
            }

            if (planData.containsKey("meal_prep_notes")) {
                dietPlan.setMealPrepNotes((String) planData.get("meal_prep_notes"));
            }

            return dietPlan;

        } catch (Exception e) {
            System.err.println("Error parsing diet plan response: " + e.getMessage());
            return createFallbackDietPlan(dietProfile);
        }
    }

    private DietPlan createFallbackDietPlan(DietProfile dietProfile) {
        // Create a basic fallback diet plan
        List<DietDay> weeklySchedule = new ArrayList<>();

        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        for (String day : days) {
            DietDay dietDay = new DietDay(day, createBasicMeals(dietProfile.getMealsPerDay()));
            weeklySchedule.add(dietDay);
        }

        DietPlan fallbackPlan = new DietPlan(dietProfile, weeklySchedule,
            "Fallback diet plan generated due to API error");
        fallbackPlan.setTitle("Basic Diet Plan - " + java.time.LocalDate.now());

        return fallbackPlan;
    }

    private List<Meal> createBasicMeals(int mealsPerDay) {
        List<Meal> meals = new ArrayList<>();

        // Create basic meals based on meals per day
        if (mealsPerDay >= 1) {
            Meal breakfast = new Meal("Basic Breakfast", "breakfast",
                "Healthy breakfast to start your day",
                List.of("oatmeal", "banana", "milk"));
            breakfast.setNutritionalInfo(new NutritionalInfo(300, 12.0, 45.0, 8.0));
            meals.add(breakfast);
        }

        if (mealsPerDay >= 2) {
            Meal lunch = new Meal("Basic Lunch", "lunch",
                "Nutritious midday meal",
                List.of("chicken breast", "brown rice", "vegetables"));
            lunch.setNutritionalInfo(new NutritionalInfo(450, 35.0, 40.0, 12.0));
            meals.add(lunch);
        }

        if (mealsPerDay >= 3) {
            Meal dinner = new Meal("Basic Dinner", "dinner",
                "Satisfying evening meal",
                List.of("salmon", "quinoa", "broccoli"));
            dinner.setNutritionalInfo(new NutritionalInfo(400, 30.0, 35.0, 15.0));
            meals.add(dinner);
        }

        // Add snacks if more meals are requested
        while (meals.size() < mealsPerDay) {
            Meal snack = new Meal("Healthy Snack", "snack",
                "Nutritious snack",
                List.of("almonds", "apple"));
            snack.setNutritionalInfo(new NutritionalInfo(200, 6.0, 20.0, 12.0));
            meals.add(snack);
        }

        return meals;
    }
}