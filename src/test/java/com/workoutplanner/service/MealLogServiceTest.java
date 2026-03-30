package com.workoutplanner.service;

import com.workoutplanner.dto.MealLogRequest;
import com.workoutplanner.dto.MealLogResponse;
import com.workoutplanner.model.MealLog;
import com.workoutplanner.model.MealRating;
import com.workoutplanner.repository.MealLogRepository;
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
 * TASK-P1-002: Unit tests for MealLogService.
 */
@ExtendWith(MockitoExtension.class)
class MealLogServiceTest {

    private static final Logger log = LoggerFactory.getLogger(MealLogServiceTest.class);

    @Mock
    private MealLogRepository repository;

    private MealLogService service;

    private static final String USER_A = "user-a-id";
    private static final String USER_B = "user-b-id";

    @BeforeEach
    void setUp() {
        service = new MealLogService(repository);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private MealLogRequest makeRequest(String mealName, LocalDate date) {
        MealLogRequest req = new MealLogRequest();
        req.setMealName(mealName);
        req.setCalories(new BigDecimal("500.00"));
        req.setProteins(new BigDecimal("40.00"));
        req.setFats(new BigDecimal("15.00"));
        req.setCarbs(new BigDecimal("50.00"));
        req.setDate(date);
        return req;
    }

    private MealLog makeEntry(String id, String userId, String mealName, LocalDate date) {
        MealLog entry = new MealLog();
        entry.setId(id);
        entry.setUserId(userId);
        entry.setMealName(mealName);
        entry.setCalories(new BigDecimal("500.00"));
        entry.setDate(date);
        return entry;
    }

    // -------------------------------------------------------------------------
    // create
    // -------------------------------------------------------------------------

    @Test
    void create_ValidRequest_ShouldPersistAndReturnResponse() {
        MealLogRequest request = makeRequest("Chicken Breast", LocalDate.of(2026, 3, 29));

        when(repository.save(any())).thenAnswer(inv -> {
            MealLog e = inv.getArgument(0);
            e.setId("uuid-001");
            return e;
        });

        MealLogResponse result = service.create(request, USER_A);

        log.info("test.create.success id={} userId={}", result.getId(), result.getUserId());
        assertEquals("uuid-001", result.getId());
        assertEquals(USER_A, result.getUserId());
        assertEquals("Chicken Breast", result.getMealName());
        assertEquals(LocalDate.of(2026, 3, 29), result.getDate());

        ArgumentCaptor<MealLog> captor = ArgumentCaptor.forClass(MealLog.class);
        verify(repository).save(captor.capture());
        assertEquals(USER_A, captor.getValue().getUserId());
        assertEquals("Chicken Breast", captor.getValue().getMealName());
    }

    @Test
    void create_WithFoodCatalogId_ShouldPersistFk() {
        MealLogRequest request = makeRequest("Oats", LocalDate.of(2026, 3, 29));
        request.setFoodCatalogId(42L);

        when(repository.save(any())).thenAnswer(inv -> {
            MealLog e = inv.getArgument(0);
            e.setId("uuid-002");
            return e;
        });

        MealLogResponse result = service.create(request, USER_A);

        log.info("test.create.withFk id={} foodCatalogId={}", result.getId(), result.getFoodCatalogId());
        assertEquals(42L, result.getFoodCatalogId());
    }

    @Test
    void create_WithFeedback_ShouldPersistRatingAndComment() {
        MealLogRequest request = makeRequest("Salmon", LocalDate.of(2026, 3, 29));
        request.setRating(MealRating.LOVED);
        request.setFeedbackComment("Delicious!");

        when(repository.save(any())).thenAnswer(inv -> {
            MealLog e = inv.getArgument(0);
            e.setId("uuid-003");
            return e;
        });

        MealLogResponse result = service.create(request, USER_A);

        log.info("test.create.feedback rating={}", result.getRating());
        assertEquals(MealRating.LOVED, result.getRating());
        assertEquals("Delicious!", result.getFeedbackComment());
    }

    // -------------------------------------------------------------------------
    // list
    // -------------------------------------------------------------------------

    @Test
    void list_NoDateRange_ShouldCallFindAllByUser() {
        MealLog e1 = makeEntry("id-1", USER_A, "Lunch", LocalDate.of(2026, 3, 29));
        MealLog e2 = makeEntry("id-2", USER_A, "Dinner", LocalDate.of(2026, 3, 28));
        when(repository.findByUserIdOrderByDateDesc(USER_A)).thenReturn(List.of(e1, e2));

        List<MealLogResponse> results = service.list(USER_A, null, null);

        log.info("test.list.noRange count={}", results.size());
        assertEquals(2, results.size());
        verify(repository).findByUserIdOrderByDateDesc(USER_A);
        verify(repository, never()).findByUserIdAndDateBetweenOrderByDateDesc(any(), any(), any());
    }

    @Test
    void list_WithDateRange_ShouldCallDateRangeQuery() {
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);
        MealLog e1 = makeEntry("id-1", USER_A, "Breakfast", LocalDate.of(2026, 3, 15));
        when(repository.findByUserIdAndDateBetweenOrderByDateDesc(USER_A, from, to)).thenReturn(List.of(e1));

        List<MealLogResponse> results = service.list(USER_A, from, to);

        log.info("test.list.dateRange count={}", results.size());
        assertEquals(1, results.size());
        assertEquals("Breakfast", results.get(0).getMealName());
        verify(repository).findByUserIdAndDateBetweenOrderByDateDesc(USER_A, from, to);
        verify(repository, never()).findByUserIdOrderByDateDesc(any());
    }

