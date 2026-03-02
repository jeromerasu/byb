package com.workoutplanner.controller;

import com.workoutplanner.dto.DietPlanResponseDto;
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

        profile.setUserId(userId);

        Optional<DietProfile> existingProfile = dietProfileRepository.findByUserId(userId);
        if (existingProfile.isPresent()) {
            profile.setId(existingProfile.get().getId());
            profile.setCreatedAt(existingProfile.get().getCreatedAt());
        } else {
            profile.setId(UUID.randomUUID().toString());
            profile.setCreatedAt(LocalDateTime.now());
        }

        profile.setUpdatedAt(LocalDateTime.now());

        DietProfile savedProfile = dietProfileRepository.save(profile);

        userRepository.findById(userId).ifPresent(user -> {
            user.setDietProfileId(savedProfile.getId());
            userRepository.save(user);
        });

        return ResponseEntity.ok(savedProfile);
    }

    @PostMapping("/plan/generate")
    public Mono<ResponseEntity<DietPlanResponseDto>> generateDietPlan() {
        String userId = getCurrentUserId();

        return Mono.fromCallable(() -> {
            userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            DietProfile dietProfile = dietProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Diet profile not found"));

            Map<String, Object> dietPlan = generateSimpleDietPlan(dietProfile);

            String planTitle = "Diet Plan - " + LocalDateTime.now().toLocalDate();

            try {
                String actualStorageKey = storageService.storeDietPlan(userId, planTitle, dietPlan);

                dietProfile.setCurrentPlanStorageKey(actualStorageKey);
                dietProfile.setCurrentPlanTitle(planTitle);
                dietProfile.setCurrentPlanCreatedAt(LocalDateTime.now());
                dietProfile.setUpdatedAt(LocalDateTime.now());
                dietProfileRepository.save(dietProfile);

                DietPlanResponseDto response = toDietResponse(
                        dietPlan,
                        planTitle,
                        actualStorageKey,
                        dietProfile.getCurrentPlanCreatedAt(),
                        "Diet plan generated successfully"
                );

                return ResponseEntity.ok(response);

            } catch (Exception e) {
                throw new RuntimeException("Failed to store diet plan: " + e.getMessage());
            }
        });
    }

    @GetMapping("/plan/current")
    public Mono<ResponseEntity<DietPlanResponseDto>> getCurrentDietPlan() {
        String userId = getCurrentUserId();

        return Mono.fromCallable(() -> {
            DietProfile profile = dietProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Diet profile not found"));

            if (profile.getCurrentPlanStorageKey() == null) {
                return ResponseEntity.notFound().build();
            }

            try {
                Map<String, Object> plan = storageService.retrieveDietPlan(userId, profile.getCurrentPlanStorageKey());

                DietPlanResponseDto response = toDietResponse(
                        plan,
                        profile.getCurrentPlanTitle(),
                        profile.getCurrentPlanStorageKey(),
                        profile.getCurrentPlanCreatedAt(),
                        "Current diet plan retrieved"
                );

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

    private DietPlanResponseDto toDietResponse(Map<String, Object> rawPlan,
                                               String planTitle,
                                               String storageKey,
                                               LocalDateTime createdAt,
                                               String message) {
        Map<String, Object> normalizedPlan = normalizeDietPlan(rawPlan);

        DietPlanResponseDto dto = new DietPlanResponseDto();
        dto.setMessage(message);
        dto.setPlanTitle(planTitle);
        dto.setStorageKey(storageKey);
        dto.setCreatedAt(createdAt);

        dto.setTitle(asString(normalizedPlan.get("title"), "Personalized Diet Plan"));
        dto.setPhaseLabel(asString(normalizedPlan.get("phaseLabel"), "Nutrition Base"));
        dto.setCalories(asPositiveInt(firstNonNull(normalizedPlan.get("calories"), normalizedPlan.get("dailyCalories")), 2000));
        dto.setProteinG(asPositiveInt(normalizedPlan.get("proteinG"), calculateDefaultMacro(dto.getCalories(), 0.25, 4)));
        dto.setCarbsG(asPositiveInt(normalizedPlan.get("carbsG"), calculateDefaultMacro(dto.getCalories(), 0.45, 4)));
        dto.setFatsG(asPositiveInt(normalizedPlan.get("fatsG"), calculateDefaultMacro(dto.getCalories(), 0.30, 9)));
        dto.setMealsPerDay(asPositiveInt(normalizedPlan.get("mealsPerDay"), 3));
        dto.setDietType(asString(normalizedPlan.get("dietType"), "BALANCED"));

        dto.setSummary(buildDietSummary(normalizedPlan, dto));
        dto.setPlan(normalizedPlan);
        return dto;
    }

    private Map<String, Object> normalizeDietPlan(Map<String, Object> rawPlan) {
        Map<String, Object> plan = rawPlan != null ? new HashMap<>(rawPlan) : new HashMap<>();

        String title = asString(plan.get("title"), "Personalized Diet Plan");
        String phaseLabel = asString(firstNonNull(plan.get("phaseLabel"), plan.get("weightGoal")), "Nutrition Base");
        Integer calories = asPositiveInt(firstNonNull(plan.get("calories"), plan.get("dailyCalories")), 2000);
        Integer mealsPerDay = asPositiveInt(plan.get("mealsPerDay"), 3);
        String dietType = asString(plan.get("dietType"), "BALANCED");

        // Lightweight fallback for unstructured/AI payloads
        if (!(plan.get("weeklyPlan") instanceof List<?>)) {
            plan.put("weeklyPlan", List.of(
                    Map.of("day", "Monday", "meals", List.of(
                            Map.of("type", "Breakfast", "foods", List.of("Oatmeal", "Berries"), "estimatedCalories", 500),
                            Map.of("type", "Lunch", "foods", List.of("Chicken Salad", "Quinoa"), "estimatedCalories", 700),
                            Map.of("type", "Dinner", "foods", List.of("Salmon", "Vegetables"), "estimatedCalories", 800)
                    ))
            ));
        }

        plan.put("title", title);
        plan.put("phaseLabel", phaseLabel);
        plan.put("calories", calories);
        plan.put("dailyCalories", calories);
        plan.put("proteinG", asPositiveInt(plan.get("proteinG"), calculateDefaultMacro(calories, 0.25, 4)));
        plan.put("carbsG", asPositiveInt(plan.get("carbsG"), calculateDefaultMacro(calories, 0.45, 4)));
        plan.put("fatsG", asPositiveInt(plan.get("fatsG"), calculateDefaultMacro(calories, 0.30, 9)));
        plan.put("mealsPerDay", mealsPerDay);
        plan.put("dietType", dietType);

        return plan;
    }

    private Map<String, Object> buildDietSummary(Map<String, Object> plan, DietPlanResponseDto dto) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("calories", dto.getCalories());
        summary.put("mealsPerDay", dto.getMealsPerDay());
        summary.put("dietType", dto.getDietType());
        summary.put("restrictions", plan.getOrDefault("restrictions", List.of()));
        summary.put("preferredCuisines", plan.getOrDefault("preferredCuisines", List.of()));
        summary.put("shoppingListCount", extractShoppingListCount(plan));
        return summary;
    }

    private int extractShoppingListCount(Map<String, Object> plan) {
        Object shoppingList = plan.get("shoppingList");
        if (shoppingList instanceof Collection<?> collection) {
            return collection.size();
        }
        return 0;
    }

    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) return value;
        }
        return null;
    }

    private String asString(Object value, String fallback) {
        if (value == null) return fallback;
        String str = String.valueOf(value).trim();
        return str.isEmpty() ? fallback : str;
    }

    private Integer asPositiveInt(Object value, int fallback) {
        if (value == null) return fallback;
        try {
            int parsed = (value instanceof Number n) ? n.intValue() : Integer.parseInt(String.valueOf(value));
            return parsed > 0 ? parsed : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private Map<String, Object> generateSimpleDietPlan(DietProfile profile) {
        Map<String, Object> plan = new HashMap<>();

        plan.put("title", "Personalized Diet Plan");
        plan.put("dietType", profile.getDietType() != null ? profile.getDietType().name() : null);
        plan.put("dailyCalories", profile.getDailyCalorieGoal());
        plan.put("mealsPerDay", profile.getMealsPerDay());
        plan.put("restrictions", profile.getDietaryRestrictions());
        plan.put("preferredCuisines", profile.getPreferredCuisines());

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
            default:
                foods.add("Apple with nuts");
                foods.add("Greek yogurt");
        }

        return foods;
    }

    private int calculateMealCalories(DietProfile profile, int mealIndex, int totalMeals) {
        int totalCalories = profile.getDailyCalorieGoal() != null ? profile.getDailyCalorieGoal() : 2000;

        double[] distribution = {0.25, 0.35, 0.30, 0.05, 0.05};

        if (mealIndex < distribution.length) {
            return (int) (totalCalories * distribution[mealIndex]);
        }

        return totalCalories / totalMeals;
    }

    private Integer calculateDefaultMacro(Integer totalCalories, double percentage, int caloriesPerGram) {
        if (totalCalories == null) totalCalories = 2000;
        return (int) ((totalCalories * percentage) / caloriesPerGram);
    }
}
