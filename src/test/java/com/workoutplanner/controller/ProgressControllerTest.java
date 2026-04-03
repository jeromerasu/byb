package com.workoutplanner.controller;

import com.workoutplanner.dto.OverloadSummaryResponse;
import com.workoutplanner.dto.WorkoutHeatmapResponse;
import com.workoutplanner.model.User;
import com.workoutplanner.repository.UserRepository;
import com.workoutplanner.service.JwtService;
import com.workoutplanner.service.OverloadService;
import com.workoutplanner.service.ProgressService;
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

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Regression test for Bug 2: GET /api/v1/progress/heatmap returns 404 because
 * the endpoint was only mapped to /workout-heatmap. Verifies the /heatmap alias.
 * Also covers overload-summary optional params (returns 400/defaults instead of 500).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProgressControllerTest {

    private static final Logger log = LoggerFactory.getLogger(ProgressControllerTest.class);

    @Mock private ProgressService progressService;
    @Mock private OverloadService overloadService;
    @Mock private UserRepository userRepository;
    @Mock private JwtService jwtService;
    @Mock private HttpServletRequest httpRequest;
    @Mock private Authentication authentication;
    @Mock private SecurityContext securityContext;

    private ProgressController controller;

    private static final String USER_ID = "user-progress-id";

    @BeforeEach
    void setUp() {
        controller = new ProgressController(progressService, overloadService, userRepository, jwtService);
        ReflectionTestUtils.setField(controller, "betaMode", false);

        User user = new User();
        user.setId(USER_ID);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/progress/workout-heatmap (alias /heatmap)
    // -------------------------------------------------------------------------

    @Test
    void workoutHeatmap_ReturnsOk() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 4, 3);
        WorkoutHeatmapResponse entry = new WorkoutHeatmapResponse(from, 2, 6, 45, null);
        when(progressService.getWorkoutHeatmap(eq(USER_ID), any(), any())).thenReturn(List.of(entry));

        ResponseEntity<List<WorkoutHeatmapResponse>> response =
                controller.workoutHeatmap(from, to, httpRequest);

        log.info("test.heatmap.200 count={}", response.getBody().size());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void workoutHeatmap_NoDateParams_DefaultsTo3MonthRange() {
        when(progressService.getWorkoutHeatmap(eq(USER_ID), any(), any())).thenReturn(List.of());

        ResponseEntity<List<WorkoutHeatmapResponse>> response =
                controller.workoutHeatmap(null, null, httpRequest);

        log.info("test.heatmap.defaults status={}", response.getStatusCode());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        // Verify service was called with a ~3-month range (from ≈ 3 months ago)
        verify(progressService).getWorkoutHeatmap(
                eq(USER_ID),
                argThat(d -> d.isBefore(LocalDate.now().minusMonths(2))),
                any()
        );
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/progress/overload-summary
    // -------------------------------------------------------------------------

    @Test
    void overloadSummary_WithFromAndTo_DelegatesToService() {
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);
        OverloadSummaryResponse r = new OverloadSummaryResponse();
        when(overloadService.getOverloadSummary(USER_ID, from, to)).thenReturn(List.of(r));

        ResponseEntity<List<OverloadSummaryResponse>> response = controller.overloadSummary(from, to, httpRequest);

        log.info("test.overloadSummary.explicit status={} count={}", response.getStatusCode(), response.getBody().size());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(overloadService).getOverloadSummary(USER_ID, from, to);
    }

    @Test
    void overloadSummary_NullFrom_UsesDefaultLast30Days() {
        LocalDate to = LocalDate.of(2026, 4, 3);
        when(overloadService.getOverloadSummary(eq(USER_ID), any(LocalDate.class), eq(to)))
                .thenReturn(List.of());

        ResponseEntity<List<OverloadSummaryResponse>> response = controller.overloadSummary(null, to, httpRequest);

        log.info("test.overloadSummary.nullFrom status={}", response.getStatusCode());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(overloadService).getOverloadSummary(
                eq(USER_ID),
                argThat(d -> !d.isAfter(LocalDate.now().minusDays(29))),
                eq(to)
        );
    }

    @Test
    void overloadSummary_NullTo_UsesDefaultToday() {
        LocalDate from = LocalDate.of(2026, 3, 1);
        when(overloadService.getOverloadSummary(eq(USER_ID), eq(from), any(LocalDate.class)))
                .thenReturn(List.of());

        ResponseEntity<List<OverloadSummaryResponse>> response = controller.overloadSummary(from, null, httpRequest);

        log.info("test.overloadSummary.nullTo status={}", response.getStatusCode());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(overloadService).getOverloadSummary(
                eq(USER_ID),
                eq(from),
                argThat(d -> !d.isAfter(LocalDate.now()) && !d.isBefore(LocalDate.now().minusDays(1)))
        );
    }

    @Test
    void overloadSummary_BothNull_UsesDefaults() {
        when(overloadService.getOverloadSummary(eq(USER_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        ResponseEntity<List<OverloadSummaryResponse>> response = controller.overloadSummary(null, null, httpRequest);

        log.info("test.overloadSummary.allNull status={}", response.getStatusCode());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(overloadService).getOverloadSummary(eq(USER_ID), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void overloadSummary_EmptyResult_Returns200EmptyList() {
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);
        when(overloadService.getOverloadSummary(USER_ID, from, to)).thenReturn(List.of());

        ResponseEntity<List<OverloadSummaryResponse>> response = controller.overloadSummary(from, to, httpRequest);

        log.info("test.overloadSummary.empty status={}", response.getStatusCode());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }
}
