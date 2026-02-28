package com.workoutplanner.controller;

import com.workoutplanner.model.WorkoutProfile;
import com.workoutplanner.model.User;
import com.workoutplanner.repository.WorkoutProfileRepository;
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
@RequestMapping("/api/v1/workout")
@CrossOrigin(origins = "*")
public class WorkoutController {

    private final WorkoutProfileRepository workoutProfileRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    @Autowired
    public WorkoutController(WorkoutProfileRepository workoutProfileRepository,
                           UserRepository userRepository,
                           StorageService storageService) {
        this.workoutProfileRepository = workoutProfileRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    @GetMapping("/profile")
    public ResponseEntity<WorkoutProfile> getWorkoutProfile() {
        String userId = getCurrentUserId();

        Optional<WorkoutProfile> profile = workoutProfileRepository.findByUserId(userId);

        if (profile.isPresent()) {
            return ResponseEntity.ok(profile.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/profile")
    public ResponseEntity<WorkoutProfile> createOrUpdateWorkoutProfile(@Valid @RequestBody WorkoutProfile profile) {
        String userId = getCurrentUserId();

        // Set user ID
        profile.setUserId(userId);

        // Check if profile already exists
        Optional<WorkoutProfile> existingProfile = workoutProfileRepository.findByUserId(userId);
        if (existingProfile.isPresent()) {
            profile.setId(existingProfile.get().getId());
            profile.setCreatedAt(existingProfile.get().getCreatedAt());
        } else {
            // Generate ID for new profile
            profile.setId(UUID.randomUUID().toString());
            profile.setCreatedAt(LocalDateTime.now());
        }

        profile.setUpdatedAt(LocalDateTime.now());

        WorkoutProfile savedProfile = workoutProfileRepository.save(profile);

        // Update user's profile reference
        userRepository.findById(userId).ifPresent(user -> {
            user.setWorkoutProfileId(savedProfile.getId());
            userRepository.save(user);
        });

        return ResponseEntity.ok(savedProfile);
    }

    @PostMapping("/plan/generate")
    public Mono<ResponseEntity<Map<String, Object>>> generateWorkoutPlan() {
        String userId = getCurrentUserId();

        return Mono.fromCallable(() -> {
            // Get user and workout profile
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            WorkoutProfile workoutProfile = workoutProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Workout profile not found"));

            // Generate a simple workout plan (placeholder implementation)
            Map<String, Object> workoutPlan = generateSimpleWorkoutPlan(workoutProfile);

            // Store the plan in object storage
            String storageKey = "workout-plans/" + userId + "/" + UUID.randomUUID() + ".json";
            String planTitle = "Workout Plan - " + LocalDateTime.now().toLocalDate();

            try {
                String actualStorageKey = storageService.storeWorkoutPlan(userId, planTitle, workoutPlan);

                // Update workout profile with current plan info
                workoutProfile.setCurrentPlanStorageKey(actualStorageKey);
                workoutProfile.setCurrentPlanTitle(planTitle);
                workoutProfile.setCurrentPlanCreatedAt(LocalDateTime.now());
                workoutProfile.setUpdatedAt(LocalDateTime.now());
                workoutProfileRepository.save(workoutProfile);

                Map<String, Object> response = new HashMap<>();
                response.put("message", "Workout plan generated successfully");
                response.put("planTitle", planTitle);
                response.put("storageKey", actualStorageKey);
                response.put("plan", workoutPlan);

                return ResponseEntity.ok(response);

            } catch (Exception e) {
                throw new RuntimeException("Failed to store workout plan: " + e.getMessage());
            }
        });
    }

    @GetMapping("/plan/current")
    public Mono<ResponseEntity<Map<String, Object>>> getCurrentWorkoutPlan() {
        String userId = getCurrentUserId();

        return Mono.fromCallable(() -> {
            WorkoutProfile profile = workoutProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Workout profile not found"));

            if (profile.getCurrentPlanStorageKey() == null) {
                return ResponseEntity.notFound().build();
            }

            try {
                Map<String, Object> plan = storageService.retrieveWorkoutPlan(userId, profile.getCurrentPlanStorageKey());

                Map<String, Object> response = new HashMap<>();
                response.put("planTitle", profile.getCurrentPlanTitle());
                response.put("createdAt", profile.getCurrentPlanCreatedAt());
                response.put("storageKey", profile.getCurrentPlanStorageKey());
                response.put("plan", plan);

                return ResponseEntity.ok(response);

            } catch (Exception e) {
                throw new RuntimeException("Failed to retrieve workout plan: " + e.getMessage());
            }
        });
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getWorkoutStats() {
        String userId = getCurrentUserId();

        Optional<WorkoutProfile> profileOpt = workoutProfileRepository.findByUserId(userId);

        if (profileOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        WorkoutProfile profile = profileOpt.get();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalWorkoutsCompleted", profile.getTotalWorkoutsCompleted());
        stats.put("lastWorkout", profile.getLastWorkout());
        stats.put("fitnessLevel", profile.getFitnessLevel());
        stats.put("workoutFrequency", profile.getWorkoutFrequency());
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

    private Map<String, Object> generateSimpleWorkoutPlan(WorkoutProfile profile) {
        Map<String, Object> plan = new HashMap<>();

        plan.put("title", "Personalized Workout Plan");
        plan.put("fitnessLevel", profile.getFitnessLevel() != null ? profile.getFitnessLevel().name() : null);
        plan.put("frequency", profile.getWorkoutFrequency());
        plan.put("duration", profile.getSessionDuration());
        plan.put("targetGoals", profile.getTargetGoals());
        plan.put("equipment", profile.getAvailableEquipment());

        // Generate sample workout days based on frequency
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

        // Sample exercises based on fitness level
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
            default: // ADVANCED
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
}