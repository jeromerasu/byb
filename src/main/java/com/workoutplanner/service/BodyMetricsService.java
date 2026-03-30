package com.workoutplanner.service;

import com.workoutplanner.dto.BodyMetricsRequest;
import com.workoutplanner.dto.BodyMetricsResponse;
import com.workoutplanner.model.BodyMetrics;
import com.workoutplanner.repository.BodyMetricsRepository;
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
public class BodyMetricsService {

    private static final Logger log = LoggerFactory.getLogger(BodyMetricsService.class);

    private final BodyMetricsRepository repository;

    @Autowired
    public BodyMetricsService(BodyMetricsRepository repository) {
        this.repository = repository;
    }

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    @Transactional
    public BodyMetricsResponse create(BodyMetricsRequest request, String userId) {
        log.info("body_metrics.create userId={} recordedAt={}", userId, request.getRecordedAt());

        BodyMetrics entry = new BodyMetrics();
        entry.setUserId(userId);
        applyFields(entry, request);

        BodyMetrics saved = repository.save(entry);
        log.info("body_metrics.created id={} userId={}", saved.getId(), userId);
        return BodyMetricsResponse.from(saved);
    }

    // -------------------------------------------------------------------------
    // List with optional date range
    // -------------------------------------------------------------------------

    public List<BodyMetricsResponse> list(String userId, LocalDate from, LocalDate to) {
        log.info("body_metrics.list userId={} from={} to={}", userId, from, to);

        List<BodyMetrics> entries;
        if (from != null && to != null) {
            entries = repository.findByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(userId, from, to);
        } else {
            entries = repository.findByUserIdOrderByRecordedAtDesc(userId);
        }

        return entries.stream().map(BodyMetricsResponse::from).collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Get latest entry
    // -------------------------------------------------------------------------

    public BodyMetricsResponse getLatest(String userId) {
        log.info("body_metrics.getLatest userId={}", userId);
        BodyMetrics entry = repository.findFirstByUserIdOrderByRecordedAtDesc(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No body metrics found for user " + userId));
        return BodyMetricsResponse.from(entry);
    }

    // -------------------------------------------------------------------------
    // Get by id (ownership enforced)
    // -------------------------------------------------------------------------

    public BodyMetricsResponse getById(Long id, String userId) {
        log.info("body_metrics.get id={} userId={}", id, userId);
        BodyMetrics entry = requireOwned(id, userId);
        return BodyMetricsResponse.from(entry);
    }

    // -------------------------------------------------------------------------
    // Update (ownership enforced)
    // -------------------------------------------------------------------------

    @Transactional
    public BodyMetricsResponse update(Long id, BodyMetricsRequest request, String userId) {
        log.info("body_metrics.update id={} userId={}", id, userId);
        BodyMetrics entry = requireOwned(id, userId);
        applyFields(entry, request);
        BodyMetrics saved = repository.save(entry);
        log.info("body_metrics.updated id={} userId={}", id, userId);
        return BodyMetricsResponse.from(saved);
    }

    // -------------------------------------------------------------------------
    // Delete (ownership enforced)
    // -------------------------------------------------------------------------

    @Transactional
    public void delete(Long id, String userId) {
        log.info("body_metrics.delete id={} userId={}", id, userId);
        BodyMetrics entry = requireOwned(id, userId);
        repository.delete(entry);
        log.info("body_metrics.deleted id={} userId={}", id, userId);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private BodyMetrics requireOwned(Long id, String userId) {
        BodyMetrics entry = repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Body metrics entry " + id + " not found"));

        if (!entry.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You do not have access to body metrics entry " + id);
        }
        return entry;
    }

    private void applyFields(BodyMetrics entry, BodyMetricsRequest request) {
        entry.setWeightKg(request.getWeightKg());
        entry.setBodyFatPct(request.getBodyFatPct());
        entry.setMuscleMassKg(request.getMuscleMassKg());
        entry.setWaistCm(request.getWaistCm());
        entry.setRecordedAt(request.getRecordedAt());
        entry.setNotes(request.getNotes());
    }
}
