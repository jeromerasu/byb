package com.workoutplanner.service;

import com.workoutplanner.model.Coach;
import com.workoutplanner.repository.CoachRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Minimal coach authentication service — Phase 1 foundation for the Phase 2 web portal.
 *
 * Coach auth is intentionally isolated from mobile user auth (does not reuse JwtService
 * or UserDetailsService). Keep this simple and clearly marked for extension.
 *
 * TODO(PHASE2-COACH-AUTH): Extend with full JWT issuance, refresh tokens, and
 * role-based authorization for the coach web portal. Target: Phase 2 delivery.
 */
@Service
public class CoachAuthService {

    private static final Logger log = LoggerFactory.getLogger(CoachAuthService.class);

    private final CoachRepository coachRepository;
    private final PasswordEncoder passwordEncoder;

    public CoachAuthService(CoachRepository coachRepository, PasswordEncoder passwordEncoder) {
        this.coachRepository = coachRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new coach with a hashed password.
     * Throws IllegalArgumentException if email is already taken.
     */
    public Coach register(String name, String email, String rawPassword, String credentials, String bio) {
        if (coachRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Coach with email already exists: " + email);
        }
        Coach coach = new Coach();
        coach.setName(name);
        coach.setEmail(email);
        coach.setHashedPassword(passwordEncoder.encode(rawPassword));
        coach.setCredentials(credentials);
        coach.setBio(bio);
        Coach saved = coachRepository.save(coach);
        log.info("coach.registered id={} email={}", saved.getId(), email);
        return saved;
    }

    /**
     * Validates coach credentials. Returns the Coach if valid, empty if invalid.
     * Phase 2 will build JWT issuance on top of this.
     */
    public Optional<Coach> authenticate(String email, String rawPassword) {
        return coachRepository.findByEmail(email)
                .filter(c -> passwordEncoder.matches(rawPassword, c.getHashedPassword()));
    }
}
