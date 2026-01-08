package com.workoutplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class OpenAIResponse {

    private List<Choice> choices;

    public OpenAIResponse() {}

    public List<Choice> getChoices() {
        return choices;
    }

    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }

    public static class Choice {
        private OpenAIRequest.OpenAIMessage message;

        @JsonProperty("finish_reason")
        private String finishReason;

        public Choice() {}

        public OpenAIRequest.OpenAIMessage getMessage() {
            return message;
        }

        public void setMessage(OpenAIRequest.OpenAIMessage message) {
            this.message = message;
        }

        public String getFinishReason() {
            return finishReason;
        }

        public void setFinishReason(String finishReason) {
            this.finishReason = finishReason;
        }
    }
}