package com.workoutplanner.controller;

import com.workoutplanner.dto.WorkoutFeedbackResponse;
import com.workoutplanner.dto.WorkoutPlanResponseDto;
import com.workoutplanner.model.WorkoutProfile;
import com.workoutplanner.model.User;
import com.workoutplanner.repository.WorkoutProfileRepository;
import com.workoutplanner.repository.UserRepository;
import com.workoutplanner.service.OverloadService;
import com.workoutplanner.service.StorageService;
import com.workoutplanner.service.WorkoutPlanGeneratorService;
import com.workoutplanner.service.PlanValidationService;
import com.workoutplanner.service.JwtService;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/workout")
@CrossOrigin(origins = "*")
public class WorkoutController {

    private final WorkoutProfileRepository workoutProfileRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final WorkoutPlanGeneratorService workoutPlanGeneratorService;
    private final PlanValidationService planValidationService;
    private final JwtService jwtService;
    private final OverloadService overloadService;

    @Value("${beta.mode:false}")
    private boolean betaMode;

    @Autowired
    public WorkoutController(WorkoutProfileRepository workoutProfileRepository,
                           UserRepository userRepository,
                           StorageService storageService,
                           WorkoutPlanGeneratorService workoutPlanGeneratorService,
                           PlanValidationService planValidationService,
                           JwtService jwtService,
                           OverloadService overloadService) {
        this.workoutProfileRepository = workoutProfileRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.workoutPlanGeneratorService = workoutPlanGeneratorService;
        this.planValidationService = planValidationService;
        this.jwtService = jwtService;
        this.overloadService = overloadService;
    }

    @GetMapping("/profile")
    public ResponseEntity<WorkoutProfile> getWorkoutProfile(HttpServletRequest request) {
        String userId = getCurrentUserId(request);

        Optional<WorkoutProfile> profile = workoutProfileRepository.findByUserId(userId);

        if (profile.isPresent()) {
            return ResponseEntity.ok(profile.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/profile")
    public ResponseEntity<WorkoutProfile> createOrUpdateWorkoutProfile(@Valid @RequestBody WorkoutProfile profile, HttpServletRequest request) {
        String userId = getCurrentUserId(request);

        profile.setUserId(userId);

        Optional<WorkoutProfile> existingProfile = workoutProfileRepository.findByUserId(userId);
        if (existingProfile.isPresent()) {
            profile.setId(existingProfile.get().getId());
            profile.setCreatedAt(existingProfile.get().getCreatedAt());
        } else {
            profile.setId(UUID.randomUUID().toString());
            profile.setCreatedAt(LocalDateTime.now());
        }

        profile.setUpdatedAt(LocalDateTime.now());

        WorkoutProfile savedProfile = workoutProfileRepository.save(profile);

        userRepository.findById(userId).ifPresent(user -> {
            user.setWorkoutProfileId(savedProfile.getId());
            userRepository.save(user);
        });

        return ResponseEntity.ok(savedProfile);
    }


    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getWorkoutStats(HttpServletRequest request) {
        String userId = getCurrentUserId(request);

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

    @GetMapping("/feedback")
    public ResponseEntity<List<WorkoutFeedbackResponse>> getWorkoutFeedback(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest request) {
        String userId = getCurrentUserId(request);
        return ResponseEntity.ok(overloadService.getWorkoutFeedback(userId, from, to));
    }

    private String getCurrentUserId(HttpServletRequest request) {
        // In BETA mode, try to extract from JWT token first
        if (betaMode) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7);
                    String username = jwtService.extractUsername(token);

                    // Look up the user by username to get the user ID
                    Optional<User> user = userRepository.findByUsername(username);
                    if (user.isPresent()) {
                        return user.get().getId();
                    }
                } catch (Exception e) {
                    System.out.println("Failed to extract user from JWT in BETA mode: " + e.getMessage());
                }
            }

            // Fallback: Use any existing user from database for BETA mode
            try {
                Optional<User> firstUser = userRepository.findAll().stream().findFirst();
                if (firstUser.isPresent()) {
                    System.out.println("Using first available user for BETA testing: " + firstUser.get().getUsername());
                    return firstUser.get().getId();
                }
            } catch (Exception e) {
                System.out.println("Failed to find users in database: " + e.getMessage());
            }

            throw new RuntimeException("No users found in database for BETA testing");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return ((User) authentication.getPrincipal()).getId();
        }
        throw new RuntimeException("User not authenticated");
    }

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

        // Keep legacy payload for existing frontend compatibility
        dto.setPlan(normalizedPlan);
        return dto;
    }

    private Map<String, Object> normalizeWorkoutPlan(Map<String, Object> rawPlan) {
        Map<String, Object> plan = rawPlan != null ? new HashMap<>(rawPlan) : new HashMap<>();

        String title = asString(plan.get("title"), "Personalized Workout Plan");
        Integer durationMin = asPositiveInt(firstNonNull(plan.get("durationMin"), plan.get("duration")), 45);
        String phaseLabel = asString(firstNonNull(plan.get("phaseLabel"), plan.get("fitnessLevel")), "Base Phase");

        List<WorkoutPlanResponseDto.ExerciseDto> exercises = extractExercises(plan);

        // Lightweight fallback for unstructured/AI payloads
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

        // Extract from new 30-day structured format: weeks.week_1.day_1.exercises[]
        if (exercises.isEmpty() && plan.get("weeks") instanceof Map<?, ?> weeks) {
            for (Object weekObj : weeks.values()) {
                if (weekObj instanceof Map<?, ?> weekMap) {
                    for (Object dayObj : weekMap.values()) {
                        if (dayObj instanceof Map<?, ?> dayMap &&
                            dayMap.get("exercises") instanceof List<?> dayExercises) {
                            for (Object exObj : dayExercises) {
                                if (exObj instanceof Map<?, ?> exerciseMap) {
                                    exercises.add(mapExercise(exerciseMap));
                                }
                            }
                        }
                    }
                }
            }
        }

        // Fallback: flatten workoutDays[].exercises[] from legacy plan structure
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

        // Handle structured exercise format with sets, reps, weight, etc.
        String prescription;
        Object sets = exerciseMap.get("sets");
        Object reps = exerciseMap.get("reps");
        Object weightType = exerciseMap.get("weight_type");

        if (sets != null && reps != null) {
            String setsStr = String.valueOf(sets);
            String repsStr = String.valueOf(reps);

            if ("time_seconds".equals(weightType)) {
                prescription = setsStr + " sets of " + repsStr + " seconds";
            } else {
                prescription = setsStr + " sets of " + repsStr + " reps";
            }
        } else {
            prescription = asString(
                firstNonNull(exerciseMap.get("prescription"), exerciseMap.get("sets"), exerciseMap.get("reps")),
                "3 sets"
            );
        }

        // Extract muscle groups - prioritize structured format
        String muscle;
        Object muscleGroups = exerciseMap.get("muscle_groups");
        if (muscleGroups instanceof List<?> groups && !groups.isEmpty()) {
            muscle = String.valueOf(groups.get(0)); // Use primary muscle group
        } else {
            muscle = asString(firstNonNull(exerciseMap.get("muscle"), exerciseMap.get("type")), "full_body");
        }

        return new WorkoutPlanResponseDto.ExerciseDto(name, prescription, muscle);
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
}
