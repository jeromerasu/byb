package com.workoutplanner.service;

import com.workoutplanner.dto.BodyMetricsRequest;
import com.workoutplanner.dto.BodyMetricsResponse;
import com.workoutplanner.model.BodyMetrics;
import com.workoutplanner.repository.BodyMetricsRepository;
import com.workoutplanner.repository.UserRepository;
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
 * P1-003: Unit tests for BodyMetricsService.
 */
@ExtendWith(MockitoExtension.class)
class BodyMetricsServiceTest {

    private static final Logger log = LoggerFactory.getLogger(BodyMetricsServiceTest.class);

    @Mock
    private BodyMetricsRepository repository;
    @Mock
    private UserRepository userRepository;

    private BodyMetricsService service;

    private static final String USER_A = "user-a-id";
    private static final String USER_B = "user-b-id";

    @BeforeEach
    void setUp() {
        service = new BodyMetricsService(repository, userRepository);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private BodyMetricsRequest makeRequest(BigDecimal weightKg, LocalDate recordedAt) {
        BodyMetricsRequest req = new BodyMetricsRequest();
        req.setWeightKg(weightKg);
        req.setRecordedAt(recordedAt);
        return req;
    }

    private BodyMetrics makeEntry(Long id, String userId, BigDecimal weightKg, LocalDate recordedAt) {
        BodyMetrics entry = new BodyMetrics();
        entry.setId(id);
        entry.setUserId(userId);
        entry.setWeightKg(weightKg);
        entry.setRecordedAt(recordedAt);
        return entry;
    }

    // -------------------------------------------------------------------------
    // create
    // -------------------------------------------------------------------------

    @Test
    void create_ValidRequest_ShouldPersistAndReturnResponse() {
        BodyMetricsRequest request = makeRequest(new BigDecimal("80.00"), LocalDate.of(2026, 3, 29));

        when(repository.save(any())).thenAnswer(inv -> {
            BodyMetrics e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        BodyMetricsResponse result = service.create(request, USER_A);

        log.info("test.create.success id={} userId={}", result.getId(), result.getUserId());
        assertEquals(1L, result.getId());
        assertEquals(USER_A, result.getUserId());
        assertEquals(new BigDecimal("80.00"), result.getWeightKg());
        assertEquals(LocalDate.of(2026, 3, 29), result.getRecordedAt());

        ArgumentCaptor<BodyMetrics> captor = ArgumentCaptor.forClass(BodyMetrics.class);
        verify(repository).save(captor.capture());
        assertEquals(USER_A, captor.getValue().getUserId());
        assertEquals(new BigDecimal("80.00"), captor.getValue().getWeightKg());
    }

    @Test
    void create_WithAllOptionalFields_ShouldPersistAll() {
        BodyMetricsRequest request = makeRequest(new BigDecimal("78.50"), LocalDate.of(2026, 3, 29));
        request.setBodyFatPct(new BigDecimal("18.5"));
        request.setMuscleMassKg(new BigDecimal("35.00"));
        request.setWaistCm(new BigDecimal("82.0"));
        request.setNotes("Post-competition weigh-in");

        when(repository.save(any())).thenAnswer(inv -> {
            BodyMetrics e = inv.getArgument(0);
            e.setId(2L);
            return e;
        });

        BodyMetricsResponse result = service.create(request, USER_A);

        log.info("test.create.allFields id={} bodyFatPct={}", result.getId(), result.getBodyFatPct());
        assertEquals(new BigDecimal("18.5"), result.getBodyFatPct());
        assertEquals(new BigDecimal("35.00"), result.getMuscleMassKg());
        assertEquals(new BigDecimal("82.0"), result.getWaistCm());
        assertEquals("Post-competition weigh-in", result.getNotes());
    }

    // -------------------------------------------------------------------------
    // list
    // -------------------------------------------------------------------------

    @Test
    void list_NoDateRange_ShouldCallFindAllByUserDesc() {
        BodyMetrics e1 = makeEntry(1L, USER_A, new BigDecimal("80.00"), LocalDate.of(2026, 3, 29));
        BodyMetrics e2 = makeEntry(2L, USER_A, new BigDecimal("79.50"), LocalDate.of(2026, 3, 15));
        when(repository.findByUserIdOrderByRecordedAtDesc(USER_A)).thenReturn(List.of(e1, e2));

        List<BodyMetricsResponse> results = service.list(USER_A, null, null);

        log.info("test.list.noRange count={}", results.size());
        assertEquals(2, results.size());
        verify(repository).findByUserIdOrderByRecordedAtDesc(USER_A);
        verify(repository, never()).findByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(any(), any(), any());
    }

    @Test
    void list_WithDateRange_ShouldCallDateRangeQuery() {
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);
        BodyMetrics e1 = makeEntry(1L, USER_A, new BigDecimal("80.00"), LocalDate.of(2026, 3, 15));
        when(repository.findByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(USER_A, from, to))
                .thenReturn(List.of(e1));

        List<BodyMetricsResponse> results = service.list(USER_A, from, to);

        log.info("test.list.dateRange count={}", results.size());
        assertEquals(1, results.size());
        verify(repository).findByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(USER_A, from, to);
        verify(repository, never()).findByUserIdOrderByRecordedAtDesc(any());
    }

    @Test
    void list_EmptyResult_ShouldReturnEmptyList() {
        when(repository.findByUserIdOrderByRecordedAtDesc(USER_A)).thenReturn(List.of());

        List<BodyMetricsResponse> results = service.list(USER_A, null, null);

        log.info("test.list.empty count={}", results.size());
        assertEquals(0, results.size());
    }

    // -------------------------------------------------------------------------
    // getLatest
    // -------------------------------------------------------------------------

    @Test
    void getLatest_EntryExists_ShouldReturnMostRecent() {
        BodyMetrics entry = makeEntry(5L, USER_A, new BigDecimal("78.00"), LocalDate.of(2026, 3, 29));
        when(repository.findFirstByUserIdOrderByRecordedAtDesc(USER_A)).thenReturn(Optional.of(entry));

        BodyMetricsResponse result = service.getLatest(USER_A);

        log.info("test.getLatest.found id={} weightKg={}", result.getId(), result.getWeightKg());
        assertEquals(5L, result.getId());
        assertEquals(new BigDecimal("78.00"), result.getWeightKg());
    }

    @Test
    void getLatest_NoEntries_ShouldThrow404() {
        when(repository.findFirstByUserIdOrderByRecordedAtDesc(USER_A)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getLatest(USER_A));

        log.info("test.getLatest.notFound status={}", ex.getStatusCode());
        assertEquals(404, ex.getStatusCode().value());
    }

    // -------------------------------------------------------------------------
    // getById
    // -------------------------------------------------------------------------

    @Test
    void getById_OwnEntry_ShouldReturnResponse() {
        BodyMetrics entry = makeEntry(1L, USER_A, new BigDecimal("80.00"), LocalDate.of(2026, 3, 29));
        when(repository.findById(1L)).thenReturn(Optional.of(entry));

        BodyMetricsResponse result = service.getById(1L, USER_A);

        log.info("test.getById.own id={}", result.getId());
        assertEquals(1L, result.getId());
    }

    @Test
    void getById_OtherUserEntry_ShouldThrow403() {
        BodyMetrics entry = makeEntry(2L, USER_B, new BigDecimal("70.00"), LocalDate.of(2026, 3, 29));
        when(repository.findById(2L)).thenReturn(Optional.of(entry));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getById(2L, USER_A));

        log.info("test.getById.forbidden status={}", ex.getStatusCode());
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void getById_NotFound_ShouldThrow404() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getById(99L, USER_A));

        log.info("test.getById.notFound status={}", ex.getStatusCode());
        assertEquals(404, ex.getStatusCode().value());
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    @Test
    void update_OwnEntry_ShouldApplyChanges() {
        BodyMetrics entry = makeEntry(1L, USER_A, new BigDecimal("80.00"), LocalDate.of(2026, 3, 28));
        when(repository.findById(1L)).thenReturn(Optional.of(entry));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BodyMetricsRequest request = makeRequest(new BigDecimal("79.50"), LocalDate.of(2026, 3, 29));
        BodyMetricsResponse result = service.update(1L, request, USER_A);

        log.info("test.update.own weightKg={}", result.getWeightKg());
        assertEquals(new BigDecimal("79.50"), result.getWeightKg());
        assertEquals(LocalDate.of(2026, 3, 29), result.getRecordedAt());
        verify(repository).save(entry);
    }

    @Test
    void update_OtherUserEntry_ShouldThrow403() {
        BodyMetrics entry = makeEntry(2L, USER_B, new BigDecimal("70.00"), LocalDate.of(2026, 3, 29));
        when(repository.findById(2L)).thenReturn(Optional.of(entry));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.update(2L, makeRequest(new BigDecimal("70.00"), LocalDate.now()), USER_A));

        log.info("test.update.forbidden status={}", ex.getStatusCode());
        assertEquals(403, ex.getStatusCode().value());
        verify(repository, never()).save(any());
    }

