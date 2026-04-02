package com.workoutplanner.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workoutplanner.dto.OpenAIRequest;
import com.workoutplanner.dto.OpenAIResponse;
import com.workoutplanner.model.DietProfile;
import com.workoutplanner.model.ExerciseCatalog;
import com.workoutplanner.model.User;
import com.workoutplanner.model.WorkoutProfile;
import com.workoutplanner.repository.ExerciseCatalogRepository;
import com.workoutplanner.strategy.ResolvedPromptContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OpenAIService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAIService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String openaiApiKey;
    private final String openaiModel;
    private final String openaiApiUrl;
    private final ExerciseCatalogRepository exerciseCatalogRepository;

    public OpenAIService(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${openai.api.key:}") String openaiApiKey,
            @Value("${openai.model:gpt-3.5-turbo}") String openaiModel,
            @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}") String openaiApiUrl,
            ExerciseCatalogRepository exerciseCatalogRepository) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.openaiApiKey = openaiApiKey;
        this.openaiModel = openaiModel;
        this.openaiApiUrl = openaiApiUrl;
        this.exerciseCatalogRepository = exerciseCatalogRepository;
    }

    public CombinedPlanResult generateCombinedPlans(User user, WorkoutProfile workoutProfile, DietProfile dietProfile) {
        return generateCombinedPlans(user, workoutProfile, dietProfile, "");
    }

    public CombinedPlanResult generateCombinedPlans(User user, WorkoutProfile workoutProfile, DietProfile dietProfile,
                                                     String feedbackBlock) {
        return generateCombinedPlans(user, workoutProfile, dietProfile, feedbackBlock, null);
    }

    /**
     * Primary generation method accepting a {@link ResolvedPromptContext}.
     * When context is non-null it overrides the configured model and base system prompt,
     * and injects any coaching directives. When null falls back to existing behaviour.
     */
    public CombinedPlanResult generateCombinedPlans(User user, WorkoutProfile workoutProfile, DietProfile dietProfile,
                                                     String feedbackBlock, ResolvedPromptContext promptContext) {
        if (openaiApiKey == null || openaiApiKey.trim().isEmpty()) {
            throw new RuntimeException("OpenAI API key not configured");
        }

        try {
            // Load system exercise names to constrain OpenAI responses
            List<String> catalogExerciseNames = exerciseCatalogRepository.findByIsSystemTrue()
                    .stream()
                    .map(ExerciseCatalog::getName)
                    .sorted()
                    .collect(Collectors.toList());

            // Create the prompt for combined plan generation
            String prompt = buildCombinedPrompt(user, workoutProfile, dietProfile, catalogExerciseNames);

            // Resolve model and system prompt from strategy context or use defaults
            String resolvedModel = (promptContext != null) ? promptContext.model() : openaiModel;
            String systemPrompt = (promptContext != null) ? promptContext.systemPrompt() : getSystemPrompt();

            // Inject coaching directives into system prompt
            if (promptContext != null && promptContext.hasDirectives()) {
                StringBuilder sb = new StringBuilder(systemPrompt);
                sb.append("\n\nCOACH DIRECTIVES (must be followed):");
                for (String directive : promptContext.directives()) {
                    sb.append("\n- ").append(directive);
                }
                systemPrompt = sb.toString();
                logger.info("openai.directives_injected count={}", promptContext.directives().size());
            }

            // Append feedback block
            if (feedbackBlock != null && !feedbackBlock.isBlank()) {
                systemPrompt += "\n\n" + feedbackBlock;
                logger.info("openai.feedback_block_injected length={}", feedbackBlock.length());
            }

            logger.info("openai.model_used model={}", resolvedModel);

            // Build OpenAI request
            OpenAIRequest request = new OpenAIRequest();
            request.setModel(resolvedModel);
            request.setTemperature(0.7);
            request.setMaxTokens(6500); // Temporary fix for 8K context limit until deployment issues resolved
            request.setMessages(Arrays.asList(
                new OpenAIRequest.OpenAIMessage("system", systemPrompt),
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
               "- Generate CONCISE but complete data for 1 week and 7 days\n" +
               "- Keep descriptions and instructions VERY brief (5-10 words max)\n" +
               "- Use SHORT ingredient lists (2-3 items max per meal)\n" +
               "- Use ONLY the EXACT exercise names provided in the user prompt's exercise catalog. Do not abbreviate, rename, or create variations.\n" +
               "- NEVER use empty arrays [] for exercises or meals - always provide at least one item\n" +
               "- Ensure all JSON is properly formatted without any markdown code blocks\n" +
               "- Every day must have complete exercise/meal data, not references or shortcuts\n" +
               "- If a day is a rest day, include a proper rest exercise/meal object\n" +
               "- PRIORITIZE COMPLETENESS over detail - ensure the full week is included\n" +
               "- Do NOT truncate or cut off the JSON response - complete all structures fully\n\n" +
               "WORKOUT FREQUENCY RULE (CRITICAL):\n" +
               "- workout_frequency = the number of REAL training days with full exercise lists\n" +
               "- Remaining days (7 - workout_frequency) are rest/recovery days with only a rest entry\n" +
               "- NEVER count an 'active recovery', 'light yoga', or 'stretching' day as one of the real training days\n\n" +
               "MINIMUM EXERCISES PER TRAINING DAY (CRITICAL — violating this is a critical error):\n" +
               "- 30-minute sessions: minimum 4 exercises per training day\n" +
               "- 45-minute sessions: minimum 5 exercises per training day\n" +
               "- 60-minute sessions: minimum 6 exercises per training day (NEVER generate fewer than 6)\n" +
               "- 75+ minute sessions: minimum 8 exercises per training day\n" +
               "- The JSON template in the user prompt shows the minimum number of exercise slots — you MUST fill all of them";
    }

    /**
     * Returns goal-specific and frequency-specific prompt guidance for workout day categories
     * and exercise selection.
     */
    private String buildCategoryGuidance(int frequency, String goalsStr) {
        String goalsLower = goalsStr != null ? goalsStr.toLowerCase() : "";

        StringBuilder sb = new StringBuilder();
        sb.append("**Workout Day Category & Split Guidance:**\n");
        sb.append("Assign a meaningful 'category' label to each workout day based on the user's frequency and goals:\n");

        if (frequency <= 3) {
            sb.append("- ").append(frequency).append(" days/week → use Full Body splits: \"Full Body A\", \"Full Body B\", \"Full Body C\"\n");
        } else if (frequency == 4) {
            sb.append("- 4 days/week → use Upper/Lower split: \"Upper Body\", \"Lower Body\", \"Upper Body\", \"Lower Body\"\n");
            sb.append("  (or Push/Pull/Legs/Upper as an alternative)\n");
        } else {
            sb.append("- ").append(frequency).append(" days/week → use Push/Pull/Legs split: \"Push (Chest/Shoulders/Triceps)\", ")
              .append("\"Pull (Back/Biceps)\", \"Legs & Glutes\", repeating as needed\n");
        }
        sb.append("- Rest days → category: \"Rest Day\" or \"Active Recovery\"\n\n");

        // Athletic performance goal guidance
        boolean isAthletic = goalsLower.contains("athletic_performance");
        if (isAthletic) {
            sb.append("**Athletic Performance Goal — Exercise Selection:**\n");
            if (goalsLower.contains("vertical") || goalsLower.contains("explosive")) {
                sb.append("- Goal: Vertical/Explosiveness → prioritize plyometric exercises: Box Jumps, Depth Jumps, Squat Jumps, ")
                  .append("Broad Jumps, Power Cleans, Hang Cleans. Use day categories like \"Power & Explosiveness\" and \"Strength Foundation\".\n");
            }
            if (goalsLower.contains("speed") || goalsLower.contains("agility")) {
                sb.append("- Goal: Speed/Agility → prioritize: Sprint Intervals, Agility Ladder Drills, Shuttle Runs, Cone Drills, ")
                  .append("Lateral Bounds. Use day categories like \"Speed & Agility\" and \"Power & Conditioning\".\n");
            }
            if (goalsLower.contains("endurance")) {
                sb.append("- Goal: Endurance → prioritize: HIIT Circuits, Circuit Training, Tempo Runs, Metabolic Conditioning, ")
                  .append("High-Rep Supersets. Use day categories like \"Endurance & Conditioning\" and \"Metabolic Training\".\n");
            }
            if (goalsLower.contains("sport_specific") || goalsLower.contains("sport-specific")) {
                sb.append("- Goal: Sport-Specific → tailor exercises to the sport context. Basketball: vertical jumps, lateral quickness; ")
                  .append("Soccer: footwork, endurance, change of direction; Football: explosive power, agility. ")
                  .append("Use day categories like \"Sport-Specific Power\" and \"Sport-Specific Conditioning\".\n");
            }
            if (!goalsLower.contains("vertical") && !goalsLower.contains("speed")
                    && !goalsLower.contains("endurance") && !goalsLower.contains("sport")) {
                sb.append("- General Athletic Performance → balance plyometrics, explosive lifts, and conditioning. ")
                  .append("Use day categories like \"Power & Explosiveness\", \"Speed & Agility\", \"Strength Foundation\".\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private String buildCombinedPrompt(User user, WorkoutProfile workoutProfile, DietProfile dietProfile,
                                        List<String> catalogExerciseNames) {
        String exerciseListBlock = catalogExerciseNames.isEmpty()
                ? ""
                : "\n**Exercise Catalog (you MUST use ONLY these exact exercise names — no variations, abbreviations, or new names):**\n" +
                  String.join(", ", catalogExerciseNames) + "\n";

        int frequency = workoutProfile.getWorkoutFrequency() != null ? workoutProfile.getWorkoutFrequency() : 3;
        int sessionDuration = workoutProfile.getSessionDuration() != null ? workoutProfile.getSessionDuration() : 45;
        int restDays = 7 - frequency;
        int minExercises;
        if (sessionDuration >= 75) minExercises = 8;
        else if (sessionDuration >= 60) minExercises = 6;
        else if (sessionDuration >= 45) minExercises = 5;
        else minExercises = 4;

        String goalsStr = workoutProfile.getTargetGoals() != null
                ? String.join(", ", workoutProfile.getTargetGoals()) : "GENERAL_FITNESS";
        String categoryGuidance = buildCategoryGuidance(frequency, goalsStr);

        String userProfile = String.format(
            "Create a personalized 1-week fitness and nutrition plan for:\n\n" +
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
            "- Dietary Restrictions: %s\n",
            user.getAge() != null ? user.getAge() : 25,
            user.getGender() != null ? user.getGender().name() : "MALE",
            user.getWeightKg() != null ? user.getWeightKg().doubleValue() : 70.0,
            user.getHeightCm() != null ? user.getHeightCm() : 175,
            workoutProfile.getFitnessLevel() != null ? workoutProfile.getFitnessLevel().name() : "BEGINNER",
            goalsStr,
            frequency,
            sessionDuration,
            workoutProfile.getAvailableEquipment() != null ? String.join(", ", workoutProfile.getAvailableEquipment()) : "BODYWEIGHT",
            dietProfile.getDietType() != null ? dietProfile.getDietType().name() : "BALANCED",
            dietProfile.getDailyCalorieGoal() != null ? dietProfile.getDailyCalorieGoal() : 2000,
            dietProfile.getMealsPerDay() != null ? dietProfile.getMealsPerDay() : 3,
            "None",
            dietProfile.getDietaryRestrictions() != null && dietProfile.getDietaryRestrictions().length > 0 ? String.join(", ", dietProfile.getDietaryRestrictions()) : "None"
        );

        return userProfile + exerciseListBlock + "\n" +
            categoryGuidance +
            "Return TWO separate JSON objects:\n\n" +

            "=== CRITICAL RULE 1: TRAINING DAYS ===\n" +
            "workout_frequency=" + frequency + " means EXACTLY " + frequency + " days must have real training exercises.\n" +
            "The remaining " + restDays + " days (7 - " + frequency + " = " + restDays + ") must be REST DAYS with only a single rest entry.\n" +
            "NEVER count 'active recovery', 'light yoga', or 'stretching only' as one of the " + frequency + " real training days.\n\n" +

            "=== CRITICAL RULE 2: MINIMUM EXERCISES PER TRAINING DAY ===\n" +
            "Session duration=" + sessionDuration + " minutes. Every training day MUST have at least " + minExercises + " exercises.\n" +
            "For " + sessionDuration + "-minute sessions, generating fewer than " + minExercises + " exercises per training day is a CRITICAL ERROR.\n\n" +

            "Generate a complete 1-week workout plan with this exact structure (no placeholders):\n\n" +
            "WORKOUT_PLAN_JSON:\n" +
            "{\n" +
            "  \"title\": \"1-Week " + frequency + "-Day Workout Plan\",\n" +
            "  \"weeks\": {\n" +
            "    \"week_1\": {\n" +
            "      \"monday\": {\n" +
            "        \"category\": \"Push (Chest/Shoulders/Triceps)\",\n" +
            "        \"exercises\": [\n" +
            "          {\n" +
            "            \"name\": \"Exercise Name\",\n" +
            "            \"sets\": 3,\n" +
            "            \"reps\": 12,\n" +
            "            \"weight_type\": \"bodyweight|dumbbell|time_seconds\",\n" +
            "            \"muscle_groups\": [\"chest\", \"triceps\"],\n" +
            "            \"instructions\": \"Brief instructions\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"Exercise Name\",\n" +
            "            \"sets\": 3,\n" +
            "            \"reps\": 10,\n" +
            "            \"weight_type\": \"bodyweight|dumbbell|time_seconds\",\n" +
            "            \"muscle_groups\": [\"back\", \"biceps\"],\n" +
            "            \"instructions\": \"Brief instructions\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"Exercise Name\",\n" +
            "            \"sets\": 4,\n" +
            "            \"reps\": 12,\n" +
            "            \"weight_type\": \"bodyweight|dumbbell|time_seconds\",\n" +
            "            \"muscle_groups\": [\"legs\", \"glutes\"],\n" +
            "            \"instructions\": \"Brief instructions\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"Exercise Name\",\n" +
            "            \"sets\": 3,\n" +
            "            \"reps\": 15,\n" +
            "            \"weight_type\": \"bodyweight|dumbbell|time_seconds\",\n" +
            "            \"muscle_groups\": [\"shoulders\", \"arms\"],\n" +
            "            \"instructions\": \"Brief instructions\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"Exercise Name\",\n" +
            "            \"sets\": 3,\n" +
            "            \"reps\": 12,\n" +
            "            \"weight_type\": \"bodyweight|dumbbell|time_seconds\",\n" +
            "            \"muscle_groups\": [\"core\", \"abs\"],\n" +
            "            \"instructions\": \"Brief instructions\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"Exercise Name\",\n" +
            "            \"sets\": 3,\n" +
            "            \"reps\": 10,\n" +
            "            \"weight_type\": \"bodyweight|dumbbell|time_seconds\",\n" +
            "            \"muscle_groups\": [\"full body\"],\n" +
            "            \"instructions\": \"Brief instructions\"\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      \"tuesday\": {\n" +
            "        \"category\": \"Pull (Back/Biceps)\",\n" +
            "        \"exercises\": [\n" +
            "          {\n" +
            "            \"name\": \"Exercise Name\",\n" +
            "            \"sets\": 3,\n" +
            "            \"reps\": 12,\n" +
            "            \"weight_type\": \"bodyweight|dumbbell|time_seconds\",\n" +
            "            \"muscle_groups\": [\"back\", \"biceps\"],\n" +
            "            \"instructions\": \"Brief instructions\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"Exercise Name\",\n" +
            "            \"sets\": 3,\n" +
            "            \"reps\": 12,\n" +
            "            \"weight_type\": \"bodyweight|dumbbell|time_seconds\",\n" +
            "            \"muscle_groups\": [\"legs\"],\n" +
            "            \"instructions\": \"Brief instructions\"\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      \"wednesday\": {\n" +
            "        \"category\": \"Active Recovery\",\n" +
            "        \"exercises\": [\n" +
            "          {\n" +
            "            \"name\": \"Rest Day Active Recovery\",\n" +
            "            \"sets\": 1,\n" +
            "            \"reps\": 1,\n" +
            "            \"weight_type\": \"rest\",\n" +
            "            \"muscle_groups\": [],\n" +
            "            \"instructions\": \"Light stretching or walking\"\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      \"thursday\": {\n" +
            "        \"category\": \"Legs & Glutes\",\n" +
            "        \"exercises\": [\n" +
            "          {\n" +
            "            \"name\": \"Exercise Name\",\n" +
            "            \"sets\": 3,\n" +
            "            \"reps\": 12,\n" +
            "            \"weight_type\": \"bodyweight|dumbbell|time_seconds\",\n" +
            "            \"muscle_groups\": [\"legs\", \"glutes\"],\n" +
            "            \"instructions\": \"Brief instructions\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"Exercise Name\",\n" +
            "            \"sets\": 3,\n" +
            "            \"reps\": 12,\n" +
            "            \"weight_type\": \"bodyweight|dumbbell|time_seconds\",\n" +
            "            \"muscle_groups\": [\"core\"],\n" +
            "            \"instructions\": \"Brief instructions\"\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      \"friday\": {\n" +
            "        \"category\": \"Full Body\",\n" +
            "        \"exercises\": [\n" +
            "          {\n" +
            "            \"name\": \"Exercise Name\",\n" +
            "            \"sets\": 3,\n" +
            "            \"reps\": 12,\n" +
            "            \"weight_type\": \"bodyweight|dumbbell|time_seconds\",\n" +
            "            \"muscle_groups\": [\"shoulders\", \"arms\"],\n" +
            "            \"instructions\": \"Brief instructions\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"Exercise Name\",\n" +
            "            \"sets\": 3,\n" +
            "            \"reps\": 12,\n" +
            "            \"weight_type\": \"bodyweight|dumbbell|time_seconds\",\n" +
            "            \"muscle_groups\": [\"chest\"],\n" +
            "            \"instructions\": \"Brief instructions\"\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      \"saturday\": {\n" +
            "        \"category\": \"Rest Day\",\n" +
            "        \"exercises\": [\n" +
            "          {\n" +
            "            \"name\": \"Rest Day\",\n" +
            "            \"sets\": 0,\n" +
            "            \"reps\": 0,\n" +
            "            \"weight_type\": \"rest\",\n" +
            "            \"muscle_groups\": [],\n" +
            "            \"instructions\": \"Complete rest day\"\n" +
            "          }\n" +
            "        ]\n" +
            "      },\n" +
            "      \"sunday\": {\n" +
            "        \"category\": \"Rest Day\",\n" +
            "        \"exercises\": [\n" +
            "          {\n" +
            "            \"name\": \"Rest Day\",\n" +
            "            \"sets\": 0,\n" +
            "            \"reps\": 0,\n" +
            "            \"weight_type\": \"rest\",\n" +
            "            \"muscle_groups\": [],\n" +
            "            \"instructions\": \"Complete rest day\"\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}\n" +
            "IMPORTANT: Replace ALL exercise names with REAL exercises from the catalog. " +
            "Every day MUST include a 'category' field based on the split guidance above. " +
            "Each of the " + frequency + " training days MUST have at least " + minExercises + " exercises (session is " + sessionDuration + " min). " +
            "The monday template above shows 6 exercise slots — ALL training days need the same number. " +
            "Tuesday/thursday/friday templates show only 2 for brevity — expand each to at least " + minExercises + " exercises. " +
            "Distribute training days based on workout_frequency=" + frequency + "; remaining " + restDays + " days are rest days.\n\n" +

            "Generate a complete 1-week nutrition plan with this exact structure (no placeholders):\n\n" +
            "DIET_PLAN_JSON:\n" +
            "{\n" +
            "  \"title\": \"1-Week Nutrition Plan\",\n" +
            "  \"weeks\": {\n" +
            "    \"week_1\": {\n" +
            "      \"monday\": {\n" +
            "        \"meals\": [\n" +
            "          {\n" +
            "            \"meal_type\": \"breakfast\",\n" +
            "            \"name\": \"Meal Name\",\n" +
            "            \"ingredients\": [\"ingredient1\", \"ingredient2\"],\n" +
            "            \"calories\": 400,\n" +
            "            \"proteins\": 25,\n" +
            "            \"carbs\": 45,\n" +
            "            \"fats\": 12,\n" +
            "            \"preparation_time\": 15,\n" +
            "            \"instructions\": \"Preparation instructions\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"meal_type\": \"lunch\",\n" +
            "            \"name\": \"Meal Name\",\n" +
            "            \"ingredients\": [\"ingredient1\", \"ingredient2\"],\n" +
            "            \"calories\": 500,\n" +
            "            \"proteins\": 30,\n" +
            "            \"carbs\": 50,\n" +
            "            \"fats\": 15,\n" +
            "            \"preparation_time\": 20,\n" +
            "            \"instructions\": \"Preparation instructions\"\n" +
            "          }\n" +
            "        ],\n" +
            "        \"daily_totals\": {\n" +
            "          \"calories\": 2000,\n" +
            "          \"proteins\": 150,\n" +
            "          \"carbs\": 200,\n" +
            "          \"fats\": 65\n" +
            "        }\n" +
            "      },\n" +
            "      \"tuesday\": {\n" +
            "        \"meals\": [\n" +
            "          {\n" +
            "            \"meal_type\": \"breakfast\",\n" +
            "            \"name\": \"Greek Yogurt Parfait\",\n" +
            "            \"ingredients\": [\"greek yogurt\", \"granola\", \"blueberries\"],\n" +
            "            \"calories\": 380,\n" +
            "            \"proteins\": 22,\n" +
            "            \"carbs\": 48,\n" +
            "            \"fats\": 8,\n" +
            "            \"preparation_time\": 5,\n" +
            "            \"instructions\": \"Layer yogurt, granola, and berries in a bowl\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"meal_type\": \"lunch\",\n" +
            "            \"name\": \"Turkey Wrap\",\n" +
            "            \"ingredients\": [\"whole wheat wrap\", \"turkey breast\", \"lettuce\", \"tomato\"],\n" +
            "            \"calories\": 480,\n" +
            "            \"proteins\": 35,\n" +
            "            \"carbs\": 45,\n" +
            "            \"fats\": 12,\n" +
            "            \"preparation_time\": 10,\n" +
            "            \"instructions\": \"Fill wrap with turkey and vegetables, roll tightly\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"meal_type\": \"dinner\",\n" +
            "            \"name\": \"Baked Salmon with Quinoa\",\n" +
            "            \"ingredients\": [\"salmon fillet\", \"quinoa\", \"asparagus\", \"lemon\"],\n" +
            "            \"calories\": 580,\n" +
            "            \"proteins\": 42,\n" +
            "            \"carbs\": 38,\n" +
            "            \"fats\": 18,\n" +
            "            \"preparation_time\": 25,\n" +
            "            \"instructions\": \"Bake salmon at 400F for 15 minutes, serve over quinoa with asparagus\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"meal_type\": \"snack\",\n" +
            "            \"name\": \"Apple with Almond Butter\",\n" +
            "            \"ingredients\": [\"apple\", \"almond butter\"],\n" +
            "            \"calories\": 220,\n" +
            "            \"proteins\": 5,\n" +
            "            \"carbs\": 28,\n" +
            "            \"fats\": 11,\n" +
            "            \"preparation_time\": 2,\n" +
            "            \"instructions\": \"Slice apple and serve with almond butter for dipping\"\n" +
            "          }\n" +
            "        ],\n" +
            "        \"daily_totals\": {\n" +
            "          \"calories\": 1660,\n" +
            "          \"proteins\": 104,\n" +
            "          \"carbs\": 159,\n" +
            "          \"fats\": 49\n" +
            "        }\n" +
            "      },\n" +
            "      \"wednesday\": {\n" +
            "        \"meals\": [\n" +
            "          {\n" +
            "            \"meal_type\": \"breakfast\",\n" +
            "            \"name\": \"Veggie Omelette\",\n" +
            "            \"ingredients\": [\"eggs\", \"spinach\", \"bell pepper\", \"mushrooms\"],\n" +
            "            \"calories\": 320,\n" +
            "            \"proteins\": 24,\n" +
            "            \"carbs\": 12,\n" +
            "            \"fats\": 18,\n" +
            "            \"preparation_time\": 10,\n" +
            "            \"instructions\": \"Whisk eggs, cook vegetables first, fold eggs around filling\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"meal_type\": \"lunch\",\n" +
            "            \"name\": \"Lentil Soup with Whole Grain Bread\",\n" +
            "            \"ingredients\": [\"red lentils\", \"carrots\", \"celery\", \"whole grain bread\"],\n" +
            "            \"calories\": 460,\n" +
            "            \"proteins\": 22,\n" +
            "            \"carbs\": 68,\n" +
            "            \"fats\": 8,\n" +
            "            \"preparation_time\": 30,\n" +
            "            \"instructions\": \"Simmer lentils with vegetables until tender, season and serve with bread\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"meal_type\": \"dinner\",\n" +
            "            \"name\": \"Grilled Chicken Stir-Fry\",\n" +
            "            \"ingredients\": [\"chicken breast\", \"broccoli\", \"snap peas\", \"brown rice\", \"soy sauce\"],\n" +
            "            \"calories\": 520,\n" +
            "            \"proteins\": 40,\n" +
            "            \"carbs\": 52,\n" +
            "            \"fats\": 10,\n" +
            "            \"preparation_time\": 20,\n" +
            "            \"instructions\": \"Stir-fry chicken and vegetables in soy sauce, serve over brown rice\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"meal_type\": \"snack\",\n" +
            "            \"name\": \"Cottage Cheese with Pineapple\",\n" +
            "            \"ingredients\": [\"cottage cheese\", \"pineapple chunks\"],\n" +
            "            \"calories\": 180,\n" +
            "            \"proteins\": 16,\n" +
            "            \"carbs\": 22,\n" +
            "            \"fats\": 3,\n" +
            "            \"preparation_time\": 2,\n" +
            "            \"instructions\": \"Mix cottage cheese with pineapple chunks\"\n" +
            "          }\n" +
            "        ],\n" +
            "        \"daily_totals\": {\n" +
            "          \"calories\": 1480,\n" +
            "          \"proteins\": 102,\n" +
            "          \"carbs\": 154,\n" +
            "          \"fats\": 39\n" +
            "        }\n" +
            "      },\n" +
            "      \"thursday\": {\n" +
            "        \"meals\": [\n" +
            "          {\n" +
            "            \"meal_type\": \"breakfast\",\n" +
            "            \"name\": \"Smoothie Bowl\",\n" +
            "            \"ingredients\": [\"frozen banana\", \"protein powder\", \"almond milk\", \"chia seeds\", \"strawberries\"],\n" +
            "            \"calories\": 420,\n" +
            "            \"proteins\": 28,\n" +
            "            \"carbs\": 55,\n" +
            "            \"fats\": 9,\n" +
            "            \"preparation_time\": 8,\n" +
            "            \"instructions\": \"Blend banana with protein powder and almond milk, top with chia seeds and berries\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"meal_type\": \"lunch\",\n" +
            "            \"name\": \"Tuna Salad Bowl\",\n" +
            "            \"ingredients\": [\"canned tuna\", \"mixed greens\", \"cucumber\", \"cherry tomatoes\", \"olive oil\"],\n" +
            "            \"calories\": 390,\n" +
            "            \"proteins\": 38,\n" +
            "            \"carbs\": 18,\n" +
            "            \"fats\": 16,\n" +
            "            \"preparation_time\": 10,\n" +
            "            \"instructions\": \"Toss drained tuna with salad greens and vegetables, drizzle with olive oil\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"meal_type\": \"dinner\",\n" +
            "            \"name\": \"Beef and Sweet Potato Bowl\",\n" +
            "            \"ingredients\": [\"lean ground beef\", \"sweet potato\", \"black beans\", \"salsa\"],\n" +
            "            \"calories\": 620,\n" +
            "            \"proteins\": 38,\n" +
            "            \"carbs\": 62,\n" +
            "            \"fats\": 18,\n" +
            "            \"preparation_time\": 25,\n" +
            "            \"instructions\": \"Brown beef, roast sweet potato cubes, combine with beans and top with salsa\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"meal_type\": \"snack\",\n" +
            "            \"name\": \"Hummus with Veggie Sticks\",\n" +
            "            \"ingredients\": [\"hummus\", \"carrot sticks\", \"celery sticks\", \"cucumber slices\"],\n" +
            "            \"calories\": 160,\n" +
            "            \"proteins\": 6,\n" +
            "            \"carbs\": 20,\n" +
            "            \"fats\": 7,\n" +
            "            \"preparation_time\": 5,\n" +
            "            \"instructions\": \"Arrange vegetables around hummus for dipping\"\n" +
            "          }\n" +
            "        ],\n" +
            "        \"daily_totals\": {\n" +
            "          \"calories\": 1590,\n" +
            "          \"proteins\": 110,\n" +
            "          \"carbs\": 155,\n" +
            "          \"fats\": 50\n" +
            "        }\n" +
            "      },\n" +
            "      \"friday\": {\n" +
            "        \"meals\": [\n" +
            "          {\n" +
            "            \"meal_type\": \"breakfast\",\n" +
            "            \"name\": \"Whole Grain Pancakes\",\n" +
            "            \"ingredients\": [\"whole wheat flour\", \"egg\", \"milk\", \"banana\", \"maple syrup\"],\n" +
            "            \"calories\": 440,\n" +
            "            \"proteins\": 18,\n" +
            "            \"carbs\": 72,\n" +
            "            \"fats\": 10,\n" +
            "            \"preparation_time\": 15,\n" +
            "            \"instructions\": \"Mix batter, cook on griddle until golden, serve with sliced banana\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"meal_type\": \"lunch\",\n" +
            "            \"name\": \"Chicken Caesar Salad\",\n" +
            "            \"ingredients\": [\"romaine lettuce\", \"grilled chicken\", \"parmesan\", \"croutons\", \"caesar dressing\"],\n" +
            "            \"calories\": 510,\n" +
            "            \"proteins\": 36,\n" +
            "            \"carbs\": 32,\n" +
            "            \"fats\": 22,\n" +
            "            \"preparation_time\": 15,\n" +
            "            \"instructions\": \"Toss lettuce with dressing, top with chicken, parmesan, and croutons\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"meal_type\": \"dinner\",\n" +
            "            \"name\": \"Shrimp Pasta\",\n" +
            "            \"ingredients\": [\"shrimp\", \"whole grain pasta\", \"cherry tomatoes\", \"garlic\", \"olive oil\"],\n" +
            "            \"calories\": 560,\n" +
            "            \"proteins\": 35,\n" +
            "            \"carbs\": 65,\n" +
            "            \"fats\": 14,\n" +
            "            \"preparation_time\": 20,\n" +
            "            \"instructions\": \"Saute shrimp and garlic, toss with cooked pasta and tomatoes\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"meal_type\": \"snack\",\n" +
            "            \"name\": \"Protein Bar\",\n" +
            "            \"ingredients\": [\"protein bar\"],\n" +
            "            \"calories\": 210,\n" +
            "            \"proteins\": 20,\n" +
            "            \"carbs\": 25,\n" +
            "            \"fats\": 7,\n" +
            "            \"preparation_time\": 0,\n" +
            "            \"instructions\": \"Ready to eat\"\n" +
            "          }\n" +
            "        ],\n" +
            "        \"daily_totals\": {\n" +
            "          \"calories\": 1720,\n" +
            "          \"proteins\": 109,\n" +
            "          \"carbs\": 194,\n" +
            "          \"fats\": 53\n" +
            "        }\n" +
            "      },\n" +
            "      \"saturday\": {\n" +
            "        \"meals\": [\n" +
            "          {\n" +
            "            \"meal_type\": \"breakfast\",\n" +
            "            \"name\": \"Avocado Toast with Poached Eggs\",\n" +
            "            \"ingredients\": [\"sourdough bread\", \"avocado\", \"eggs\", \"cherry tomatoes\", \"red pepper flakes\"],\n" +
            "            \"calories\": 480,\n" +
            "            \"proteins\": 22,\n" +
            "            \"carbs\": 42,\n" +
            "            \"fats\": 24,\n" +
            "            \"preparation_time\": 12,\n" +
            "            \"instructions\": \"Toast bread, mash avocado on top, place poached eggs, garnish with tomatoes\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"meal_type\": \"lunch\",\n" +
            "            \"name\": \"Black Bean Burrito\",\n" +
            "            \"ingredients\": [\"tortilla\", \"black beans\", \"brown rice\", \"salsa\", \"greek yogurt\"],\n" +
            "            \"calories\": 520,\n" +
            "            \"proteins\": 24,\n" +
            "            \"carbs\": 78,\n" +
            "            \"fats\": 10,\n" +
            "            \"preparation_time\": 15,\n" +
            "            \"instructions\": \"Warm tortilla, fill with beans, rice, and salsa, fold and serve\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"meal_type\": \"dinner\",\n" +
            "            \"name\": \"Grilled Steak with Roasted Vegetables\",\n" +
            "            \"ingredients\": [\"lean sirloin\", \"zucchini\", \"bell peppers\", \"onion\", \"olive oil\"],\n" +
            "            \"calories\": 580,\n" +
            "            \"proteins\": 46,\n" +
            "            \"carbs\": 22,\n" +
            "            \"fats\": 28,\n" +
            "            \"preparation_time\": 30,\n" +
            "            \"instructions\": \"Grill steak to desired doneness, roast vegetables with olive oil at 425F\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"meal_type\": \"snack\",\n" +
            "            \"name\": \"Mixed Nuts and Dried Fruit\",\n" +
            "            \"ingredients\": [\"almonds\", \"walnuts\", \"dried cranberries\"],\n" +
            "            \"calories\": 240,\n" +
            "            \"proteins\": 7,\n" +
            "            \"carbs\": 22,\n" +
            "            \"fats\": 16,\n" +
            "            \"preparation_time\": 0,\n" +
            "            \"instructions\": \"Mix together and portion into a bowl\"\n" +
            "          }\n" +
            "        ],\n" +
            "        \"daily_totals\": {\n" +
            "          \"calories\": 1820,\n" +
            "          \"proteins\": 99,\n" +
            "          \"carbs\": 164,\n" +
            "          \"fats\": 78\n" +
            "        }\n" +
            "      },\n" +
            "      \"sunday\": {\n" +
            "        \"meals\": [\n" +
            "          {\n" +
            "            \"meal_type\": \"breakfast\",\n" +
            "            \"name\": \"Overnight Oats\",\n" +
            "            \"ingredients\": [\"rolled oats\", \"almond milk\", \"honey\", \"banana\", \"walnuts\"],\n" +
            "            \"calories\": 410,\n" +
            "            \"proteins\": 14,\n" +
            "            \"carbs\": 62,\n" +
            "            \"fats\": 14,\n" +
            "            \"preparation_time\": 5,\n" +
            "            \"instructions\": \"Combine oats and milk the night before, top with banana and walnuts in the morning\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"meal_type\": \"lunch\",\n" +
            "            \"name\": \"Tomato Basil Soup with Grilled Cheese\",\n" +
            "            \"ingredients\": [\"tomatoes\", \"basil\", \"whole grain bread\", \"cheese\", \"butter\"],\n" +
            "            \"calories\": 490,\n" +
            "            \"proteins\": 18,\n" +
            "            \"carbs\": 52,\n" +
            "            \"fats\": 22,\n" +
            "            \"preparation_time\": 20,\n" +
            "            \"instructions\": \"Simmer tomatoes and basil, blend until smooth; grill cheese sandwich separately\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"meal_type\": \"dinner\",\n" +
            "            \"name\": \"Roasted Chicken Thighs with Mashed Sweet Potato\",\n" +
            "            \"ingredients\": [\"chicken thighs\", \"sweet potato\", \"garlic\", \"rosemary\", \"olive oil\"],\n" +
            "            \"calories\": 610,\n" +
            "            \"proteins\": 40,\n" +
            "            \"carbs\": 48,\n" +
            "            \"fats\": 24,\n" +
            "            \"preparation_time\": 40,\n" +
            "            \"instructions\": \"Roast chicken thighs at 425F for 35 minutes, mash boiled sweet potato with garlic\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"meal_type\": \"snack\",\n" +
            "            \"name\": \"Dark Chocolate and Berries\",\n" +
            "            \"ingredients\": [\"dark chocolate\", \"mixed berries\"],\n" +
            "            \"calories\": 190,\n" +
            "            \"proteins\": 3,\n" +
            "            \"carbs\": 26,\n" +
            "            \"fats\": 10,\n" +
            "            \"preparation_time\": 0,\n" +
            "            \"instructions\": \"Serve dark chocolate squares with fresh mixed berries\"\n" +
            "          }\n" +
            "        ],\n" +
            "        \"daily_totals\": {\n" +
            "          \"calories\": 1700,\n" +
            "          \"proteins\": 75,\n" +
            "          \"carbs\": 188,\n" +
            "          \"fats\": 70\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}\n" +
            "IMPORTANT: Expand this structure completely for all 7 days (monday through sunday) with actual meal details and daily totals for each day. Replace all /* comments */ with real meal data.";
    }

    private CombinedPlanResult parseCombinedResponse(String content) {
        try {
            logger.debug("OpenAI Response Content Length: {}", content.length());
            logger.debug("OpenAI Response Preview: {}", content.substring(0, Math.min(500, content.length())));

            // Try multiple marker variations for flexibility
            String[] workoutMarkers = {"WORKOUT_PLAN_JSON:", "workout plan", "workout:", "\"workout\"", "```json"};
            String[] dietMarkers = {"DIET_PLAN_JSON:", "diet plan", "nutrition plan", "diet:", "\"diet\"", "\"nutrition\""};

            int workoutStart = -1;
            int dietStart = -1;
            String actualWorkoutMarker = "";
            String actualDietMarker = "";

            // Find workout markers
            for (String marker : workoutMarkers) {
                int pos = content.toLowerCase().indexOf(marker.toLowerCase());
                if (pos != -1) {
                    workoutStart = pos;
                    actualWorkoutMarker = marker;
                    break;
                }
            }

            // Find diet markers
            for (String marker : dietMarkers) {
                int pos = content.toLowerCase().indexOf(marker.toLowerCase());
                if (pos != -1 && pos > workoutStart) {
                    dietStart = pos;
                    actualDietMarker = marker;
                    break;
                }
            }

            logger.debug("Found workout marker '{}' at position: {}", actualWorkoutMarker, workoutStart);
            logger.debug("Found diet marker '{}' at position: {}", actualDietMarker, dietStart);

            if (workoutStart == -1 || dietStart == -1) {
                // Try to extract JSON objects directly
                return extractJSONObjectsDirectly(content);
            }

            // Extract workout plan JSON
            String workoutJsonStr = content.substring(workoutStart + actualWorkoutMarker.length(), dietStart).trim();
            workoutJsonStr = cleanJsonString(workoutJsonStr);

            // Extract diet plan JSON
            String dietJsonStr = content.substring(dietStart + actualDietMarker.length()).trim();
            dietJsonStr = cleanJsonString(dietJsonStr);

            logger.debug("Extracted workout JSON length: {}", workoutJsonStr.length());
            logger.debug("Extracted diet JSON length: {}", dietJsonStr.length());

            // Parse JSON strings
            Map<String, Object> workoutPlan = objectMapper.readValue(workoutJsonStr, Map.class);
            Map<String, Object> dietPlan = objectMapper.readValue(dietJsonStr, Map.class);

            return new CombinedPlanResult(workoutPlan, dietPlan);

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse OpenAI response: " + e.getMessage() + " | Content preview: " + content.substring(0, Math.min(200, content.length())), e);
        }
    }

    private CombinedPlanResult extractJSONObjectsDirectly(String content) {
        try {
            logger.debug("Attempting direct JSON extraction");

            // Find all JSON objects in the response
            int firstBrace = content.indexOf('{');
            if (firstBrace == -1) {
                throw new RuntimeException("No JSON objects found in response");
            }

            // Extract the first JSON object (workout plan)
            int braceCount = 0;
            int workoutEnd = -1;
            for (int i = firstBrace; i < content.length(); i++) {
                if (content.charAt(i) == '{') braceCount++;
                else if (content.charAt(i) == '}') braceCount--;

                if (braceCount == 0) {
                    workoutEnd = i + 1;
                    break;
                }
            }

            String workoutJsonStr = content.substring(firstBrace, workoutEnd);
            workoutJsonStr = cleanJsonString(workoutJsonStr);

            // Find the second JSON object (diet plan)
            int secondBrace = content.indexOf('{', workoutEnd);
            if (secondBrace == -1) {
                throw new RuntimeException("Could not find second JSON object for diet plan");
            }

            braceCount = 0;
            int dietEnd = -1;
            for (int i = secondBrace; i < content.length(); i++) {
                if (content.charAt(i) == '{') braceCount++;
                else if (content.charAt(i) == '}') braceCount--;

                if (braceCount == 0) {
                    dietEnd = i + 1;
                    break;
                }
            }

            String dietJsonStr = content.substring(secondBrace, dietEnd);
            dietJsonStr = cleanJsonString(dietJsonStr);

            logger.debug("Direct extraction - workout JSON length: {}", workoutJsonStr.length());
            logger.debug("Direct extraction - diet JSON length: {}", dietJsonStr.length());

            Map<String, Object> workoutPlan = objectMapper.readValue(workoutJsonStr, Map.class);
            Map<String, Object> dietPlan = objectMapper.readValue(dietJsonStr, Map.class);

            return new CombinedPlanResult(workoutPlan, dietPlan);

        } catch (Exception e) {
            throw new RuntimeException("Direct JSON extraction failed: " + e.getMessage());
        }
    }

    String cleanJsonString(String jsonStr) {
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

        // Handle empty meal arrays - log a warning so we can detect generation failures,
        // then substitute a clearly-labelled placeholder so the app does not crash.
        java.util.regex.Matcher emptyMealsMatcher =
            java.util.regex.Pattern.compile("\"meals\":\\s*\\[\\s*\\]").matcher(cleaned);
        if (emptyMealsMatcher.find()) {
            logger.warn("Empty meal array detected in JSON response - OpenAI did not generate meals for at least one day. Consider regenerating the plan.");
            cleaned = emptyMealsMatcher.replaceAll(
                "\"meals\": [{\"meal_type\": \"none\", \"name\": \"No meals generated - please regenerate plan\", \"ingredients\": [], \"calories\": 0, \"proteins\": 0, \"carbs\": 0, \"fats\": 0, \"preparation_time\": 0, \"instructions\": \"No meals were generated for this day\"}]");
        }

        // Remove any trailing commas before closing brackets/braces
        cleaned = cleaned.replaceAll(",\\s*([\\]}])", "$1");

        // Normalize whitespace
        cleaned = cleaned.replaceAll("\\n\\s*\\n", "\n")  // Remove empty lines
                        .replaceAll("^\\s+", "")
                        .replaceAll("\\s+$", "")
                        .trim();

        // Attempt to handle truncated JSON by validating brace balance and fixing structure.
        // The counter must skip characters inside quoted strings so that literal braces
        // in values like "Do 3 sets {rest 60s}" don't corrupt the count.
        if (!cleaned.endsWith("}")) {
            logger.warn("JSON does not end with closing brace - attempting to repair");

            int braceCount = 0;
            int lastValidPosition = -1;
            boolean inString = false;

            for (int i = 0; i < cleaned.length(); i++) {
                char c = cleaned.charAt(i);
                if (inString) {
                    if (c == '\\') {
                        i++; // skip the escaped character
                    } else if (c == '"') {
                        inString = false;
                    }
                } else {
                    if (c == '"') {
                        inString = true;
                    } else if (c == '{') {
                        braceCount++;
                    } else if (c == '}') {
                        braceCount--;
                        if (braceCount == 0) {
                            lastValidPosition = i + 1;
                        }
                    }
                }
            }

            if (lastValidPosition > 0 && lastValidPosition < cleaned.length()) {
                cleaned = cleaned.substring(0, lastValidPosition);
                logger.info("Truncated JSON at position {} to maintain valid structure", lastValidPosition);
            } else if (braceCount > 0) {
                // Add missing closing braces
                int missingBraces = braceCount;
                StringBuilder sb = new StringBuilder(cleaned);
                while (braceCount > 0) {
                    sb.append("}");
                    braceCount--;
                }
                cleaned = sb.toString();
                logger.info("Added {} missing closing braces to repair truncated JSON", missingBraces);
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