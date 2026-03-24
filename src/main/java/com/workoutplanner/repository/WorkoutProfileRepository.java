package com.workoutplanner.repository;

import com.workoutplanner.model.WorkoutProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkoutProfileRepository extends JpaRepository<WorkoutProfile, String> {

    // Find by user
    Optional<WorkoutProfile> findByUserId(String userId);

    // Find by fitness level
    List<WorkoutProfile> findByFitnessLevel(WorkoutProfile.FitnessLevel fitnessLevel);

    // Find by workout frequency
    List<WorkoutProfile> findByWorkoutFrequencyGreaterThanEqual(Integer minFrequency);

    // Find by activity level
    List<WorkoutProfile> findByActivityLevel(WorkoutProfile.ActivityLevel activityLevel);

    // Find profiles with current plans
    @Query("SELECT w FROM WorkoutProfile w WHERE w.currentPlanStorageKey IS NOT NULL")
    List<WorkoutProfile> findProfilesWithCurrentPlan();

    // Find profiles without current plans
    @Query("SELECT w FROM WorkoutProfile w WHERE w.currentPlanStorageKey IS NULL")
    List<WorkoutProfile> findProfilesWithoutCurrentPlan();

    // Find recently active profiles
    @Query("SELECT w FROM WorkoutProfile w WHERE w.lastWorkout >= :fromDate ORDER BY w.lastWorkout DESC")
    List<WorkoutProfile> findRecentlyActiveProfiles(@Param("fromDate") LocalDateTime fromDate);

    // Get profiles by age range
    @Query("SELECT w FROM WorkoutProfile w WHERE w.age BETWEEN :minAge AND :maxAge")
    List<WorkoutProfile> findByAgeRange(@Param("minAge") Integer minAge, @Param("maxAge") Integer maxAge);

    // Get profiles by BMI range (calculated)
    @Query("SELECT w FROM WorkoutProfile w WHERE w.heightCm IS NOT NULL AND w.weightKg IS NOT NULL")
    List<WorkoutProfile> findProfilesWithBMIData();

    // Find most active users
    @Query("SELECT w FROM WorkoutProfile w ORDER BY w.totalWorkoutsCompleted DESC")
    List<WorkoutProfile> findMostActiveUsers();

    // Temporarily disabled - array queries need different syntax for PostgreSQL
    // @Query("SELECT w FROM WorkoutProfile w WHERE :goal = ANY(w.targetGoals)")
    // List<WorkoutProfile> findByTargetGoal(@Param("goal") String goal);

    // @Query("SELECT w FROM WorkoutProfile w WHERE :equipment = ANY(w.availableEquipment)")
    // List<WorkoutProfile> findByAvailableEquipment(@Param("equipment") String equipment);

    // Statistics queries
    @Query("SELECT w.fitnessLevel, COUNT(w) FROM WorkoutProfile w GROUP BY w.fitnessLevel")
    List<Object[]> getFitnessLevelDistribution();

    @Query("SELECT w.activityLevel, COUNT(w) FROM WorkoutProfile w GROUP BY w.activityLevel")
    List<Object[]> getActivityLevelDistribution();

    @Query("SELECT w.gender, COUNT(w) FROM WorkoutProfile w WHERE w.gender IS NOT NULL GROUP BY w.gender")
    List<Object[]> getGenderDistribution();

    @Query("SELECT AVG(w.workoutFrequency) FROM WorkoutProfile w WHERE w.workoutFrequency IS NOT NULL")
    Double getAverageWorkoutFrequency();

    @Query("SELECT AVG(w.sessionDuration) FROM WorkoutProfile w WHERE w.sessionDuration IS NOT NULL")
    Double getAverageSessionDuration();

    @Query("SELECT AVG(w.totalWorkoutsCompleted) FROM WorkoutProfile w")
    Double getAverageWorkoutsCompleted();

    // Find profiles needing updates
    @Query("SELECT w FROM WorkoutProfile w WHERE w.updatedAt < :thresholdDate")
    List<WorkoutProfile> findProfilesNeedingUpdate(@Param("thresholdDate") LocalDateTime thresholdDate);

    // Count profiles by criteria
    long countByFitnessLevel(WorkoutProfile.FitnessLevel fitnessLevel);
    long countByActivityLevel(WorkoutProfile.ActivityLevel activityLevel);

    @Query("SELECT COUNT(w) FROM WorkoutProfile w WHERE w.currentPlanStorageKey IS NOT NULL")
    long countProfilesWithPlans();

    // Delete by user ID
    void deleteByUserId(String userId);
}