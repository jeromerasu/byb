package com.workoutplanner.repository;

import com.workoutplanner.model.CoachPromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CoachPromptTemplateRepository extends JpaRepository<CoachPromptTemplate, String> {

    /** Per-client template (coach + specific user). */
    Optional<CoachPromptTemplate> findByCoachIdAndUserId(String coachId, String userId);

    /** Coach default template (coach + no user). */
    Optional<CoachPromptTemplate> findByCoachIdAndUserIdIsNull(String coachId);
}
