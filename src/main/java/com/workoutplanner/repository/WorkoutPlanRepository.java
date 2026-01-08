package com.workoutplanner.repository;

import com.workoutplanner.model.WorkoutPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, String> {

    @Query("SELECT w FROM WorkoutPlan w ORDER BY w.generatedAt DESC")
    List<WorkoutPlan> findAllByOrderByGeneratedAtDesc();

    @Query("SELECT w FROM WorkoutPlan w WHERE w.generatedAt >= :fromDate ORDER BY w.generatedAt DESC")
    List<WorkoutPlan> findRecentWorkoutPlans(LocalDateTime fromDate);

    @Query("SELECT w FROM WorkoutPlan w WHERE w.title LIKE %:keyword% OR w.aiResponse LIKE %:keyword% ORDER BY w.generatedAt DESC")
    List<WorkoutPlan> findByKeyword(String keyword);
}