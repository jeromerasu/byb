package com.workoutplanner.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Equipment {
    @JsonProperty("none")
    NONE("Bodyweight only", "figure.walk"),

    @JsonProperty("basic")
    BASIC("Basic equipment (dumbbells, resistance bands)", "dumbbell"),

    @JsonProperty("gym")
    GYM("Full gym access", "building.2"),

    @JsonProperty("home_gym")
    HOME_GYM("Complete home gym setup", "house");

    private final String displayName;
    private final String iconName;

    Equipment(String displayName, String iconName) {
        this.displayName = displayName;
        this.iconName = iconName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIconName() {
        return iconName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}