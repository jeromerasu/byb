package com.workoutplanner.service;

import com.workoutplanner.model.Coach;
import com.workoutplanner.repository.CoachRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * CRUD operations for Coach entities.
 * Password hashing is delegated to CoachAuthService to keep concerns separated.
 */
@Service
@Transactional
public class CoachService {

    private static final Logger log = LoggerFactory.getLogger(CoachService.class);

    private final CoachRepository coachRepository;

    public CoachService(CoachRepository coachRepository) {
        this.coachRepository = coachRepository;
    }

    public Coach save(Coach coach) {
        Coach saved = coachRepository.save(coach);
        log.info("coach.saved id={} email={}", saved.getId(), saved.getEmail());
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<Coach> findById(String id) {
        return coachRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Coach> findByEmail(String email) {
        return coachRepository.findByEmail(email);
    }

    @Transactional(readOnly = true)
    public List<Coach> findAll() {
        return coachRepository.findAll();
    }

    public void deleteById(String id) {
        coachRepository.deleteById(id);
        log.info("coach.deleted id={}", id);
    }
}
