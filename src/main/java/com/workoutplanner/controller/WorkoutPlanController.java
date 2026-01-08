package com.workoutplanner.controller;

import com.workoutplanner.model.UserProfile;
import com.workoutplanner.model.WorkoutPlan;
import com.workoutplanner.service.OpenAIService;
import com.workoutplanner.service.WorkoutPlanService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/workout-plans")
@CrossOrigin(origins = "*") // Configure appropriately for production
public class WorkoutPlanController {

    private final OpenAIService openAIService;
    private final WorkoutPlanService workoutPlanService;

    @Autowired
    public WorkoutPlanController(OpenAIService openAIService, WorkoutPlanService workoutPlanService) {
        this.openAIService = openAIService;
        this.workoutPlanService = workoutPlanService;
    }

    @PostMapping("/generate")
    public Mono<ResponseEntity<WorkoutPlan>> generateWorkoutPlan(@Valid @RequestBody UserProfile userProfile) {
        return openAIService.generateWorkoutPlan(userProfile)
                .map(workoutPlan -> ResponseEntity.ok(workoutPlan))
                .onErrorReturn(ResponseEntity.internalServerError().build());
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Workout AI Service is running");
    }

    @PostMapping("/validate")
    public ResponseEntity<String> validateUserProfile(@Valid @RequestBody UserProfile userProfile) {
        return ResponseEntity.ok("User profile is valid");
    }

    @PostMapping("/save")
    public Mono<ResponseEntity<WorkoutPlan>> saveWorkoutPlan(@Valid @RequestBody WorkoutPlan workoutPlan) {
        return workoutPlanService.saveWorkoutPlan(workoutPlan)
                .map(savedPlan -> ResponseEntity.ok(savedPlan))
                .onErrorReturn(ResponseEntity.internalServerError().build());
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<WorkoutPlan>> getWorkoutPlan(@PathVariable String id) {
        return workoutPlanService.findById(id)
                .map(workoutPlan -> {
                    if (workoutPlan != null) {
                        return ResponseEntity.ok(workoutPlan);
                    } else {
                        return ResponseEntity.<WorkoutPlan>notFound().build();
                    }
                });
    }

    @GetMapping("/saved")
    public Flux<WorkoutPlan> getAllSavedWorkoutPlans() {
        return workoutPlanService.findAllWorkoutPlans();
    }

    @GetMapping("/recent")
    public Flux<WorkoutPlan> getRecentWorkoutPlans(@RequestParam(defaultValue = "30") int days) {
        return workoutPlanService.findRecentWorkoutPlans(days);
    }

    @GetMapping("/search")
    public Flux<WorkoutPlan> searchWorkoutPlans(@RequestParam String keyword) {
        return workoutPlanService.searchWorkoutPlans(keyword);
    }

    @PutMapping("/{id}/title")
    public Mono<ResponseEntity<WorkoutPlan>> updateWorkoutPlanTitle(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {
        String title = request.get("title");
        return workoutPlanService.updateWorkoutPlanTitle(id, title)
                .map(workoutPlan -> {
                    if (workoutPlan != null) {
                        return ResponseEntity.ok(workoutPlan);
                    } else {
                        return ResponseEntity.<WorkoutPlan>notFound().build();
                    }
                });
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteWorkoutPlan(@PathVariable String id) {
        return workoutPlanService.deleteWorkoutPlan(id)
                .map(deleted -> deleted ?
                    ResponseEntity.ok().<Void>build() :
                    ResponseEntity.notFound().<Void>build());
    }
}