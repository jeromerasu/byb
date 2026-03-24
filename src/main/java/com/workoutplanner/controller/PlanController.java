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
                // TEMPORARY DEBUG: Show original error message
                String originalError = e.getMessage();
                System.out.println("🚨 ORIGINAL ERROR FROM CombinedPlanService: " + originalError);

                // Handle specific error scenarios with appropriate HTTP status codes
                String errorMessage = e.getMessage();

                if (errorMessage.contains("User not found")) {
                    throw new RuntimeException("User not authenticated or not found. Original: " + originalError);
                } else if (errorMessage.contains("profile not found")) {
                    throw new RuntimeException("User profiles not complete. Please set up workout and diet profiles first. Original: " + originalError);
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

    @GetMapping("/debug-userid")
    public ResponseEntity<Map<String, Object>> debugUserId(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Get the actual user ID that getCurrentUserId() returns
            String actualUserId = getCurrentUserId(request);
            result.put("actual_user_id_from_getCurrentUserId", actualUserId);

            // Check profiles for this specific user ID
            Optional<WorkoutProfile> workoutProfile = workoutProfileRepository.findByUserId(actualUserId);
            result.put("workout_profile_found_for_actual_user", workoutProfile.isPresent());
            if (workoutProfile.isPresent()) {
                result.put("workout_profile_id", workoutProfile.get().getId());
            }

            Optional<DietProfile> dietProfile = dietProfileRepository.findByUserId(actualUserId);
            result.put("diet_profile_found_for_actual_user", dietProfile.isPresent());
            if (dietProfile.isPresent()) {
                result.put("diet_profile_id", dietProfile.get().getId());
            }

            // Also show profiles that exist for any user
            java.util.List<WorkoutProfile> allWorkoutProfiles = workoutProfileRepository.findAll();
            java.util.List<DietProfile> allDietProfiles = dietProfileRepository.findAll();

            result.put("total_workout_profiles", allWorkoutProfiles.size());
            result.put("total_diet_profiles", allDietProfiles.size());

            if (!allWorkoutProfiles.isEmpty()) {
                result.put("existing_workout_profile_user_id", allWorkoutProfiles.get(0).getUserId());
            }
            if (!allDietProfiles.isEmpty()) {
                result.put("existing_diet_profile_user_id", allDietProfiles.get(0).getUserId());
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("error_class", e.getClass().getSimpleName());
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
        // In BETA mode, always allow access with fallback user
        if (betaMode) {
            System.out.println("BETA mode active - allowing public access");

            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7);
                    String username = jwtService.extractUsername(token);

                    // Look up the user by username to get the user ID
                    Optional<User> user = userRepository.findByUsername(username);
                    if (user.isPresent()) {
                        System.out.println("Using authenticated user: " + user.get().getUsername());
                        return user.get().getId();
                    }
                } catch (Exception e) {
                    System.out.println("Failed to extract user from JWT in BETA mode: " + e.getMessage());
                }
            }

            // Fallback: use first available user from database
            java.util.List<User> allUsers = userRepository.findAll();
            if (!allUsers.isEmpty()) {
                User firstUser = allUsers.get(0);
                System.out.println("Using first available user for BETA testing: " + firstUser.getUsername());
                return firstUser.getId();
            }

            throw new RuntimeException("No users found in database for BETA testing");
        }

        // Production mode: use normal authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return ((User) authentication.getPrincipal()).getId();
        }
        throw new RuntimeException("User not authenticated");
    }
}