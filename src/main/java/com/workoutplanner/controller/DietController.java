package com.workoutplanner.controller;

import com.workoutplanner.model.DietProfile;
import com.workoutplanner.model.User;
import com.workoutplanner.repository.DietProfileRepository;
import com.workoutplanner.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/diet")
@CrossOrigin(origins = "*")
public class DietController {

    private final DietProfileRepository dietProfileRepository;
    private final UserRepository userRepository;

    @Autowired
    public DietController(DietProfileRepository dietProfileRepository,
                         UserRepository userRepository) {
        this.dietProfileRepository = dietProfileRepository;
        this.userRepository = userRepository;
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

}
