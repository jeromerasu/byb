package com.workoutplanner.repository;

import com.workoutplanner.model.DietPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DietPlanRepository extends JpaRepository<DietPlan, String> {

    @Query("SELECT d FROM DietPlan d ORDER BY d.generatedAt DESC")
    List<DietPlan> findAllByOrderByGeneratedAtDesc();

    @Query("SELECT d FROM DietPlan d WHERE d.generatedAt >= :fromDate ORDER BY d.generatedAt DESC")
    List<DietPlan> findRecentDietPlans(LocalDateTime fromDate);

    @Query("SELECT d FROM DietPlan d WHERE d.title LIKE %:keyword% OR d.aiResponse LIKE %:keyword% OR d.mealPrepNotes LIKE %:keyword% ORDER BY d.generatedAt DESC")
    List<DietPlan> findByKeyword(String keyword);

    @Query("SELECT d FROM DietPlan d WHERE d.dietProfileJson LIKE %:dietGoal% ORDER BY d.generatedAt DESC")
    List<DietPlan> findByDietGoal(String dietGoal);

    @Query("SELECT COUNT(d) FROM DietPlan d WHERE d.generatedAt >= :fromDate")
    long countPlansGeneratedSince(LocalDateTime fromDate);
}