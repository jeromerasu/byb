package com.workoutplanner.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Exercise {

    private String name;
    private String sets;
    private String reps;

    @JsonProperty("rest_period")
    private String restPeriod;

    private String instructions;

    public Exercise() {}

    public Exercise(String name, String sets, String reps, String restPeriod, String instructions) {
        this.name = name;
        this.sets = sets;
        this.reps = reps;
        this.restPeriod = restPeriod;
        this.instructions = instructions;
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSets() {
        return sets;
    }

    public void setSets(String sets) {
        this.sets = sets;
    }

    public String getReps() {
        return reps;
    }

    public void setReps(String reps) {
        this.reps = reps;
    }

    public String getRestPeriod() {
        return restPeriod;
    }

    public void setRestPeriod(String restPeriod) {
        this.restPeriod = restPeriod;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }
}