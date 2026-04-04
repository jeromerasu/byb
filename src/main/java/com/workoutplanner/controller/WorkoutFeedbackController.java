package com.workoutplanner.controller;

import com.workoutplanner.dto.WorkoutFeedbackRequest;
import com.workoutplanner.model.User;
import com.workoutplanner.model.WorkoutFeedback;
import com.workoutplanner.repository.UserRepository;
import com.workoutplanner.repository.WorkoutFeedbackRepository;
import com.workoutplanner.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/workout")
@CrossOrigin(origins = "*")
public class WorkoutFeedbackController {

    private final WorkoutFeedbackRepository workoutFeedbackRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${beta.mode:false}")
    private boolean betaMode;

    public WorkoutFeedbackController(WorkoutFeedbackRepository workoutFeedbackRepository,
                                     UserRepository userRepository,
                                     JwtService jwtService) {
        this.workoutFeedbackRepository = workoutFeedbackRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/feedback")
    public ResponseEntity<WorkoutFeedback> submitFeedback(@RequestBody WorkoutFeedbackRequest request,
                                                          HttpServletRequest httpRequest) {
        String userId = getCurrentUserId(httpRequest);

        WorkoutFeedback feedback = new WorkoutFeedback();
        feedback.setUserId(userId);
        feedback.setWorkoutDate(request.getWorkoutDate() != null ? request.getWorkoutDate() : LocalDate.now());
        feedback.setRating(request.getRating());
        feedback.setSessionComments(request.getSessionComments());
        feedback.setFlaggedExercises(request.getFlaggedExercises());
        feedback.setFreeFormNote(request.getFreeFormNote());

        return ResponseEntity.ok(workoutFeedbackRepository.save(feedback));
    }

    @GetMapping("/feedback")
    public ResponseEntity<List<WorkoutFeedback>> getRecentFeedback(
            @RequestParam(required = false, defaultValue = "7") int days,
            HttpServletRequest httpRequest) {
        String userId = getCurrentUserId(httpRequest);
        LocalDate since = LocalDate.now().minusDays(days);
        return ResponseEntity.ok(workoutFeedbackRepository.findByUserIdAndWorkoutDateAfter(userId, since));
    }

    private String getCurrentUserId(HttpServletRequest request) {
        if (betaMode) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7);
                    String username = jwtService.extractUsername(token);
                    Optional<User> user = userRepository.findByUsername(username);
                    if (user.isPresent()) {
                        return user.get().getId();
                    }
                } catch (Exception e) {
                    System.out.println("Failed to extract user from JWT in BETA mode: " + e.getMessage());
                }
            }
            try {
                Optional<User> firstUser = userRepository.findAll().stream().findFirst();
                if (firstUser.isPresent()) {
                    return firstUser.get().getId();
                }
            } catch (Exception e) {
                System.out.println("Failed to find users in database: " + e.getMessage());
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No users found in database for BETA testing");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return ((User) authentication.getPrincipal()).getId();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
    }
}
