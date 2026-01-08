package com.workoutplanner.service;

import com.workoutplanner.model.WorkoutPlan;
import com.workoutplanner.repository.WorkoutPlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class WorkoutPlanService {

    private final WorkoutPlanRepository workoutPlanRepository;

    @Autowired
    public WorkoutPlanService(WorkoutPlanRepository workoutPlanRepository) {
        this.workoutPlanRepository = workoutPlanRepository;
    }

    public Mono<WorkoutPlan> saveWorkoutPlan(WorkoutPlan workoutPlan) {
        return Mono.fromCallable(() -> {
            if (workoutPlan.getId() == null || workoutPlan.getId().isEmpty()) {
                workoutPlan.setId(java.util.UUID.randomUUID().toString());
            }
            return workoutPlanRepository.save(workoutPlan);
        });
    }

    public Mono<WorkoutPlan> findById(String id) {
        return Mono.fromCallable(() -> workoutPlanRepository.findById(id).orElse(null));
    }

    public Flux<WorkoutPlan> findAllWorkoutPlans() {
        return Mono.fromCallable(() -> workoutPlanRepository.findAllByOrderByGeneratedAtDesc())
                .flatMapMany(Flux::fromIterable);
    }

    public Flux<WorkoutPlan> findRecentWorkoutPlans(int days) {
        LocalDateTime fromDate = LocalDateTime.now().minusDays(days);
        return Mono.fromCallable(() -> workoutPlanRepository.findRecentWorkoutPlans(fromDate))
                .flatMapMany(Flux::fromIterable);
    }

    public Flux<WorkoutPlan> searchWorkoutPlans(String keyword) {
        return Mono.fromCallable(() -> workoutPlanRepository.findByKeyword(keyword))
                .flatMapMany(Flux::fromIterable);
    }

    public Mono<Boolean> deleteWorkoutPlan(String id) {
        return Mono.fromCallable(() -> {
            if (workoutPlanRepository.existsById(id)) {
                workoutPlanRepository.deleteById(id);
                return true;
            }
            return false;
        });
    }

    public Mono<WorkoutPlan> updateWorkoutPlanTitle(String id, String title) {
        return findById(id)
                .filter(plan -> plan != null)
                .map(plan -> {
                    plan.setTitle(title);
                    return workoutPlanRepository.save(plan);
                });
    }
}