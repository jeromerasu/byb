package com.workoutplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class OpenAIRequest {

    private String model;
    private List<OpenAIMessage> messages;
    private Double temperature;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    public OpenAIRequest() {}

    public OpenAIRequest(String model, List<OpenAIMessage> messages, Double temperature, Integer maxTokens) {
        this.model = model;
        this.messages = messages;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
    }

    // Getters and setters
    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<OpenAIMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<OpenAIMessage> messages) {
        this.messages = messages;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public static class OpenAIMessage {
        private String role;
        private String content;

        public OpenAIMessage() {}

        public OpenAIMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}