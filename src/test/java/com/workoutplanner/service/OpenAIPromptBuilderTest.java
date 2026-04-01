package com.workoutplanner.service;

import com.workoutplanner.model.DietProfile;
import com.workoutplanner.model.User;
import com.workoutplanner.model.WorkoutProfile;
import com.workoutplanner.dto.OpenAIRequest;
import com.workoutplanner.repository.ExerciseCatalogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workoutplanner.dto.OpenAIResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * P1-011: Tests for OpenAIService feedback block injection into system prompt.
 */
@ExtendWith(MockitoExtension.class)
class OpenAIPromptBuilderTest {

    private static final Logger log = LoggerFactory.getLogger(OpenAIPromptBuilderTest.class);

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
    }

    // Builds a minimal OpenAI response
    private ResponseEntity<OpenAIResponse> fakeResponse(String content) {
        OpenAIResponse response = new OpenAIResponse();
        OpenAIResponse.Choice choice = new OpenAIResponse.Choice();
        OpenAIRequest.OpenAIMessage msg = new OpenAIRequest.OpenAIMessage("assistant", content);
        choice.setMessage(msg);
        response.setChoices(List.of(choice));
        return ResponseEntity.ok(response);
    }

    private String minimalCombinedJson() {
        return "WORKOUT_PLAN_JSON:\n{\"title\":\"Test\",\"weeks\":{}}\n\n" +
               "DIET_PLAN_JSON:\n{\"title\":\"Diet\",\"weeks\":{}}";
    }

    @Test
    @SuppressWarnings("unchecked")
    void generateCombinedPlans_NoFeedback_SystemPromptHasNoFeedbackBlock() {
        when(restTemplate.postForEntity(anyString(), any(), eq(OpenAIResponse.class)))
                .thenReturn(fakeResponse(minimalCombinedJson()));

        openAIService.generateCombinedPlans(new User(), new WorkoutProfile(), new DietProfile(), "");

        ArgumentCaptor<HttpEntity<OpenAIRequest>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(anyString(), captor.capture(), eq(OpenAIResponse.class));

        String systemPrompt = captor.getValue().getBody().getMessages().get(0).getContent();
        log.info("test.noFeedback systemPrompt_length={}", systemPrompt.length());
        assertFalse(systemPrompt.contains("Previous Week Feedback"),
                "System prompt should NOT contain feedback block when no feedback provided");
    }

    @Test
    @SuppressWarnings("unchecked")
    void generateCombinedPlans_WithFeedbackBlock_SystemPromptContainsFeedback() {
        when(restTemplate.postForEntity(anyString(), any(), eq(OpenAIResponse.class)))
                .thenReturn(fakeResponse(minimalCombinedJson()));

        String feedbackBlock = "Previous Week Feedback:\n- Bench Press: completed 3x10@135lbs. Rated JUST_RIGHT. Suggest: increase to 140lbs.";

        openAIService.generateCombinedPlans(new User(), new WorkoutProfile(), new DietProfile(), feedbackBlock);

        ArgumentCaptor<HttpEntity<OpenAIRequest>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(anyString(), captor.capture(), eq(OpenAIResponse.class));

        String systemPrompt = captor.getValue().getBody().getMessages().get(0).getContent();
        log.info("test.withFeedback systemPrompt_contains_feedback={}", systemPrompt.contains("Bench Press"));
        assertTrue(systemPrompt.contains("Previous Week Feedback"),
                "System prompt should contain feedback block");
        assertTrue(systemPrompt.contains("Bench Press"));
        assertTrue(systemPrompt.contains("JUST_RIGHT"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void generateCombinedPlans_WithPainFlagFeedback_SystemPromptContainsPainFlag() {
        when(restTemplate.postForEntity(anyString(), any(), eq(OpenAIResponse.class)))
                .thenReturn(fakeResponse(minimalCombinedJson()));

        String feedbackBlock = "Previous Week Feedback:\n- Squat: PAIN FLAG. Comment: lower back tightness. Suggest: substitute with alternative exercise.";

        openAIService.generateCombinedPlans(new User(), new WorkoutProfile(), new DietProfile(), feedbackBlock);

        ArgumentCaptor<HttpEntity<OpenAIRequest>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(anyString(), captor.capture(), eq(OpenAIResponse.class));

        String systemPrompt = captor.getValue().getBody().getMessages().get(0).getContent();
        log.info("test.painFlag systemPrompt_contains_pain_flag={}", systemPrompt.contains("PAIN FLAG"));
        assertTrue(systemPrompt.contains("PAIN FLAG"));
        assertTrue(systemPrompt.contains("substitute"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void generateCombinedPlans_WithMealFeedback_SystemPromptContainsMealFeedback() {
        when(restTemplate.postForEntity(anyString(), any(), eq(OpenAIResponse.class)))
                .thenReturn(fakeResponse(minimalCombinedJson()));

        String feedbackBlock = "Previous Week Feedback:\n- Grilled Salmon: DISLIKED. Suggest: replace with different option in the same meal slot.";

        openAIService.generateCombinedPlans(new User(), new WorkoutProfile(), new DietProfile(), feedbackBlock);

        ArgumentCaptor<HttpEntity<OpenAIRequest>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(anyString(), captor.capture(), eq(OpenAIResponse.class));

        String systemPrompt = captor.getValue().getBody().getMessages().get(0).getContent();
        log.info("test.mealFeedback systemPrompt_contains_meal={}", systemPrompt.contains("Grilled Salmon"));
        assertTrue(systemPrompt.contains("Grilled Salmon"));
        assertTrue(systemPrompt.contains("DISLIKED"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void generateCombinedPlans_NullFeedback_TreatedAsEmpty() {
        when(restTemplate.postForEntity(anyString(), any(), eq(OpenAIResponse.class)))
                .thenReturn(fakeResponse(minimalCombinedJson()));

        openAIService.generateCombinedPlans(new User(), new WorkoutProfile(), new DietProfile(), null);

        ArgumentCaptor<HttpEntity<OpenAIRequest>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(anyString(), captor.capture(), eq(OpenAIResponse.class));

        String systemPrompt = captor.getValue().getBody().getMessages().get(0).getContent();
        assertFalse(systemPrompt.contains("Previous Week Feedback"),
                "Null feedback should not inject feedback block");
    }

    @Test
    @SuppressWarnings("unchecked")
    void generateCombinedPlans_WithMixedFeedback_AllEntriesPresent() {
        when(restTemplate.postForEntity(anyString(), any(), eq(OpenAIResponse.class)))
                .thenReturn(fakeResponse(minimalCombinedJson()));

        String feedbackBlock = "Previous Week Feedback:\n" +
                "- Bench Press: completed 3x10@135lbs. Rated JUST_RIGHT. Suggest: increase to 140lbs.\n" +
                "- Deadlift: PAIN FLAG. Comment: lower back tightness. Suggest: substitute with alternative exercise.\n" +
                "- Grilled Salmon: DISLIKED. Suggest: replace with different option in the same meal slot.";

        openAIService.generateCombinedPlans(new User(), new WorkoutProfile(), new DietProfile(), feedbackBlock);

        ArgumentCaptor<HttpEntity<OpenAIRequest>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(anyString(), captor.capture(), eq(OpenAIResponse.class));

        String systemPrompt = captor.getValue().getBody().getMessages().get(0).getContent();
        log.info("test.mixedFeedback systemPrompt_length={}", systemPrompt.length());
        assertTrue(systemPrompt.contains("Bench Press"));
        assertTrue(systemPrompt.contains("Deadlift"));
        assertTrue(systemPrompt.contains("Grilled Salmon"));
    }

    @Test
    void generateCombinedPlans_NoApiKey_ThrowsException() {
        OpenAIService serviceNoKey = new OpenAIService(
                restTemplate, new ObjectMapper(), "", FAKE_MODEL, FAKE_URL, exerciseCatalogRepository);

        assertThrows(RuntimeException.class,
                () -> serviceNoKey.generateCombinedPlans(new User(), new WorkoutProfile(), new DietProfile(), ""));
    }
}
