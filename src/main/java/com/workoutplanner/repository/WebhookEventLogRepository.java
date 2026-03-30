package com.workoutplanner.repository;

import com.workoutplanner.model.WebhookEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebhookEventLogRepository extends JpaRepository<WebhookEventLog, Long> {

    List<WebhookEventLog> findByUserId(String userId);

    List<WebhookEventLog> findByProviderCustomerId(String providerCustomerId);

    List<WebhookEventLog> findByEventType(String eventType);

    List<WebhookEventLog> findByProcessedSuccessfully(boolean processedSuccessfully);
}
