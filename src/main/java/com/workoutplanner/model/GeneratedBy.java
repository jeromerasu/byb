package com.workoutplanner.model;

/**
 * Indicates who triggered plan generation for a given queue entry or week plan row.
 */
public enum GeneratedBy {
    /** Triggered by the nightly cron scheduler. */
    CRON,
    /** Triggered by a coach on behalf of their client. */
    COACH,
    /** Triggered manually by the user themselves or a direct API call. */
    MANUAL
}
