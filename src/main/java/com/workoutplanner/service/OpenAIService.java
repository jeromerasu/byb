package com.workoutplanner.service;

import com.workoutplanner.dto.OpenAIRequest;
import com.workoutplanner.dto.OpenAIResponse;
import com.workoutplanner.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class OpenAIService {

    private final WebClient webClient;
    private final String apiKey;

    public OpenAIService(@Value("${openai.api.key}") String apiKey) {
        // Validate and clean the API key
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.equals("your-openai-api-key-here")) {
            throw new IllegalArgumentException("OpenAI API key is not properly configured. Please set OPENAI_API_KEY environment variable.");
        }

        this.apiKey = apiKey.trim();

        // Validate API key format
        if (!this.apiKey.startsWith("sk-")) {
            throw new IllegalArgumentException("Invalid OpenAI API key format. API key should start with 'sk-'");
        }

        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public Mono<WorkoutPlan> generateWorkoutPlan(UserProfile profile) {
        String prompt = constructPrompt(profile);

        return makeOpenAIRequest(prompt)
                .map(aiResponse -> {
                    List<WorkoutDay> workoutDays = parseWorkoutDays(aiResponse, profile.getWeeklyFrequency());
                    return new WorkoutPlan(profile, workoutDays, aiResponse);
                });
    }

    private String constructPrompt(UserProfile profile) {
        return String.format("""
                Create a personalized workout plan for a %d-year-old person with the following constraints:

                Equipment available: %s
                Workout frequency: %d days per week

                IMPORTANT: Provide a COMPLETE 7-day weekly schedule. For workout days, include full exercise details. For rest days, clearly mark them as "REST DAY" with recovery activities.

                Format for each day of the week:

                DAY 1: [MONDAY - Workout Type or REST DAY]
                Estimated Duration: [X] minutes
                EXERCISES: (or REST ACTIVITIES for rest days)
                1. [Exercise Name]
                   - Sets: [number]
                   - Reps: [number or range]
                   - Rest: [time]
                   - Notes: [any specific instructions]

                DAY 2: [TUESDAY - Workout Type or REST DAY]
                ... continue for all 7 days

                Guidelines:
                - Include exactly 7 days (Monday through Sunday)
                - Mark non-workout days as "REST DAY" with light activities like walking, stretching, or complete rest
                - Distribute %d workout days throughout the week with proper rest spacing
                - Include safety considerations for this age group
                - Focus on balanced muscle group targeting across workout days
                - Progressive overload suggestions for workout days

                Example rest day format:
                DAY X: REST DAY
                Estimated Duration: 20-30 minutes
                ACTIVITIES:
                1. Light walking or stretching
                2. Hydration and nutrition focus
                3. Sleep quality emphasis
                """,
                profile.getAge(),
                profile.getEquipment().getDisplayName(),
                profile.getWeeklyFrequency(),
                profile.getWeeklyFrequency());
    }

    private Mono<String> makeOpenAIRequest(String prompt) {
        OpenAIRequest request = new OpenAIRequest(
                "gpt-3.5-turbo",
                Arrays.asList(
                        new OpenAIRequest.OpenAIMessage("system", "You are a professional fitness trainer creating personalized workout plans."),
                        new OpenAIRequest.OpenAIMessage("user", prompt)
                ),
                0.7,
                2000
        );

        return webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OpenAIResponse.class)
                .map(response -> {
                    if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                        return response.getChoices().get(0).getMessage().getContent();
                    }
                    throw new RuntimeException("No response from OpenAI");
                })
                .onErrorMap(WebClientResponseException.class, ex ->
                        new RuntimeException("OpenAI API error: " + ex.getResponseBodyAsString(), ex));
    }

    private List<WorkoutDay> parseWorkoutDays(String response, Integer frequency) {
        List<WorkoutDay> workoutDays = new ArrayList<>();
        String[] lines = response.split("\n");

        Integer currentDay = null;
        String currentDayName = null;
        List<Exercise> currentExercises = new ArrayList<>();
        int estimatedDuration = 45;

        Pattern dayPattern = Pattern.compile("^\\*{0,2}DAY\\s+(\\d+):");

        for (String line : lines) {
            String trimmedLine = line.trim();

            var dayMatcher = dayPattern.matcher(trimmedLine);
            if (dayMatcher.find()) {
                // Save previous day if exists
                if (currentDay != null && currentDayName != null) {
                    workoutDays.add(new WorkoutDay(
                            currentDay,
                            currentDayName,
                            new ArrayList<>(currentExercises),
                            estimatedDuration,
                            null
                    ));
                }

                // Parse new day
                currentDay = Integer.parseInt(dayMatcher.group(1));
                String[] parts = trimmedLine.split(":", 2);
                if (parts.length > 1) {
                    currentDayName = parts[1].trim().replaceAll("\\*{1,2}", "").trim();
                }
                currentExercises.clear();
            } else if (trimmedLine.startsWith("Estimated Duration:")) {
                String durationStr = trimmedLine.replaceAll("Estimated Duration:|minutes", "").trim();
                try {
                    estimatedDuration = Integer.parseInt(durationStr.replaceAll("[^\\d]", ""));
                } catch (NumberFormatException e) {
                    estimatedDuration = 45; // default
                }
            } else if (trimmedLine.matches("^\\d+\\..*")) {
                // Parse exercise
                String exerciseName = trimmedLine.replaceAll("^\\d+\\.", "").trim();
                currentExercises.add(new Exercise(
                        exerciseName,
                        "3",
                        "8-12",
                        "60s",
                        null
                ));
            }
        }

        // Save last day
        if (currentDay != null && currentDayName != null) {
            workoutDays.add(new WorkoutDay(
                    currentDay,
                    currentDayName,
                    new ArrayList<>(currentExercises),
                    estimatedDuration,
                    null
            ));
        }

        // If no days were parsed, throw an error
        if (workoutDays.isEmpty()) {
            throw new RuntimeException("Failed to parse workout plan from OpenAI response");
        }

        // Return all 7 days or throw error if not complete
        if (workoutDays.size() != 7) {
            throw new RuntimeException("Incomplete workout plan received - expected 7 days, got " + workoutDays.size());
        }

        return workoutDays;
    }

}