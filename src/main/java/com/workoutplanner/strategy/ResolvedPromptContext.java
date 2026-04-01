package com.workoutplanner.strategy;

import java.util.List;

/**
 * Immutable result of PromptStrategy resolution.
 * Carries the model identifier, system prompt, and any active coaching directives
 * to be injected into the OpenAI request.
 */
public record ResolvedPromptContext(
        String model,
        String systemPrompt,
        List<String> directives
) {
    public boolean hasDirectives() {
        return directives != null && !directives.isEmpty();
    }
}
