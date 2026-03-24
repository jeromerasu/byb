package com.workoutplanner.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workoutplanner.dto.OpenAIRequest;
import com.workoutplanner.dto.OpenAIResponse;
import com.workoutplanner.model.DietProfile;
import com.workoutplanner.model.WorkoutProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Service
public class OpenAIService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String openaiApiKey;
    private final String openaiModel;
    private final String openaiApiUrl;

    public OpenAIService(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${openai.api.key:}") String openaiApiKey,
            @Value("${openai.model:gpt-3.5-turbo}") String openaiModel,
            @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}") String openaiApiUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.openaiApiKey = openaiApiKey;
        this.openaiModel = openaiModel;
        this.openaiApiUrl = openaiApiUrl;
    }

    public CombinedPlanResult generateCombinedPlans(WorkoutProfile workoutProfile, DietProfile dietProfile) {
        if (openaiApiKey == null || openaiApiKey.trim().isEmpty()) {
            throw new RuntimeException("OpenAI API key not configured");
        }

        try {
            // Create the prompt for combined plan generation
            String prompt = buildCombinedPrompt(workoutProfile, dietProfile);

            // Build OpenAI request
            OpenAIRequest request = new OpenAIRequest();
            request.setModel(openaiModel);
            request.setTemperature(0.7);
            request.setMaxTokens(4000);
            request.setMessages(Arrays.asList(
                new OpenAIRequest.OpenAIMessage("system", getSystemPrompt()),
                new OpenAIRequest.OpenAIMessage("user", prompt)
            ));

            // Make API call
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);

            HttpEntity<OpenAIRequest> entity = new HttpEntity<>(request, headers);
            ResponseEntity<OpenAIResponse> response = restTemplate.postForEntity(
                openaiApiUrl, entity, OpenAIResponse.class);

            if (response.getBody() == null || response.getBody().getChoices().isEmpty()) {
                throw new RuntimeException("Empty response from OpenAI API");
            }

            // Extract and parse the response
            String content = response.getBody().getChoices().get(0).getMessage().getContent();
            return parseCombinedResponse(content);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate plans with OpenAI: " + e.getMessage(), e);
        }
    }

    private String getSystemPrompt() {
        return "You are a professional fitness and nutrition AI. You must respond with exactly TWO separate JSON objects, clearly separated. " +
               "First provide a workout plan JSON, then provide a diet plan JSON. Use the following format:\n\n" +
               "WORKOUT_PLAN_JSON:\n{workout plan here}\n\n" +
               "DIET_PLAN_JSON:\n{diet plan here}\n\n" +
               "CRITICAL REQUIREMENTS:\n" +
               "- Each JSON must be valid and complete - NO placeholders like [...] or {...}\n" +
               "- Generate COMPLETE data for all 4 weeks and all 7 days\n" +
               "- NEVER use empty arrays [] for exercises or meals - always provide at least one item\n" +
               "- Do NOT use shorthand notation, placeholders, or incomplete structures\n" +
               "- Ensure all JSON is properly formatted without any markdown code blocks\n" +
               "- Every day must have complete exercise/meal data, not references or shortcuts\n" +
               "- If a day is a rest day, include a proper rest exercise/meal object\n" +
               "- Do NOT truncate or cut off the JSON response - complete all structures fully";
    }

    private String buildCombinedPrompt(WorkoutProfile workoutProfile, DietProfile dietProfile) {
        return String.format(
            "Create a personalized 4-week fitness and nutrition plan for:\n\n" +
            "**User Profile:**\n" +
            "- Age: %d, Gender: %s\n" +
            "- Weight: %.1fkg, Height: %dcm\n" +
            "- Fitness Level: %s\n" +
            "- Goals: %s\n" +
            "- Workout Frequency: %d times/week\n" +
            "- Session Duration: %d minutes\n" +
            "- Available Equipment: %s\n" +
            "- Diet Type: %s\n" +
            "- Daily Calorie Goal: %d calories\n" +
            "- Meals Per Day: %d\n" +
            "- Allergies: %s\n" +
            "- Dietary Restrictions: %s\n\n" +

            "Return TWO separate JSON objects:\n\n" +

            "Generate a complete 4-week workout plan with this exact structure (no placeholders):\n\n" +
            "WORKOUT_PLAN_JSON:\n" +
            "{\n" +
            "  \"title\": \"4-Week Workout Plan\",\n" +
            "  \"weeks\": {\n" +
            "    \"week_1\": {\n" +
            "      \"day_1\": {\n" +
            "        \"exercises\": [\n" +
            "          {\n" +
            "            \"name\": \"Exercise Name\",\n" +
            "            \"sets\": 3,\n" +
            "            \"reps\": 12,\n" +
            "            \"weight_type\": \"bodyweight|dumbbell|time_seconds\",\n" +
            "            \"muscle_groups\": [\"chest\", \"triceps\"],\n" +
            "            \"instructions\": \"Detailed instructions\"\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}\n" +
            "IMPORTANT: Expand this structure completely for all 4 weeks (week_1, week_2, week_3, week_4) and all 7 days (day_1 through day_7) with full exercise details for each day.\n\n" +

            "Generate a complete 4-week nutrition plan with this exact structure (no placeholders):\n\n" +
            "DIET_PLAN_JSON:\n" +
            "{\n" +
            "  \"title\": \"4-Week Nutrition Plan\",\n" +
            "  \"weeks\": {\n" +
            "    \"week_1\": {\n" +
            "      \"day_1\": {\n" +
            "        \"meals\": [\n" +
            "          {\n" +
            "            \"meal_type\": \"breakfast|lunch|dinner|snack\",\n" +
            "            \"name\": \"Meal Name\",\n" +
            "            \"ingredients\": [\"ingredient1\", \"ingredient2\"],\n" +
            "            \"calories\": 400,\n" +
            "            \"proteins\": 25,\n" +
            "            \"carbs\": 45,\n" +
            "            \"fats\": 12,\n" +
            "            \"preparation_time\": 15,\n" +
            "            \"instructions\": \"Preparation instructions\"\n" +
            "          }\n" +
            "        ],\n" +
            "        \"daily_totals\": {\n" +
            "          \"calories\": 2000,\n" +
            "          \"proteins\": 150,\n" +
            "          \"carbs\": 200,\n" +
            "          \"fats\": 65\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}\n" +
            "IMPORTANT: Expand this structure completely for all 4 weeks (week_1, week_2, week_3, week_4) and all 7 days (day_1 through day_7) with full meal details and daily totals for each day.",

            workoutProfile.getAge() != null ? workoutProfile.getAge() : 25,
            workoutProfile.getGender() != null ? workoutProfile.getGender().name() : "MALE",
            workoutProfile.getWeightKg() != null ? workoutProfile.getWeightKg().doubleValue() : 70.0,
            workoutProfile.getHeightCm() != null ? workoutProfile.getHeightCm() : 175,
            workoutProfile.getFitnessLevel() != null ? workoutProfile.getFitnessLevel().name() : "BEGINNER",
            workoutProfile.getTargetGoals() != null ? String.join(", ", workoutProfile.getTargetGoals()) : "WEIGHT_LOSS",
            workoutProfile.getWorkoutFrequency() != null ? workoutProfile.getWorkoutFrequency() : 3,
            workoutProfile.getSessionDuration() != null ? workoutProfile.getSessionDuration() : 45,
            workoutProfile.getAvailableEquipment() != null ? String.join(", ", workoutProfile.getAvailableEquipment()) : "BODYWEIGHT",
            dietProfile.getDietType() != null ? dietProfile.getDietType().name() : "BALANCED",
            dietProfile.getDailyCalorieGoal() != null ? dietProfile.getDailyCalorieGoal() : 2000,
            dietProfile.getMealsPerDay() != null ? dietProfile.getMealsPerDay() : 3,
            "None",
            dietProfile.getDietaryRestrictions() != null && dietProfile.getDietaryRestrictions().length > 0 ? String.join(", ", dietProfile.getDietaryRestrictions()) : "None"
        );
    }

    private CombinedPlanResult parseCombinedResponse(String content) {
        try {
            // Find the workout plan JSON
            String workoutMarker = "WORKOUT_PLAN_JSON:";
            String dietMarker = "DIET_PLAN_JSON:";

            int workoutStart = content.indexOf(workoutMarker);
            int dietStart = content.indexOf(dietMarker);

            if (workoutStart == -1 || dietStart == -1) {
                throw new RuntimeException("Could not find required JSON markers in response");
            }

            // Extract workout plan JSON
            String workoutJsonStr = content.substring(workoutStart + workoutMarker.length(), dietStart).trim();
            workoutJsonStr = cleanJsonString(workoutJsonStr);

            // Extract diet plan JSON
            String dietJsonStr = content.substring(dietStart + dietMarker.length()).trim();
            dietJsonStr = cleanJsonString(dietJsonStr);

            // Parse JSON strings
            Map<String, Object> workoutPlan = objectMapper.readValue(workoutJsonStr, Map.class);
            Map<String, Object> dietPlan = objectMapper.readValue(dietJsonStr, Map.class);

            return new CombinedPlanResult(workoutPlan, dietPlan);

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse OpenAI response: " + e.getMessage(), e);
        }
    }

    private String cleanJsonString(String jsonStr) {
        // Remove markdown code blocks and comments
        String cleaned = jsonStr.replaceAll("```json", "")
                                .replaceAll("```", "")
                                .replaceAll("//.*$", "")  // Remove single line comments
                                .replaceAll("/\\*.*?\\*/", "")  // Remove multi-line comments
                                .trim();

        // Fix placeholder patterns
        cleaned = cleaned.replaceAll("\\[\\.\\.\\.]", "[]")  // Replace [...] placeholders
                        .replaceAll("\\{\\.\\.\\.\\}", "{}")  // Replace {...} placeholders
                        .replaceAll("\\.\\.\\.", "");  // Remove any remaining ellipsis

        // Handle empty exercise arrays - fill with rest day data
        cleaned = cleaned.replaceAll("\"exercises\":\\s*\\[\\s*\\]",
            "\"exercises\": [{\"name\": \"Rest Day\", \"sets\": 0, \"reps\": 0, \"weight_type\": \"rest\", \"muscle_groups\": [], \"instructions\": \"Rest day - no exercises\"}]");

        // Handle empty meal arrays - fill with rest day data
        cleaned = cleaned.replaceAll("\"meals\":\\s*\\[\\s*\\]",
            "\"meals\": [{\"meal_type\": \"rest\", \"name\": \"Rest Day\", \"ingredients\": [], \"calories\": 0, \"proteins\": 0, \"carbs\": 0, \"fats\": 0, \"preparation_time\": 0, \"instructions\": \"Rest day\"}]");

        // Remove any trailing commas before closing brackets/braces
        cleaned = cleaned.replaceAll(",\\s*([\\]}])", "$1");

        // Normalize whitespace
        cleaned = cleaned.replaceAll("\\n\\s*\\n", "\n")  // Remove empty lines
                        .replaceAll("^\\s+", "")
                        .replaceAll("\\s+$", "")
                        .trim();

        // Attempt to handle truncated JSON by finding the last valid closing brace
        if (!cleaned.endsWith("}")) {
            int lastBrace = cleaned.lastIndexOf("}");
            if (lastBrace > 0) {
                cleaned = cleaned.substring(0, lastBrace + 1);
            }
        }

        return cleaned;
    }

    public static class CombinedPlanResult {
        private final Map<String, Object> workoutPlan;
        private final Map<String, Object> dietPlan;

        public CombinedPlanResult(Map<String, Object> workoutPlan, Map<String, Object> dietPlan) {
            this.workoutPlan = workoutPlan;
            this.dietPlan = dietPlan;
        }

        public Map<String, Object> getWorkoutPlan() {
            return workoutPlan;
        }

        public Map<String, Object> getDietPlan() {
            return dietPlan;
        }
    }
}