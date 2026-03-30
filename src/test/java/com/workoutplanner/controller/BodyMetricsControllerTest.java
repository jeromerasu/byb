package com.workoutplanner.controller;

import com.workoutplanner.dto.BodyMetricsRequest;
import com.workoutplanner.dto.BodyMetricsResponse;
import com.workoutplanner.model.User;
import com.workoutplanner.repository.UserRepository;
import com.workoutplanner.service.BodyMetricsService;
import com.workoutplanner.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * P1-003: Unit tests for BodyMetricsController.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BodyMetricsControllerTest {

    private static final Logger log = LoggerFactory.getLogger(BodyMetricsControllerTest.class);

    @Mock
    private BodyMetricsService bodyMetricsService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletRequest httpRequest;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    private BodyMetricsController controller;

    private static final String USER_ID = "user-a-id";

    @BeforeEach
    void setUp() {
        controller = new BodyMetricsController(bodyMetricsService, userRepository, jwtService);
        ReflectionTestUtils.setField(controller, "betaMode", false);

        User user = new User();
        user.setId(USER_ID);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private BodyMetricsResponse makeResponse(Long id) {
        BodyMetricsResponse r = new BodyMetricsResponse();
        ReflectionTestUtils.setField(r, "id", id);
        ReflectionTestUtils.setField(r, "userId", USER_ID);
        ReflectionTestUtils.setField(r, "weightKg", new BigDecimal("80.00"));
        ReflectionTestUtils.setField(r, "recordedAt", LocalDate.of(2026, 3, 29));
        ReflectionTestUtils.setField(r, "createdAt", LocalDateTime.now());
        return r;
    }

    private BodyMetricsRequest makeRequest() {
        BodyMetricsRequest req = new BodyMetricsRequest();
        req.setWeightKg(new BigDecimal("80.00"));
        req.setRecordedAt(LocalDate.of(2026, 3, 29));
        return req;
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/progress/body-metrics
    // -------------------------------------------------------------------------

    @Test
    void create_ValidRequest_Returns201() {
        BodyMetricsResponse created = makeResponse(1L);
        when(bodyMetricsService.create(any(), eq(USER_ID))).thenReturn(created);

        ResponseEntity<BodyMetricsResponse> response = controller.create(makeRequest(), httpRequest);

        log.info("test.create.201 status={} id={}", response.getStatusCode(), response.getBody().getId());
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(1L, response.getBody().getId());
        verify(bodyMetricsService).create(any(), eq(USER_ID));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/progress/body-metrics
    // -------------------------------------------------------------------------

    @Test
    void list_NoDateRange_Returns200WithEntries() {
        List<BodyMetricsResponse> entries = List.of(makeResponse(1L), makeResponse(2L));
        when(bodyMetricsService.list(eq(USER_ID), isNull(), isNull())).thenReturn(entries);

        ResponseEntity<List<BodyMetricsResponse>> response = controller.list(null, null, httpRequest);

        log.info("test.list.200 count={}", response.getBody().size());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
    }

    @Test
    void list_WithDateRange_DelegatesToService() {
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);
        when(bodyMetricsService.list(eq(USER_ID), eq(from), eq(to))).thenReturn(List.of(makeResponse(1L)));

        ResponseEntity<List<BodyMetricsResponse>> response = controller.list(from, to, httpRequest);

        log.info("test.list.dateRange count={}", response.getBody().size());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(bodyMetricsService).list(USER_ID, from, to);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/progress/body-metrics/latest
    // -------------------------------------------------------------------------

    @Test
    void getLatest_EntryExists_Returns200() {
        when(bodyMetricsService.getLatest(USER_ID)).thenReturn(makeResponse(5L));

        ResponseEntity<BodyMetricsResponse> response = controller.getLatest(httpRequest);

        log.info("test.getLatest.200 id={}", response.getBody().getId());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(5L, response.getBody().getId());
    }

    @Test
    void getLatest_NoEntries_PropagatesException() {
        when(bodyMetricsService.getLatest(USER_ID))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "No body metrics found"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.getLatest(httpRequest));

        log.info("test.getLatest.404 status={}", ex.getStatusCode());
        assertEquals(404, ex.getStatusCode().value());
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/progress/body-metrics/{id}
    // -------------------------------------------------------------------------

    @Test
    void getById_ExistingOwned_Returns200() {
        when(bodyMetricsService.getById(1L, USER_ID)).thenReturn(makeResponse(1L));

        ResponseEntity<BodyMetricsResponse> response = controller.getById(1L, httpRequest);

        log.info("test.getById.200 id={}", response.getBody().getId());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().getId());
    }

    @Test
    void getById_NotFound_PropagatesException() {
        when(bodyMetricsService.getById(99L, USER_ID))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.getById(99L, httpRequest));

        log.info("test.getById.404 status={}", ex.getStatusCode());
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void getById_OtherUser_PropagatesForbidden() {
        when(bodyMetricsService.getById(2L, USER_ID))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.getById(2L, httpRequest));

        log.info("test.getById.403 status={}", ex.getStatusCode());
        assertEquals(403, ex.getStatusCode().value());
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/progress/body-metrics/{id}
    // -------------------------------------------------------------------------

    @Test
    void update_OwnEntry_Returns200() {
        BodyMetricsResponse updated = makeResponse(1L);
        when(bodyMetricsService.update(eq(1L), any(), eq(USER_ID))).thenReturn(updated);

        ResponseEntity<BodyMetricsResponse> response = controller.update(1L, makeRequest(), httpRequest);

        log.info("test.update.200 id={}", response.getBody().getId());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().getId());
    }

    @Test
    void update_OtherUser_PropagatesForbidden() {
        when(bodyMetricsService.update(eq(2L), any(), eq(USER_ID)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.update(2L, makeRequest(), httpRequest));

        log.info("test.update.403 status={}", ex.getStatusCode());
        assertEquals(403, ex.getStatusCode().value());
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/progress/body-metrics/{id}
    // -------------------------------------------------------------------------

    @Test
    void delete_OwnEntry_Returns204() {
        doNothing().when(bodyMetricsService).delete(1L, USER_ID);

        ResponseEntity<Void> response = controller.delete(1L, httpRequest);

        log.info("test.delete.204 status={}", response.getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(bodyMetricsService).delete(1L, USER_ID);
    }

    @Test
    void delete_OtherUser_PropagatesForbidden() {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden"))
                .when(bodyMetricsService).delete(2L, USER_ID);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.delete(2L, httpRequest));

        log.info("test.delete.403 status={}", ex.getStatusCode());
        assertEquals(403, ex.getStatusCode().value());
        verify(bodyMetricsService).delete(2L, USER_ID);
    }
}
