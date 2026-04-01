package com.workoutplanner.repository;

import com.workoutplanner.model.Coach;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CoachRepository extends JpaRepository<Coach, String> {

    Optional<Coach> findByEmail(String email);

    boolean existsByEmail(String email);
}
