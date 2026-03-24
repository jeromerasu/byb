package com.workoutplanner.controller;

import com.workoutplanner.dto.CombinedPlanResponseDto;
import com.workoutplanner.dto.CurrentWeekResponseDto;
import com.workoutplanner.dto.DietFoodCatalogResponseDto;
import com.workoutplanner.model.User;
import com.workoutplanner.model.WorkoutProfile;
import com.workoutplanner.model.DietProfile;
import com.workoutplanner.repository.WorkoutProfileRepository;
import com.workoutplanner.repository.DietProfileRepository;
import com.workoutplanner.repository.UserRepository;
import com.workoutplanner.service.CombinedPlanService;
import com.workoutplanner.service.PlanParsingService;
import com.workoutplanner.service.StorageService;
import com.workoutplanner.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/plan")
@CrossOrigin(origins = "*")
public class PlanController {

    private final CombinedPlanService combinedPlanService;
    private final PlanParsingService planParsingService;
    private final StorageService storageService;
    private final WorkoutProfileRepository workoutProfileRepository;
    private final DietProfileRepository dietProfileRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${beta.mode:false}")
    private boolean betaMode;

    @Autowired
    public PlanController(CombinedPlanService combinedPlanService,
                         PlanParsingService planParsingService,
                         StorageService storageService,
                         WorkoutProfileRepository workoutProfileRepository,
                         DietProfileRepository dietProfileRepository,
                         UserRepository userRepository,
                         JwtService jwtService) {
        this.combinedPlanService = combinedPlanService;
        this.planParsingService = planParsingService;
        this.storageService = storageService;
        this.workoutProfileRepository = workoutProfileRepository;
        this.dietProfileRepository = dietProfileRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/generate")
    public Mono<ResponseEntity<CombinedPlanResponseDto>> generateCombinedPlan(HttpServletRequest request) {
        String userId = getCurrentUserId(request);

        return Mono.fromCallable(() -> {
            try {
                CombinedPlanResponseDto response = combinedPlanService.generateCombinedPlan(userId);
                return ResponseEntity.ok(response);
            } catch (RuntimeException e) {
                // Handle specific error scenarios with appropriate HTTP status codes
                String errorMessage = e.getMessage();

                if (errorMessage.contains("User not found")) {
                    throw new RuntimeException("User not authenticated or not found");
                } else if (errorMessage.contains("profile not found")) {
                    throw new RuntimeException("User profiles not complete. Please set up workout and diet profiles first");
                } else {
                    throw new RuntimeException("Failed to generate combined plan: " + errorMessage);
                }
            }
        }).onErrorMap(throwable -> {
            if (throwable instanceof RuntimeException) {
                return throwable;
            }
            return new RuntimeException("Unexpected error during plan generation", throwable);
        });
    }

    @GetMapping("/current-week")
    public Mono<ResponseEntity<CurrentWeekResponseDto>> getCurrentWeek(HttpServletRequest request) {
        String userId = getCurrentUserId(request);

        return Mono.fromCallable(() -> {
            try {
                Optional<WorkoutProfile> workoutProfile = workoutProfileRepository.findByUserId(userId);
                Optional<DietProfile> dietProfile = dietProfileRepository.findByUserId(userId);

                if (workoutProfile.isEmpty() || dietProfile.isEmpty()) {
                    throw new RuntimeException("User profiles not complete. Please set up workout and diet profiles first.");
                }

                String workoutStorageKey = workoutProfile.get().getCurrentPlanStorageKey();
                String dietStorageKey = dietProfile.get().getCurrentPlanStorageKey();

                if (workoutStorageKey == null || dietStorageKey == null) {
                    throw new RuntimeException("No current plans found. Please generate plans first.");
                }

                String workoutBucketName = betaMode ? "workoutbeta" : "workout";
                String dietBucketName = betaMode ? "dietbeta" : "diet";

                Map<String, Object> workoutPlan = storageService.retrieveWorkoutPlan(workoutBucketName, userId, workoutStorageKey);
                Map<String, Object> dietPlan = storageService.retrieveDietPlan(dietBucketName, userId, dietStorageKey);

                if (workoutPlan == null || dietPlan == null) {
                    throw new RuntimeException("Failed to retrieve current plans from storage.");
                }

                int weekIndex = 1;
                CurrentWeekResponseDto response = planParsingService.extractCurrentWeek(workoutPlan, dietPlan, weekIndex);
                return ResponseEntity.ok(response);

            } catch (RuntimeException e) {
                String errorMessage = e.getMessage();

                if (errorMessage.contains("User not authenticated")) {
                    throw new RuntimeException("User not authenticated");
                } else if (errorMessage.contains("profiles not complete")) {
                    throw new RuntimeException("User profiles not complete. Please set up workout and diet profiles first.");
                } else if (errorMessage.contains("No current plans found")) {
                    throw new RuntimeException("No current plans found. Please generate plans first.");
                } else {
                    throw new RuntimeException("Failed to retrieve current week data: " + errorMessage);
                }
            }
        }).onErrorMap(throwable -> {
            if (throwable instanceof RuntimeException) {
                return throwable;
            }
            return new RuntimeException("Unexpected error during current week retrieval", throwable);
        });
    }


    @GetMapping("/debug-status")
    public ResponseEntity<Map<String, Object>> debugStatus(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();

        try {
            String userId = getCurrentUserId(request);
            result.put("user_id", userId);
            result.put("beta_mode", betaMode);

            // Check user existence
            Optional<User> user = userRepository.findById(userId);
            result.put("user_exists", user.isPresent());
            if (user.isPresent()) {
                result.put("username", user.get().getUsername());
            }

            // Check profiles
            Optional<WorkoutProfile> workoutProfile = workoutProfileRepository.findByUserId(userId);
            result.put("workout_profile_exists", workoutProfile.isPresent());

            Optional<DietProfile> dietProfile = dietProfileRepository.findByUserId(userId);
            result.put("diet_profile_exists", dietProfile.isPresent());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    @GetMapping("/debug-users")
    public ResponseEntity<Map<String, Object>> debugUsers() {
        Map<String, Object> result = new HashMap<>();

        try {
            java.util.List<User> allUsers = userRepository.findAll();
            result.put("total_users", allUsers.size());

            java.util.List<Map<String, Object>> userList = new java.util.ArrayList<>();
            for (User user : allUsers) {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", user.getId());
                userInfo.put("username", user.getUsername());
                userInfo.put("email", user.getEmail());
                userInfo.put("first_name", user.getFirstName());
                userInfo.put("last_name", user.getLastName());

                // Check if user has profiles
                Optional<WorkoutProfile> workoutProfile = workoutProfileRepository.findByUserId(user.getId());
                Optional<DietProfile> dietProfile = dietProfileRepository.findByUserId(user.getId());
                userInfo.put("has_workout_profile", workoutProfile.isPresent());
                userInfo.put("has_diet_profile", dietProfile.isPresent());
                userInfo.put("has_both_profiles", workoutProfile.isPresent() && dietProfile.isPresent());

                userList.add(userInfo);
            }
            result.put("users", userList);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    @PostMapping("/cleanup-test-users")
    public ResponseEntity<Map<String, Object>> cleanupTestUsers() {
        Map<String, Object> result = new HashMap<>();

        try {
            java.util.List<User> allUsers = userRepository.findAll();
            result.put("initial_user_count", allUsers.size());

            // Keep one test user (create if none exist)
            User keepUser = null;
            java.util.List<User> usersToDelete = new java.util.ArrayList<>();

            // Look for an existing test user with both profiles
            for (User user : allUsers) {
                Optional<WorkoutProfile> workoutProfile = workoutProfileRepository.findByUserId(user.getId());
                Optional<DietProfile> dietProfile = dietProfileRepository.findByUserId(user.getId());

                if (keepUser == null && workoutProfile.isPresent() && dietProfile.isPresent()) {
                    keepUser = user;
                } else {
                    usersToDelete.add(user);
                }
            }

            // If no suitable user found, keep the first user or create one
            if (keepUser == null && !allUsers.isEmpty()) {
                keepUser = allUsers.get(0);
                usersToDelete.remove(keepUser);
            }

            // Delete users and their profiles
            int deletedUsers = 0;
            for (User user : usersToDelete) {
                workoutProfileRepository.deleteByUserId(user.getId());
                dietProfileRepository.deleteByUserId(user.getId());
                userRepository.delete(user);
                deletedUsers++;
            }

            result.put("deleted_user_count", deletedUsers);
            result.put("kept_user", keepUser != null ? Map.of(
                "id", keepUser.getId(),
                "username", keepUser.getUsername(),
                "email", keepUser.getEmail()
            ) : null);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("error", e.getMessage());
            return ResponseEntity.ok(result);
        }
    }

    @GetMapping("/diet-foods")
    public Mono<ResponseEntity<DietFoodCatalogResponseDto>> getDietFoods(HttpServletRequest request) {
        String userId = getCurrentUserId(request);

        return Mono.fromCallable(() -> {
            try {
                Optional<DietProfile> dietProfile = dietProfileRepository.findByUserId(userId);

                if (dietProfile.isEmpty()) {
                    throw new RuntimeException("Diet profile not found. Please set up diet profile first.");
                }

                String dietStorageKey = dietProfile.get().getCurrentPlanStorageKey();

                if (dietStorageKey == null) {
                    throw new RuntimeException("No current diet plan found. Please generate plans first.");
                }

                String dietBucketName = betaMode ? "dietbeta" : "diet";
                Map<String, Object> dietPlan = storageService.retrieveDietPlan(dietBucketName, userId, dietStorageKey);

                if (dietPlan == null) {
                    throw new RuntimeException("Failed to retrieve current diet plan from storage.");
                }

                DietFoodCatalogResponseDto response = planParsingService.extractDietFoodCatalog(dietPlan);
                return ResponseEntity.ok(response);

            } catch (RuntimeException e) {
                String errorMessage = e.getMessage();

                if (errorMessage.contains("User not authenticated")) {
                    throw new RuntimeException("User not authenticated");
                } else if (errorMessage.contains("Diet profile not found")) {
                    throw new RuntimeException("Diet profile not found. Please set up diet profile first.");
                } else if (errorMessage.contains("No current diet plan found")) {
                    throw new RuntimeException("No current diet plan found. Please generate plans first.");
                } else {
                    throw new RuntimeException("Failed to retrieve diet food catalog: " + errorMessage);
                }
            }
        }).onErrorMap(throwable -> {
            if (throwable instanceof RuntimeException) {
                return throwable;
            }
            return new RuntimeException("Unexpected error during diet food catalog retrieval", throwable);
        });
    }

    private String getCurrentUserId(HttpServletRequest request) {
        // In BETA mode, find a user that actually has both profiles
        if (betaMode) {
            System.out.println("🔍 BETA mode active - finding user with both profiles");

            // Find the first user that has both workout and diet profiles
            java.util.List<WorkoutProfile> workoutProfiles = workoutProfileRepository.findAll();
            if (!workoutProfiles.isEmpty()) {
                String candidateUserId = workoutProfiles.get(0).getUserId();
                System.out.println("🏋️ Found workout profile for user: " + candidateUserId);

                // Check if this user also has a diet profile
                Optional<DietProfile> dietProfile = dietProfileRepository.findByUserId(candidateUserId);
                if (dietProfile.isPresent()) {
                    System.out.println("🍽️ Same user has diet profile - using: " + candidateUserId);
                    return candidateUserId;
                }
            }

            // Fallback to hardcoded test user
            String fallbackUserId = "3d91b1cd-aa94-48ec-b91f-edcb1e69bbbf";
            System.out.println("⚠️ Fallback to hardcoded test user ID: " + fallbackUserId);
            return fallbackUserId;
        }

        // Production mode: use normal authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return ((User) authentication.getPrincipal()).getId();
        }
        throw new RuntimeException("User not authenticated");
    }
}