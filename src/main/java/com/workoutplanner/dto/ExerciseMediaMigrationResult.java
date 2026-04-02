package com.workoutplanner.dto;

/**
 * Summary returned by {@code ExerciseMediaMigrationService#migrateAll()}.
 */
public class ExerciseMediaMigrationResult {

    private final int succeeded;
    private final int failed;
    private final int skipped;
    private final String message;

    public ExerciseMediaMigrationResult(int succeeded, int failed, int skipped) {
        this(succeeded, failed, skipped, null);
    }

    public ExerciseMediaMigrationResult(int succeeded, int failed, int skipped, String message) {
        this.succeeded = succeeded;
        this.failed = failed;
        this.skipped = skipped;
        this.message = message;
    }

    public int getSucceeded() { return succeeded; }
    public int getFailed()    { return failed; }
    public int getSkipped()   { return skipped; }
    public String getMessage() { return message; }
}
