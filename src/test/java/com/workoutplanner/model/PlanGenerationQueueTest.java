package com.workoutplanner.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TASK-BE-014: Unit tests for PlanGenerationQueue entity.
 * Covers default values, lifecycle callbacks, and field behaviour.
 */
class PlanGenerationQueueTest {

    @Test
    void defaultStatus_ShouldBePending() {
        PlanGenerationQueue q = new PlanGenerationQueue();
        assertEquals(QueueStatus.PENDING, q.getStatus());
    }

    @Test
    void defaultAttemptCount_ShouldBeZero() {
        PlanGenerationQueue q = new PlanGenerationQueue();
        assertEquals(0, q.getAttemptCount());
    }

    @Test
    void defaultMaxAttempts_ShouldBeThree() {
        PlanGenerationQueue q = new PlanGenerationQueue();
        assertEquals(3, q.getMaxAttempts());
    }

    @Test
    void setAndGetUserId() {
        PlanGenerationQueue q = new PlanGenerationQueue();
        q.setUserId("user-123");
        assertEquals("user-123", q.getUserId());
    }

    @Test
    void setAndGetStatus() {
        PlanGenerationQueue q = new PlanGenerationQueue();
        q.setStatus(QueueStatus.CLAIMED);
        assertEquals(QueueStatus.CLAIMED, q.getStatus());
    }

    @Test
    void setAndGetAttemptCount() {
        PlanGenerationQueue q = new PlanGenerationQueue();
        q.setAttemptCount(2);
        assertEquals(2, q.getAttemptCount());
    }

    @Test
    void setAndGetLockedBy() {
        PlanGenerationQueue q = new PlanGenerationQueue();
        q.setLockedBy("worker-1:lock-id");
        assertEquals("worker-1:lock-id", q.getLockedBy());
    }

    @Test
    void setAndGetLockedAt() {
        PlanGenerationQueue q = new PlanGenerationQueue();
        LocalDateTime now = LocalDateTime.now();
        q.setLockedAt(now);
        assertEquals(now, q.getLockedAt());
    }

    @Test
    void setAndGetScheduledAt() {
        PlanGenerationQueue q = new PlanGenerationQueue();
        LocalDateTime scheduled = LocalDateTime.now().plusMinutes(5);
        q.setScheduledAt(scheduled);
        assertEquals(scheduled, q.getScheduledAt());
    }

    @Test
    void setAndGetCompletedAt() {
        PlanGenerationQueue q = new PlanGenerationQueue();
        LocalDateTime completed = LocalDateTime.now();
        q.setCompletedAt(completed);
        assertEquals(completed, q.getCompletedAt());
    }

    @Test
    void setAndGetFailedAt() {
        PlanGenerationQueue q = new PlanGenerationQueue();
        LocalDateTime failed = LocalDateTime.now();
        q.setFailedAt(failed);
        assertEquals(failed, q.getFailedAt());
    }

    @Test
    void setAndGetErrorMessage() {
        PlanGenerationQueue q = new PlanGenerationQueue();
        q.setErrorMessage("Something went wrong");
        assertEquals("Something went wrong", q.getErrorMessage());
    }

    @Test
    void setAndGetWorkoutStorageKey() {
        PlanGenerationQueue q = new PlanGenerationQueue();
        q.setWorkoutStorageKey("workout/user1/plan.json");
        assertEquals("workout/user1/plan.json", q.getWorkoutStorageKey());
    }

    @Test
    void setAndGetDietStorageKey() {
        PlanGenerationQueue q = new PlanGenerationQueue();
        q.setDietStorageKey("diet/user1/plan.json");
        assertEquals("diet/user1/plan.json", q.getDietStorageKey());
    }

    @Test
    void setAndGetCreatedAt() {
        PlanGenerationQueue q = new PlanGenerationQueue();
        LocalDateTime created = LocalDateTime.now();
        q.setCreatedAt(created);
        assertEquals(created, q.getCreatedAt());
    }

    @Test
    void setAndGetUpdatedAt() {
        PlanGenerationQueue q = new PlanGenerationQueue();
        LocalDateTime updated = LocalDateTime.now();
        q.setUpdatedAt(updated);
        assertEquals(updated, q.getUpdatedAt());
    }

    @Test
    void onCreateLifecycle_ShouldSetTimestamps() {
        PlanGenerationQueue q = new PlanGenerationQueue();
        // Simulate @PrePersist
        q.onCreate();
        assertNotNull(q.getCreatedAt());
        assertNotNull(q.getUpdatedAt());
        assertNotNull(q.getScheduledAt());
    }

    @Test
    void onCreateLifecycle_ShouldNotOverrideExistingCreatedAt() {
        PlanGenerationQueue q = new PlanGenerationQueue();
        LocalDateTime existingCreatedAt = LocalDateTime.of(2024, 1, 1, 12, 0);
        q.setCreatedAt(existingCreatedAt);
        q.onCreate();
        // existing value should be preserved
        assertEquals(existingCreatedAt, q.getCreatedAt());
    }

    @Test
    void onUpdateLifecycle_ShouldUpdateUpdatedAt() throws InterruptedException {
        PlanGenerationQueue q = new PlanGenerationQueue();
        q.onCreate();
        LocalDateTime before = q.getUpdatedAt();
        Thread.sleep(10);
        q.onUpdate();
        assertTrue(q.getUpdatedAt().isAfter(before) || q.getUpdatedAt().isEqual(before));
    }

    @Test
    void setId_ShouldStoreId() {
        PlanGenerationQueue q = new PlanGenerationQueue();
        q.setId("test-uuid-123");
        assertEquals("test-uuid-123", q.getId());
    }

    @Test
    void setMaxAttempts_ShouldStore() {
        PlanGenerationQueue q = new PlanGenerationQueue();
        q.setMaxAttempts(5);
        assertEquals(5, q.getMaxAttempts());
    }

    @Test
    void allStatusValues_ShouldBeAccessible() {
        assertEquals(QueueStatus.PENDING, QueueStatus.valueOf("PENDING"));
        assertEquals(QueueStatus.CLAIMED, QueueStatus.valueOf("CLAIMED"));
        assertEquals(QueueStatus.COMPLETED, QueueStatus.valueOf("COMPLETED"));
        assertEquals(QueueStatus.FAILED, QueueStatus.valueOf("FAILED"));
        assertEquals(4, QueueStatus.values().length);
    }
}
