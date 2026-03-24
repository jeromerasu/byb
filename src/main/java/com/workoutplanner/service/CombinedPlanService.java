package com.workoutplanner.service;

import com.workoutplanner.dto.CombinedPlanResponseDto;
import com.workoutplanner.dto.DietPlanResponseDto;
import com.workoutplanner.dto.WorkoutPlanResponseDto;
import com.workoutplanner.model.DietProfile;
import com.workoutplanner.model.WorkoutProfile;
import com.workoutplanner.model.User;
import com.workoutplanner.repository.DietProfileRepository;
import com.workoutplanner.repository.WorkoutProfileRepository;
import com.workoutplanner.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class CombinedPlanService {

    private static final Logger logger = LoggerFactory.getLogger(CombinedPlanService.class);

    private final WorkoutProfileRepository workoutProfileRepository;
    private final DietProfileRepository dietProfileRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final OpenAIService openAIService;

    @Value("${beta.mode:false}")
    private boolean betaMode;

    @Autowired
    public CombinedPlanService(WorkoutProfileRepository workoutProfileRepository,
                              DietProfileRepository dietProfileRepository,
                              UserRepository userRepository,
                              StorageService storageService,
                              OpenAIService openAIService) {
        this.workoutProfileRepository = workoutProfileRepository;
        this.dietProfileRepository = dietProfileRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.openAIService = openAIService;
    }

    public CombinedPlanResponseDto generateCombinedPlan(String userId) {
        try {
            logger.debug("generateCombinedPlan called with userId: {}", userId);

            // Validate user exists first
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                throw new RuntimeException("User not found with ID: " + userId);
            }
            logger.info("User found: {}", userOpt.get().getUsername());

            // Get or validate profiles exist
            Optional<WorkoutProfile> workoutProfileOpt = workoutProfileRepository.findByUserId(userId);
            logger.debug("Workout profile search result: {}", workoutProfileOpt.isPresent() ? "FOUND" : "NOT_FOUND");

            WorkoutProfile workoutProfile = workoutProfileOpt
                    .orElseThrow(() -> new RuntimeException("Workout profile not found"));

            Optional<DietProfile> dietProfileOpt = dietProfileRepository.findByUserId(userId);
            logger.debug("Diet profile search result: {}", dietProfileOpt.isPresent() ? "FOUND" : "NOT_FOUND");

            DietProfile dietProfile = dietProfileOpt
                    .orElseThrow(() -> new RuntimeException("Diet profile not found"));

            LocalDateTime now = LocalDateTime.now();

            // Use environment-specific bucket names
            String workoutBucketName = betaMode ? "workoutbeta" : "workout";
            String dietBucketName = betaMode ? "dietbeta" : "diet";

            // Use OpenAI to generate both plans in a single API call
            OpenAIService.CombinedPlanResult openAIResult = openAIService.generateCombinedPlans(workoutProfile, dietProfile);

            // Store both plans using dynamic bucket names
            WorkoutPlanResult workoutResult = storeWorkoutPlan(workoutBucketName, userId, workoutProfile, openAIResult.getWorkoutPlan(), now);
            DietPlanResult dietResult = storeDietPlan(dietBucketName, userId, dietProfile, openAIResult.getDietPlan(), now);

            // Create combined response
            CombinedPlanResponseDto response = new CombinedPlanResponseDto();
            response.setMessage("AI-generated combined workout and diet plan created successfully");

            // Build plan metadata
            CombinedPlanResponseDto.PlanMetaDto planMeta = new CombinedPlanResponseDto.PlanMetaDto(
                    "30 days", // AI generates 30-day plans
                    now,
                    "2.0", // Version 2.0 with OpenAI integration
                    userId,
                    workoutResult.storageKey,
                    dietResult.storageKey
            );
            response.setPlanMeta(planMeta);

            // Set individual plan responses
            response.setWorkout(workoutResult.response);
            response.setDiet(dietResult.response);

            return response;

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate combined plan: " + e.getMessage(), e);
        }
    }

    // Removed old generateBucketName method - now using separate "workout" and "diet" buckets

    private WorkoutPlanResult storeWorkoutPlan(String bucketName, String userId, WorkoutProfile profile,
                                                Map<String, Object> workoutPlan, LocalDateTime timestamp) {
        String planTitle = "AI Workout Plan - " + timestamp.toLocalDate();

        try {
            String actualStorageKey = storageService.storeWorkoutPlan(bucketName, userId, planTitle, workoutPlan);

            // Update profile with new plan info
            profile.setCurrentPlanStorageKey(actualStorageKey);
            profile.setCurrentPlanTitle(planTitle);
            profile.setCurrentPlanCreatedAt(timestamp);
            profile.setUpdatedAt(timestamp);
            workoutProfileRepository.save(profile);

            // Create response DTO
            WorkoutPlanResponseDto response = toWorkoutResponse(
                    workoutPlan,
                    planTitle,
                    actualStorageKey,
                    timestamp,
                    "AI-generated workout plan stored in bucket: " + bucketName
            );

            return new WorkoutPlanResult(response, actualStorageKey);

        } catch (Exception e) {
            throw new RuntimeException("Failed to store workout plan: " + e.getMessage(), e);
        }
    }

    private DietPlanResult storeDietPlan(String bucketName, String userId, DietProfile profile,
                                         Map<String, Object> dietPlan, LocalDateTime timestamp) {
        String planTitle = "AI Diet Plan - " + timestamp.toLocalDate();

        try {
            String actualStorageKey = storageService.storeDietPlan(bucketName, userId, planTitle, dietPlan);

            // Update profile with new plan info
            profile.setCurrentPlanStorageKey(actualStorageKey);
            profile.setCurrentPlanTitle(planTitle);
            profile.setCurrentPlanCreatedAt(timestamp);
            profile.setUpdatedAt(timestamp);
            dietProfileRepository.save(profile);

            // Create response DTO
            DietPlanResponseDto response = toDietResponse(
                    dietPlan,
                    planTitle,
                    actualStorageKey,
                    timestamp,
                    "AI-generated diet plan stored in bucket: " + bucketName
            );

            return new DietPlanResult(response, actualStorageKey);

        } catch (Exception e) {
            throw new RuntimeException("Failed to store diet plan: " + e.getMessage(), e);
        }
    }

    // Note: Removed old generateWorkoutPlan and generateDietPlan methods
    // as we now use OpenAI for plan generation

    // Helper classes for internal results
    private static class WorkoutPlanResult {
        final WorkoutPlanResponseDto response;
        final String storageKey;

        WorkoutPlanResult(WorkoutPlanResponseDto response, String storageKey) {
            this.response = response;
            this.storageKey = storageKey;
        }
    }

    private static class DietPlanResult {
        final DietPlanResponseDto response;
        final String storageKey;

        DietPlanResult(DietPlanResponseDto response, String storageKey) {
            this.response = response;
            this.storageKey = storageKey;
        }
    }

    // Workout plan generation methods (copied from WorkoutController)
    private Map<String, Object> generateSimpleWorkoutPlan(WorkoutProfile profile) {
        Map<String, Object> plan = new HashMap<>();

        plan.put("title", "Personalized Workout Plan");
        plan.put("fitnessLevel", profile.getFitnessLevel() != null ? profile.getFitnessLevel().name() : null);
        plan.put("frequency", profile.getWorkoutFrequency());
        plan.put("duration", profile.getSessionDuration());
        plan.put("targetGoals", profile.getTargetGoals());
        plan.put("equipment", profile.getAvailableEquipment());

        List<Map<String, Object>> workoutDays = new ArrayList<>();

        for (int i = 1; i <= (profile.getWorkoutFrequency() != null ? profile.getWorkoutFrequency() : 3); i++) {
            Map<String, Object> day = new HashMap<>();
            day.put("day", i);
            day.put("focus", getDayFocus(i));
            day.put("exercises", generateSampleExercises(profile));
            day.put("duration", profile.getSessionDuration() != null ? profile.getSessionDuration() : 45);
            workoutDays.add(day);
        }

        plan.put("workoutDays", workoutDays);
        plan.put("generatedAt", LocalDateTime.now());

        return plan;
    }

    private String getDayFocus(int day) {
        String[] focuses = {"Upper Body", "Lower Body", "Cardio", "Full Body", "Core & Flexibility"};
        return focuses[(day - 1) % focuses.length];
    }

    private List<Map<String, Object>> generateSampleExercises(WorkoutProfile profile) {
        List<Map<String, Object>> exercises = new ArrayList<>();

        String fitnessLevel = profile.getFitnessLevel() != null ? profile.getFitnessLevel().name() : "BEGINNER";

        switch (fitnessLevel.toUpperCase()) {
            case "BEGINNER":
                exercises.add(createExercise("Push-ups", "3 sets of 8-12 reps"));
                exercises.add(createExercise("Bodyweight Squats", "3 sets of 10-15 reps"));
                exercises.add(createExercise("Plank", "3 sets of 30 seconds"));
                break;
            case "INTERMEDIATE":
                exercises.add(createExercise("Push-ups", "3 sets of 12-18 reps"));
                exercises.add(createExercise("Jump Squats", "3 sets of 10-15 reps"));
                exercises.add(createExercise("Mountain Climbers", "3 sets of 30 seconds"));
                break;
            default:
                exercises.add(createExercise("Burpees", "3 sets of 8-12 reps"));
                exercises.add(createExercise("Single-leg Squats", "3 sets of 6-10 reps each leg"));
                exercises.add(createExercise("Plank to Push-up", "3 sets of 8-12 reps"));
        }

        return exercises;
    }

    private Map<String, Object> createExercise(String name, String sets) {
        Map<String, Object> exercise = new HashMap<>();
        exercise.put("name", name);
        exercise.put("sets", sets);
        exercise.put("type", "strength");
        return exercise;
    }

    // Diet plan generation methods (copied from DietController)
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

    // Response mapping methods (adapted from controllers)
    private WorkoutPlanResponseDto toWorkoutResponse(Map<String, Object> rawPlan,
                                                     String planTitle,
                                                     String storageKey,
                                                     LocalDateTime createdAt,
                                                     String message) {
        Map<String, Object> normalizedPlan = normalizeWorkoutPlan(rawPlan);

        WorkoutPlanResponseDto dto = new WorkoutPlanResponseDto();
        dto.setMessage(message);
        dto.setPlanTitle(planTitle);
        dto.setStorageKey(storageKey);
        dto.setCreatedAt(createdAt);

        dto.setTitle(asString(normalizedPlan.get("title"), "Personalized Workout Plan"));
        dto.setPhaseLabel(asString(normalizedPlan.get("phaseLabel"), "Base Phase"));
        dto.setDurationMin(asPositiveInt(normalizedPlan.get("durationMin"), 45));
        dto.setCalories(asPositiveInt(normalizedPlan.get("calories"), 300));
        dto.setExercises(extractExercises(normalizedPlan));

        dto.setPlan(normalizedPlan);
        return dto;
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

    // Utility methods for normalization (simplified versions from controllers)
    private Map<String, Object> normalizeWorkoutPlan(Map<String, Object> rawPlan) {
        Map<String, Object> plan = rawPlan != null ? new HashMap<>(rawPlan) : new HashMap<>();

        String title = asString(plan.get("title"), "Personalized Workout Plan");
        Integer durationMin = asPositiveInt(firstNonNull(plan.get("durationMin"), plan.get("duration")), 45);
        String phaseLabel = asString(firstNonNull(plan.get("phaseLabel"), plan.get("fitnessLevel")), "Base Phase");

        List<WorkoutPlanResponseDto.ExerciseDto> exercises = extractExercises(plan);

        if (exercises.isEmpty()) {
            exercises = List.of(
                    new WorkoutPlanResponseDto.ExerciseDto("Push-ups", "3 sets of 10 reps", "chest"),
                    new WorkoutPlanResponseDto.ExerciseDto("Bodyweight Squats", "3 sets of 12 reps", "legs"),
                    new WorkoutPlanResponseDto.ExerciseDto("Plank", "3 x 30 sec", "core")
            );
        }

        plan.put("title", title);
        plan.put("phaseLabel", phaseLabel);
        plan.put("durationMin", durationMin);
        plan.put("calories", Math.max(150, exercises.size() * 100));

        List<Map<String, Object>> normalizedExercises = new ArrayList<>();
        for (WorkoutPlanResponseDto.ExerciseDto ex : exercises) {
            normalizedExercises.add(Map.of(
                    "name", ex.getName(),
                    "prescription", ex.getPrescription(),
                    "muscle", ex.getMuscle()
            ));
        }
        plan.put("exercises", normalizedExercises);

        return plan;
    }

    private List<WorkoutPlanResponseDto.ExerciseDto> extractExercises(Map<String, Object> plan) {
        List<WorkoutPlanResponseDto.ExerciseDto> exercises = new ArrayList<>();

        Object directExercises = plan.get("exercises");
        if (directExercises instanceof List<?> directList) {
            for (Object item : directList) {
                if (item instanceof Map<?, ?> exerciseMap) {
                    exercises.add(mapExercise(exerciseMap));
                }
            }
        }

        if (exercises.isEmpty() && plan.get("workoutDays") instanceof List<?> days) {
            for (Object dayObj : days) {
                if (dayObj instanceof Map<?, ?> dayMap && dayMap.get("exercises") instanceof List<?> dayExercises) {
                    for (Object exObj : dayExercises) {
                        if (exObj instanceof Map<?, ?> exerciseMap) {
                            exercises.add(mapExercise(exerciseMap));
                        }
                    }
                }
            }
        }

        return exercises;
    }

    private WorkoutPlanResponseDto.ExerciseDto mapExercise(Map<?, ?> exerciseMap) {
        String name = asString(firstNonNull(exerciseMap.get("name"), exerciseMap.get("exercise")), "Unknown Exercise");
        String prescription = asString(
                firstNonNull(exerciseMap.get("prescription"), exerciseMap.get("sets"), exerciseMap.get("reps")),
                "3 sets"
        );
        String muscle = asString(firstNonNull(exerciseMap.get("muscle"), exerciseMap.get("type")), "full_body");
        return new WorkoutPlanResponseDto.ExerciseDto(name, prescription, muscle);
    }

    private Map<String, Object> normalizeDietPlan(Map<String, Object> rawPlan) {
        Map<String, Object> plan = rawPlan != null ? new HashMap<>(rawPlan) : new HashMap<>();

        String title = asString(plan.get("title"), "Personalized Diet Plan");
        String phaseLabel = asString(firstNonNull(plan.get("phaseLabel"), plan.get("weightGoal")), "Nutrition Base");
        Integer calories = asPositiveInt(firstNonNull(plan.get("calories"), plan.get("dailyCalories")), 2000);
        Integer mealsPerDay = asPositiveInt(plan.get("mealsPerDay"), 3);
        String dietType = asString(plan.get("dietType"), "BALANCED");

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

    // Helper utility methods
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
}