package com.workoutplanner.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression tests for Bug 1: POST /api/v1/workout/logs 500 — sets field fails
 * when the frontend sends an array of set objects instead of a scalar Integer.
 */
class WorkoutLogRequestDeserializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void sets_AsScalarInteger_Deserializes() throws Exception {
        String json = """
                {
                  "exercise": "Bench Press",
                  "sets": 3,
                  "reps": 10,
                  "date": "2026-04-03"
                }
                """;

        WorkoutLogRequest req = objectMapper.readValue(json, WorkoutLogRequest.class);

        assertThat(req.getSets()).isEqualTo(3);
        assertThat(req.getReps()).isEqualTo(10);
    }

    @Test
    void sets_AsArrayOfObjects_UsesArrayLengthAsCount() throws Exception {
        // This is the format the frontend sends when logging from the exercise plan
        String json = """
                {
                  "exercise": "Squat",
                  "sets": [
                    {"reps": 10, "weight": 80},
                    {"reps": 10, "weight": 80},
                    {"reps": 8,  "weight": 85}
                  ],
                  "reps": 10,
                  "date": "2026-04-03"
                }
                """;

        WorkoutLogRequest req = objectMapper.readValue(json, WorkoutLogRequest.class);

        assertThat(req.getSets()).isEqualTo(3);
    }

    @Test
    void sets_AsEmptyArray_ReturnsZero() throws Exception {
        String json = """
                {
                  "exercise": "Deadlift",
                  "sets": [],
                  "date": "2026-04-03"
                }
                """;

        WorkoutLogRequest req = objectMapper.readValue(json, WorkoutLogRequest.class);

        assertThat(req.getSets()).isEqualTo(0);
    }

    @Test
    void sets_AsNull_ReturnsNull() throws Exception {
        String json = """
                {
                  "exercise": "Pull-up",
                  "sets": null,
                  "date": "2026-04-03"
                }
                """;

        WorkoutLogRequest req = objectMapper.readValue(json, WorkoutLogRequest.class);

        assertThat(req.getSets()).isNull();
    }

    @Test
    void sets_AsArrayOfIntegers_UsesArrayLength() throws Exception {
        String json = """
                {
                  "exercise": "Curl",
                  "sets": [12, 10, 8],
                  "date": "2026-04-03"
                }
                """;

        WorkoutLogRequest req = objectMapper.readValue(json, WorkoutLogRequest.class);

        assertThat(req.getSets()).isEqualTo(3);
    }
}
