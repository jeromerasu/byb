package com.workoutplanner.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpenAIResponseTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void constructor_DefaultConstructor_ShouldCreateEmptyObject() {
        OpenAIResponse response = new OpenAIResponse();

        assertNull(response.getChoices());
    }

    @Test
    void settersAndGetters_ShouldWorkCorrectly() {
        OpenAIResponse response = new OpenAIResponse();

        OpenAIRequest.OpenAIMessage message = new OpenAIRequest.OpenAIMessage("assistant", "Test response");
        OpenAIResponse.Choice choice = new OpenAIResponse.Choice();
        choice.setMessage(message);
        choice.setFinishReason("stop");

        List<OpenAIResponse.Choice> choices = Arrays.asList(choice);

        response.setChoices(choices);

        assertEquals(choices, response.getChoices());
        assertEquals(1, response.getChoices().size());
        assertEquals("assistant", response.getChoices().get(0).getMessage().getRole());
        assertEquals("Test response", response.getChoices().get(0).getMessage().getContent());
        assertEquals("stop", response.getChoices().get(0).getFinishReason());
    }

    @Test
    void jsonDeserialization_ShouldDeserializeCorrectly() throws Exception {
        String json = """
                {
                    "choices": [
                        {
                            "message": {
                                "role": "assistant",
                                "content": "Here's your workout plan..."
                            },
                            "finish_reason": "stop"
                        }
                    ]
                }
                """;

        OpenAIResponse response = objectMapper.readValue(json, OpenAIResponse.class);

        assertNotNull(response.getChoices());
        assertEquals(1, response.getChoices().size());
        assertEquals("assistant", response.getChoices().get(0).getMessage().getRole());
        assertEquals("Here's your workout plan...", response.getChoices().get(0).getMessage().getContent());
        assertEquals("stop", response.getChoices().get(0).getFinishReason());
    }

    @Test
    void jsonSerialization_ShouldSerializeCorrectly() throws Exception {
        OpenAIResponse response = new OpenAIResponse();

        OpenAIRequest.OpenAIMessage message = new OpenAIRequest.OpenAIMessage("assistant", "Workout plan content");
        OpenAIResponse.Choice choice = new OpenAIResponse.Choice();
        choice.setMessage(message);
        choice.setFinishReason("stop");

        response.setChoices(Arrays.asList(choice));

        String json = objectMapper.writeValueAsString(response);

        assertTrue(json.contains("\"choices\""));
        assertTrue(json.contains("\"role\":\"assistant\""));
        assertTrue(json.contains("\"content\":\"Workout plan content\""));
        assertTrue(json.contains("\"finish_reason\":\"stop\""));
    }

    @Test
    void choice_DefaultConstructor_ShouldCreateEmptyObject() {
        OpenAIResponse.Choice choice = new OpenAIResponse.Choice();

        assertNull(choice.getMessage());
        assertNull(choice.getFinishReason());
    }

    @Test
    void choice_SettersAndGetters_ShouldWorkCorrectly() {
        OpenAIResponse.Choice choice = new OpenAIResponse.Choice();
        OpenAIRequest.OpenAIMessage message = new OpenAIRequest.OpenAIMessage("assistant", "Test message");
        String finishReason = "stop";

        choice.setMessage(message);
        choice.setFinishReason(finishReason);

        assertEquals(message, choice.getMessage());
        assertEquals("assistant", choice.getMessage().getRole());
        assertEquals("Test message", choice.getMessage().getContent());
        assertEquals(finishReason, choice.getFinishReason());
    }

    @Test
    void multipleChoices_ShouldHandleCorrectly() throws Exception {
        String json = """
                {
                    "choices": [
                        {
                            "message": {
                                "role": "assistant",
                                "content": "First response"
                            },
                            "finish_reason": "stop"
                        },
                        {
                            "message": {
                                "role": "assistant",
                                "content": "Second response"
                            },
                            "finish_reason": "length"
                        }
                    ]
                }
                """;

        OpenAIResponse response = objectMapper.readValue(json, OpenAIResponse.class);

        assertNotNull(response.getChoices());
        assertEquals(2, response.getChoices().size());
        assertEquals("First response", response.getChoices().get(0).getMessage().getContent());
        assertEquals("stop", response.getChoices().get(0).getFinishReason());
        assertEquals("Second response", response.getChoices().get(1).getMessage().getContent());
        assertEquals("length", response.getChoices().get(1).getFinishReason());
    }
}