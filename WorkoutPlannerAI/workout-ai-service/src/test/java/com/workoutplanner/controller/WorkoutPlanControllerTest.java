package com.workoutplanner.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workoutplanner.model.*;
import com.workoutplanner.service.OpenAIService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WorkoutPlanController.class)
class WorkoutPlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OpenAIService openAIService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserProfile validUserProfile;
    private WorkoutPlan mockWorkoutPlan;

    @BeforeEach
    void setUp() {
        validUserProfile = new UserProfile(25, Equipment.NONE, 3);

        List<Exercise> exercises = Arrays.asList(
                new Exercise("Push-ups", "3", "10-15", "60s", "Standard push-ups"),
                new Exercise("Squats", "3", "15-20", "60s", "Bodyweight squats")
        );

        List<WorkoutDay> workoutDays = Arrays.asList(
                new WorkoutDay(1, "MONDAY - Upper Body", exercises, 45, "Focus on proper form"),
                new WorkoutDay(2, "TUESDAY - REST DAY", Arrays.asList(), 0, "Complete rest"),
                new WorkoutDay(3, "WEDNESDAY - Lower Body", exercises, 45, "Focus on leg muscles"),
                new WorkoutDay(4, "THURSDAY - REST DAY", Arrays.asList(), 0, "Light stretching"),
                new WorkoutDay(5, "FRIDAY - Full Body", exercises, 45, "Combined workout"),
                new WorkoutDay(6, "SATURDAY - REST DAY", Arrays.asList(), 0, "Recovery day"),
                new WorkoutDay(7, "SUNDAY - REST DAY", Arrays.asList(), 0, "Complete rest")
        );

        mockWorkoutPlan = new WorkoutPlan(validUserProfile, workoutDays, "Mock AI response");
    }

    @Test
    void generateWorkoutPlan_WithValidUserProfile_ShouldReturnWorkoutPlan() throws Exception {
        when(openAIService.generateWorkoutPlan(any(UserProfile.class)))
                .thenReturn(Mono.just(mockWorkoutPlan));

        mockMvc.perform(post("/api/v1/workout-plans/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUserProfile)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.user_profile.age").value(25))
                .andExpect(jsonPath("$.user_profile.weekly_frequency").value(3))
                .andExpect(jsonPath("$.weekly_schedule").isArray())
                .andExpect(jsonPath("$.weekly_schedule.length()").value(7));
    }

    @Test
    void generateWorkoutPlan_WithInvalidAge_ShouldReturnBadRequest() throws Exception {
        UserProfile invalidProfile = new UserProfile(15, Equipment.NONE, 3); // Age below minimum

        mockMvc.perform(post("/api/v1/workout-plans/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidProfile)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generateWorkoutPlan_WithInvalidFrequency_ShouldReturnBadRequest() throws Exception {
        UserProfile invalidProfile = new UserProfile(25, Equipment.NONE, 8); // Frequency above maximum

        mockMvc.perform(post("/api/v1/workout-plans/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidProfile)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generateWorkoutPlan_WithNullRequiredFields_ShouldReturnBadRequest() throws Exception {
        UserProfile invalidProfile = new UserProfile();
        // Not setting required fields

        mockMvc.perform(post("/api/v1/workout-plans/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidProfile)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generateWorkoutPlan_WhenServiceThrowsException_ShouldReturnInternalServerError() throws Exception {
        when(openAIService.generateWorkoutPlan(any(UserProfile.class)))
                .thenReturn(Mono.error(new RuntimeException("OpenAI API error")));

        mockMvc.perform(post("/api/v1/workout-plans/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUserProfile)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void health_ShouldReturnOkWithHealthMessage() throws Exception {
        mockMvc.perform(get("/api/v1/workout-plans/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Workout AI Service is running"));
    }

    @Test
    void validateUserProfile_WithValidProfile_ShouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/v1/workout-plans/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validUserProfile)))
                .andExpect(status().isOk())
                .andExpect(content().string("User profile is valid"));
    }

    @Test
    void validateUserProfile_WithInvalidProfile_ShouldReturnBadRequest() throws Exception {
        UserProfile invalidProfile = new UserProfile();

        mockMvc.perform(post("/api/v1/workout-plans/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidProfile)))
                .andExpect(status().isBadRequest());
    }
}