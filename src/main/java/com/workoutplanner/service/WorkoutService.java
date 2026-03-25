package com.workoutplanner.service;

import com.workoutplanner.model.WorkoutProfile;
import com.workoutplanner.model.User;
import com.workoutplanner.repository.WorkoutProfileRepository;
import com.workoutplanner.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;
import java.util.Arrays;

@Service
public class WorkoutService {

    private final WorkoutProfileRepository workoutProfileRepository;
    private final UserRepository userRepository;
    private final Optional<ObjectStorageService> objectStorageService;

    @Autowired
    public WorkoutService(WorkoutProfileRepository workoutProfileRepository,
                         UserRepository userRepository,
                         Optional<ObjectStorageService> objectStorageService) {
        this.workoutProfileRepository = workoutProfileRepository;
        this.userRepository = userRepository;
        this.objectStorageService = objectStorageService;
    }

    @Transactional
    public Mono<WorkoutProfile> createOrUpdateProfile(String userId, WorkoutProfile profile) {
        return Mono.fromCallable(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            profile.setUserId(userId);

            // Check if profile already exists
            Optional<WorkoutProfile> existingProfile = workoutProfileRepository.findByUserId(userId);
            if (existingProfile.isPresent()) {
                profile.setId(existingProfile.get().getId());
                profile.setCreatedAt(existingProfile.get().getCreatedAt());
            }

            profile.setUpdatedAt(LocalDateTime.now());

            WorkoutProfile savedProfile = workoutProfileRepository.save(profile);

            // Update user's profile reference
            user.setWorkoutProfileId(savedProfile.getId());
            userRepository.save(user);

            return savedProfile;
        });
    }

    @Transactional
    public Mono<Map<String, Object>> generateWorkoutPlan(String userId) {
        return Mono.fromCallable(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            WorkoutProfile workoutProfile = workoutProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Workout profile not found for user"));

            // Generate workout plan based on profile
            Map<String, Object> workoutPlan = createWorkoutPlan(workoutProfile);

            // Store the plan in object storage
            String storageKey = "workout-plans/" + userId + "/" + UUID.randomUUID() + ".json";
            String planTitle = "Workout Plan - " + LocalDateTime.now().toLocalDate();

            try {
                objectStorageService.orElseThrow(() -> new RuntimeException("Object storage service not available"))
                    .storeWorkoutPlan("workout", userId, planTitle, workoutPlan);

                // Update workout profile with current plan info
                workoutProfile.setCurrentPlanStorageKey(storageKey);
                workoutProfile.setCurrentPlanTitle(planTitle);
                workoutProfile.setCurrentPlanCreatedAt(LocalDateTime.now());
                workoutProfile.setUpdatedAt(LocalDateTime.now());
                workoutProfileRepository.save(workoutProfile);

                Map<String, Object> response = new HashMap<>();
                response.put("message", "Workout plan generated successfully");
                response.put("planTitle", planTitle);
                response.put("storageKey", storageKey);
                response.put("plan", workoutPlan);
                response.put("user", Map.of("id", user.getId(), "username", user.getUsername()));
                response.put("profile", workoutProfile);

                return response;

            } catch (Exception e) {
                throw new RuntimeException("Failed to store workout plan: " + e.getMessage(), e);
            }
        });
    }

    public Mono<Map<String, Object>> getCurrentWorkoutPlan(String userId) {
        return Mono.fromCallable(() -> {
            WorkoutProfile profile = workoutProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Workout profile not found"));

            if (profile.getCurrentPlanStorageKey() == null) {
                throw new RuntimeException("No current workout plan found");
            }

            try {
                Map<String, Object> plan = objectStorageService.orElseThrow(() -> new RuntimeException("Object storage service not available"))
                    .retrieveWorkoutPlan("workout", userId, profile.getCurrentPlanStorageKey());

                Map<String, Object> response = new HashMap<>();
                response.put("planTitle", profile.getCurrentPlanTitle());
                response.put("createdAt", profile.getCurrentPlanCreatedAt());
                response.put("storageKey", profile.getCurrentPlanStorageKey());
                response.put("plan", plan);

                return response;

            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve workout plan: " + e.getMessage(), e);
            }
        });
    }

    @Transactional
    public Mono<WorkoutProfile> logWorkout(String userId, Map<String, Object> workoutData) {
        return Mono.fromCallable(() -> {
            WorkoutProfile profile = workoutProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Workout profile not found"));

            // Update workout tracking
            profile.setLastWorkout(LocalDateTime.now());

            Integer currentTotal = profile.getTotalWorkoutsCompleted();
            if (currentTotal == null) currentTotal = 0;
            profile.setTotalWorkoutsCompleted(currentTotal + 1);

            profile.setUpdatedAt(LocalDateTime.now());

            return workoutProfileRepository.save(profile);
        });
    }

    public Mono<Optional<WorkoutProfile>> getWorkoutProfile(String userId) {
        return Mono.fromCallable(() -> workoutProfileRepository.findByUserId(userId));
    }

    public Mono<Map<String, Object>> getWorkoutStats(String userId) {
        return Mono.fromCallable(() -> {
            Optional<WorkoutProfile> profileOpt = workoutProfileRepository.findByUserId(userId);

            if (profileOpt.isEmpty()) {
                throw new RuntimeException("Workout profile not found");
            }

            WorkoutProfile profile = profileOpt.get();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalWorkoutsCompleted", profile.getTotalWorkoutsCompleted());
            stats.put("lastWorkout", profile.getLastWorkout());
            stats.put("fitnessLevel", profile.getFitnessLevel());
            stats.put("workoutFrequency", profile.getWorkoutFrequency());
            stats.put("sessionDuration", profile.getSessionDuration());
            stats.put("targetGoals", profile.getTargetGoals());
            stats.put("hasCurrentPlan", profile.getCurrentPlanStorageKey() != null);
            stats.put("profileCreated", profile.getCreatedAt());
            stats.put("lastUpdated", profile.getUpdatedAt());

            return stats;
        });
    }

    private Map<String, Object> createWorkoutPlan(WorkoutProfile profile) {
        Map<String, Object> plan = new HashMap<>();

        plan.put("title", "Personalized Workout Plan");
        plan.put("fitnessLevel", profile.getFitnessLevel());
        plan.put("frequency", profile.getWorkoutFrequency());
        plan.put("duration", profile.getSessionDuration());
        plan.put("targetGoals", profile.getTargetGoals());
        plan.put("equipment", profile.getAvailableEquipment());
        plan.put("workoutTypes", profile.getPreferredWorkoutTypes());

        // Generate workout days based on frequency
        List<Map<String, Object>> workoutDays = new ArrayList<>();
        int frequency = profile.getWorkoutFrequency() != null ? profile.getWorkoutFrequency() : 3;

        for (int i = 1; i <= frequency; i++) {
            Map<String, Object> day = new HashMap<>();
            day.put("day", i);
            day.put("title", "Day " + i + " - " + getDayFocus(i, profile));
            day.put("focus", getDayFocus(i, profile));
            day.put("exercises", generateExercises(profile, i));
            day.put("estimatedDuration", profile.getSessionDuration() != null ? profile.getSessionDuration() : 45);
            workoutDays.add(day);
        }

        plan.put("workoutDays", workoutDays);
        plan.put("generatedAt", LocalDateTime.now());
        plan.put("notes", generatePlanNotes(profile));

        return plan;
    }

    private String getDayFocus(int day, WorkoutProfile profile) {
        String[] targetGoalsArray = profile.getTargetGoals();
        List<String> targetGoals = targetGoalsArray != null ? Arrays.asList(targetGoalsArray) : new ArrayList<>();

        if (targetGoals != null && !targetGoals.isEmpty()) {
            // Cycle through target goals
            String primaryGoal = targetGoals.get((day - 1) % targetGoals.size());

            switch (primaryGoal.toLowerCase()) {
                case "muscle_gain":
                case "strength":
                    return day % 2 == 1 ? "Upper Body Strength" : "Lower Body Strength";
                case "weight_loss":
                case "cardio":
                    return "Cardio & Fat Burning";
                case "flexibility":
                case "mobility":
                    return "Flexibility & Mobility";
                case "endurance":
                    return "Endurance Training";
                default:
                    return "Full Body Workout";
            }
        }

        // Default rotation
        String[] focuses = {"Upper Body", "Lower Body", "Cardio", "Full Body", "Core & Flexibility"};
        return focuses[(day - 1) % focuses.length];
    }

    private List<Map<String, Object>> generateExercises(WorkoutProfile profile, int day) {
        List<Map<String, Object>> exercises = new ArrayList<>();

        String fitnessLevel = profile.getFitnessLevel() != null ? profile.getFitnessLevel().name().toLowerCase() : "beginner";
        String[] availableEquipment = profile.getAvailableEquipment();
        boolean hasGym = availableEquipment != null &&
            (Arrays.asList(availableEquipment).contains("FULL_GYM") || Arrays.asList(availableEquipment).contains("WEIGHTS"));

        String dayFocus = getDayFocus(day, profile).toLowerCase();

        if (dayFocus.contains("upper")) {
            if (hasGym) {
                exercises.add(createExercise("Bench Press", getSetsByLevel(fitnessLevel), "strength"));
                exercises.add(createExercise("Pull-ups", getSetsByLevel(fitnessLevel), "strength"));
                exercises.add(createExercise("Shoulder Press", getSetsByLevel(fitnessLevel), "strength"));
            } else {
                exercises.add(createExercise("Push-ups", getBodyweightSetsByLevel(fitnessLevel), "strength"));
                exercises.add(createExercise("Pike Push-ups", getBodyweightSetsByLevel(fitnessLevel), "strength"));
                exercises.add(createExercise("Tricep Dips", getBodyweightSetsByLevel(fitnessLevel), "strength"));
            }
        } else if (dayFocus.contains("lower")) {
            if (hasGym) {
                exercises.add(createExercise("Squats", getSetsByLevel(fitnessLevel), "strength"));
                exercises.add(createExercise("Deadlifts", getSetsByLevel(fitnessLevel), "strength"));
                exercises.add(createExercise("Lunges", getSetsByLevel(fitnessLevel), "strength"));
            } else {
                exercises.add(createExercise("Bodyweight Squats", getBodyweightSetsByLevel(fitnessLevel), "strength"));
                exercises.add(createExercise("Single Leg Glute Bridges", getBodyweightSetsByLevel(fitnessLevel), "strength"));
                exercises.add(createExercise("Calf Raises", getBodyweightSetsByLevel(fitnessLevel), "strength"));
            }
        } else if (dayFocus.contains("cardio")) {
            exercises.add(createExercise("Jumping Jacks", "3 sets of 1 minute", "cardio"));
            exercises.add(createExercise("High Knees", "3 sets of 30 seconds", "cardio"));
            exercises.add(createExercise("Burpees", getCardioSetsByLevel(fitnessLevel), "cardio"));
        } else {
            // Full body or core
            exercises.add(createExercise("Plank", getPlanksSetsByLevel(fitnessLevel), "core"));
            exercises.add(createExercise("Mountain Climbers", "3 sets of 30 seconds", "cardio"));
            exercises.add(createExercise("Russian Twists", getSetsByLevel(fitnessLevel), "core"));
        }

        return exercises;
    }

    private Map<String, Object> createExercise(String name, String sets, String type) {
        Map<String, Object> exercise = new HashMap<>();
        exercise.put("name", name);
        exercise.put("sets", sets);
        exercise.put("type", type);
        return exercise;
    }

    private String getSetsByLevel(String level) {
        switch (level.toLowerCase()) {
            case "beginner":
                return "3 sets of 8-10 reps";
            case "intermediate":
                return "4 sets of 10-12 reps";
            case "advanced":
                return "4 sets of 12-15 reps";
            default:
                return "3 sets of 8-10 reps";
        }
    }

    private String getBodyweightSetsByLevel(String level) {
        switch (level.toLowerCase()) {
            case "beginner":
                return "3 sets of 8-12 reps";
            case "intermediate":
                return "4 sets of 12-18 reps";
            case "advanced":
                return "4 sets of 15-25 reps";
            default:
                return "3 sets of 8-12 reps";
        }
    }

    private String getCardioSetsByLevel(String level) {
        switch (level.toLowerCase()) {
            case "beginner":
                return "3 sets of 5 reps";
            case "intermediate":
                return "3 sets of 8 reps";
            case "advanced":
                return "4 sets of 10 reps";
            default:
                return "3 sets of 5 reps";
        }
    }

    private String getPlanksSetsByLevel(String level) {
        switch (level.toLowerCase()) {
            case "beginner":
                return "3 sets of 20-30 seconds";
            case "intermediate":
                return "3 sets of 45-60 seconds";
            case "advanced":
                return "3 sets of 60-90 seconds";
            default:
                return "3 sets of 20-30 seconds";
        }
    }

    private List<String> generatePlanNotes(WorkoutProfile profile) {
        List<String> notes = new ArrayList<>();

        notes.add("Rest for 30-60 seconds between sets");
        notes.add("Focus on proper form over heavy weights");
        notes.add("Stay hydrated throughout your workout");

        if ("beginner".equals(profile.getFitnessLevel())) {
            notes.add("Start with lighter weights and focus on learning proper technique");
            notes.add("It's normal to feel sore after workouts - this will improve with time");
        }

        if (profile.getTargetGoals() != null && Arrays.asList(profile.getTargetGoals()).contains("weight_loss")) {
            notes.add("Combine this workout with a balanced diet for best weight loss results");
        }

        return notes;
    }
}