package com.workoutplanner.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WorkoutPlanTest {

    private UserProfile testUserProfile;
    private List<WorkoutDay> testWorkoutDays;
    private String testAiResponse;

    @BeforeEach
    void setUp() {
        testUserProfile = new UserProfile(25, Equipment.NONE, 3);

        List<Exercise> exercises = Arrays.asList(
                new Exercise("Push-ups", "3", "10-15", "60s", "Standard push-ups"),
                new Exercise("Squats", "3", "15-20", "60s", "Bodyweight squats")
        );

        testWorkoutDays = Arrays.asList(
                new WorkoutDay(1, "MONDAY - Upper Body", exercises, 45, "Focus on proper form"),
                new WorkoutDay(2, "TUESDAY - REST DAY", Arrays.asList(), 0, "Complete rest"),
                new WorkoutDay(3, "WEDNESDAY - Lower Body", exercises, 45, "Focus on leg muscles"),
                new WorkoutDay(4, "THURSDAY - REST DAY", Arrays.asList(), 0, "Light stretching"),
                new WorkoutDay(5, "FRIDAY - Full Body", exercises, 45, "Combined workout"),
                new WorkoutDay(6, "SATURDAY - REST DAY", Arrays.asList(), 0, "Recovery day"),
                new WorkoutDay(7, "SUNDAY - REST DAY", Arrays.asList(), 0, "Complete rest")
        );

        testAiResponse = "Mock AI response for workout plan generation";
    }

    @Test
    void constructor_DefaultConstructor_ShouldSetGeneratedAt() {
        WorkoutPlan plan = new WorkoutPlan();

        assertNotNull(plan.getGeneratedAt());
        assertTrue(plan.getGeneratedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void constructor_WithParameters_ShouldSetAllFieldsCorrectly() {
        WorkoutPlan plan = new WorkoutPlan(testUserProfile, testWorkoutDays, testAiResponse);

        assertNotNull(plan.getId());
        assertEquals(testUserProfile, plan.getUserProfile());
        assertEquals(testWorkoutDays, plan.getWeeklySchedule());
        assertEquals(testAiResponse, plan.getAiResponse());
        assertNotNull(plan.getGeneratedAt());
        assertTrue(plan.getGeneratedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertNotNull(plan.getTitle());
        assertTrue(plan.getTitle().startsWith("Workout Plan - "));
    }

    @Test
    void constructor_WithParameters_ShouldGenerateUniqueIds() {
        WorkoutPlan plan1 = new WorkoutPlan(testUserProfile, testWorkoutDays, testAiResponse);
        WorkoutPlan plan2 = new WorkoutPlan(testUserProfile, testWorkoutDays, testAiResponse);

        assertNotNull(plan1.getId());
        assertNotNull(plan2.getId());
        assertNotEquals(plan1.getId(), plan2.getId());
    }

    @Test
    void settersAndGetters_ShouldWorkCorrectly() {
        WorkoutPlan plan = new WorkoutPlan();
        String id = "test-id-123";
        LocalDateTime generatedAt = LocalDateTime.now().minusDays(1);
        String title = "Custom Workout Plan";

        plan.setId(id);
        plan.setUserProfile(testUserProfile);
        plan.setWeeklySchedule(testWorkoutDays);
        plan.setAiResponse(testAiResponse);
        plan.setGeneratedAt(generatedAt);
        plan.setTitle(title);

        assertEquals(id, plan.getId());
        assertEquals(testUserProfile, plan.getUserProfile());
        assertEquals(testWorkoutDays, plan.getWeeklySchedule());
        assertEquals(testAiResponse, plan.getAiResponse());
        assertEquals(generatedAt, plan.getGeneratedAt());
        assertEquals(title, plan.getTitle());
    }

    @Test
    void constructor_WithNullUserProfile_ShouldStillWork() {
        WorkoutPlan plan = new WorkoutPlan(null, testWorkoutDays, testAiResponse);

        assertNotNull(plan.getId());
        assertNull(plan.getUserProfile());
        assertEquals(testWorkoutDays, plan.getWeeklySchedule());
        assertEquals(testAiResponse, plan.getAiResponse());
    }

    @Test
    void constructor_WithNullWorkoutDays_ShouldStillWork() {
        WorkoutPlan plan = new WorkoutPlan(testUserProfile, null, testAiResponse);

        assertNotNull(plan.getId());
        assertEquals(testUserProfile, plan.getUserProfile());
        assertNull(plan.getWeeklySchedule());
        assertEquals(testAiResponse, plan.getAiResponse());
    }

    @Test
    void constructor_WithNullAiResponse_ShouldStillWork() {
        WorkoutPlan plan = new WorkoutPlan(testUserProfile, testWorkoutDays, null);

        assertNotNull(plan.getId());
        assertEquals(testUserProfile, plan.getUserProfile());
        assertEquals(testWorkoutDays, plan.getWeeklySchedule());
        assertNull(plan.getAiResponse());
    }
}