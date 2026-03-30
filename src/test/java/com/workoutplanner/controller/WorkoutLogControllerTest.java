package com.workoutplanner.controller;

import com.workoutplanner.dto.WorkoutLogRequest;
import com.workoutplanner.dto.WorkoutLogResponse;
import com.workoutplanner.model.User;
import com.workoutplanner.repository.UserRepository;
import com.workoutplanner.service.JwtService;
import com.workoutplanner.service.WorkoutLogService;
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
 * P1-001: Unit tests for WorkoutLogController.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkoutLogControllerTest {

    private static final Logger log = LoggerFactory.getLogger(WorkoutLogControllerTest.class);

    @Mock
    private WorkoutLogService workoutLogService;

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

    private WorkoutLogController controller;

    private static final String USER_ID = "user-a-id";

    @BeforeEach
    void setUp() {
        controller = new WorkoutLogController(workoutLogService, userRepository, jwtService);
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

    private WorkoutLogResponse makeResponse(String id) {
        WorkoutLogResponse r = new WorkoutLogResponse();
        ReflectionTestUtils.setField(r, "id", id);
        ReflectionTestUtils.setField(r, "userId", USER_ID);
        ReflectionTestUtils.setField(r, "exercise", "Bench Press");
        ReflectionTestUtils.setField(r, "date", LocalDate.of(2026, 3, 29));
        ReflectionTestUtils.setField(r, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(r, "updatedAt", LocalDateTime.now());
        return r;
    }

    private WorkoutLogRequest makeRequest() {
        WorkoutLogRequest req = new WorkoutLogRequest();
        req.setExercise("Bench Press");
        req.setWeight(new BigDecimal("100.00"));
        req.setDate(LocalDate.of(2026, 3, 29));
        return req;
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/workout/logs
    // -------------------------------------------------------------------------

    @Test
    void create_ValidRequest_Returns201() {
        WorkoutLogResponse created = makeResponse("uuid-001");
        when(workoutLogService.create(any(), eq(USER_ID))).thenReturn(created);

        ResponseEntity<WorkoutLogResponse> response = controller.create(makeRequest(), httpRequest);

        log.info("test.create.201 status={} id={}", response.getStatusCode(), response.getBody().getId());
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("uuid-001", response.getBody().getId());
        verify(workoutLogService).create(any(), eq(USER_ID));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/workout/logs
    // -------------------------------------------------------------------------

    @Test
    void list_NoDateRange_Returns200WithEntries() {
        List<WorkoutLogResponse> entries = List.of(makeResponse("uuid-001"), makeResponse("uuid-002"));
        when(workoutLogService.list(eq(USER_ID), isNull(), isNull())).thenReturn(entries);

        ResponseEntity<List<WorkoutLogResponse>> response = controller.list(null, null, httpRequest);

        log.info("test.list.200 count={}", response.getBody().size());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
    }

    @Test
    void list_WithDateRange_DelegatesToService() {
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);
        when(workoutLogService.list(eq(USER_ID), eq(from), eq(to))).thenReturn(List.of(makeResponse("uuid-001")));

        ResponseEntity<List<WorkoutLogResponse>> response = controller.list(from, to, httpRequest);

        log.info("test.list.dateRange count={}", response.getBody().size());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(workoutLogService).list(USER_ID, from, to);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/workout/logs/{id}
    // -------------------------------------------------------------------------

    @Test
    void getById_ExistingOwned_Returns200() {
        when(workoutLogService.getById("uuid-001", USER_ID)).thenReturn(makeResponse("uuid-001"));

        ResponseEntity<WorkoutLogResponse> response = controller.getById("uuid-001", httpRequest);

        log.info("test.getById.200 id={}", response.getBody().getId());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("uuid-001", response.getBody().getId());
    }

    @Test
    void getById_NotFound_PropagatesException() {
        when(workoutLogService.getById("missing", USER_ID))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.getById("missing", httpRequest));

        log.info("test.getById.404 status={}", ex.getStatusCode());
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void getById_OtherUser_PropagatesForbidden() {
        when(workoutLogService.getById("uuid-002", USER_ID))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.getById("uuid-002", httpRequest));

        log.info("test.getById.403 status={}", ex.getStatusCode());
        assertEquals(403, ex.getStatusCode().value());
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/workout/logs/{id}
    // -------------------------------------------------------------------------

    @Test
    void update_OwnEntry_Returns200() {
        WorkoutLogResponse updated = makeResponse("uuid-001");
        when(workoutLogService.update(eq("uuid-001"), any(), eq(USER_ID))).thenReturn(updated);

        ResponseEntity<WorkoutLogResponse> response = controller.update("uuid-001", makeRequest(), httpRequest);

        log.info("test.update.200 id={}", response.getBody().getId());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("uuid-001", response.getBody().getId());
    }

    @Test
    void update_OtherUser_PropagatesForbidden() {
        when(workoutLogService.update(eq("uuid-002"), any(), eq(USER_ID)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.update("uuid-002", makeRequest(), httpRequest));

        log.info("test.update.403 status={}", ex.getStatusCode());
        assertEquals(403, ex.getStatusCode().value());
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/workout/logs/{id}
    // -------------------------------------------------------------------------

    @Test
    void delete_OwnEntry_Returns204() {
        doNothing().when(workoutLogService).delete("uuid-001", USER_ID);

        ResponseEntity<Void> response = controller.delete("uuid-001", httpRequest);

        log.info("test.delete.204 status={}", response.getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(workoutLogService).delete("uuid-001", USER_ID);
    }

    @Test
    void delete_OtherUser_PropagatesForbidden() {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden"))
                .when(workoutLogService).delete("uuid-002", USER_ID);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.delete("uuid-002", httpRequest));

        log.info("test.delete.403 status={}", ex.getStatusCode());
        assertEquals(403, ex.getStatusCode().value());
        verify(workoutLogService).delete("uuid-002", USER_ID);
    }
}
