package com.workoutplanner.dto;

import com.workoutplanner.model.WorkoutRating;

import java.math.BigDecimal;

public class OverloadSummaryResponse {

    private String exerciseName;
    private Integer prescribedSets;
    private Integer prescribedReps;
    private BigDecimal prescribedWeight;
    private Integer actualSets;
    private Integer actualReps;
    private BigDecimal actualWeight;
    private double completionRate;
    private WorkoutRating rating;
    private boolean painFlag;
    private boolean substitutionRequested;
    private String suggestedProgression;

    public OverloadSummaryResponse() {}

    public String getExerciseName() { return exerciseName; }
    public void setExerciseName(String exerciseName) { this.exerciseName = exerciseName; }

    public Integer getPrescribedSets() { return prescribedSets; }
    public void setPrescribedSets(Integer prescribedSets) { this.prescribedSets = prescribedSets; }

    public Integer getPrescribedReps() { return prescribedReps; }
    public void setPrescribedReps(Integer prescribedReps) { this.prescribedReps = prescribedReps; }

    public BigDecimal getPrescribedWeight() { return prescribedWeight; }
    public void setPrescribedWeight(BigDecimal prescribedWeight) { this.prescribedWeight = prescribedWeight; }

    public Integer getActualSets() { return actualSets; }
    public void setActualSets(Integer actualSets) { this.actualSets = actualSets; }

    public Integer getActualReps() { return actualReps; }
    public void setActualReps(Integer actualReps) { this.actualReps = actualReps; }

    public BigDecimal getActualWeight() { return actualWeight; }
    public void setActualWeight(BigDecimal actualWeight) { this.actualWeight = actualWeight; }

    public double getCompletionRate() { return completionRate; }
    public void setCompletionRate(double completionRate) { this.completionRate = completionRate; }

    public WorkoutRating getRating() { return rating; }
    public void setRating(WorkoutRating rating) { this.rating = rating; }

    public boolean isPainFlag() { return painFlag; }
    public void setPainFlag(boolean painFlag) { this.painFlag = painFlag; }

    public boolean isSubstitutionRequested() { return substitutionRequested; }
    public void setSubstitutionRequested(boolean substitutionRequested) { this.substitutionRequested = substitutionRequested; }

    public String getSuggestedProgression() { return suggestedProgression; }
    public void setSuggestedProgression(String suggestedProgression) { this.suggestedProgression = suggestedProgression; }
}
