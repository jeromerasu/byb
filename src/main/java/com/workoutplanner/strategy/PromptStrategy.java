package com.workoutplanner.strategy;

/**
 * Extension point for model and prompt resolution.
 *
 * All tier-specific branching lives here — no {@code if (tier == X)} checks
 * anywhere in the generation service code.
 *
 * Implementations:
 *   {@link StandardPromptStrategy}  — gpt-4o-mini, base system prompt, no directives
 *   {@link CoachingPromptStrategy}  — gpt-4o, resolved coach template, active directives
 */
public interface PromptStrategy {

    /**
     * Returns the OpenAI model identifier to use (e.g. "gpt-4o-mini", "gpt-4o").
     */
    String resolveModel();

    /**
     * Returns the system prompt for the given user.
     * For STANDARD tier this is a fixed base prompt.
     * For COACHING tier this resolves per-client → coach default → base fallback chain.
     */
    String resolveSystemPrompt(String userId);

    /**
     * Returns the list of active coaching directive strings to inject.
     * Returns an empty list for STANDARD tier.
     */
    java.util.List<String> resolveDirectives(String userId);

    /**
     * Convenience method: resolve all three into a single context object.
     */
    default ResolvedPromptContext resolve(String userId) {
        return new ResolvedPromptContext(
                resolveModel(),
                resolveSystemPrompt(userId),
                resolveDirectives(userId)
        );
    }
}
