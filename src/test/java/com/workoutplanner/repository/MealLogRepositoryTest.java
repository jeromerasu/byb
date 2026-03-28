package com.workoutplanner.repository;

import com.workoutplanner.model.MealLog;
import com.workoutplanner.model.MealRating;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Repository tests for MealLog entity including TASK-API-005 feedback fields
 * (rating, feedback_comment).
 */
@DataJpaTest
@ActiveProfiles("test")
class MealLogRepositoryTest {

    private static final Logger log = LoggerFactory.getLogger(MealLogRepositoryTest.class);

    @Autowired
    private MealLogRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    // -- Helpers ---------------------------------------------------------

    private MealLog buildMeal(String userId, String mealName, LocalDate date) {
        MealLog entry = new MealLog(userId, mealName,
                new BigDecimal("500.00"), new BigDecimal("30.00"),
                new BigDecimal("15.00"), new BigDecimal("60.00"), date);
        return repository.save(entry);
    }

    private MealLog buildMealWithFeedback(String userId, String mealName, LocalDate date,
                                          MealRating rating, String comment) {
        MealLog entry = new MealLog(userId, mealName,
                new BigDecimal("500.00"), new BigDecimal("30.00"),
                new BigDecimal("15.00"), new BigDecimal("60.00"), date);
        entry.setRating(rating);
        entry.setFeedbackComment(comment);
        return repository.save(entry);
    }

    // -- Basic persistence ----------------------------------------------

    @Test
    void save_BasicMeal_ShouldPersistWithNullFeedback() {
        MealLog entry = new MealLog("u1", "Oatmeal",
                new BigDecimal("350.00"), new BigDecimal("10.00"),
                new BigDecimal("5.00"), new BigDecimal("60.00"), LocalDate.now());

        MealLog saved = repository.save(entry);

        assertNotNull(saved.getId());
        assertEquals("Oatmeal", saved.getMealName());
        assertNull(saved.getRating());
        assertNull(saved.getFeedbackComment());
    }

    // -- Feedback fields: rating, feedback_comment ----------------------

    @Test
    void save_WithLovedRating_ShouldPersistFeedback() {
        MealLog entry = new MealLog("u2", "Grilled Chicken",
                new BigDecimal("450.00"), new BigDecimal("45.00"),
                new BigDecimal("10.00"), new BigDecimal("20.00"), LocalDate.now());
        entry.setRating(MealRating.LOVED);
        entry.setFeedbackComment("Delicious and filling");

        MealLog saved = repository.save(entry);

        log.info("test.save_LovedRating id={}", saved.getId());
        assertEquals(MealRating.LOVED, saved.getRating());
        assertEquals("Delicious and filling", saved.getFeedbackComment());
    }

    @Test
    void save_WithDislikedRating_ShouldPersistFeedback() {
        MealLog entry = new MealLog("u3", "Brussels Sprouts",
                new BigDecimal("80.00"), new BigDecimal("4.00"),
                new BigDecimal("1.00"), new BigDecimal("12.00"), LocalDate.now());
        entry.setRating(MealRating.DISLIKED);
        entry.setFeedbackComment("Cannot stand the taste");

        MealLog saved = repository.save(entry);

        assertEquals(MealRating.DISLIKED, saved.getRating());
        assertEquals("Cannot stand the taste", saved.getFeedbackComment());
    }

    @Test
    void save_WithOkayRating_ShouldPersistRatingOnly() {
        MealLog entry = new MealLog("u4", "Rice and Beans",
                new BigDecimal("400.00"), new BigDecimal("20.00"),
                new BigDecimal("5.00"), new BigDecimal("70.00"), LocalDate.now());
        entry.setRating(MealRating.OKAY);

        MealLog saved = repository.save(entry);

        assertEquals(MealRating.OKAY, saved.getRating());
        assertNull(saved.getFeedbackComment());
    }

    // -- findByUserId ---------------------------------------------------

    @Test
    void findByUserId_ShouldReturnAllMealsForUser() {
        buildMeal("meal-user", "Breakfast", LocalDate.now());
        buildMeal("meal-user", "Lunch", LocalDate.now());
        buildMeal("other-user", "Dinner", LocalDate.now());

        List<MealLog> result = repository.findByUserId("meal-user");

        log.info("test.findByUserId count={}", result.size());
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(m -> "meal-user".equals(m.getUserId())));
    }

    @Test
    void findByUserIdAndDate_ShouldReturnOnlyMatchingDate() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        buildMeal("date-user", "Breakfast", today);
        buildMeal("date-user", "Lunch", yesterday);

        List<MealLog> result = repository.findByUserIdAndDate("date-user", today);

        assertEquals(1, result.size());
        assertEquals("Breakfast", result.get(0).getMealName());
    }

    // -- Feedback query: findByUserIdAndRatingIsNotNullAndDateBetween ---

    @Test
    void findFeedbackByDateRange_ShouldReturnOnlyRatedMeals() {
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);

        buildMealWithFeedback("fb-user", "Chicken", today, MealRating.LOVED, "Great");
        buildMealWithFeedback("fb-user", "Oatmeal", weekAgo, MealRating.DISLIKED, "Bland");
        buildMeal("fb-user", "Rice", today); // no feedback

        List<MealLog> result = repository.findByUserIdAndRatingIsNotNullAndDateBetween(
                "fb-user", weekAgo, today);

        log.info("test.findMealFeedback count={}", result.size());
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(m -> m.getRating() != null));
    }

    @Test
    void findFeedbackByDateRange_ShouldNotReturnOtherUsersEntries() {
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);

        buildMealWithFeedback("user-X", "Salad", today, MealRating.OKAY, null);
        buildMealWithFeedback("user-Y", "Steak", today, MealRating.LOVED, "Excellent");

        List<MealLog> resultX = repository.findByUserIdAndRatingIsNotNullAndDateBetween(
                "user-X", weekAgo, today);

        assertEquals(1, resultX.size());
        assertEquals("user-X", resultX.get(0).getUserId());
    }

    @Test
    void findFeedbackByDateRange_OutsideRange_ShouldReturnEmpty() {
        LocalDate today = LocalDate.now();

        buildMealWithFeedback("range-user", "Pasta", today, MealRating.LOVED, null);

        List<MealLog> result = repository.findByUserIdAndRatingIsNotNullAndDateBetween(
                "range-user", today.minusDays(14), today.minusDays(8));

        assertTrue(result.isEmpty());
    }

    // -- Persistence basics ---------------------------------------------

    @Test
    void save_ShouldPersistAndAssignId() {
        MealLog entry = buildMeal("persist-user", "Smoothie", LocalDate.now());
        assertNotNull(entry.getId());
    }

    @Test
    void delete_ShouldRemoveEntry() {
        MealLog entry = buildMeal("del-user", "Toast", LocalDate.now());
        String id = entry.getId();
        repository.delete(entry);
        assertTrue(repository.findById(id).isEmpty());
    }
}
