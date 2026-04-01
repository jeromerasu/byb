package com.workoutplanner.repository;

import com.workoutplanner.model.UserWeekPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserWeekPlanRepository extends JpaRepository<UserWeekPlan, String> {

    Optional<UserWeekPlan> findByUserIdAndWeekStart(String userId, LocalDate weekStart);

    List<UserWeekPlan> findByUserIdOrderByWeekStartDesc(String userId);

    List<UserWeekPlan> findByUserIdAndWeekStartBetweenOrderByWeekStartAsc(
            String userId, LocalDate from, LocalDate to);
}
