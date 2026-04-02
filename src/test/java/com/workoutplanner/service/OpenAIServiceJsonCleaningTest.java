package com.workoutplanner.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workoutplanner.repository.ExerciseCatalogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for OpenAIService.cleanJsonString() — specifically the brace-balance repair
 * logic that must skip characters inside quoted strings.
 */
@ExtendWith(MockitoExtension.class)
class OpenAIServiceJsonCleaningTest {

    @Mock private RestTemplate restTemplate;
    @Mock private ExerciseCatalogRepository exerciseCatalogRepository;

    private OpenAIService openAIService;

    @BeforeEach
    void setUp() {
        openAIService = new OpenAIService(
                restTemplate, new ObjectMapper(),
                "test-key", "gpt-4",
                "https://api.openai.com/v1/chat/completions",
                exerciseCatalogRepository);
    }

    // ── Bug 1: braces inside quoted strings must not affect the brace counter ──

    @Test
    void cleanJsonString_bracesInQuotedString_notTruncated() {
        // Instruction field contains literal braces — old code would decrement counter
        // and think the object ended early, truncating the rest.
        String json = "{\"exercises\": [{\"name\": \"Squat\", \"instructions\": \"Do 3 sets {rest 60s}\"}]}";
        String result = openAIService.cleanJsonString(json);

        assertThat(result).contains("Do 3 sets {rest 60s}");
        assertThat(result).endsWith("}");
        assertThat(countChar(result, '{')).isEqualTo(countChar(result, '}'));
    }

    @Test
    void cleanJsonString_multipleBracePairsInStrings_notTruncated() {
        String json = "{\"a\": \"value {x}\", \"b\": \"another {y} string\", \"c\": 42}";
        String result = openAIService.cleanJsonString(json);

        assertThat(result).contains("value {x}");
        assertThat(result).contains("another {y} string");
        assertThat(countChar(result, '{')).isEqualTo(countChar(result, '}'));
    }

    @Test
    void cleanJsonString_escapedQuoteInsideString_handledCorrectly() {
        // Escaped quote must not prematurely end the string context
        String json = "{\"name\": \"Bench \\\"heavy\\\" Press\", \"sets\": 3}";
        String result = openAIService.cleanJsonString(json);

        assertThat(result).contains("Bench");
        assertThat(result).contains("Press");
        assertThat(countChar(result, '{')).isEqualTo(countChar(result, '}'));
    }

    @Test
    void cleanJsonString_nestedObjectsNoStrings_preservesStructure() {
        String json = "{\"workout\": {\"monday\": {\"exercises\": [{\"name\": \"Push-up\"}]}}}";
        String result = openAIService.cleanJsonString(json);

        assertThat(result).contains("Push-up");
        assertThat(countChar(result, '{')).isEqualTo(countChar(result, '}'));
    }

    // ── Truncation repair: missing closing braces should be added ──

    @Test
    void cleanJsonString_truncatedJson_missingClosingBracesAdded() {
        // Three levels deep, only the innermost is closed
        String json = "{\"workout\": {\"monday\": {\"exercises\": []";
        String result = openAIService.cleanJsonString(json);

        assertThat(result).endsWith("}");
        assertThat(countChar(result, '{')).isEqualTo(countChar(result, '}'));
    }

    @Test
    void cleanJsonString_truncatedJsonWithBraceInString_repairsCorrectly() throws Exception {
        // Truncated JSON — outer object is never closed.  The balanced {slow tempo} inside the
        // string must not fool the repair counter into adding the wrong number of braces.
        String json = "{\"note\": \"Use {slow tempo}\", \"nested\": {\"x\": 1";
        // Repair should add the 2 missing closing braces (one for "nested", one for outer).
        String result = openAIService.cleanJsonString(json);

        assertThat(result).contains("Use {slow tempo}");
        assertThat(result).endsWith("}");
        // Result must be valid JSON
        new ObjectMapper().readValue(result, Object.class);
    }

    // ── Already-valid JSON should pass through unchanged (structurally) ──

    @Test
    void cleanJsonString_validJson_bracesRemainBalanced() throws Exception {
        String json = "{\"title\": \"Plan\", \"weeks\": {\"week_1\": {\"monday\": {\"meals\": []}}}}";
        String result = openAIService.cleanJsonString(json);

        assertThat(countChar(result, '{')).isEqualTo(countChar(result, '}'));
        // Should still be parseable
        new ObjectMapper().readValue(result, Object.class);
    }

    // ── Helper ──

    private static long countChar(String s, char c) {
        return s.chars().filter(ch -> ch == c).count();
    }
}
