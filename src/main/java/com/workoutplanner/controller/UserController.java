package com.workoutplanner.controller;

import com.workoutplanner.dto.UserPhysicalProfileRequest;
import com.workoutplanner.model.User;
import com.workoutplanner.repository.UserRepository;
import com.workoutplanner.repository.WorkoutProfileRepository;
import com.workoutplanner.repository.DietProfileRepository;
import com.workoutplanner.service.UserService;
import com.workoutplanner.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/user")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final WorkoutProfileRepository workoutProfileRepository;
    private final DietProfileRepository dietProfileRepository;
    private final JwtService jwtService;

    @Value("${beta.mode:false}")
    private boolean betaMode;

    @Autowired
    public UserController(UserService userService,
                         UserRepository userRepository,
                         WorkoutProfileRepository workoutProfileRepository,
                         DietProfileRepository dietProfileRepository,
                         JwtService jwtService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.workoutProfileRepository = workoutProfileRepository;
        this.dietProfileRepository = dietProfileRepository;
        this.jwtService = jwtService;
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getUserProfile(HttpServletRequest request) {
        String userId = getCurrentUserId(request);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("email", user.getEmail());
        profile.put("firstName", user.getFirstName());
        profile.put("lastName", user.getLastName());
        profile.put("role", user.getRole());
        profile.put("isActive", user.isActive());
        profile.put("createdAt", user.getCreatedAt());
        profile.put("height_cm", user.getHeightCm());
        profile.put("weight_kg", user.getWeightKg());
        profile.put("age", user.getAge());
        profile.put("gender", user.getGender());
        profile.put("activity_level", user.getActivityLevel());

        return ResponseEntity.ok(profile);
    }

    @PostMapping("/physical-profile")
    public ResponseEntity<Map<String, Object>> savePhysicalProfile(
            @RequestBody UserPhysicalProfileRequest request,
            HttpServletRequest httpRequest) {
        String userId = getCurrentUserId(httpRequest);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getHeightCm() != null) user.setHeightCm(request.getHeightCm());
        if (request.getWeightKg() != null) user.setWeightKg(request.getWeightKg());
        if (request.getAge() != null) user.setAge(request.getAge());
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getActivityLevel() != null) user.setActivityLevel(request.getActivityLevel());

        userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Physical profile updated successfully");
        response.put("height_cm", user.getHeightCm());
        response.put("weight_kg", user.getWeightKg());
        response.put("age", user.getAge());
        response.put("gender", user.getGender());
        response.put("activity_level", user.getActivityLevel());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public Mono<ResponseEntity<Map<String, Object>>> registerUser(@Valid @RequestBody User user) {
        return userService.registerUser(user)
                .map(registeredUser -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("message", "User registered successfully");
                    response.put("userId", registeredUser.getId());
                    response.put("username", registeredUser.getUsername());
                    return ResponseEntity.ok(response);
                })
                .onErrorReturn(ResponseEntity.badRequest().body(Map.of("error", "Registration failed")));
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
}