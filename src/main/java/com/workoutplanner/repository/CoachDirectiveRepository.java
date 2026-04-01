package com.workoutplanner.repository;

import com.workoutplanner.model.CoachDirective;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoachDirectiveRepository extends JpaRepository<CoachDirective, String> {

    List<CoachDirective> findByCoachIdAndUserId(String coachId, String userId);

    List<CoachDirective> findByCoachIdAndUserIdAndActive(String coachId, String userId, boolean active);

    List<CoachDirective> findByUserIdAndActive(String userId, boolean active);
}
