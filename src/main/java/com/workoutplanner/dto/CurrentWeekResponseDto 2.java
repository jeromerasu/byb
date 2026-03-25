package com.workoutplanner.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class CurrentWeekResponseDto {
    private String source;
    private Integer weekIndex;
    private String weekKey;
    private WorkoutWeekDto workoutWeek;
    private DietWeekDto dietWeek;
    private List<ExerciseCatalogDto> exerciseCatalog;
    private List<DietFoodCatalogDto> dietFoodCatalog;

    // Constructors
    public CurrentWeekResponseDto() {}

    public CurrentWeekResponseDto(String source, Integer weekIndex, String weekKey) {
        this.source = source;
        this.weekIndex = weekIndex;
        this.weekKey = weekKey;
    }

    // Getters and setters
    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Integer getWeekIndex() {
        return weekIndex;
    }

    public void setWeekIndex(Integer weekIndex) {
        this.weekIndex = weekIndex;
    }

    public String getWeekKey() {
        return weekKey;
    }

    public void setWeekKey(String weekKey) {
        this.weekKey = weekKey;
    }

    public WorkoutWeekDto getWorkoutWeek() {
        return workoutWeek;
    }

    public void setWorkoutWeek(WorkoutWeekDto workoutWeek) {
        this.workoutWeek = workoutWeek;
    }

    public DietWeekDto getDietWeek() {
        return dietWeek;
    }

    public void setDietWeek(DietWeekDto dietWeek) {
        this.dietWeek = dietWeek;
    }

    public List<ExerciseCatalogDto> getExerciseCatalog() {
        return exerciseCatalog;
    }

    public void setExerciseCatalog(List<ExerciseCatalogDto> exerciseCatalog) {
        this.exerciseCatalog = exerciseCatalog;
    }

    public List<DietFoodCatalogDto> getDietFoodCatalog() {
        return dietFoodCatalog;
    }

    public void setDietFoodCatalog(List<DietFoodCatalogDto> dietFoodCatalog) {
        this.dietFoodCatalog = dietFoodCatalog;
    }

    // Nested DTOs
    public static class WorkoutWeekDto {
        private Boolean done;
        private Map<String, WorkoutDayDto> days;

        public WorkoutWeekDto() {}

        public WorkoutWeekDto(Boolean done, Map<String, WorkoutDayDto> days) {
            this.done = done;
            this.days = days;
        }

        public Boolean getDone() {
            return done;
        }

        public void setDone(Boolean done) {
            this.done = done;
        }

        public Map<String, WorkoutDayDto> getDays() {
            return days;
        }

        public void setDays(Map<String, WorkoutDayDto> days) {
            this.days = days;
        }
    }

    public static class WorkoutDayDto {
        private Boolean done;
        private String focus;
        private List<ExerciseDto> exercises;

        public WorkoutDayDto() {}

        public WorkoutDayDto(Boolean done, String focus, List<ExerciseDto> exercises) {
            this.done = done;
            this.focus = focus;
            this.exercises = exercises;
        }

        public Boolean getDone() {
            return done;
        }

        public void setDone(Boolean done) {
            this.done = done;
        }

        public String getFocus() {
            return focus;
        }

        public void setFocus(String focus) {
            this.focus = focus;
        }

        public List<ExerciseDto> getExercises() {
            return exercises;
        }

        public void setExercises(List<ExerciseDto> exercises) {
            this.exercises = exercises;
        }
    }

    public static class ExerciseDto {
        private String name;
        private Integer sets;
        private String reps;
        private Integer weightLbs;
        private String weightType;
        private Integer restSeconds;

        public ExerciseDto() {}

        public ExerciseDto(String name, Integer sets, String reps, Integer weightLbs) {
            this.name = name;
            this.sets = sets;
            this.reps = reps;
            this.weightLbs = weightLbs;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getSets() {
            return sets;
        }

        public void setSets(Integer sets) {
            this.sets = sets;
        }

        public String getReps() {
            return reps;
        }

        public void setReps(String reps) {
            this.reps = reps;
        }

        public Integer getWeightLbs() {
            return weightLbs;
        }

        public void setWeightLbs(Integer weightLbs) {
            this.weightLbs = weightLbs;
        }

        public String getWeightType() {
            return weightType;
        }

        public void setWeightType(String weightType) {
            this.weightType = weightType;
        }

        public Integer getRestSeconds() {
            return restSeconds;
        }

        public void setRestSeconds(Integer restSeconds) {
            this.restSeconds = restSeconds;
        }
    }

    public static class DietWeekDto {
        private Boolean done;
        private Map<String, DietDayDto> days;

        public DietWeekDto() {}

        public DietWeekDto(Boolean done, Map<String, DietDayDto> days) {
            this.done = done;
            this.days = days;
        }

        public Boolean getDone() {
            return done;
        }

        public void setDone(Boolean done) {
            this.done = done;
        }

        public Map<String, DietDayDto> getDays() {
            return days;
        }

        public void setDays(Map<String, DietDayDto> days) {
            this.days = days;
        }
    }

    public static class DietDayDto {
        private Boolean done;
        private List<MealDto> meals;

        public DietDayDto() {}

        public DietDayDto(Boolean done, List<MealDto> meals) {
            this.done = done;
            this.meals = meals;
        }

        public Boolean getDone() {
            return done;
        }

        public void setDone(Boolean done) {
            this.done = done;
        }

        public List<MealDto> getMeals() {
            return meals;
        }

        public void setMeals(List<MealDto> meals) {
            this.meals = meals;
        }
    }

    public static class MealDto {
        private String name;
        private String mealType;
        private Integer calories;
        private Integer proteinGrams;
        private Integer carbsGrams;
        private Integer fatGrams;

        public MealDto() {}

        public MealDto(String name, String mealType, Integer calories, Integer proteinGrams) {
            this.name = name;
            this.mealType = mealType;
            this.calories = calories;
            this.proteinGrams = proteinGrams;
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
    }

    public static class ExerciseCatalogDto {
        private String exerciseId;
        private String name;

        public ExerciseCatalogDto() {}

        public ExerciseCatalogDto(String exerciseId, String name) {
            this.exerciseId = exerciseId;
            this.name = name;
        }

        public String getExerciseId() {
            return exerciseId;
        }

        public void setExerciseId(String exerciseId) {
            this.exerciseId = exerciseId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class DietFoodCatalogDto {
        private String foodId;
        private String name;
        private String mealType;

        public DietFoodCatalogDto() {}

        public DietFoodCatalogDto(String foodId, String name, String mealType) {
            this.foodId = foodId;
            this.name = name;
            this.mealType = mealType;
        }

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
    }
}