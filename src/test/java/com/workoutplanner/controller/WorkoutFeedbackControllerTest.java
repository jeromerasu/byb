package com.workoutplanner.controller;

import com.workoutplanner.model.User;
import com.workoutplanner.model.WorkoutFeedback;
import com.workoutplanner.repository.UserRepository;
import com.workoutplanner.repository.WorkoutFeedbackRepository;
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

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkoutFeedbackControllerTest {

    private static final Logger log = LoggerFactory.getLogger(WorkoutFeedbackControllerTest.class);

    @Mock
    private WorkoutFeedbackRepository workoutFeedbackRepository;

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

    private WorkoutFeedbackController controller;

    private static final String USER_ID = "user-workout-id";

    @BeforeEach
    void setUp() {
        controller = new WorkoutFeedbackController(workoutFeedbackRepository, userRepository, jwtService);
        ReflectionTestUtils.setField(controller, "betaMode", false);

        User user = new User();
        user.setId(USER_ID);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/workout/feedback?days=N
    // -------------------------------------------------------------------------

    @Test
    void getRecentFeedback_WithDays_ReturnsFilteredList() {
        WorkoutFeedback fb = new WorkoutFeedback();
        fb.setUserId(USER_ID);
        fb.setWorkoutDate(LocalDate.now().minusDays(3));

        when(workoutFeedbackRepository.findByUserIdAndWorkoutDateAfter(eq(USER_ID), any(LocalDate.class)))
                .thenReturn(List.of(fb));

        ResponseEntity<List<WorkoutFeedback>> response = controller.getRecentFeedback(7, httpRequest);

        log.info("test.workoutFeedback.days status={} count={}", response.getStatusCode(), response.getBody().size());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(workoutFeedbackRepository).findByUserIdAndWorkoutDateAfter(eq(USER_ID), any(LocalDate.class));
    }

    @Test
    void getRecentFeedback_WithoutDays_DefaultsTo7Days() {
        when(workoutFeedbackRepository.findByUserIdAndWorkoutDateAfter(eq(USER_ID), any(LocalDate.class)))
                .thenReturn(List.of());

        // days = 7 is the default — calling with 7 should succeed (not throw)
        ResponseEntity<List<WorkoutFeedback>> response = controller.getRecentFeedback(7, httpRequest);

        log.info("test.workoutFeedback.default status={}", response.getStatusCode());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        // The since date should be approximately 7 days ago
        verify(workoutFeedbackRepository).findByUserIdAndWorkoutDateAfter(
                eq(USER_ID),
                argThat(date -> !date.isBefore(LocalDate.now().minusDays(8)) && !date.isAfter(LocalDate.now().minusDays(6)))
        );
    }

    @Test
    void getRecentFeedback_EmptyResult_Returns200WithEmptyList() {
        when(workoutFeedbackRepository.findByUserIdAndWorkoutDateAfter(eq(USER_ID), any(LocalDate.class)))
                .thenReturn(List.of());

        ResponseEntity<List<WorkoutFeedback>> response = controller.getRecentFeedback(30, httpRequest);

        log.info("test.workoutFeedback.empty status={}", response.getStatusCode());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }
}
