package com.workoutplanner.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExerciseTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void constructor_DefaultConstructor_ShouldCreateEmptyObject() {
        Exercise exercise = new Exercise();

        assertNull(exercise.getName());
        assertNull(exercise.getSets());
        assertNull(exercise.getReps());
        assertNull(exercise.getRestPeriod());
        assertNull(exercise.getInstructions());
    }

    @Test
    void constructor_WithParameters_ShouldSetAllFields() {
        String name = "Push-ups";
        String sets = "3";
        String reps = "10-15";
        String restPeriod = "60s";
        String instructions = "Keep your body straight";

        Exercise exercise = new Exercise(name, sets, reps, restPeriod, instructions);

        assertEquals(name, exercise.getName());
        assertEquals(sets, exercise.getSets());
        assertEquals(reps, exercise.getReps());
        assertEquals(restPeriod, exercise.getRestPeriod());
        assertEquals(instructions, exercise.getInstructions());
    }

    @Test
    void settersAndGetters_ShouldWorkCorrectly() {
        Exercise exercise = new Exercise();
        String name = "Squats";
        String sets = "4";
        String reps = "12-15";
        String restPeriod = "90s";
        String instructions = "Go down until thighs are parallel";

        exercise.setName(name);
        exercise.setSets(sets);
        exercise.setReps(reps);
        exercise.setRestPeriod(restPeriod);
        exercise.setInstructions(instructions);

        assertEquals(name, exercise.getName());
        assertEquals(sets, exercise.getSets());
        assertEquals(reps, exercise.getReps());
        assertEquals(restPeriod, exercise.getRestPeriod());
        assertEquals(instructions, exercise.getInstructions());
    }

    @Test
    void constructor_WithNullValues_ShouldAcceptNulls() {
        Exercise exercise = new Exercise(null, null, null, null, null);

        assertNull(exercise.getName());
        assertNull(exercise.getSets());
        assertNull(exercise.getReps());
        assertNull(exercise.getRestPeriod());
        assertNull(exercise.getInstructions());
    }

    @Test
    void jsonSerialization_ShouldSerializeCorrectly() throws Exception {
        Exercise exercise = new Exercise("Deadlifts", "3", "5", "2min", "Keep back straight");

        String json = objectMapper.writeValueAsString(exercise);

        assertTrue(json.contains("\"name\":\"Deadlifts\""));
        assertTrue(json.contains("\"sets\":\"3\""));
        assertTrue(json.contains("\"reps\":\"5\""));
        assertTrue(json.contains("\"rest_period\":\"2min\""));
        assertTrue(json.contains("\"instructions\":\"Keep back straight\""));
    }

    @Test
    void jsonDeserialization_ShouldDeserializeCorrectly() throws Exception {
        String json = """
                {
                    "name": "Bench Press",
                    "sets": "4",
                    "reps": "8-10",
                    "rest_period": "3min",
                    "instructions": "Control the weight"
                }
                """;

        Exercise exercise = objectMapper.readValue(json, Exercise.class);

        assertEquals("Bench Press", exercise.getName());
        assertEquals("4", exercise.getSets());
        assertEquals("8-10", exercise.getReps());
        assertEquals("3min", exercise.getRestPeriod());
        assertEquals("Control the weight", exercise.getInstructions());
    }

    @Test
    void jsonDeserialization_WithMissingFields_ShouldHandleGracefully() throws Exception {
        String json = """
                {
                    "name": "Pull-ups",
                    "sets": "3"
                }
                """;

        Exercise exercise = objectMapper.readValue(json, Exercise.class);

        assertEquals("Pull-ups", exercise.getName());
        assertEquals("3", exercise.getSets());
        assertNull(exercise.getReps());
        assertNull(exercise.getRestPeriod());
        assertNull(exercise.getInstructions());
    }
}