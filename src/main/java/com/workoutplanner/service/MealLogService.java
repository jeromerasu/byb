package com.workoutplanner.service;

import com.workoutplanner.dto.MealLogRequest;
import com.workoutplanner.dto.MealLogResponse;
import com.workoutplanner.model.MealLog;
import com.workoutplanner.repository.MealLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MealLogService {

    private static final Logger log = LoggerFactory.getLogger(MealLogService.class);

    private final MealLogRepository repository;

    @Autowired
    public MealLogService(MealLogRepository repository) {
        this.repository = repository;
    }

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    @Transactional
    public MealLogResponse create(MealLogRequest request, String userId) {
        log.info("meal_log.create userId={} mealName={}", userId, request.getMealName());

        MealLog log_ = new MealLog();
        log_.setUserId(userId);
        applyFields(log_, request);

        MealLog saved = repository.save(log_);
        log.info("meal_log.created id={} userId={}", saved.getId(), userId);
        return MealLogResponse.from(saved);
    }

    // -------------------------------------------------------------------------
    // List with optional date range
    // -------------------------------------------------------------------------

    public List<MealLogResponse> list(String userId, LocalDate startDate, LocalDate endDate) {
        log.info("meal_log.list userId={} startDate={} endDate={}", userId, startDate, endDate);

        List<MealLog> entries;
        if (startDate != null && endDate != null) {
            entries = repository.findByUserIdAndDateBetweenOrderByDateDesc(userId, startDate, endDate);
        } else {
            entries = repository.findByUserIdOrderByDateDesc(userId);
        }

        return entries.stream().map(MealLogResponse::from).collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Get by id (ownership enforced)
    // -------------------------------------------------------------------------

    public MealLogResponse getById(String id, String userId) {
        log.info("meal_log.get id={} userId={}", id, userId);
        MealLog entry = requireOwned(id, userId);
        return MealLogResponse.from(entry);
    }

    // -------------------------------------------------------------------------
    // Update (ownership enforced)
    // -------------------------------------------------------------------------

    @Transactional
    public MealLogResponse update(String id, MealLogRequest request, String userId) {
        log.info("meal_log.update id={} userId={}", id, userId);
        MealLog entry = requireOwned(id, userId);
        applyFields(entry, request);
        MealLog saved = repository.save(entry);
        log.info("meal_log.updated id={} userId={}", id, userId);
        return MealLogResponse.from(saved);
    }

    // -------------------------------------------------------------------------
    // Delete (ownership enforced)
    // -------------------------------------------------------------------------

    @Transactional
    public void delete(String id, String userId) {
        log.info("meal_log.delete id={} userId={}", id, userId);
        MealLog entry = requireOwned(id, userId);
        repository.delete(entry);
        log.info("meal_log.deleted id={} userId={}", id, userId);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private MealLog requireOwned(String id, String userId) {
        MealLog entry = repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Meal log entry " + id + " not found"));

        if (!entry.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You do not have access to meal log entry " + id);
        }
        return entry;
    }

    private void applyFields(MealLog entry, MealLogRequest request) {
        entry.setMealName(request.getMealName());
        entry.setCalories(request.getCalories());
        entry.setProteins(request.getProteins());
        entry.setFats(request.getFats());
        entry.setCarbs(request.getCarbs());
        entry.setDate(request.getDate());
        entry.setFoodCatalogId(request.getFoodCatalogId());
        entry.setRating(request.getRating());
        entry.setFeedbackComment(request.getFeedbackComment());
    }
}
