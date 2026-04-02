package com.workoutplanner.model;

/**
 * Supported workout goal values for WorkoutProfile.targetGoals.
 * These are stored as strings in the database — the enum documents the valid set.
 */
public enum WorkoutGoal {

    // ── Core goals ───────────────────────────────────────────────────────────
    BUILD_MUSCLE,
    LOSE_WEIGHT,
    WEIGHT_LOSS,       // legacy alias kept for backwards compatibility
    GET_STRONGER,
    GENERAL_FITNESS,

    // ── Athletic performance ─────────────────────────────────────────────────
    /** Plyometrics-focused: box jumps, depth jumps, power cleans, squat jumps */
    ATHLETIC_PERFORMANCE_VERTICAL,

    /** Sprint work: agility ladder, shuttle runs, cone drills */
    ATHLETIC_PERFORMANCE_SPEED,

    /** Metabolic: HIIT, circuit training, tempo runs */
    ATHLETIC_PERFORMANCE_ENDURANCE,

    /** Sport-tailored: basketball, soccer, football, etc. */
    ATHLETIC_PERFORMANCE_SPORT_SPECIFIC,

    /** General athletic base without a specific subcategory */
    ATHLETIC_PERFORMANCE;

    /** Returns true if this goal belongs to the athletic performance category. */
    public boolean isAthleticPerformance() {
        return this == ATHLETIC_PERFORMANCE
                || this == ATHLETIC_PERFORMANCE_VERTICAL
                || this == ATHLETIC_PERFORMANCE_SPEED
                || this == ATHLETIC_PERFORMANCE_ENDURANCE
                || this == ATHLETIC_PERFORMANCE_SPORT_SPECIFIC;
    }
}
