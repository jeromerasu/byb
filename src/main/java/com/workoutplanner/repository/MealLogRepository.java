package com.workoutplanner.repository;

import com.workoutplanner.model.MealLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // Feedback queries — entries with any feedback field non-null
    @Query("SELECT m FROM MealLog m WHERE m.userId = :userId AND m.date BETWEEN :from AND :to " +
           "AND (m.rating IS NOT NULL OR m.feedbackComment IS NOT NULL) " +
           "ORDER BY m.date DESC")
    List<MealLog> findWithFeedbackByUserIdAndDateBetween(@Param("userId") String userId,
                                                          @Param("from") LocalDate from,
                                                          @Param("to") LocalDate to);
}
