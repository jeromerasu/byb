package com.workoutplanner.controller;

import com.workoutplanner.model.DietProfile;
import com.workoutplanner.model.DietPlan;
import com.workoutplanner.service.DietAIService;
import com.workoutplanner.service.DietPlanService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/diet-plans")
@CrossOrigin(origins = "*") // Configure appropriately for production
public class DietPlanController {

    private final DietAIService dietAIService;
    private final DietPlanService dietPlanService;

    @Autowired
    public DietPlanController(DietAIService dietAIService, DietPlanService dietPlanService) {
        this.dietAIService = dietAIService;
        this.dietPlanService = dietPlanService;
    }

    @PostMapping("/generate")
    public Mono<ResponseEntity<DietPlan>> generateDietPlan(@Valid @RequestBody DietProfile dietProfile) {
        return dietAIService.generateDietPlan(dietProfile)
                .map(dietPlan -> ResponseEntity.ok(dietPlan))
                .onErrorReturn(ResponseEntity.internalServerError().build());
    }

    @PostMapping("/save")
    public Mono<ResponseEntity<DietPlan>> saveDietPlan(@Valid @RequestBody DietPlan dietPlan) {
        return dietPlanService.saveDietPlan(dietPlan)
                .map(savedPlan -> ResponseEntity.ok(savedPlan))
                .onErrorReturn(ResponseEntity.internalServerError().build());
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<DietPlan>> getDietPlan(@PathVariable String id) {
        return dietPlanService.findById(id)
                .map(dietPlan -> {
                    if (dietPlan != null) {
                        return ResponseEntity.ok(dietPlan);
                    } else {
                        return ResponseEntity.<DietPlan>notFound().build();
                    }
                });
    }

    @GetMapping("/saved")
    public Flux<DietPlan> getAllSavedDietPlans() {
        return dietPlanService.findAllDietPlans();
    }

    @GetMapping("/recent")
    public Flux<DietPlan> getRecentDietPlans(@RequestParam(defaultValue = "30") int days) {
        return dietPlanService.findRecentDietPlans(days);
    }

    @GetMapping("/search")
    public Flux<DietPlan> searchDietPlans(@RequestParam String keyword) {
        return dietPlanService.searchDietPlans(keyword);
    }

    @GetMapping("/by-goal")
    public Flux<DietPlan> getDietPlansByGoal(@RequestParam String goal) {
        return dietPlanService.findByDietGoal(goal);
    }

    @PutMapping("/{id}/title")
    public Mono<ResponseEntity<DietPlan>> updateDietPlanTitle(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {
        String title = request.get("title");
        return dietPlanService.updateDietPlanTitle(id, title)
                .map(dietPlan -> {
                    if (dietPlan != null) {
                        return ResponseEntity.ok(dietPlan);
                    } else {
                        return ResponseEntity.<DietPlan>notFound().build();
                    }
                });
    }

    @PutMapping("/{id}/meal-prep-notes")
    public Mono<ResponseEntity<DietPlan>> updateMealPrepNotes(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {
        String mealPrepNotes = request.get("mealPrepNotes");
        return dietPlanService.updateMealPrepNotes(id, mealPrepNotes)
                .map(dietPlan -> {
                    if (dietPlan != null) {
                        return ResponseEntity.ok(dietPlan);
                    } else {
                        return ResponseEntity.<DietPlan>notFound().build();
                    }
                });
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteDietPlan(@PathVariable String id) {
        return dietPlanService.deleteDietPlan(id)
                .map(deleted -> deleted ?
                    ResponseEntity.ok().<Void>build() :
                    ResponseEntity.notFound().<Void>build());
    }

    @GetMapping("/stats/count")
    public Mono<ResponseEntity<Map<String, Object>>> getDietPlanStats(
            @RequestParam(defaultValue = "30") int days) {
        return dietPlanService.getPlansGeneratedCount(days)
                .map(count -> ResponseEntity.ok(Map.of(
                    "totalPlansGenerated", count,
                    "periodDays", days,
                    "averagePerDay", Math.round((double) count / days * 100.0) / 100.0
                )));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Diet AI Service is running");
    }

    @PostMapping("/validate")
    public ResponseEntity<String> validateDietProfile(@Valid @RequestBody DietProfile dietProfile) {
        return ResponseEntity.ok("Diet profile is valid");
    }

    @PostMapping("/generate-and-save")
    public Mono<ResponseEntity<DietPlan>> generateAndSaveDietPlan(@Valid @RequestBody DietProfile dietProfile) {
        return dietAIService.generateDietPlan(dietProfile)
                .flatMap(dietPlan -> dietPlanService.saveDietPlan(dietPlan))
                .map(savedPlan -> ResponseEntity.ok(savedPlan))
                .onErrorReturn(ResponseEntity.internalServerError().build());
    }
}