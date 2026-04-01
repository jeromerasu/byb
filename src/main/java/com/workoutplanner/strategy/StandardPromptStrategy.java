package com.workoutplanner.strategy;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Strategy for STANDARD tier users.
 * Uses gpt-4o-mini and the base system prompt; no coaching directives are injected.
 */
@Component
public class StandardPromptStrategy implements PromptStrategy {

    private static final String MODEL = "gpt-4o-mini";

    static final String BASE_SYSTEM_PROMPT =
            "You are a professional fitness and nutrition AI. You must respond with exactly TWO separate JSON objects, clearly separated. " +
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
            "- Do NOT truncate or cut off the JSON response - complete all structures fully";

    @Override
    public String resolveModel() {
        return MODEL;
    }

    @Override
    public String resolveSystemPrompt(String userId) {
        return BASE_SYSTEM_PROMPT;
    }

    @Override
    public List<String> resolveDirectives(String userId) {
        return Collections.emptyList();
    }
}
