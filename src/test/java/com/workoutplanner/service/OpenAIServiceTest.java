package com.workoutplanner.service;

import com.workoutplanner.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenAIServiceTest {

    @Test
    void constructor_WithValidApiKey_ShouldInitializeService() {
        String validApiKey = "sk-test12345678901234567890";

        OpenAIService service = new OpenAIService(validApiKey);

        assertNotNull(service);
    }

    @Test
    void constructor_WithInvalidApiKey_ShouldThrowException() {
        String invalidApiKey = "invalid-key";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new OpenAIService(invalidApiKey));

        assertTrue(exception.getMessage().contains("Invalid OpenAI API key format"));
    }

    @Test
    void constructor_WithNullApiKey_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new OpenAIService(null));

        assertTrue(exception.getMessage().contains("OpenAI API key is not properly configured"));
    }

    @Test
    void constructor_WithEmptyApiKey_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new OpenAIService(""));

        assertTrue(exception.getMessage().contains("OpenAI API key is not properly configured"));
    }

    @Test
    void constructor_WithDefaultPlaceholderApiKey_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new OpenAIService("your-openai-api-key-here"));

        assertTrue(exception.getMessage().contains("OpenAI API key is not properly configured"));
    }

    @Test
    void constructor_WithWhitespaceApiKey_ShouldThrowException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new OpenAIService("   "));

        assertTrue(exception.getMessage().contains("OpenAI API key is not properly configured"));
    }

    @Test
    void constructor_WithValidApiKeyWithWhitespace_ShouldTrimAndInitialize() {
        String validApiKeyWithSpaces = "  sk-test12345678901234567890  ";

        OpenAIService service = new OpenAIService(validApiKeyWithSpaces);

        assertNotNull(service);
    }
}