    @Test
    void update_NotFound_ShouldThrow404() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.update(99L, makeRequest(new BigDecimal("80.00"), LocalDate.now()), USER_A));

        assertEquals(404, ex.getStatusCode().value());
    }

    // -------------------------------------------------------------------------
    // delete
    // -------------------------------------------------------------------------

    @Test
    void delete_OwnEntry_ShouldCallRepositoryDelete() {
        BodyMetrics entry = makeEntry(1L, USER_A, new BigDecimal("80.00"), LocalDate.of(2026, 3, 29));
        when(repository.findById(1L)).thenReturn(Optional.of(entry));

        service.delete(1L, USER_A);

        log.info("test.delete.own id=1");
        verify(repository).delete(entry);
    }

    @Test
    void delete_OtherUserEntry_ShouldThrow403AndNotDelete() {
        BodyMetrics entry = makeEntry(2L, USER_B, new BigDecimal("70.00"), LocalDate.of(2026, 3, 29));
        when(repository.findById(2L)).thenReturn(Optional.of(entry));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.delete(2L, USER_A));

        log.info("test.delete.forbidden status={}", ex.getStatusCode());
        assertEquals(403, ex.getStatusCode().value());
        verify(repository, never()).delete(any());
    }

    @Test
    void delete_NotFound_ShouldThrow404() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.delete(99L, USER_A));

        assertEquals(404, ex.getStatusCode().value());
    }
}
