package com.workoutplanner.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "webhook_event_log")
public class WebhookEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "provider_customer_id")
    private String providerCustomerId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "event_payload", nullable = false, columnDefinition = "TEXT")
    private String eventPayload;

    @Column(name = "processed_successfully", nullable = false)
    private boolean processedSuccessfully = false;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    public WebhookEventLog() {
        this.receivedAt = LocalDateTime.now();
    }

    public WebhookEventLog(String userId, String providerCustomerId, String eventType, String eventPayload) {
        this();
        this.userId = userId;
        this.providerCustomerId = providerCustomerId;
        this.eventType = eventType;
        this.eventPayload = eventPayload;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getProviderCustomerId() { return providerCustomerId; }
    public void setProviderCustomerId(String providerCustomerId) { this.providerCustomerId = providerCustomerId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getEventPayload() { return eventPayload; }
    public void setEventPayload(String eventPayload) { this.eventPayload = eventPayload; }

    public boolean isProcessedSuccessfully() { return processedSuccessfully; }
    public void setProcessedSuccessfully(boolean processedSuccessfully) { this.processedSuccessfully = processedSuccessfully; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }
}
