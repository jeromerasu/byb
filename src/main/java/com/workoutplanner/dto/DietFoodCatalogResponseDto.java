package com.workoutplanner.dto;

import java.util.List;

public class DietFoodCatalogResponseDto {
    private String source;
    private Integer count;
    private List<FoodDto> foods;

    // Constructors
    public DietFoodCatalogResponseDto() {}

    public DietFoodCatalogResponseDto(String source, Integer count, List<FoodDto> foods) {
        this.source = source;
        this.count = count;
        this.foods = foods;
    }

    // Getters and setters
    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public List<FoodDto> getFoods() {
        return foods;
    }

    public void setFoods(List<FoodDto> foods) {
        this.foods = foods;
    }

    // Nested DTO
    public static class FoodDto {
        private String foodId;
        private String name;
        private String mealType;
        private Integer calories;
        private Integer proteinGrams;
        private Integer carbsGrams;
        private Integer fatGrams;
        private String lastSeenWeek;
        private String lastSeenDay;

        // Constructors
        public FoodDto() {}

        public FoodDto(String foodId, String name, String mealType, Integer calories, Integer proteinGrams) {
            this.foodId = foodId;
            this.name = name;
            this.mealType = mealType;
            this.calories = calories;
            this.proteinGrams = proteinGrams;
        }

        // Getters and setters
        public String getFoodId() {
            return foodId;
        }

        public void setFoodId(String foodId) {
            this.foodId = foodId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getMealType() {
            return mealType;
        }

        public void setMealType(String mealType) {
            this.mealType = mealType;
        }

        public Integer getCalories() {
            return calories;
        }

        public void setCalories(Integer calories) {
            this.calories = calories;
        }

        public Integer getProteinGrams() {
            return proteinGrams;
        }

        public void setProteinGrams(Integer proteinGrams) {
            this.proteinGrams = proteinGrams;
        }

        public Integer getCarbsGrams() {
            return carbsGrams;
        }

        public void setCarbsGrams(Integer carbsGrams) {
            this.carbsGrams = carbsGrams;
        }

        public Integer getFatGrams() {
            return fatGrams;
        }

        public void setFatGrams(Integer fatGrams) {
            this.fatGrams = fatGrams;
        }

        public String getLastSeenWeek() {
            return lastSeenWeek;
        }

        public void setLastSeenWeek(String lastSeenWeek) {
            this.lastSeenWeek = lastSeenWeek;
        }

        public String getLastSeenDay() {
            return lastSeenDay;
        }

        public void setLastSeenDay(String lastSeenDay) {
            this.lastSeenDay = lastSeenDay;
        }
    }
}