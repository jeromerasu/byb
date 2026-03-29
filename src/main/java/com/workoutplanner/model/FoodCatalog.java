package com.workoutplanner.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "food_catalog",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_food_catalog_name_user",
        columnNames = {"name", "created_by_user_id"}
    )
)
public class FoodCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Food name is required")
    @Column(nullable = false)
    private String name;

    @Column(length = 50)
    private String category;

    @JsonProperty("serving_size")
    @Column(name = "serving_size", length = 100)
    private String servingSize;

    @Column
    private Integer calories;

    @JsonProperty("protein_grams")
    @Column(name = "protein_grams", precision = 8, scale = 2)
    private BigDecimal proteinGrams;

    @JsonProperty("carbs_grams")
    @Column(name = "carbs_grams", precision = 8, scale = 2)
    private BigDecimal carbsGrams;

    @JsonProperty("fat_grams")
    @Column(name = "fat_grams", precision = 8, scale = 2)
    private BigDecimal fatGrams;

    @JsonProperty("fiber_grams")
    @Column(name = "fiber_grams", precision = 8, scale = 2)
    private BigDecimal fiberGrams;

    @JsonProperty("is_system")
    @Column(name = "is_system", nullable = false)
    private boolean isSystem = true;

    @JsonProperty("created_by_user_id")
    @Column(name = "created_by_user_id")
    private String createdByUserId;

    @JsonProperty("created_at")
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public FoodCatalog() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getServingSize() { return servingSize; }
    public void setServingSize(String servingSize) { this.servingSize = servingSize; }

    public Integer getCalories() { return calories; }
    public void setCalories(Integer calories) { this.calories = calories; }

    public BigDecimal getProteinGrams() { return proteinGrams; }
    public void setProteinGrams(BigDecimal proteinGrams) { this.proteinGrams = proteinGrams; }

    public BigDecimal getCarbsGrams() { return carbsGrams; }
    public void setCarbsGrams(BigDecimal carbsGrams) { this.carbsGrams = carbsGrams; }

    public BigDecimal getFatGrams() { return fatGrams; }
    public void setFatGrams(BigDecimal fatGrams) { this.fatGrams = fatGrams; }

    public BigDecimal getFiberGrams() { return fiberGrams; }
    public void setFiberGrams(BigDecimal fiberGrams) { this.fiberGrams = fiberGrams; }

    public boolean isSystem() { return isSystem; }
    public void setSystem(boolean system) { isSystem = system; }

    public String getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(String createdByUserId) { this.createdByUserId = createdByUserId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
