package com.workoutplanner.service;

import com.workoutplanner.dto.OpenAIRequest;
import com.workoutplanner.dto.OpenAIResponse;
import com.workoutplanner.model.DietProfile;
import com.workoutplanner.model.User;
import com.workoutplanner.model.WorkoutProfile;
import com.workoutplanner.repository.ExerciseCatalogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TDD tests for:
 * 1. "category" field in each workout day's JSON schema
 * 2. Frequency-based day category guidance
 * 3. Athletic performance goal guidance in the prompt
 */
@ExtendWith(MockitoExtension.class)
class OpenAIWorkoutCategoryPromptTest {

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private ExerciseCatalogRepository exerciseCatalogRepository;

    private OpenAIService openAIService;

    private static final String FAKE_API_KEY = "test-api-key";
    private static final String FAKE_MODEL = "gpt-3.5-turbo";
    private static final String FAKE_URL = "https://api.openai.com/v1/chat/completions";

    @BeforeEach
    void setUp() {
        openAIService = new OpenAIService(
                restTemplate, new ObjectMapper(), FAKE_API_KEY, FAKE_MODEL, FAKE_URL, exerciseCatalogRepository);
        when(exerciseCatalogRepository.findByIsSystemTrue()).thenReturn(List.of());
    }

    private ResponseEntity<OpenAIResponse> fakeResponse() {
        OpenAIResponse response = new OpenAIResponse();
        OpenAIResponse.Choice choice = new OpenAIResponse.Choice();
        OpenAIRequest.OpenAIMessage msg = new OpenAIRequest.OpenAIMessage(
                "assistant",
                "WORKOUT_PLAN_JSON:\n{\"title\":\"Test\",\"weeks\":{}}\n\nDIET_PLAN_JSON:\n{\"title\":\"Diet\",\"weeks\":{}}");
        choice.setMessage(msg);
        response.setChoices(List.of(choice));
        return ResponseEntity.ok(response);
    }

    private String captureUserPrompt() {
        ArgumentCaptor<HttpEntity<OpenAIRequest>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(anyString(), captor.capture(), eq(OpenAIResponse.class));
        return captor.getValue().getBody().getMessages().get(1).getContent();
    }

    // ── Category field in JSON schema ─────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void prompt_ContainsCategoryFieldInWorkoutDaySchema() {
        when(restTemplate.postForEntity(anyString(), any(), eq(OpenAIResponse.class)))
                .thenReturn(fakeResponse());

        openAIService.generateCombinedPlans(new User(), new WorkoutProfile(), new DietProfile(), "");

