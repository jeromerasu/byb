package com.workoutplanner.repository;

import com.workoutplanner.model.PlanGenerationQueue;
import com.workoutplanner.model.QueueStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlanGenerationQueueRepository extends JpaRepository<PlanGenerationQueue, String> {

    // Check if user has an active (non-terminal) queue entry — for idempotency
    List<PlanGenerationQueue> findByUserIdAndStatusIn(String userId, List<QueueStatus> statuses);

    // Scan for claimable PENDING rows (used by 016A)
    List<PlanGenerationQueue> findByStatusAndAttemptCountLessThanAndScheduledAtBeforeOrderByScheduledAtAsc(
            QueueStatus status, int maxAttempts, LocalDateTime before, Pageable pageable);

    // Find stale CLAIMED rows (for lock recovery in 016A)
    List<PlanGenerationQueue> findByStatusAndLockedAtBefore(QueueStatus status, LocalDateTime before);

    // Count by status (used by 016E ops metrics)
    long countByStatus(QueueStatus status);

    // Count COMPLETED rows created after a timestamp (used by 016E 24h metric)
    long countByStatusAndCreatedAtAfter(QueueStatus status, LocalDateTime after);

    // Find oldest PENDING (used by 016E metrics)
    Optional<PlanGenerationQueue> findFirstByStatusOrderByScheduledAtAsc(QueueStatus status);

    // Cleanup: find terminal rows older than retention date (used by 016E cleanup)
    List<PlanGenerationQueue> findByStatusInAndUpdatedAtBefore(List<QueueStatus> statuses, LocalDateTime before);
}
