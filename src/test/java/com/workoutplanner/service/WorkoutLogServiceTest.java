package com.workoutplanner.service;

import com.workoutplanner.dto.WorkoutLogRequest;
import com.workoutplanner.dto.WorkoutLogResponse;
import com.workoutplanner.model.ExerciseType;
import com.workoutplanner.model.WorkoutLog;
import com.workoutplanner.model.WorkoutRating;
import com.workoutplanner.repository.WorkoutLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * P1-001: Unit tests for WorkoutLogService.
 */
@ExtendWith(MockitoExtension.class)
class WorkoutLogServiceTest {

    private static final Logger log = LoggerFactory.getLogger(WorkoutLogServiceTest.class);

    @Mock
    private WorkoutLogRepository repository;

    private WorkoutLogService service;

    private static final String USER_A = "user-a-id";
    private static final String USER_B = "user-b-id";

    @BeforeEach
    void setUp() {
        service = new WorkoutLogService(repository);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private WorkoutLogRequest makeRequest(String exercise, LocalDate date) {
        WorkoutLogRequest req = new WorkoutLogRequest();
        req.setExercise(exercise);
        req.setWeight(new BigDecimal("100.00"));
        req.setSets(3);
        req.setReps(10);
        req.setDate(date);
        return req;
    }

    private WorkoutLog makeEntry(String id, String userId, String exercise, LocalDate date) {
        WorkoutLog entry = new WorkoutLog();
        entry.setId(id);
        entry.setUserId(userId);
        entry.setExercise(exercise);
        entry.setWeight(new BigDecimal("100.00"));
        entry.setDate(date);
        return entry;
    }

    // -------------------------------------------------------------------------
    // create
    // -------------------------------------------------------------------------

    @Test
    void create_ValidRequest_ShouldPersistAndReturnResponse() {
        WorkoutLogRequest request = makeRequest("Bench Press", LocalDate.of(2026, 3, 29));

        when(repository.save(any())).thenAnswer(inv -> {
            WorkoutLog e = inv.getArgument(0);
            e.setId("uuid-001");
            return e;
        });

        WorkoutLogResponse result = service.create(request, USER_A);

        log.info("test.create.success id={} userId={}", result.getId(), result.getUserId());
        assertEquals("uuid-001", result.getId());
        assertEquals(USER_A, result.getUserId());
        assertEquals("Bench Press", result.getExercise());
        assertEquals(LocalDate.of(2026, 3, 29), result.getDate());

        ArgumentCaptor<WorkoutLog> captor = ArgumentCaptor.forClass(WorkoutLog.class);
        verify(repository).save(captor.capture());
        assertEquals(USER_A, captor.getValue().getUserId());
        assertEquals("Bench Press", captor.getValue().getExercise());
    }

    @Test
    void create_WithExerciseCatalogId_ShouldPersistFk() {
        WorkoutLogRequest request = makeRequest("Squat", LocalDate.of(2026, 3, 29));
        request.setExerciseCatalogId(10L);

        when(repository.save(any())).thenAnswer(inv -> {
            WorkoutLog e = inv.getArgument(0);
            e.setId("uuid-002");
            return e;
        });

        WorkoutLogResponse result = service.create(request, USER_A);

        log.info("test.create.withFk id={} exerciseCatalogId={}", result.getId(), result.getExerciseCatalogId());
        assertEquals(10L, result.getExerciseCatalogId());
    }

    @Test
    void create_WithFeedback_ShouldPersistRatingAndComment() {
        WorkoutLogRequest request = makeRequest("Deadlift", LocalDate.of(2026, 3, 29));
        request.setRating(WorkoutRating.TOO_HARD);
        request.setFeedbackComment("Too heavy today");
        request.setPainFlag(true);

        when(repository.save(any())).thenAnswer(inv -> {
            WorkoutLog e = inv.getArgument(0);
            e.setId("uuid-003");
            return e;
        });

        WorkoutLogResponse result = service.create(request, USER_A);

        log.info("test.create.feedback rating={}", result.getRating());
        assertEquals(WorkoutRating.TOO_HARD, result.getRating());
        assertEquals("Too heavy today", result.getFeedbackComment());
        assertTrue(result.isPainFlag());
    }

    @Test
    void create_WithExerciseType_ShouldPersistType() {
        WorkoutLogRequest request = makeRequest("Running", LocalDate.of(2026, 3, 29));
        request.setExerciseType(ExerciseType.CARDIO);
        request.setDurationMinutes(30);

        when(repository.save(any())).thenAnswer(inv -> {
            WorkoutLog e = inv.getArgument(0);
            e.setId("uuid-004");
            return e;
        });

        WorkoutLogResponse result = service.create(request, USER_A);

        log.info("test.create.type exerciseType={}", result.getExerciseType());
        assertEquals(ExerciseType.CARDIO, result.getExerciseType());
        assertEquals(30, result.getDurationMinutes());
    }

    // -------------------------------------------------------------------------
    // list
    // -------------------------------------------------------------------------

    @Test
    void list_NoDateRange_ShouldCallFindAllByUser() {
        WorkoutLog e1 = makeEntry("id-1", USER_A, "Bench Press", LocalDate.of(2026, 3, 29));
        WorkoutLog e2 = makeEntry("id-2", USER_A, "Squat", LocalDate.of(2026, 3, 28));
        when(repository.findByUserIdOrderByDateDesc(USER_A)).thenReturn(List.of(e1, e2));

        List<WorkoutLogResponse> results = service.list(USER_A, null, null);

        log.info("test.list.noRange count={}", results.size());
        assertEquals(2, results.size());
        verify(repository).findByUserIdOrderByDateDesc(USER_A);
        verify(repository, never()).findByUserIdAndDateBetweenOrderByDateDesc(any(), any(), any());
    }

    @Test
    void list_WithDateRange_ShouldCallDateRangeQuery() {
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);
        WorkoutLog e1 = makeEntry("id-1", USER_A, "Bench Press", LocalDate.of(2026, 3, 15));
        when(repository.findByUserIdAndDateBetweenOrderByDateDesc(USER_A, from, to)).thenReturn(List.of(e1));

        List<WorkoutLogResponse> results = service.list(USER_A, from, to);

        log.info("test.list.dateRange count={}", results.size());
        assertEquals(1, results.size());
        assertEquals("Bench Press", results.get(0).getExercise());
        verify(repository).findByUserIdAndDateBetweenOrderByDateDesc(USER_A, from, to);
        verify(repository, never()).findByUserIdOrderByDateDesc(any());
    }

