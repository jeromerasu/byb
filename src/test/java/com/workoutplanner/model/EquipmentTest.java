package com.workoutplanner.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EquipmentTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void enumValues_ShouldHaveCorrectDisplayNames() {
        assertEquals("Bodyweight only", Equipment.NONE.getDisplayName());
        assertEquals("Basic equipment (dumbbells, resistance bands)", Equipment.BASIC.getDisplayName());
        assertEquals("Full gym access", Equipment.GYM.getDisplayName());
        assertEquals("Complete home gym setup", Equipment.HOME_GYM.getDisplayName());
    }

    @Test
    void enumValues_ShouldHaveCorrectIconNames() {
        assertEquals("figure.walk", Equipment.NONE.getIconName());
        assertEquals("dumbbell", Equipment.BASIC.getIconName());
        assertEquals("building.2", Equipment.GYM.getIconName());
        assertEquals("house", Equipment.HOME_GYM.getIconName());
    }

    @Test
    void toString_ShouldReturnDisplayName() {
        assertEquals("Bodyweight only", Equipment.NONE.toString());
        assertEquals("Basic equipment (dumbbells, resistance bands)", Equipment.BASIC.toString());
        assertEquals("Full gym access", Equipment.GYM.toString());
        assertEquals("Complete home gym setup", Equipment.HOME_GYM.toString());
    }

    @Test
    void enumValues_ShouldHaveCorrectCount() {
        Equipment[] values = Equipment.values();
        assertEquals(4, values.length);
    }

    @Test
    void enumValues_ShouldBeDistinct() {
        Equipment[] values = Equipment.values();

        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                assertNotEquals(values[i], values[j]);
                assertNotEquals(values[i].getDisplayName(), values[j].getDisplayName());
                assertNotEquals(values[i].getIconName(), values[j].getIconName());
            }
        }
    }

    @Test
    void jsonSerialization_ShouldSerializeToJsonProperty() throws Exception {
        assertEquals("\"none\"", objectMapper.writeValueAsString(Equipment.NONE));
        assertEquals("\"basic\"", objectMapper.writeValueAsString(Equipment.BASIC));
        assertEquals("\"gym\"", objectMapper.writeValueAsString(Equipment.GYM));
        assertEquals("\"home_gym\"", objectMapper.writeValueAsString(Equipment.HOME_GYM));
    }

    @Test
    void jsonDeserialization_ShouldDeserializeFromJsonProperty() throws Exception {
        assertEquals(Equipment.NONE, objectMapper.readValue("\"none\"", Equipment.class));
        assertEquals(Equipment.BASIC, objectMapper.readValue("\"basic\"", Equipment.class));
        assertEquals(Equipment.GYM, objectMapper.readValue("\"gym\"", Equipment.class));
        assertEquals(Equipment.HOME_GYM, objectMapper.readValue("\"home_gym\"", Equipment.class));
    }

    @Test
    void valueOf_ShouldReturnCorrectEnumValues() {
        assertEquals(Equipment.NONE, Equipment.valueOf("NONE"));
        assertEquals(Equipment.BASIC, Equipment.valueOf("BASIC"));
        assertEquals(Equipment.GYM, Equipment.valueOf("GYM"));
        assertEquals(Equipment.HOME_GYM, Equipment.valueOf("HOME_GYM"));
    }

    @Test
    void valueOf_WithInvalidValue_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> Equipment.valueOf("INVALID"));
    }
}