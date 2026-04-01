package com.workoutplanner.service;

import com.workoutplanner.model.GeneratedBy;
import com.workoutplanner.model.UserWeekPlan;
import com.workoutplanner.repository.UserWeekPlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Manages the user_week_plan registry.
 *
 * Upsert semantics: if a row already exists for (userId, weekStart) it is
 * updated with the new storage keys and generatedBy — never duplicated.
 */
@Service
@Transactional
public class UserWeekPlanService {

    private static final Logger log = LoggerFactory.getLogger(UserWeekPlanService.class);

    private final UserWeekPlanRepository userWeekPlanRepository;

    public UserWeekPlanService(UserWeekPlanRepository userWeekPlanRepository) {
        this.userWeekPlanRepository = userWeekPlanRepository;
    }

    /**
     * Write (or overwrite) the week-plan registry row for the given user and the
     * week that contains {@code referenceDate} (week starts Monday per ISO-8601).
     */
    public UserWeekPlan upsert(String userId, LocalDate referenceDate,
                                String workoutStorageKey, String dietStorageKey,
                                GeneratedBy generatedBy) {
        LocalDate weekStart = referenceDate.with(DayOfWeek.MONDAY);

        Optional<UserWeekPlan> existing = userWeekPlanRepository.findByUserIdAndWeekStart(userId, weekStart);

        UserWeekPlan plan = existing.orElseGet(() -> {
            UserWeekPlan p = new UserWeekPlan();
            p.setUserId(userId);
            p.setWeekStart(weekStart);
            return p;
        });

        plan.setWorkoutStorageKey(workoutStorageKey);
        plan.setDietStorageKey(dietStorageKey);
        plan.setGeneratedBy(generatedBy);
        plan.setGeneratedAt(java.time.LocalDateTime.now());

        UserWeekPlan saved = userWeekPlanRepository.save(plan);
        log.info("user_week_plan.upserted id={} userId={} weekStart={} generatedBy={}",
                saved.getId(), userId, weekStart, generatedBy);
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<UserWeekPlan> findByUserAndWeek(String userId, LocalDate weekStart) {
        return userWeekPlanRepository.findByUserIdAndWeekStart(userId, weekStart);
    }

    @Transactional(readOnly = true)
    public List<UserWeekPlan> findHistory(String userId) {
        return userWeekPlanRepository.findByUserIdOrderByWeekStartDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<UserWeekPlan> findByRange(String userId, LocalDate from, LocalDate to) {
        return userWeekPlanRepository.findByUserIdAndWeekStartBetweenOrderByWeekStartAsc(userId, from, to);
    }
}
