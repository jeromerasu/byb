package com.workoutplanner.service;

import com.workoutplanner.model.DietProfile;
import com.workoutplanner.model.User;
import com.workoutplanner.repository.DietProfileRepository;
import com.workoutplanner.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;
import java.util.Arrays;

@Service
public class DietService {

    private final DietProfileRepository dietProfileRepository;
    private final UserRepository userRepository;
    private final Optional<ObjectStorageService> objectStorageService;

    @Autowired
    public DietService(DietProfileRepository dietProfileRepository,
                      UserRepository userRepository,
                      Optional<ObjectStorageService> objectStorageService) {
        this.dietProfileRepository = dietProfileRepository;
        this.userRepository = userRepository;
        this.objectStorageService = objectStorageService;
    }

    @Transactional
    public Mono<DietProfile> createOrUpdateProfile(String userId, DietProfile profile) {
        return Mono.fromCallable(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            profile.setUserId(userId);

            // Check if profile already exists
            Optional<DietProfile> existingProfile = dietProfileRepository.findByUserId(userId);
            if (existingProfile.isPresent()) {
                profile.setId(existingProfile.get().getId());
                profile.setCreatedAt(existingProfile.get().getCreatedAt());
            }

            profile.setUpdatedAt(LocalDateTime.now());

            DietProfile savedProfile = dietProfileRepository.save(profile);

            // Update user's profile reference
            user.setDietProfileId(savedProfile.getId());
            userRepository.save(user);

            return savedProfile;
        });
    }

    @Transactional
    public Mono<Map<String, Object>> generateDietPlan(String userId) {
        return Mono.fromCallable(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            DietProfile dietProfile = dietProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Diet profile not found for user"));

            // Generate diet plan based on profile
            Map<String, Object> dietPlan = createDietPlan(dietProfile);

            // Store the plan in object storage
            String storageKey = "diet-plans/" + userId + "/" + UUID.randomUUID() + ".json";
            String planTitle = "Diet Plan - " + LocalDateTime.now().toLocalDate();

            try {
                objectStorageService.orElseThrow(() -> new RuntimeException("Object storage service not available"))
                    .storeDietPlan("diet", userId, planTitle, dietPlan);

                // Update diet profile with current plan info
                dietProfile.setCurrentPlanStorageKey(storageKey);
                dietProfile.setCurrentPlanTitle(planTitle);
                dietProfile.setCurrentPlanCreatedAt(LocalDateTime.now());
                dietProfile.setUpdatedAt(LocalDateTime.now());
                dietProfileRepository.save(dietProfile);

                Map<String, Object> response = new HashMap<>();
                response.put("message", "Diet plan generated successfully");
                response.put("planTitle", planTitle);
                response.put("storageKey", storageKey);
                response.put("plan", dietPlan);
                response.put("user", Map.of("id", user.getId(), "username", user.getUsername()));
                response.put("profile", dietProfile);

                return response;

            } catch (Exception e) {
                throw new RuntimeException("Failed to store diet plan: " + e.getMessage(), e);
            }
        });
    }

    public Mono<Map<String, Object>> getCurrentDietPlan(String userId) {
        return Mono.fromCallable(() -> {
            DietProfile profile = dietProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Diet profile not found"));

            if (profile.getCurrentPlanStorageKey() == null) {
                throw new RuntimeException("No current diet plan found");
            }

            try {
                Map<String, Object> plan = objectStorageService.orElseThrow(() -> new RuntimeException("Object storage service not available"))
                    .retrieveDietPlan("diet", userId, profile.getCurrentPlanStorageKey());

                Map<String, Object> response = new HashMap<>();
                response.put("planTitle", profile.getCurrentPlanTitle());
                response.put("createdAt", profile.getCurrentPlanCreatedAt());
                response.put("storageKey", profile.getCurrentPlanStorageKey());
                response.put("plan", plan);

                return response;

            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve diet plan: " + e.getMessage(), e);
            }
        });
    }

    @Transactional
    public Mono<DietProfile> logMeal(String userId, Map<String, Object> mealData) {
        return Mono.fromCallable(() -> {
            DietProfile profile = dietProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Diet profile not found"));

            // Update meal tracking
            profile.setLastMealLogged(LocalDateTime.now());

            Integer currentTotal = profile.getTotalMealsLogged();
            if (currentTotal == null) currentTotal = 0;
            profile.setTotalMealsLogged(currentTotal + 1);

            profile.setUpdatedAt(LocalDateTime.now());

            return dietProfileRepository.save(profile);
        });
    }

    public Mono<Optional<DietProfile>> getDietProfile(String userId) {
        return Mono.fromCallable(() -> dietProfileRepository.findByUserId(userId));
    }

    public Mono<Map<String, Object>> getDietStats(String userId) {
        return Mono.fromCallable(() -> {
            Optional<DietProfile> profileOpt = dietProfileRepository.findByUserId(userId);

            if (profileOpt.isEmpty()) {
                throw new RuntimeException("Diet profile not found");
            }

            DietProfile profile = profileOpt.get();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalMealsLogged", profile.getTotalMealsLogged());
            stats.put("lastMealLogged", profile.getLastMealLogged());
            stats.put("dailyCalorieGoal", profile.getDailyCalorieGoal());
            stats.put("mealsPerDay", profile.getMealsPerDay());
            stats.put("dietType", profile.getDietType());
            stats.put("weightGoal", profile.getWeightGoal());
            stats.put("hasCurrentPlan", profile.getCurrentPlanStorageKey() != null);
            stats.put("profileCreated", profile.getCreatedAt());
            stats.put("lastUpdated", profile.getUpdatedAt());

            return stats;
        });
    }

    private Map<String, Object> createDietPlan(DietProfile profile) {
        Map<String, Object> plan = new HashMap<>();

        plan.put("title", "Personalized Diet Plan");
        plan.put("dietType", profile.getDietType());
        plan.put("dailyCalories", profile.getDailyCalorieGoal());
        plan.put("mealsPerDay", profile.getMealsPerDay());
        plan.put("restrictions", profile.getDietaryRestrictions());
        plan.put("preferredCuisines", profile.getPreferredCuisines());
        plan.put("dislikedFoods", profile.getDislikedFoods());
        plan.put("weightGoal", profile.getWeightGoal());

        // Nutritional breakdown
        Map<String, Object> nutrition = calculateNutritionalBreakdown(profile);
        plan.put("nutritionalGoals", nutrition);

        // Generate weekly meal plan
        List<Map<String, Object>> weeklyPlan = generateWeeklyMealPlan(profile);
        plan.put("weeklyPlan", weeklyPlan);

        // Shopping list
        Set<String> shoppingList = generateShoppingList(weeklyPlan);
        plan.put("shoppingList", new ArrayList<>(shoppingList));

        plan.put("generatedAt", LocalDateTime.now());
        plan.put("notes", generatePlanNotes(profile));

        return plan;
    }

    private Map<String, Object> calculateNutritionalBreakdown(DietProfile profile) {
        Map<String, Object> nutrition = new HashMap<>();

        int totalCalories = profile.getDailyCalorieGoal() != null ? profile.getDailyCalorieGoal() : 2000;

        // Calculate macros based on diet type and weight goal
        String dietType = profile.getDietType() != null ? profile.getDietType().name() : "BALANCED";
        String weightGoal = profile.getWeightGoal() != null ? profile.getWeightGoal().name() : "MAINTAIN";

        double proteinRatio, carbRatio, fatRatio;

        switch (dietType.toUpperCase()) {
            case "HIGH_PROTEIN":
                proteinRatio = 0.35;
                carbRatio = 0.35;
                fatRatio = 0.30;
                break;
            case "LOW_CARB":
                proteinRatio = 0.30;
                carbRatio = 0.20;
                fatRatio = 0.50;
                break;
            case "KETO":
                proteinRatio = 0.25;
                carbRatio = 0.05;
                fatRatio = 0.70;
                break;
            case "VEGETARIAN":
            case "VEGAN":
                proteinRatio = 0.20;
                carbRatio = 0.55;
                fatRatio = 0.25;
                break;
            default: // BALANCED
                proteinRatio = 0.25;
                carbRatio = 0.45;
                fatRatio = 0.30;
        }

        // Adjust for weight goals
        if ("LOSE".equals(weightGoal)) {
            proteinRatio += 0.05; // Increase protein for weight loss
            carbRatio -= 0.05;
        } else if ("GAIN".equals(weightGoal)) {
            carbRatio += 0.05; // Increase carbs for weight gain
            fatRatio -= 0.05;
        }

        // Calculate macro grams (protein = 4 cal/g, carbs = 4 cal/g, fat = 9 cal/g)
        int proteinGrams = (int) (totalCalories * proteinRatio / 4);
        int carbGrams = (int) (totalCalories * carbRatio / 4);
        int fatGrams = (int) (totalCalories * fatRatio / 9);

        nutrition.put("calories", totalCalories);
        nutrition.put("protein", Map.of("grams", proteinGrams, "calories", proteinGrams * 4, "percentage", (int)(proteinRatio * 100)));
        nutrition.put("carbohydrates", Map.of("grams", carbGrams, "calories", carbGrams * 4, "percentage", (int)(carbRatio * 100)));
        nutrition.put("fat", Map.of("grams", fatGrams, "calories", fatGrams * 9, "percentage", (int)(fatRatio * 100)));

        // Set goals from profile if available
        if (profile.getProteinGoalGrams() != null) {
            nutrition.put("proteinGoal", profile.getProteinGoalGrams());
        }
        if (profile.getCarbGoalGrams() != null) {
            nutrition.put("carbGoal", profile.getCarbGoalGrams());
        }
        if (profile.getFatGoalGrams() != null) {
            nutrition.put("fatGoal", profile.getFatGoalGrams());
        }
        if (profile.getFiberGoalGrams() != null) {
            nutrition.put("fiberGoal", profile.getFiberGoalGrams());
        }

        return nutrition;
    }

    private List<Map<String, Object>> generateWeeklyMealPlan(DietProfile profile) {
        List<Map<String, Object>> weeklyPlan = new ArrayList<>();
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

        for (String day : days) {
            Map<String, Object> dayPlan = new HashMap<>();
            dayPlan.put("day", day);
            dayPlan.put("meals", generateDailyMeals(profile));
            dayPlan.put("totalEstimatedCalories", profile.getDailyCalorieGoal());
            weeklyPlan.add(dayPlan);
        }

        return weeklyPlan;
    }

    private List<Map<String, Object>> generateDailyMeals(DietProfile profile) {
        List<Map<String, Object>> meals = new ArrayList<>();

        int mealsPerDay = profile.getMealsPerDay() != null ? profile.getMealsPerDay() : 3;
        String[] mealTypes = {"Breakfast", "Lunch", "Dinner", "Morning Snack", "Evening Snack"};

        for (int i = 0; i < Math.min(mealsPerDay, mealTypes.length); i++) {
            Map<String, Object> meal = new HashMap<>();
            meal.put("type", mealTypes[i]);
            meal.put("foods", generateMealFoods(profile, mealTypes[i]));
            meal.put("estimatedCalories", calculateMealCalories(profile, i, mealsPerDay));
            meal.put("prepTime", "15-30 minutes");
            meals.add(meal);
        }

        return meals;
    }

    private List<Map<String, Object>> generateMealFoods(DietProfile profile, String mealType) {
        List<Map<String, Object>> foods = new ArrayList<>();

        String dietType = profile.getDietType() != null ? profile.getDietType().name() : "BALANCED";
        String[] restrictionsArray = profile.getDietaryRestrictions();
        String[] dislikedArray = profile.getDislikedFoods();
        List<String> restrictions = restrictionsArray != null ? Arrays.asList(restrictionsArray) : new ArrayList<>();
        List<String> disliked = dislikedArray != null ? Arrays.asList(dislikedArray) : new ArrayList<>();

        switch (mealType) {
            case "Breakfast":
                if ("VEGAN".equals(dietType)) {
                    foods.add(createFood("Overnight oats with chia seeds", "1 cup", 350));
                    foods.add(createFood("Mixed berries", "1/2 cup", 40));
                    foods.add(createFood("Almond milk", "1/2 cup", 20));
                } else if ("VEGETARIAN".equals(dietType)) {
                    foods.add(createFood("Greek yogurt", "1 cup", 150));
                    foods.add(createFood("Granola", "1/4 cup", 120));
                    foods.add(createFood("Fresh banana", "1 medium", 80));
                } else if ("KETO".equals(dietType)) {
                    foods.add(createFood("Scrambled eggs", "3 eggs", 210));
                    foods.add(createFood("Avocado", "1/2 medium", 120));
                    foods.add(createFood("Bacon", "2 strips", 90));
                } else {
                    foods.add(createFood("Scrambled eggs", "2 eggs", 140));
                    foods.add(createFood("Whole grain toast", "2 slices", 160));
                    foods.add(createFood("Orange", "1 medium", 60));
                }
                break;

            case "Lunch":
                if ("VEGAN".equals(dietType)) {
                    foods.add(createFood("Quinoa bowl", "1 cup", 220));
                    foods.add(createFood("Roasted vegetables", "1 cup", 100));
                    foods.add(createFood("Tahini dressing", "2 tbsp", 120));
                } else if ("KETO".equals(dietType)) {
                    foods.add(createFood("Grilled chicken salad", "6 oz", 300));
                    foods.add(createFood("Mixed greens", "2 cups", 20));
                    foods.add(createFood("Olive oil dressing", "2 tbsp", 240));
                } else {
                    foods.add(createFood("Grilled chicken breast", "4 oz", 180));
                    foods.add(createFood("Brown rice", "1/2 cup", 110));
                    foods.add(createFood("Steamed broccoli", "1 cup", 30));
                }
                break;

            case "Dinner":
                if ("VEGAN".equals(dietType)) {
                    foods.add(createFood("Lentil curry", "1 cup", 200));
                    foods.add(createFood("Brown rice", "1/2 cup", 110));
                    foods.add(createFood("Naan bread", "1 small", 160));
                } else if ("KETO".equals(dietType)) {
                    foods.add(createFood("Baked salmon", "6 oz", 350));
                    foods.add(createFood("Asparagus", "1 cup", 25));
                    foods.add(createFood("Butter", "1 tbsp", 100));
                } else {
                    foods.add(createFood("Baked salmon", "4 oz", 230));
                    foods.add(createFood("Sweet potato", "1 medium", 100));
                    foods.add(createFood("Green beans", "1 cup", 35));
                }
                break;

            default: // Snacks
                if ("VEGAN".equals(dietType)) {
                    foods.add(createFood("Apple with almond butter", "1 medium + 1 tbsp", 190));
                } else if ("KETO".equals(dietType)) {
                    foods.add(createFood("Cheese and olives", "1 oz + 5 olives", 140));
                } else {
                    foods.add(createFood("Greek yogurt", "1/2 cup", 75));
                    foods.add(createFood("Mixed nuts", "1/4 cup", 170));
                }
        }

        return foods;
    }

    private Map<String, Object> createFood(String name, String serving, int calories) {
        Map<String, Object> food = new HashMap<>();
        food.put("name", name);
        food.put("serving", serving);
        food.put("calories", calories);
        return food;
    }

    private int calculateMealCalories(DietProfile profile, int mealIndex, int totalMeals) {
        int totalCalories = profile.getDailyCalorieGoal() != null ? profile.getDailyCalorieGoal() : 2000;

        // Standard distribution
        if (totalMeals <= 3) {
            double[] distribution = {0.25, 0.40, 0.35}; // breakfast, lunch, dinner
            return (int) (totalCalories * distribution[mealIndex]);
        } else {
            // With snacks
            double[] distribution = {0.25, 0.35, 0.30, 0.05, 0.05}; // breakfast, lunch, dinner, snack1, snack2
            if (mealIndex < distribution.length) {
                return (int) (totalCalories * distribution[mealIndex]);
            }
        }

        return totalCalories / totalMeals;
    }

    private Set<String> generateShoppingList(List<Map<String, Object>> weeklyPlan) {
        Set<String> shoppingList = new HashSet<>();

        for (Map<String, Object> day : weeklyPlan) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> meals = (List<Map<String, Object>>) day.get("meals");

            for (Map<String, Object> meal : meals) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> foods = (List<Map<String, Object>>) meal.get("foods");

                for (Map<String, Object> food : foods) {
                    String foodName = (String) food.get("name");
                    // Extract base ingredient from food description
                    String ingredient = extractIngredient(foodName);
                    shoppingList.add(ingredient);
                }
            }
        }

        return shoppingList;
    }

    private String extractIngredient(String foodName) {
        // Simple extraction - in a real app this would be more sophisticated
        if (foodName.contains("eggs")) return "Eggs";
        if (foodName.contains("chicken")) return "Chicken breast";
        if (foodName.contains("salmon")) return "Salmon";
        if (foodName.contains("quinoa")) return "Quinoa";
        if (foodName.contains("oats")) return "Rolled oats";
        if (foodName.contains("yogurt")) return "Greek yogurt";
        if (foodName.contains("avocado")) return "Avocado";
        if (foodName.contains("berries")) return "Mixed berries";
        if (foodName.contains("broccoli")) return "Broccoli";
        if (foodName.contains("rice")) return "Brown rice";

        return foodName.split(" ")[0]; // Take first word as ingredient
    }

    private List<String> generatePlanNotes(DietProfile profile) {
        List<String> notes = new ArrayList<>();

        notes.add("Drink plenty of water throughout the day (8-10 glasses)");
        notes.add("Eat slowly and mindfully, paying attention to hunger cues");

        String dietType = profile.getDietType() != null ? profile.getDietType().name() : "BALANCED";
        if ("KETO".equals(dietType)) {
            notes.add("Track your carb intake to stay under 20-50g daily");
            notes.add("Include MCT oil or coconut oil for quick energy");
        } else if ("HIGH_PROTEIN".equals(dietType)) {
            notes.add("Spread protein intake evenly throughout the day");
            notes.add("Include protein with every meal and snack");
        } else if ("VEGAN".equals(dietType) || "VEGETARIAN".equals(dietType)) {
            notes.add("Ensure adequate B12, iron, and omega-3 intake");
            notes.add("Combine different plant proteins for complete amino acid profiles");
        }

        if (profile.getWeightGoal() != null) {
            if ("LOSE".equals(profile.getWeightGoal())) {
                notes.add("Create a moderate calorie deficit through diet and exercise");
                notes.add("Focus on whole, unprocessed foods for satiety");
            } else if ("GAIN".equals(profile.getWeightGoal())) {
                notes.add("Eat regular meals and don't skip eating opportunities");
                notes.add("Include healthy fats and complex carbs for sustained energy");
            }
        }

        notes.add("Meal prep on weekends to stay consistent during busy weekdays");

        return notes;
    }
}