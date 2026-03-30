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

    // Feedback queries — entries that have a rating set
    List<WorkoutLog> findByUserIdAndRatingIsNotNullAndDateBetween(String userId, LocalDate from, LocalDate to);

    // Progress: fetch logs for a specific exercise in a date range
    List<WorkoutLog> findByUserIdAndExerciseIgnoreCaseAndDateBetweenOrderByDateAsc(
            String userId, String exercise, LocalDate from, LocalDate to);

    // Progress: max weight per (exercise, reps) pair — used for all-time PR detection
    @Query("SELECT w.exercise, w.reps, MAX(w.weight) FROM WorkoutLog w WHERE w.userId = :userId GROUP BY w.exercise, w.reps")
    List<Object[]> findMaxWeightPerExerciseReps(@Param("userId") String userId);

    // Progress: all logs in date range ordered by date
    List<WorkoutLog> findByUserIdAndDateBetweenOrderByDateAsc(String userId, LocalDate from, LocalDate to);

    // Progress: all logs for a user ordered by date asc (used for streak calculation)
    List<WorkoutLog> findByUserIdOrderByDateAsc(String userId);

    // Progress: distinct workout days in date range (used for heatmap / streak)
    @Query("SELECT DISTINCT w.date FROM WorkoutLog w WHERE w.userId = :userId AND w.date BETWEEN :from AND :to ORDER BY w.date")
    List<LocalDate> findDistinctDatesByUserIdAndDateBetween(
            @Param("userId") String userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    // Progress: logs that have an exercise catalog link (for muscle-balance)
    @Query("SELECT w FROM WorkoutLog w WHERE w.userId = :userId AND w.exerciseCatalogId IS NOT NULL AND w.date BETWEEN :from AND :to ORDER BY w.date ASC")
    List<WorkoutLog> findByUserIdWithCatalogLinkAndDateBetween(
            @Param("userId") String userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
