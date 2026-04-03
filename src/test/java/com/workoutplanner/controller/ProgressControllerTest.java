package com.workoutplanner.controller;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Regression test for Bug 2: GET /api/v1/progress/heatmap returns 404 because
 * the endpoint was only mapped to /workout-heatmap. Verifies the /heatmap alias.
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
}
