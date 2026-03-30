package com.workoutplanner.service;

import com.workoutplanner.dto.WorkoutLogRequest;
import com.workoutplanner.dto.WorkoutLogResponse;
import com.workoutplanner.model.WorkoutLog;
import com.workoutplanner.repository.WorkoutLogRepository;
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
public class WorkoutLogService {

    private static final Logger log = LoggerFactory.getLogger(WorkoutLogService.class);

    private final WorkoutLogRepository repository;

    @Autowired
    public WorkoutLogService(WorkoutLogRepository repository) {
        this.repository = repository;
    }

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    @Transactional
    public WorkoutLogResponse create(WorkoutLogRequest request, String userId) {
        log.info("workout_log.create userId={} exercise={}", userId, request.getExercise());

        WorkoutLog entry = new WorkoutLog();
        entry.setUserId(userId);
        applyFields(entry, request);

        WorkoutLog saved = repository.save(entry);
        log.info("workout_log.created id={} userId={}", saved.getId(), userId);
        return WorkoutLogResponse.from(saved);
    }

    // -------------------------------------------------------------------------
    // List with optional date range
    // -------------------------------------------------------------------------

    public List<WorkoutLogResponse> list(String userId, LocalDate from, LocalDate to) {
        log.info("workout_log.list userId={} from={} to={}", userId, from, to);

        List<WorkoutLog> entries;
        if (from != null && to != null) {
            entries = repository.findByUserIdAndDateBetweenOrderByDateDesc(userId, from, to);
        } else {
            entries = repository.findByUserIdOrderByDateDesc(userId);
        }

        return entries.stream().map(WorkoutLogResponse::from).collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Get by id (ownership enforced)
    // -------------------------------------------------------------------------

    public WorkoutLogResponse getById(String id, String userId) {
        log.info("workout_log.get id={} userId={}", id, userId);
        WorkoutLog entry = requireOwned(id, userId);
        return WorkoutLogResponse.from(entry);
    }

    // -------------------------------------------------------------------------
    // Update (ownership enforced)
    // -------------------------------------------------------------------------

    @Transactional
    public WorkoutLogResponse update(String id, WorkoutLogRequest request, String userId) {
        log.info("workout_log.update id={} userId={}", id, userId);
        WorkoutLog entry = requireOwned(id, userId);
        applyFields(entry, request);
        WorkoutLog saved = repository.save(entry);
        log.info("workout_log.updated id={} userId={}", id, userId);
        return WorkoutLogResponse.from(saved);
    }

    // -------------------------------------------------------------------------
    // Delete (ownership enforced)
    // -------------------------------------------------------------------------

    @Transactional
    public void delete(String id, String userId) {
        log.info("workout_log.delete id={} userId={}", id, userId);
        WorkoutLog entry = requireOwned(id, userId);
        repository.delete(entry);
        log.info("workout_log.deleted id={} userId={}", id, userId);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private WorkoutLog requireOwned(String id, String userId) {
        WorkoutLog entry = repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout log entry " + id + " not found"));

        if (!entry.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You do not have access to workout log entry " + id);
        }
        return entry;
    }

    private void applyFields(WorkoutLog entry, WorkoutLogRequest request) {
        entry.setExercise(request.getExercise());
        entry.setWeight(request.getWeight());
        entry.setSets(request.getSets());
        entry.setReps(request.getReps());
        entry.setDurationMinutes(request.getDurationMinutes());
        entry.setExerciseType(request.getExerciseType());
        entry.setNotes(request.getNotes());
        entry.setRating(request.getRating());
        entry.setFeedbackComment(request.getFeedbackComment());
        entry.setPainFlag(request.isPainFlag());
        entry.setSubstitutionRequested(request.isSubstitutionRequested());
        entry.setDate(request.getDate());
        entry.setExerciseCatalogId(request.getExerciseCatalogId());
    }
}
