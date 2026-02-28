package com.workoutplanner.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpenAIRequestTest {

    private ObjectMapper objectMapper;
    private List<OpenAIRequest.OpenAIMessage> testMessages;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        testMessages = Arrays.asList(
                new OpenAIRequest.OpenAIMessage("system", "You are a fitness trainer"),
                new OpenAIRequest.OpenAIMessage("user", "Create a workout plan")
        );
    }

    @Test
    void constructor_DefaultConstructor_ShouldCreateEmptyObject() {
        OpenAIRequest request = new OpenAIRequest();

        assertNull(request.getModel());
        assertNull(request.getMessages());
        assertNull(request.getTemperature());
        assertNull(request.getMaxTokens());
    }

    @Test
    void constructor_WithParameters_ShouldSetAllFields() {
        String model = "gpt-3.5-turbo";
        Double temperature = 0.7;
        Integer maxTokens = 2000;

        OpenAIRequest request = new OpenAIRequest(model, testMessages, temperature, maxTokens);

        assertEquals(model, request.getModel());
        assertEquals(testMessages, request.getMessages());
        assertEquals(temperature, request.getTemperature());
        assertEquals(maxTokens, request.getMaxTokens());
    }

    @Test
    void settersAndGetters_ShouldWorkCorrectly() {
        OpenAIRequest request = new OpenAIRequest();
        String model = "gpt-4";
        Double temperature = 0.5;
        Integer maxTokens = 1500;

        request.setModel(model);
        request.setMessages(testMessages);
        request.setTemperature(temperature);
        request.setMaxTokens(maxTokens);

        assertEquals(model, request.getModel());
        assertEquals(testMessages, request.getMessages());
        assertEquals(temperature, request.getTemperature());
        assertEquals(maxTokens, request.getMaxTokens());
    }

    @Test
    void jsonSerialization_ShouldSerializeCorrectly() throws Exception {
        OpenAIRequest request = new OpenAIRequest("gpt-3.5-turbo", testMessages, 0.7, 2000);

        String json = objectMapper.writeValueAsString(request);

        assertTrue(json.contains("\"model\":\"gpt-3.5-turbo\""));
        assertTrue(json.contains("\"temperature\":0.7"));
        assertTrue(json.contains("\"max_tokens\":2000"));
        assertTrue(json.contains("\"messages\""));
    }

    @Test
    void jsonDeserialization_ShouldDeserializeCorrectly() throws Exception {
        String json = """
                {
                    "model": "gpt-3.5-turbo",
                    "temperature": 0.7,
                    "max_tokens": 2000,
                    "messages": [
                        {"role": "system", "content": "You are a fitness trainer"},
                        {"role": "user", "content": "Create a workout plan"}
                    ]
                }
                """;

        OpenAIRequest request = objectMapper.readValue(json, OpenAIRequest.class);

        assertEquals("gpt-3.5-turbo", request.getModel());
        assertEquals(0.7, request.getTemperature());
        assertEquals(2000, request.getMaxTokens());
        assertNotNull(request.getMessages());
        assertEquals(2, request.getMessages().size());
        assertEquals("system", request.getMessages().get(0).getRole());
        assertEquals("You are a fitness trainer", request.getMessages().get(0).getContent());
    }

    @Test
    void openAIMessage_DefaultConstructor_ShouldCreateEmptyObject() {
        OpenAIRequest.OpenAIMessage message = new OpenAIRequest.OpenAIMessage();

        assertNull(message.getRole());
        assertNull(message.getContent());
    }

    @Test
    void openAIMessage_WithParameters_ShouldSetFields() {
        String role = "user";
        String content = "Hello, how are you?";

        OpenAIRequest.OpenAIMessage message = new OpenAIRequest.OpenAIMessage(role, content);

        assertEquals(role, message.getRole());
        assertEquals(content, message.getContent());
    }

    @Test
    void openAIMessage_SettersAndGetters_ShouldWorkCorrectly() {
        OpenAIRequest.OpenAIMessage message = new OpenAIRequest.OpenAIMessage();
        String role = "assistant";
        String content = "I'm doing well, thank you!";

        message.setRole(role);
        message.setContent(content);

        assertEquals(role, message.getRole());
        assertEquals(content, message.getContent());
    }
}