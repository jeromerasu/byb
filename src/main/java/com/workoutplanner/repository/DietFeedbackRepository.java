package com.workoutplanner.repository;

import com.workoutplanner.model.DietFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DietFeedbackRepository extends JpaRepository<DietFeedback, String> {

    List<DietFeedback> findByUserIdAndFeedbackDateAfter(String userId, LocalDate date);
}
