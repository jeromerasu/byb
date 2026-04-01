package com.workoutplanner.model;

/**
 * Classifies a CoachDirective by plan domain.
 * GENERAL directives are injected into both workout and diet prompts.
 */
public enum DirectiveType {
    WORKOUT,
    DIET,
    GENERAL
}
