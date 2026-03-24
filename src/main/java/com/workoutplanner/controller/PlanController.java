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
import com.workoutplanner.service.ObjectStorageService;
import com.workoutplanner.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/plan")
@CrossOrigin(origins = "*")
public class PlanController {

    private static final Logger logger = LoggerFactory.getLogger(PlanController.class);

    private final CombinedPlanService combinedPlanService;
    private final PlanParsingService planParsingService;
    private final StorageService storageService;
    private final ObjectStorageService objectStorageService;
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
                         ObjectStorageService objectStorageService,
                         WorkoutProfileRepository workoutProfileRepository,
                         DietProfileRepository dietProfileRepository,
                         UserRepository userRepository,
                         JwtService jwtService) {
        this.combinedPlanService = combinedPlanService;
        this.planParsingService = planParsingService;
        this.storageService = storageService;
        this.objectStorageService = objectStorageService;
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
    @Transactional
    public ResponseEntity<Map<String, Object>> cleanupTestUsers() {
        Map<String, Object> result = new HashMap<>();

        try {
            logger.info("Starting test user cleanup process");
            java.util.List<User> allUsers = userRepository.findAll();
            result.put("initial_user_count", allUsers.size());
            logger.info("Found {} total users in database", allUsers.size());

            // Find a user with both profiles to keep
            User keepUser = null;
            for (User user : allUsers) {
                Optional<WorkoutProfile> workoutProfile = workoutProfileRepository.findByUserId(user.getId());
                Optional<DietProfile> dietProfile = dietProfileRepository.findByUserId(user.getId());

                if (workoutProfile.isPresent() && dietProfile.isPresent()) {
                    keepUser = user;
                    logger.info("Found user with both profiles to keep: {} ({})", user.getUsername(), user.getId());
                    break;
                }
            }

            // If no user with both profiles found, keep the first user
            if (keepUser == null && !allUsers.isEmpty()) {
                keepUser = allUsers.get(0);
                logger.info("No user with both profiles found, keeping first user: {} ({})", keepUser.getUsername(), keepUser.getId());
            }

            // Delete all other users and their profiles
            int deletedUsers = 0;
            for (User user : allUsers) {
                if (!user.getId().equals(keepUser.getId())) {
                    logger.info("Deleting user: {} ({})", user.getUsername(), user.getId());
                    try {
                        workoutProfileRepository.deleteByUserId(user.getId());
                        dietProfileRepository.deleteByUserId(user.getId());
                        userRepository.delete(user);
                        deletedUsers++;
                    } catch (Exception e) {
                        logger.error("Error deleting user {}: {}", user.getId(), e.getMessage());
                    }
                }
            }

            result.put("deleted_user_count", deletedUsers);
            if (keepUser != null) {
                result.put("kept_user", Map.of(
                    "id", keepUser.getId(),
                    "username", keepUser.getUsername(),
                    "email", keepUser.getEmail()
                ));
            }

            logger.info("Cleanup complete: deleted {} users, kept 1 user", deletedUsers);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error during cleanup: {}", e.getMessage(), e);
            result.put("error", e.getMessage());
            result.put("errorType", e.getClass().getSimpleName());
            return ResponseEntity.ok(result);
        }
    }

    @GetMapping("/debug-storage")
    public ResponseEntity<Map<String, Object>> debugStorage() {
        try {
            logger.debug("Debug storage endpoint called");
            Map<String, Object> debugInfo = objectStorageService.getStorageDebugInfo();
            return ResponseEntity.ok(debugInfo);
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", e.getMessage());
            errorResult.put("errorType", e.getClass().getSimpleName());
            return ResponseEntity.ok(errorResult);
        }
    }

    @GetMapping("/debug-buckets")
    public ResponseEntity<Map<String, Object>> debugBuckets() {
        try {
            logger.debug("Debug buckets endpoint called");
            List<String> buckets = objectStorageService.listAllBuckets();

            Map<String, Object> result = new HashMap<>();
            result.put("totalBuckets", buckets.size());
            result.put("bucketNames", buckets);

            // Get object count for each bucket
            Map<String, Integer> bucketObjectCounts = new HashMap<>();
            for (String bucketName : buckets) {
                try {
                    List<Map<String, Object>> objects = objectStorageService.listAllObjectsInBucket(bucketName);
                    bucketObjectCounts.put(bucketName, objects.size());
                } catch (Exception e) {
                    bucketObjectCounts.put(bucketName, -1); // Error indicator
                }
            }
            result.put("bucketObjectCounts", bucketObjectCounts);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", e.getMessage());
            errorResult.put("errorType", e.getClass().getSimpleName());
            return ResponseEntity.ok(errorResult);
        }
    }

    @GetMapping("/debug-bucket/{bucketName}")
    public ResponseEntity<Map<String, Object>> debugBucketObjects(@PathVariable String bucketName) {
        try {
            logger.debug("Debug bucket objects endpoint called for bucket: {}", bucketName);
            List<Map<String, Object>> objects = objectStorageService.listAllObjectsInBucket(bucketName);

            Map<String, Object> result = new HashMap<>();
            result.put("bucketName", bucketName);
            result.put("objectCount", objects.size());
            result.put("objects", objects);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("bucketName", bucketName);
            errorResult.put("error", e.getMessage());
            errorResult.put("errorType", e.getClass().getSimpleName());
            return ResponseEntity.ok(errorResult);
        }
    }

    @PostMapping("/debug-test-bucket/{bucketName}")
    public ResponseEntity<Map<String, Object>> testBucketCreation(@PathVariable String bucketName) {
        try {
            logger.debug("Test bucket creation endpoint called for bucket: {}", bucketName);
            Map<String, Object> testResult = objectStorageService.testBucketCreation(bucketName);
            return ResponseEntity.ok(testResult);
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("bucketName", bucketName);
            errorResult.put("error", e.getMessage());
            errorResult.put("errorType", e.getClass().getSimpleName());
            return ResponseEntity.ok(errorResult);
        }
    }

    @GetMapping("/debug-connectivity")
    public ResponseEntity<Map<String, Object>> testConnectivity() {
        try {
            logger.debug("Connectivity test endpoint called");
            Map<String, Object> connectivityResult = objectStorageService.testConnectivity();
            return ResponseEntity.ok(connectivityResult);
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", e.getMessage());
            errorResult.put("errorType", e.getClass().getSimpleName());
            errorResult.put("testResult", "ENDPOINT_FAILURE");
            return ResponseEntity.ok(errorResult);
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
        logger.debug("getCurrentUserId called - betaMode: {}", betaMode);

        // In BETA mode, use the single test user approach
        if (betaMode) {
            logger.debug("BETA mode active - using single test user");

            // First try to extract from JWT if available
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7);
                    String username = jwtService.extractUsername(token);
                    Optional<User> user = userRepository.findByUsername(username);
                    if (user.isPresent()) {
                        String userId = user.get().getId();
                        logger.info("BETA mode: JWT authentication successful for user: {} ({})", username, userId);
                        return userId;
                    }
                } catch (Exception e) {
                    logger.warn("BETA mode: JWT parsing failed: {}", e.getMessage());
                }
            }

            // Fallback: use first user with both profiles
            java.util.List<User> allUsers = userRepository.findAll();
            for (User user : allUsers) {
                Optional<WorkoutProfile> workoutProfile = workoutProfileRepository.findByUserId(user.getId());
                Optional<DietProfile> dietProfile = dietProfileRepository.findByUserId(user.getId());

                if (workoutProfile.isPresent() && dietProfile.isPresent()) {
                    logger.info("BETA mode fallback: Using user with both profiles: {} ({})", user.getUsername(), user.getId());
                    return user.getId();
                }
            }

            // Final fallback: use first user
            if (!allUsers.isEmpty()) {
                String userId = allUsers.get(0).getId();
                logger.warn("BETA mode final fallback: Using first user: {} ({})", allUsers.get(0).getUsername(), userId);
                return userId;
            }

            throw new RuntimeException("BETA mode: No users found in database");
        }

        // Production mode: use normal authentication
        logger.debug("Production mode: checking SecurityContext authentication");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            String userId = ((User) authentication.getPrincipal()).getId();
            logger.info("Authenticated user found: {}", userId);
            return userId;
        }
        logger.warn("User not authenticated in production mode");
        throw new RuntimeException("User not authenticated");
    }
}