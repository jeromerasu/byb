package com.workoutplanner.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for Issue 3: flaggedExercises and flaggedMeals must accept JSON arrays.
 */
class FeedbackRequestDeserializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void workoutFeedbackRequest_FlaggedExercisesArray_Deserializes() throws Exception {
        String json = """
                {
                  "rating": 4,
                  "flagged_exercises": ["squats", "bench press"],
                  "free_form_note": "knee felt off"
                }
                """;

        WorkoutFeedbackRequest req = objectMapper.readValue(json, WorkoutFeedbackRequest.class);

        assertThat(req.getFlaggedExercises()).containsExactly("squats", "bench press");
        assertThat(req.getRating()).isEqualTo(4);
    }

    @Test
    void workoutFeedbackRequest_EmptyFlaggedExercises_Deserializes() throws Exception {
        String json = """
                {
                  "rating": 5,
                  "flagged_exercises": []
                }
                """;

        WorkoutFeedbackRequest req = objectMapper.readValue(json, WorkoutFeedbackRequest.class);

        assertThat(req.getFlaggedExercises()).isEmpty();
    }

    @Test
    void workoutFeedbackRequest_NullFlaggedExercises_Deserializes() throws Exception {
        String json = """
                {
                  "rating": 3
                }
                """;

        assertThatNoException().isThrownBy(() -> {
            WorkoutFeedbackRequest req = objectMapper.readValue(json, WorkoutFeedbackRequest.class);
            assertThat(req.getFlaggedExercises()).isNull();
        });
    }

    @Test
    void dietFeedbackRequest_FlaggedMealsArray_Deserializes() throws Exception {
        String json = """
                {
                  "rating": 3,
                  "flagged_meals": ["chicken salad", "protein shake"],
                  "free_form_note": "too much sodium"
                }
                """;

        DietFeedbackRequest req = objectMapper.readValue(json, DietFeedbackRequest.class);

        assertThat(req.getFlaggedMeals()).containsExactly("chicken salad", "protein shake");
        assertThat(req.getRating()).isEqualTo(3);
    }

    @Test
    void dietFeedbackRequest_EmptyFlaggedMeals_Deserializes() throws Exception {
        String json = """
                {
                  "rating": 5,
                  "flagged_meals": []
                }
                """;

        DietFeedbackRequest req = objectMapper.readValue(json, DietFeedbackRequest.class);

        assertThat(req.getFlaggedMeals()).isEmpty();
    }

    @Test
    void dietFeedbackRequest_NullFlaggedMeals_Deserializes() throws Exception {
        String json = """
                {
                  "rating": 4
                }
                """;

        assertThatNoException().isThrownBy(() -> {
            DietFeedbackRequest req = objectMapper.readValue(json, DietFeedbackRequest.class);
            assertThat(req.getFlaggedMeals()).isNull();
        });
    }

    @Test
    void workoutFeedbackRequest_SessionCommentsAndFlaggedExercises_BothDeserialize() throws Exception {
        String json = """
                {
                  "rating": 4,
                  "session_comments": ["felt strong", "good form"],
                  "flagged_exercises": ["deadlift"],
                  "free_form_note": "lower back tightness"
                }
                """;

        WorkoutFeedbackRequest req = objectMapper.readValue(json, WorkoutFeedbackRequest.class);

        assertThat(req.getSessionComments()).containsExactly("felt strong", "good form");
        assertThat(req.getFlaggedExercises()).containsExactly("deadlift");
        assertThat(req.getFreeFormNote()).isEqualTo("lower back tightness");
    }
}