    @Test
    void list_EmptyResult_ShouldReturnEmptyList() {
        when(repository.findByUserIdOrderByDateDesc(USER_A)).thenReturn(List.of());

        List<MealLogResponse> results = service.list(USER_A, null, null);

        log.info("test.list.empty count={}", results.size());
        assertEquals(0, results.size());
    }

    // -------------------------------------------------------------------------
    // getById
    // -------------------------------------------------------------------------

    @Test
    void getById_OwnEntry_ShouldReturnResponse() {
        MealLog entry = makeEntry("id-1", USER_A, "Lunch", LocalDate.of(2026, 3, 29));
        when(repository.findById("id-1")).thenReturn(Optional.of(entry));

        MealLogResponse result = service.getById("id-1", USER_A);

        log.info("test.getById.own id={}", result.getId());
        assertEquals("id-1", result.getId());
        assertEquals("Lunch", result.getMealName());
    }

    @Test
    void getById_OtherUserEntry_ShouldThrow403() {
        MealLog entry = makeEntry("id-2", USER_B, "Dinner", LocalDate.of(2026, 3, 29));
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
        MealLog entry = makeEntry("id-1", USER_A, "Old Name", LocalDate.of(2026, 3, 28));
        when(repository.findById("id-1")).thenReturn(Optional.of(entry));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MealLogRequest request = makeRequest("Updated Name", LocalDate.of(2026, 3, 29));
        MealLogResponse result = service.update("id-1", request, USER_A);

        log.info("test.update.own mealName={}", result.getMealName());
        assertEquals("Updated Name", result.getMealName());
        assertEquals(LocalDate.of(2026, 3, 29), result.getDate());
        verify(repository).save(entry);
    }

    @Test
    void update_OtherUserEntry_ShouldThrow403() {
        MealLog entry = makeEntry("id-2", USER_B, "Dinner", LocalDate.of(2026, 3, 29));
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
        MealLog entry = makeEntry("id-1", USER_A, "Lunch", LocalDate.of(2026, 3, 29));
        when(repository.findById("id-1")).thenReturn(Optional.of(entry));

        service.delete("id-1", USER_A);

        log.info("test.delete.own id=id-1");
        verify(repository).delete(entry);
    }

    @Test
    void delete_OtherUserEntry_ShouldThrow403AndNotDelete() {
        MealLog entry = makeEntry("id-2", USER_B, "Dinner", LocalDate.of(2026, 3, 29));
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
