package com.workoutplanner.controller;

import com.workoutplanner.dto.CombinedPlanResponseDto;
import com.workoutplanner.model.User;
import com.workoutplanner.service.CombinedPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/plan")
@CrossOrigin(origins = "*")
public class PlanController {

    private final CombinedPlanService combinedPlanService;

    @Autowired
    public PlanController(CombinedPlanService combinedPlanService) {
        this.combinedPlanService = combinedPlanService;
    }

    @PostMapping("/generate")
    public Mono<ResponseEntity<CombinedPlanResponseDto>> generateCombinedPlan() {
        String userId = getCurrentUserId();

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
                    throw new RuntimeException("User profiles not complete. Please set up workout and diet profiles first.");
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

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return ((User) authentication.getPrincipal()).getId();
        }
        throw new RuntimeException("User not authenticated");
    }
}