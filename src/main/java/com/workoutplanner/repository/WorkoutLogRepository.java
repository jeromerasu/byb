package com.workoutplanner.repository;

import com.workoutplanner.model.ExerciseType;
import com.workoutplanner.model.WorkoutLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkoutLogRepository extends JpaRepository<WorkoutLog, String> {

    // Basic lookups
    List<WorkoutLog> findByUserId(String userId);

    List<WorkoutLog> findByUserIdOrderByDateDesc(String userId);

    List<WorkoutLog> findByUserIdAndDate(String userId, LocalDate date);

    List<WorkoutLog> findByUserIdAndDateBetweenOrderByDateDesc(String userId, LocalDate from, LocalDate to);

    List<WorkoutLog> findByUserIdAndExerciseType(String userId, ExerciseType exerciseType);

    // PR (personal record) queries — max weight per exercise per user
    @Query("SELECT MAX(w.weight) FROM WorkoutLog w WHERE w.userId = :userId AND w.exercise = :exercise")
    Optional<BigDecimal> findMaxWeightByUserIdAndExercise(@Param("userId") String userId,
                                                          @Param("exercise") String exercise);

    // All distinct exercises logged by a user (for PR dashboard)
    @Query("SELECT DISTINCT w.exercise FROM WorkoutLog w WHERE w.userId = :userId ORDER BY w.exercise")
    List<String> findDistinctExercisesByUserId(@Param("userId") String userId);

    // Most recent PR entry per exercise (latest log where weight = max weight)
    @Query("SELECT w FROM WorkoutLog w WHERE w.userId = :userId AND w.exercise = :exercise " +
           "AND w.weight = (SELECT MAX(w2.weight) FROM WorkoutLog w2 WHERE w2.userId = :userId AND w2.exercise = :exercise) " +
           "ORDER BY w.date DESC")
    List<WorkoutLog> findPersonalRecordEntriesByUserIdAndExercise(@Param("userId") String userId,
                                                                   @Param("exercise") String exercise);
}
