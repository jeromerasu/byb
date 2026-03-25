package com.workoutplanner.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RevenueCatWebhookDto {

    @JsonProperty("event")
    private Event event;

    @JsonProperty("api_version")
    private String apiVersion;

    public static class Event {
        @JsonProperty("type")
        private String type;

        @JsonProperty("id")
        private String id;

        @JsonProperty("event_timestamp_ms")
        private Long eventTimestampMs;

        @JsonProperty("app_id")
        private String appId;

        @JsonProperty("app_user_id")
        private String appUserId;

        @JsonProperty("original_app_user_id")
        private String originalAppUserId;

        @JsonProperty("aliases")
        private String[] aliases;

        @JsonProperty("product_id")
        private String productId;

        @JsonProperty("period_type")
        private String periodType;

        @JsonProperty("purchased_at_ms")
        private Long purchasedAtMs;

        @JsonProperty("expiration_at_ms")
        private Long expirationAtMs;

        @JsonProperty("environment")
        private String environment;

        @JsonProperty("entitlement_id")
        private String entitlementId;

        @JsonProperty("entitlement_ids")
        private String[] entitlementIds;

        @JsonProperty("presented_offering_id")
        private String presentedOfferingId;

        @JsonProperty("transaction_id")
        private String transactionId;

        @JsonProperty("original_transaction_id")
        private String originalTransactionId;

        @JsonProperty("is_family_share")
        private Boolean isFamilyShare;

        @JsonProperty("country_code")
        private String countryCode;

        @JsonProperty("app_version")
        private String appVersion;

        @JsonProperty("commission_percentage")
        private Double commissionPercentage;

        @JsonProperty("price")
        private Double price;

        @JsonProperty("price_in_usd")
        private Double priceInUsd;

        @JsonProperty("currency")
        private String currency;

        @JsonProperty("subscriber_attributes")
        private Object subscriberAttributes;

        @JsonProperty("store")
        private String store;

        @JsonProperty("takehome_percentage")
        private Double takehomePercentage;

        @JsonProperty("offer_code")
        private String offerCode;

        // Getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public Long getEventTimestampMs() { return eventTimestampMs; }
        public void setEventTimestampMs(Long eventTimestampMs) { this.eventTimestampMs = eventTimestampMs; }

        public String getAppId() { return appId; }
        public void setAppId(String appId) { this.appId = appId; }

        public String getAppUserId() { return appUserId; }
        public void setAppUserId(String appUserId) { this.appUserId = appUserId; }

        public String getOriginalAppUserId() { return originalAppUserId; }
        public void setOriginalAppUserId(String originalAppUserId) { this.originalAppUserId = originalAppUserId; }

        public String[] getAliases() { return aliases; }
        public void setAliases(String[] aliases) { this.aliases = aliases; }

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }

        public String getPeriodType() { return periodType; }
        public void setPeriodType(String periodType) { this.periodType = periodType; }

        public Long getPurchasedAtMs() { return purchasedAtMs; }
        public void setPurchasedAtMs(Long purchasedAtMs) { this.purchasedAtMs = purchasedAtMs; }

        public Long getExpirationAtMs() { return expirationAtMs; }
        public void setExpirationAtMs(Long expirationAtMs) { this.expirationAtMs = expirationAtMs; }

        public String getEnvironment() { return environment; }
        public void setEnvironment(String environment) { this.environment = environment; }

        public String getEntitlementId() { return entitlementId; }
        public void setEntitlementId(String entitlementId) { this.entitlementId = entitlementId; }

        public String[] getEntitlementIds() { return entitlementIds; }
        public void setEntitlementIds(String[] entitlementIds) { this.entitlementIds = entitlementIds; }

        public String getPresentedOfferingId() { return presentedOfferingId; }
        public void setPresentedOfferingId(String presentedOfferingId) { this.presentedOfferingId = presentedOfferingId; }

        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

        public String getOriginalTransactionId() { return originalTransactionId; }
        public void setOriginalTransactionId(String originalTransactionId) { this.originalTransactionId = originalTransactionId; }

        public Boolean getIsFamilyShare() { return isFamilyShare; }
        public void setIsFamilyShare(Boolean isFamilyShare) { this.isFamilyShare = isFamilyShare; }

        public String getCountryCode() { return countryCode; }
        public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

        public String getAppVersion() { return appVersion; }
        public void setAppVersion(String appVersion) { this.appVersion = appVersion; }

        public Double getCommissionPercentage() { return commissionPercentage; }
        public void setCommissionPercentage(Double commissionPercentage) { this.commissionPercentage = commissionPercentage; }

        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }

        public Double getPriceInUsd() { return priceInUsd; }
        public void setPriceInUsd(Double priceInUsd) { this.priceInUsd = priceInUsd; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }

        public Object getSubscriberAttributes() { return subscriberAttributes; }
        public void setSubscriberAttributes(Object subscriberAttributes) { this.subscriberAttributes = subscriberAttributes; }

        public String getStore() { return store; }
        public void setStore(String store) { this.store = store; }

        public Double getTakehomePercentage() { return takehomePercentage; }
        public void setTakehomePercentage(Double takehomePercentage) { this.takehomePercentage = takehomePercentage; }

        public String getOfferCode() { return offerCode; }
        public void setOfferCode(String offerCode) { this.offerCode = offerCode; }

        // Helper methods
        public LocalDateTime getEventTimestamp() {
            return eventTimestampMs != null ?
                LocalDateTime.now().plusNanos((eventTimestampMs - System.currentTimeMillis()) * 1_000_000) : null;
        }

        public LocalDateTime getPurchasedAt() {
            return purchasedAtMs != null ?
                LocalDateTime.now().plusNanos((purchasedAtMs - System.currentTimeMillis()) * 1_000_000) : null;
        }

        public LocalDateTime getExpirationAt() {
            return expirationAtMs != null ?
                LocalDateTime.now().plusNanos((expirationAtMs - System.currentTimeMillis()) * 1_000_000) : null;
        }
    }

    // Getters and setters
    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }
}