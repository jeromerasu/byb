package com.workoutplanner.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BodyweightResponse {

    private LocalDate date;
    private BigDecimal weight;
    private String unit;

    public BodyweightResponse() {}

    public BodyweightResponse(LocalDate date, BigDecimal weight, String unit) {
        this.date = date;
        this.weight = weight;
        this.unit = unit;
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
}
