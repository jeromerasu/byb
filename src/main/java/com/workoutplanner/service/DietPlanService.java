package com.workoutplanner.service;

import com.workoutplanner.model.DietPlan;
import com.workoutplanner.repository.DietPlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class DietPlanService {

    private final DietPlanRepository dietPlanRepository;

    @Autowired
    public DietPlanService(DietPlanRepository dietPlanRepository) {
        this.dietPlanRepository = dietPlanRepository;
    }

    public Mono<DietPlan> saveDietPlan(DietPlan dietPlan) {
        return Mono.fromCallable(() -> {
            if (dietPlan.getId() == null || dietPlan.getId().isEmpty()) {
                dietPlan.setId(java.util.UUID.randomUUID().toString());
            }
            return dietPlanRepository.save(dietPlan);
        });
    }

    public Mono<DietPlan> findById(String id) {
        return Mono.fromCallable(() -> dietPlanRepository.findById(id).orElse(null));
    }

    public Flux<DietPlan> findAllDietPlans() {
        return Mono.fromCallable(() -> dietPlanRepository.findAllByOrderByGeneratedAtDesc())
                .flatMapMany(Flux::fromIterable);
    }

    public Flux<DietPlan> findRecentDietPlans(int days) {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(days);
        return Mono.fromCallable(() -> dietPlanRepository.findRecentDietPlans(fromDate))
                .flatMapMany(Flux::fromIterable);
    }

    public Flux<DietPlan> searchDietPlans(String keyword) {
        return Mono.fromCallable(() -> dietPlanRepository.findByKeyword(keyword))
                .flatMapMany(Flux::fromIterable);
    }

    public Flux<DietPlan> findByDietGoal(String dietGoal) {
        return Mono.fromCallable(() -> dietPlanRepository.findByDietGoal(dietGoal))
                .flatMapMany(Flux::fromIterable);
    }

    public Mono<Boolean> deleteDietPlan(String id) {
        return Mono.fromCallable(() -> {
            if (dietPlanRepository.existsById(id)) {
                dietPlanRepository.deleteById(id);
                return true;
            }
            return false;
        });
    }

    public Mono<DietPlan> updateDietPlanTitle(String id, String title) {
        return findById(id)
                .filter(plan -> plan != null)
                .map(plan -> {
                    plan.setTitle(title);
                    return dietPlanRepository.save(plan);
                });
    }

    public Mono<DietPlan> updateMealPrepNotes(String id, String mealPrepNotes) {
        return findById(id)
                .filter(plan -> plan != null)
                .map(plan -> {
                    plan.setMealPrepNotes(mealPrepNotes);
                    return dietPlanRepository.save(plan);
                });
    }

    public Mono<Long> getPlansGeneratedCount(int days) {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(days);
        return Mono.fromCallable(() -> dietPlanRepository.countPlansGeneratedSince(fromDate));
    }
}