package com.workoutplanner.controller;

import com.workoutplanner.model.DietProfile;
import com.workoutplanner.model.User;
import com.workoutplanner.repository.DietProfileRepository;
import com.workoutplanner.repository.UserRepository;
import com.workoutplanner.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/diet")
@CrossOrigin(origins = "*")
public class DietController {

    private final DietProfileRepository dietProfileRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${beta.mode:false}")
    private boolean betaMode;

    @Autowired
    public DietController(DietProfileRepository dietProfileRepository,
                         UserRepository userRepository,
                         JwtService jwtService) {
        this.dietProfileRepository = dietProfileRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @GetMapping("/profile")
    public ResponseEntity<DietProfile> getDietProfile(HttpServletRequest request) {
        String userId = getCurrentUserId(request);

        Optional<DietProfile> profile = dietProfileRepository.findByUserId(userId);

        if (profile.isPresent()) {
            return ResponseEntity.ok(profile.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/profile")
    public ResponseEntity<DietProfile> createOrUpdateDietProfile(@Valid @RequestBody DietProfile profile, HttpServletRequest request) {
        String userId = getCurrentUserId(request);

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


    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDietStats(HttpServletRequest request) {
        String userId = getCurrentUserId(request);

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

            // Fallback to hardcoded user ID for BETA mode if no valid token
            return "3d91b1cd-aa94-48ec-b91f-edcb1e69bbbf"; // test_public_endpoint user ID
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return ((User) authentication.getPrincipal()).getId();
        }
        throw new RuntimeException("User not authenticated");
    }

}