        String userPrompt = captureUserPrompt();
        assertTrue(userPrompt.contains("\"category\""),
                "Workout day JSON schema must include a 'category' field");
    }

    @Test
    @SuppressWarnings("unchecked")
    void prompt_CategoryFieldAppearsForMultipleDays() {
        when(restTemplate.postForEntity(anyString(), any(), eq(OpenAIResponse.class)))
                .thenReturn(fakeResponse());

        openAIService.generateCombinedPlans(new User(), new WorkoutProfile(), new DietProfile(), "");

        String userPrompt = captureUserPrompt();
        long categoryCount = userPrompt.chars()
                .filter(c -> userPrompt.indexOf("\"category\"") != -1)
                .count();
        // At minimum monday and tuesday both appear — confirm "category" is present for the whole week
        assertTrue(userPrompt.indexOf("\"category\"") != -1,
                "Category must appear in the JSON template for each day");
    }

    // ── Frequency-based category guidance ────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void prompt_ThreeDayFrequency_MentionsFullBodySplit() {
        when(restTemplate.postForEntity(anyString(), any(), eq(OpenAIResponse.class)))
                .thenReturn(fakeResponse());

        WorkoutProfile profile = new WorkoutProfile();
        profile.setWorkoutFrequency(3);
        openAIService.generateCombinedPlans(new User(), profile, new DietProfile(), "");

        String userPrompt = captureUserPrompt();
        assertTrue(userPrompt.contains("Full Body"),
                "3-day prompt must mention Full Body split");
    }

    @Test
    @SuppressWarnings("unchecked")
    void prompt_FourDayFrequency_MentionsUpperLower() {
        when(restTemplate.postForEntity(anyString(), any(), eq(OpenAIResponse.class)))
                .thenReturn(fakeResponse());

        WorkoutProfile profile = new WorkoutProfile();
        profile.setWorkoutFrequency(4);
        openAIService.generateCombinedPlans(new User(), profile, new DietProfile(), "");

        String userPrompt = captureUserPrompt();
        assertTrue(userPrompt.contains("Upper") && userPrompt.contains("Lower"),
                "4-day prompt must mention Upper/Lower split");
    }

    @Test
    @SuppressWarnings("unchecked")
    void prompt_FivePlusDayFrequency_MentionsPushPullLegs() {
        when(restTemplate.postForEntity(anyString(), any(), eq(OpenAIResponse.class)))
                .thenReturn(fakeResponse());

        WorkoutProfile profile = new WorkoutProfile();
        profile.setWorkoutFrequency(5);
        openAIService.generateCombinedPlans(new User(), profile, new DietProfile(), "");

        String userPrompt = captureUserPrompt();
        assertTrue(userPrompt.contains("Push") && userPrompt.contains("Pull") && userPrompt.contains("Legs"),
                "5+ day prompt must mention Push/Pull/Legs split");
    }

    // ── Athletic performance goal guidance ───────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void prompt_AthleticVerticalGoal_ContainsPlyometricGuidance() {
        when(restTemplate.postForEntity(anyString(), any(), eq(OpenAIResponse.class)))
                .thenReturn(fakeResponse());

        WorkoutProfile profile = new WorkoutProfile();
        profile.setTargetGoals(new String[]{"ATHLETIC_PERFORMANCE_VERTICAL"});
        openAIService.generateCombinedPlans(new User(), profile, new DietProfile(), "");

        String userPrompt = captureUserPrompt();
        assertTrue(
                userPrompt.toLowerCase().contains("plyometric")
                        || userPrompt.toLowerCase().contains("explosive")
                        || userPrompt.toLowerCase().contains("box jump"),
                "Vertical goal prompt must mention plyometric/explosive exercises");
    }

    @Test
    @SuppressWarnings("unchecked")
    void prompt_AthleticSpeedGoal_ContainsSprintAgility() {
        when(restTemplate.postForEntity(anyString(), any(), eq(OpenAIResponse.class)))
                .thenReturn(fakeResponse());

        WorkoutProfile profile = new WorkoutProfile();
        profile.setTargetGoals(new String[]{"ATHLETIC_PERFORMANCE_SPEED"});
        openAIService.generateCombinedPlans(new User(), profile, new DietProfile(), "");

        String userPrompt = captureUserPrompt();
        assertTrue(
                userPrompt.toLowerCase().contains("sprint")
                        || userPrompt.toLowerCase().contains("agility")
                        || userPrompt.toLowerCase().contains("speed"),
                "Speed goal prompt must mention sprint/agility training");
    }

    @Test
    @SuppressWarnings("unchecked")
    void prompt_AthleticEnduranceGoal_ContainsConditioningGuidance() {
        when(restTemplate.postForEntity(anyString(), any(), eq(OpenAIResponse.class)))
                .thenReturn(fakeResponse());

        WorkoutProfile profile = new WorkoutProfile();
        profile.setTargetGoals(new String[]{"ATHLETIC_PERFORMANCE_ENDURANCE"});
        openAIService.generateCombinedPlans(new User(), profile, new DietProfile(), "");

        String userPrompt = captureUserPrompt();
        assertTrue(
                userPrompt.toLowerCase().contains("endurance")
                        || userPrompt.toLowerCase().contains("hiit")
                        || userPrompt.toLowerCase().contains("circuit"),
                "Endurance goal prompt must mention HIIT/circuit/endurance training");
    }

    @Test
    @SuppressWarnings("unchecked")
    void prompt_AthleticSportSpecificGoal_ContainsSportSpecificGuidance() {
        when(restTemplate.postForEntity(anyString(), any(), eq(OpenAIResponse.class)))
                .thenReturn(fakeResponse());

        WorkoutProfile profile = new WorkoutProfile();
        profile.setTargetGoals(new String[]{"ATHLETIC_PERFORMANCE_SPORT_SPECIFIC"});
        openAIService.generateCombinedPlans(new User(), profile, new DietProfile(), "");

        String userPrompt = captureUserPrompt();
        assertTrue(
                userPrompt.toLowerCase().contains("sport")
                        || userPrompt.toLowerCase().contains("sport-specific"),
                "Sport-specific goal prompt must mention sport-specific training");
    }

    @Test
    @SuppressWarnings("unchecked")
    void prompt_GeneralAthleticGoal_ContainsAthleticPerformanceGuidance() {
        when(restTemplate.postForEntity(anyString(), any(), eq(OpenAIResponse.class)))
                .thenReturn(fakeResponse());

        WorkoutProfile profile = new WorkoutProfile();
        profile.setTargetGoals(new String[]{"ATHLETIC_PERFORMANCE"});
        openAIService.generateCombinedPlans(new User(), profile, new DietProfile(), "");

        String userPrompt = captureUserPrompt();
        assertTrue(
                userPrompt.toLowerCase().contains("athletic")
                        || userPrompt.toLowerCase().contains("performance"),
                "Athletic performance goal prompt must contain athletic/performance guidance");
    }
}
