package com.workoutplanner.repository;

import com.workoutplanner.model.WorkoutFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WorkoutFeedbackRepository extends JpaRepository<WorkoutFeedback, String> {

    List<WorkoutFeedback> findByUserIdAndWorkoutDateAfter(String userId, LocalDate date);
}