    @Test
    void list_EmptyResult_ShouldReturnEmptyList() {
        when(repository.findByUserIdOrderByDateDesc(USER_A)).thenReturn(List.of());

        List<WorkoutLogResponse> results = service.list(USER_A, null, null);

        log.info("test.list.empty count={}", results.size());
        assertEquals(0, results.size());
    }

    // -------------------------------------------------------------------------
    // getById
    // -------------------------------------------------------------------------

    @Test
    void getById_OwnEntry_ShouldReturnResponse() {
        WorkoutLog entry = makeEntry("id-1", USER_A, "Bench Press", LocalDate.of(2026, 3, 29));
        when(repository.findById("id-1")).thenReturn(Optional.of(entry));

        WorkoutLogResponse result = service.getById("id-1", USER_A);

        log.info("test.getById.own id={}", result.getId());
        assertEquals("id-1", result.getId());
        assertEquals("Bench Press", result.getExercise());
    }

    @Test
    void getById_OtherUserEntry_ShouldThrow403() {
        WorkoutLog entry = makeEntry("id-2", USER_B, "Squat", LocalDate.of(2026, 3, 29));
        when(repository.findById("id-2")).thenReturn(Optional.of(entry));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getById("id-2", USER_A));

        log.info("test.getById.forbidden status={}", ex.getStatusCode());
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void getById_NotFound_ShouldThrow404() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getById("missing", USER_A));

        log.info("test.getById.notFound status={}", ex.getStatusCode());
        assertEquals(404, ex.getStatusCode().value());
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    @Test
    void update_OwnEntry_ShouldApplyChanges() {
        WorkoutLog entry = makeEntry("id-1", USER_A, "Old Exercise", LocalDate.of(2026, 3, 28));
        when(repository.findById("id-1")).thenReturn(Optional.of(entry));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkoutLogRequest request = makeRequest("Updated Exercise", LocalDate.of(2026, 3, 29));
        WorkoutLogResponse result = service.update("id-1", request, USER_A);

        log.info("test.update.own exercise={}", result.getExercise());
        assertEquals("Updated Exercise", result.getExercise());
        assertEquals(LocalDate.of(2026, 3, 29), result.getDate());
        verify(repository).save(entry);
    }

    @Test
    void update_OtherUserEntry_ShouldThrow403() {
        WorkoutLog entry = makeEntry("id-2", USER_B, "Squat", LocalDate.of(2026, 3, 29));
        when(repository.findById("id-2")).thenReturn(Optional.of(entry));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.update("id-2", makeRequest("X", LocalDate.now()), USER_A));

        log.info("test.update.forbidden status={}", ex.getStatusCode());
        assertEquals(403, ex.getStatusCode().value());
        verify(repository, never()).save(any());
    }

    @Test
    void update_NotFound_ShouldThrow404() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.update("missing", makeRequest("X", LocalDate.now()), USER_A));

        assertEquals(404, ex.getStatusCode().value());
    }

    // -------------------------------------------------------------------------
    // delete
    // -------------------------------------------------------------------------

    @Test
    void delete_OwnEntry_ShouldCallRepositoryDelete() {
        WorkoutLog entry = makeEntry("id-1", USER_A, "Bench Press", LocalDate.of(2026, 3, 29));
        when(repository.findById("id-1")).thenReturn(Optional.of(entry));

        service.delete("id-1", USER_A);

        log.info("test.delete.own id=id-1");
        verify(repository).delete(entry);
    }

    @Test
    void delete_OtherUserEntry_ShouldThrow403AndNotDelete() {
        WorkoutLog entry = makeEntry("id-2", USER_B, "Squat", LocalDate.of(2026, 3, 29));
        when(repository.findById("id-2")).thenReturn(Optional.of(entry));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.delete("id-2", USER_A));

        log.info("test.delete.forbidden status={}", ex.getStatusCode());
        assertEquals(403, ex.getStatusCode().value());
        verify(repository, never()).delete(any());
    }

    @Test
    void delete_NotFound_ShouldThrow404() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.delete("missing", USER_A));

        assertEquals(404, ex.getStatusCode().value());
    }
}
