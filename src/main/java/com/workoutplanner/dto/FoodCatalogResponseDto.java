package com.workoutplanner.dto;

import com.workoutplanner.model.FoodCatalog;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class FoodCatalogResponseDto {

    private Long id;
    private String name;
    private String category;
    private String servingSize;
    private Integer calories;
    private BigDecimal proteinGrams;
    private BigDecimal carbsGrams;
    private BigDecimal fatGrams;
    private BigDecimal fiberGrams;
    private boolean isSystem;
    private String createdByUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static FoodCatalogResponseDto from(FoodCatalog entity) {
        FoodCatalogResponseDto dto = new FoodCatalogResponseDto();
        dto.id = entity.getId();
        dto.name = entity.getName();
        dto.category = entity.getCategory();
        dto.servingSize = entity.getServingSize();
        dto.calories = entity.getCalories();
        dto.proteinGrams = entity.getProteinGrams();
        dto.carbsGrams = entity.getCarbsGrams();
        dto.fatGrams = entity.getFatGrams();
        dto.fiberGrams = entity.getFiberGrams();
        dto.isSystem = entity.isSystem();
        dto.createdByUserId = entity.getCreatedByUserId();
        dto.createdAt = entity.getCreatedAt();
        dto.updatedAt = entity.getUpdatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getServingSize() { return servingSize; }
    public Integer getCalories() { return calories; }
    public BigDecimal getProteinGrams() { return proteinGrams; }
    public BigDecimal getCarbsGrams() { return carbsGrams; }
    public BigDecimal getFatGrams() { return fatGrams; }
    public BigDecimal getFiberGrams() { return fiberGrams; }
    public boolean isSystem() { return isSystem; }
    public String getCreatedByUserId() { return createdByUserId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
