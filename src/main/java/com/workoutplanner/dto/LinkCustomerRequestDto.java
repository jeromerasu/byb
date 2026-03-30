package com.workoutplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LinkCustomerRequestDto {

    @JsonProperty("revenueCatCustomerId")
    private String revenueCatCustomerId;

    public LinkCustomerRequestDto() {}

    public LinkCustomerRequestDto(String revenueCatCustomerId) {
        this.revenueCatCustomerId = revenueCatCustomerId;
    }

    public String getRevenueCatCustomerId() { return revenueCatCustomerId; }
    public void setRevenueCatCustomerId(String revenueCatCustomerId) { this.revenueCatCustomerId = revenueCatCustomerId; }
}
