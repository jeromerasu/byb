package com.workoutplanner.repository;

import com.workoutplanner.model.DietProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DietProfileRepository extends JpaRepository<DietProfile, String> {

    // Find by user
    Optional<DietProfile> findByUserId(String userId);

    // Find by diet type
    List<DietProfile> findByDietType(DietProfile.DietType dietType);

    // Find by weight goal
    List<DietProfile> findByWeightGoal(DietProfile.WeightGoal weightGoal);

    // Find by calorie range
    @Query("SELECT d FROM DietProfile d WHERE d.dailyCalorieGoal BETWEEN :minCalories AND :maxCalories")
    List<DietProfile> findByCalorieRange(@Param("minCalories") Integer minCalories, @Param("maxCalories") Integer maxCalories);

    // Find by meals per day
    List<DietProfile> findByMealsPerDay(Integer mealsPerDay);

    // Find profiles with current plans
    @Query("SELECT d FROM DietProfile d WHERE d.currentPlanStorageKey IS NOT NULL")
    List<DietProfile> findProfilesWithCurrentPlan();

    // Find profiles without current plans
    @Query("SELECT d FROM DietProfile d WHERE d.currentPlanStorageKey IS NULL")
    List<DietProfile> findProfilesWithoutCurrentPlan();

    // Find recently active profiles
    @Query("SELECT d FROM DietProfile d WHERE d.lastMealLogged >= :fromDate ORDER BY d.lastMealLogged DESC")
    List<DietProfile> findRecentlyActiveProfiles(@Param("fromDate") LocalDateTime fromDate);

    // Get profiles by age range
    @Query("SELECT d FROM DietProfile d WHERE d.age BETWEEN :minAge AND :maxAge")
    List<DietProfile> findByAgeRange(@Param("minAge") Integer minAge, @Param("maxAge") Integer maxAge);

    // Temporarily disabled - array queries need different syntax for PostgreSQL
    // @Query("SELECT d FROM DietProfile d WHERE :restriction = ANY(d.dietaryRestrictions)")
    // List<DietProfile> findByDietaryRestriction(@Param("restriction") String restriction);

    // @Query("SELECT d FROM DietProfile d WHERE :cuisine = ANY(d.preferredCuisines)")
    // List<DietProfile> findByPreferredCuisine(@Param("cuisine") String cuisine);

    // Find most active meal loggers
    @Query("SELECT d FROM DietProfile d ORDER BY d.totalMealsLogged DESC")
    List<DietProfile> findMostActiveMealLoggers();

    // Find by protein goal range
    @Query("SELECT d FROM DietProfile d WHERE d.proteinGoalGrams BETWEEN :minProtein AND :maxProtein")
    List<DietProfile> findByProteinGoalRange(@Param("minProtein") Integer minProtein, @Param("maxProtein") Integer maxProtein);

    // Statistics queries
    @Query("SELECT d.dietType, COUNT(d) FROM DietProfile d WHERE d.dietType IS NOT NULL GROUP BY d.dietType")
    List<Object[]> getDietTypeDistribution();

    @Query("SELECT d.weightGoal, COUNT(d) FROM DietProfile d WHERE d.weightGoal IS NOT NULL GROUP BY d.weightGoal")
    List<Object[]> getWeightGoalDistribution();

    @Query("SELECT d.gender, COUNT(d) FROM DietProfile d WHERE d.gender IS NOT NULL GROUP BY d.gender")
    List<Object[]> getGenderDistribution();

    @Query("SELECT AVG(d.dailyCalorieGoal) FROM DietProfile d WHERE d.dailyCalorieGoal IS NOT NULL")
    Double getAverageCalorieGoal();

    @Query("SELECT AVG(d.mealsPerDay) FROM DietProfile d WHERE d.mealsPerDay IS NOT NULL")
    Double getAverageMealsPerDay();

    @Query("SELECT AVG(d.proteinGoalGrams) FROM DietProfile d WHERE d.proteinGoalGrams IS NOT NULL")
    Double getAverageProteinGoal();

    @Query("SELECT AVG(d.totalMealsLogged) FROM DietProfile d")
    Double getAverageMealsLogged();

    // BMR/TDEE calculations for users
    @Query("SELECT d FROM DietProfile d WHERE d.heightCm IS NOT NULL AND d.weightKg IS NOT NULL AND d.age IS NOT NULL AND d.gender IS NOT NULL")
    List<DietProfile> findProfilesWithCompleteMetrics();

    // Find profiles needing updates
    @Query("SELECT d FROM DietProfile d WHERE d.updatedAt < :thresholdDate")
    List<DietProfile> findProfilesNeedingUpdate(@Param("thresholdDate") LocalDateTime thresholdDate);

    // Count profiles by criteria
    long countByDietType(DietProfile.DietType dietType);
    long countByWeightGoal(DietProfile.WeightGoal weightGoal);

    @Query("SELECT COUNT(d) FROM DietProfile d WHERE d.currentPlanStorageKey IS NOT NULL")
    long countProfilesWithPlans();

    // Advanced nutritional analysis
    @Query("SELECT d FROM DietProfile d WHERE d.dailyCalorieGoal < (SELECT AVG(dp.dailyCalorieGoal) FROM DietProfile dp WHERE dp.dailyCalorieGoal IS NOT NULL)")
    List<DietProfile> findBelowAverageCalorieProfiles();

    @Query("SELECT d FROM DietProfile d WHERE d.proteinGoalGrams > :minProtein AND d.dietType = :dietType")
    List<DietProfile> findHighProteinProfilesByDietType(@Param("minProtein") Integer minProtein, @Param("dietType") DietProfile.DietType dietType);
}