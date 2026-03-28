package com.workoutplanner.repository;

import com.workoutplanner.model.MealLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MealLogRepository extends JpaRepository<MealLog, String> {

    List<MealLog> findByUserId(String userId);

    List<MealLog> findByUserIdOrderByDateDesc(String userId);

    List<MealLog> findByUserIdAndDate(String userId, LocalDate date);

    List<MealLog> findByUserIdAndDateBetweenOrderByDateDesc(String userId, LocalDate from, LocalDate to);

    // Feedback queries — entries that have a rating set
    List<MealLog> findByUserIdAndRatingIsNotNullAndDateBetween(String userId, LocalDate from, LocalDate to);
}
