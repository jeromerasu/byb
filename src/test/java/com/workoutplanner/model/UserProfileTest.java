package com.workoutplanner.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserProfileTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void constructor_DefaultConstructor_ShouldSetCreatedAt() {
        UserProfile profile = new UserProfile();

        assertNotNull(profile.getCreatedAt());
        assertTrue(profile.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void constructor_WithParameters_ShouldSetAllFieldsAndCreatedAt() {
        Integer age = 25;
        Equipment equipment = Equipment.NONE;
        Integer weeklyFrequency = 3;

        UserProfile profile = new UserProfile(age, equipment, weeklyFrequency);

        assertEquals(age, profile.getAge());
        assertEquals(equipment, profile.getEquipment());
        assertEquals(weeklyFrequency, profile.getWeeklyFrequency());
        assertNotNull(profile.getCreatedAt());
        assertTrue(profile.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void validation_ValidProfile_ShouldHaveNoViolations() {
        UserProfile profile = new UserProfile(25, Equipment.NONE, 3);

        Set<ConstraintViolation<UserProfile>> violations = validator.validate(profile);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validation_AgeBelow16_ShouldHaveViolation() {
        UserProfile profile = new UserProfile(15, Equipment.NONE, 3);

        Set<ConstraintViolation<UserProfile>> violations = validator.validate(profile);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("age")));
    }

    @Test
    void validation_AgeAbove80_ShouldHaveViolation() {
        UserProfile profile = new UserProfile(85, Equipment.NONE, 3);

        Set<ConstraintViolation<UserProfile>> violations = validator.validate(profile);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("age")));
    }

    @Test
    void validation_NullAge_ShouldHaveViolation() {
        UserProfile profile = new UserProfile(null, Equipment.NONE, 3);

        Set<ConstraintViolation<UserProfile>> violations = validator.validate(profile);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("age")));
    }

    @Test
    void validation_NullEquipment_ShouldHaveViolation() {
        UserProfile profile = new UserProfile(25, null, 3);

        Set<ConstraintViolation<UserProfile>> violations = validator.validate(profile);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("equipment")));
    }

    @Test
    void validation_WeeklyFrequencyBelow1_ShouldHaveViolation() {
        UserProfile profile = new UserProfile(25, Equipment.NONE, 0);

        Set<ConstraintViolation<UserProfile>> violations = validator.validate(profile);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("weeklyFrequency")));
    }

    @Test
    void validation_WeeklyFrequencyAbove7_ShouldHaveViolation() {
        UserProfile profile = new UserProfile(25, Equipment.NONE, 8);

        Set<ConstraintViolation<UserProfile>> violations = validator.validate(profile);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("weeklyFrequency")));
    }

    @Test
    void validation_NullWeeklyFrequency_ShouldHaveViolation() {
        UserProfile profile = new UserProfile(25, Equipment.NONE, null);

        Set<ConstraintViolation<UserProfile>> violations = validator.validate(profile);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("weeklyFrequency")));
    }

    @Test
    void settersAndGetters_ShouldWorkCorrectly() {
        UserProfile profile = new UserProfile();
        Integer age = 30;
        Equipment equipment = Equipment.GYM;
        Integer weeklyFrequency = 5;
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);

        profile.setAge(age);
        profile.setEquipment(equipment);
        profile.setWeeklyFrequency(weeklyFrequency);
        profile.setCreatedAt(createdAt);

        assertEquals(age, profile.getAge());
        assertEquals(equipment, profile.getEquipment());
        assertEquals(weeklyFrequency, profile.getWeeklyFrequency());
        assertEquals(createdAt, profile.getCreatedAt());
    }
}