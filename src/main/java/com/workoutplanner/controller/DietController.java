package com.workoutplanner.controller;

import com.workoutplanner.model.DietProfile;
import com.workoutplanner.model.User;
import com.workoutplanner.repository.DietProfileRepository;
import com.workoutplanner.repository.UserRepository;
import com.workoutplanner.service.StorageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/diet")
@CrossOrigin(origins = "*")
public class DietController {

    private final DietProfileRepository dietProfileRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    @Autowired
    public DietController(DietProfileRepository dietProfileRepository,
                         UserRepository userRepository,
                         StorageService storageService) {
        this.dietProfileRepository = dietProfileRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    @GetMapping("/profile")
    public ResponseEntity<DietProfile> getDietProfile() {
        String userId = getCurrentUserId();

        Optional<DietProfile> profile = dietProfileRepository.findByUserId(userId);

        if (profile.isPresent()) {
            return ResponseEntity.ok(profile.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/profile")
    public ResponseEntity<DietProfile> createOrUpdateDietProfile(@Valid @RequestBody DietProfile profile) {
        String userId = getCurrentUserId();

        // Set user ID
        profile.setUserId(userId);

        // Check if profile already exists
        Optional<DietProfile> existingProfile = dietProfileRepository.findByUserId(userId);
        if (existingProfile.isPresent()) {
            profile.setId(existingProfile.get().getId());
            profile.setCreatedAt(existingProfile.get().getCreatedAt());
        } else {
            // Generate ID for new profile
            profile.setId(UUID.randomUUID().toString());
            profile.setCreatedAt(LocalDateTime.now());
        }

        profile.setUpdatedAt(LocalDateTime.now());

        DietProfile savedProfile = dietProfileRepository.save(profile);

        // Update user's profile reference
        userRepository.findById(userId).ifPresent(user -> {
            user.setDietProfileId(savedProfile.getId());
            userRepository.save(user);
        });

        return ResponseEntity.ok(savedProfile);
    }

    @PostMapping("/plan/generate")
    public Mono<ResponseEntity<Map<String, Object>>> generateDietPlan() {
        String userId = getCurrentUserId();

        return Mono.fromCallable(() -> {
            // Get user and diet profile
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            DietProfile dietProfile = dietProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Diet profile not found"));

            // Generate a simple diet plan (placeholder implementation)
            Map<String, Object> dietPlan = generateSimpleDietPlan(dietProfile);

            // Store the plan in object storage
            String storageKey = "diet-plans/" + userId + "/" + UUID.randomUUID() + ".json";
            String planTitle = "Diet Plan - " + LocalDateTime.now().toLocalDate();

            try {
                String actualStorageKey = storageService.storeDietPlan(userId, planTitle, dietPlan);

                // Update diet profile with current plan info
                dietProfile.setCurrentPlanStorageKey(actualStorageKey);
                dietProfile.setCurrentPlanTitle(planTitle);
                dietProfile.setCurrentPlanCreatedAt(LocalDateTime.now());
                dietProfile.setUpdatedAt(LocalDateTime.now());
                dietProfileRepository.save(dietProfile);

                Map<String, Object> response = new HashMap<>();
                response.put("message", "Diet plan generated successfully");
                response.put("planTitle", planTitle);
                response.put("storageKey", actualStorageKey);
                response.put("plan", dietPlan);

                return ResponseEntity.ok(response);

            } catch (Exception e) {
                throw new RuntimeException("Failed to store diet plan: " + e.getMessage());
            }
        });
    }

    @GetMapping("/plan/current")
    public Mono<ResponseEntity<Map<String, Object>>> getCurrentDietPlan() {
        String userId = getCurrentUserId();

        return Mono.fromCallable(() -> {
            DietProfile profile = dietProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Diet profile not found"));

            if (profile.getCurrentPlanStorageKey() == null) {
                return ResponseEntity.notFound().build();
            }

            try {
                Map<String, Object> plan = storageService.retrieveDietPlan(userId, profile.getCurrentPlanStorageKey());

                Map<String, Object> response = new HashMap<>();
                response.put("planTitle", profile.getCurrentPlanTitle());
                response.put("createdAt", profile.getCurrentPlanCreatedAt());
                response.put("storageKey", profile.getCurrentPlanStorageKey());
                response.put("plan", plan);

                return ResponseEntity.ok(response);

            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve diet plan: " + e.getMessage());
            }
        });
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDietStats() {
        String userId = getCurrentUserId();

        Optional<DietProfile> profileOpt = dietProfileRepository.findByUserId(userId);

        if (profileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        DietProfile profile = profileOpt.get();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMealsLogged", profile.getTotalMealsLogged());
        stats.put("lastMealLogged", profile.getLastMealLogged());
        stats.put("dailyCalorieGoal", profile.getDailyCalorieGoal());
        stats.put("mealsPerDay", profile.getMealsPerDay());
        stats.put("dietType", profile.getDietType());
        stats.put("hasCurrentPlan", profile.getCurrentPlanStorageKey() != null);

        return ResponseEntity.ok(stats);
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return ((User) authentication.getPrincipal()).getId();
        }
        throw new RuntimeException("User not authenticated");
    }

    private Map<String, Object> generateSimpleDietPlan(DietProfile profile) {
        Map<String, Object> plan = new HashMap<>();

        plan.put("title", "Personalized Diet Plan");
        plan.put("dietType", profile.getDietType() != null ? profile.getDietType().name() : null);
        plan.put("dailyCalories", profile.getDailyCalorieGoal());
        plan.put("mealsPerDay", profile.getMealsPerDay());
        plan.put("restrictions", profile.getDietaryRestrictions());
        plan.put("preferredCuisines", profile.getPreferredCuisines());

        // Generate sample meal plan for a week
        List<Map<String, Object>> weeklyPlan = new ArrayList<>();

        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

        for (String day : days) {
            Map<String, Object> dayPlan = new HashMap<>();
            dayPlan.put("day", day);
            dayPlan.put("meals", generateDailyMeals(profile));
            weeklyPlan.add(dayPlan);
        }

        plan.put("weeklyPlan", weeklyPlan);
        plan.put("generatedAt", LocalDateTime.now());

        return plan;
    }

    private List<Map<String, Object>> generateDailyMeals(DietProfile profile) {
        List<Map<String, Object>> meals = new ArrayList<>();

        int mealsPerDay = profile.getMealsPerDay() != null ? profile.getMealsPerDay() : 3;
        String[] mealTypes = {"Breakfast", "Lunch", "Dinner", "Snack 1", "Snack 2"};

        for (int i = 0; i < Math.min(mealsPerDay, mealTypes.length); i++) {
            Map<String, Object> meal = new HashMap<>();
            meal.put("type", mealTypes[i]);
            meal.put("foods", generateSampleFoods(profile, mealTypes[i]));
            meal.put("estimatedCalories", calculateMealCalories(profile, i, mealsPerDay));
            meals.add(meal);
        }

        return meals;
    }

    private List<String> generateSampleFoods(DietProfile profile, String mealType) {
        List<String> foods = new ArrayList<>();

        String dietType = profile.getDietType() != null ? profile.getDietType().name() : "BALANCED";

        switch (mealType) {
            case "Breakfast":
                if ("VEGETARIAN".equals(dietType) || "VEGAN".equals(dietType)) {
                    foods.add("Oatmeal with berries");
                    foods.add("Almond milk");
                } else {
                    foods.add("Scrambled eggs");
                    foods.add("Whole grain toast");
                }
                break;
            case "Lunch":
                if ("VEGETARIAN".equals(dietType)) {
                    foods.add("Quinoa salad");
                    foods.add("Mixed vegetables");
                } else if ("VEGAN".equals(dietType)) {
                    foods.add("Lentil soup");
                    foods.add("Green salad");
                } else {
                    foods.add("Grilled chicken");
                    foods.add("Brown rice");
                    foods.add("Steamed broccoli");
                }
                break;
            case "Dinner":
                if ("VEGAN".equals(dietType)) {
                    foods.add("Tofu stir-fry");
                    foods.add("Quinoa");
                } else {
                    foods.add("Baked salmon");
                    foods.add("Sweet potato");
                    foods.add("Asparagus");
                }
                break;
            default: // Snacks
                foods.add("Apple with nuts");
                foods.add("Greek yogurt");
        }

        return foods;
    }

    private int calculateMealCalories(DietProfile profile, int mealIndex, int totalMeals) {
        int totalCalories = profile.getDailyCalorieGoal() != null ? profile.getDailyCalorieGoal() : 2000;

        // Distribute calories: breakfast 25%, lunch 35%, dinner 30%, snacks 10%
        double[] distribution = {0.25, 0.35, 0.30, 0.05, 0.05};

        if (mealIndex < distribution.length) {
            return (int) (totalCalories * distribution[mealIndex]);
        }

        return totalCalories / totalMeals;
    }
}