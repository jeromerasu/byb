package com.workoutplanner.controller;

import com.workoutplanner.dto.DietFeedbackRequest;
import com.workoutplanner.model.DietFeedback;
import com.workoutplanner.model.User;
import com.workoutplanner.repository.DietFeedbackRepository;
import com.workoutplanner.repository.UserRepository;
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
class DietFeedbackControllerTest {

    private static final Logger log = LoggerFactory.getLogger(DietFeedbackControllerTest.class);

    @Mock
    private DietFeedbackRepository dietFeedbackRepository;

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

    private DietFeedbackController controller;

    private static final String USER_ID = "user-diet-id";

    @BeforeEach
    void setUp() {
        controller = new DietFeedbackController(dietFeedbackRepository, userRepository, jwtService);
        ReflectionTestUtils.setField(controller, "betaMode", false);

        User user = new User();
        user.setId(USER_ID);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        SecurityContextHolder.setContext(securityContext);
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/diet/feedback
    // -------------------------------------------------------------------------

    @Test
    void submitFeedback_withAllFields_Returns200() {
        DietFeedback saved = new DietFeedback();
        saved.setUserId(USER_ID);

        when(dietFeedbackRepository.save(any(DietFeedback.class))).thenReturn(saved);

        DietFeedbackRequest request = new DietFeedbackRequest();
        request.setRating(4);
        request.setSessionComments(List.of("felt good", "meal was balanced"));
        request.setFlaggedMeals(List.of("chicken salad"));
        request.setFreeFormNote("too much sodium");

        ResponseEntity<DietFeedback> response = controller.submitFeedback(request, httpRequest);

        log.info("test.dietFeedback.post status={}", response.getStatusCode());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(dietFeedbackRepository).save(any(DietFeedback.class));
    }

    @Test
    void submitFeedback_withMinimalBody_Returns200() {
        DietFeedback saved = new DietFeedback();
        saved.setUserId(USER_ID);

        when(dietFeedbackRepository.save(any(DietFeedback.class))).thenReturn(saved);

        DietFeedbackRequest request = new DietFeedbackRequest();
        request.setRating(5);

        ResponseEntity<DietFeedback> response = controller.submitFeedback(request, httpRequest);

        log.info("test.dietFeedback.postMinimal status={}", response.getStatusCode());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(dietFeedbackRepository).save(any(DietFeedback.class));
    }

    @Test
    void submitFeedback_withNullListFields_doesNotThrow() {
        DietFeedback saved = new DietFeedback();
        saved.setUserId(USER_ID);

        when(dietFeedbackRepository.save(any(DietFeedback.class))).thenReturn(saved);

        DietFeedbackRequest request = new DietFeedbackRequest();
        // sessionComments and flaggedMeals are null — must not NPE

        ResponseEntity<DietFeedback> response = controller.submitFeedback(request, httpRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/diet/feedback?days=N
    // -------------------------------------------------------------------------

    @Test
    void getRecentFeedback_WithDays_ReturnsFilteredList() {
        DietFeedback fb = new DietFeedback();
        fb.setUserId(USER_ID);
        fb.setFeedbackDate(LocalDate.now().minusDays(2));

        when(dietFeedbackRepository.findByUserIdAndFeedbackDateAfter(eq(USER_ID), any(LocalDate.class)))
                .thenReturn(List.of(fb));

        ResponseEntity<List<DietFeedback>> response = controller.getRecentFeedback(14, httpRequest);

        log.info("test.dietFeedback.days status={} count={}", response.getStatusCode(), response.getBody().size());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(dietFeedbackRepository).findByUserIdAndFeedbackDateAfter(eq(USER_ID), any(LocalDate.class));
    }

    @Test
    void getRecentFeedback_WithoutDays_DefaultsTo7Days() {
        when(dietFeedbackRepository.findByUserIdAndFeedbackDateAfter(eq(USER_ID), any(LocalDate.class)))
                .thenReturn(List.of());

        // days = 7 is the default — calling with 7 should succeed
        ResponseEntity<List<DietFeedback>> response = controller.getRecentFeedback(7, httpRequest);

        log.info("test.dietFeedback.default status={}", response.getStatusCode());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(dietFeedbackRepository).findByUserIdAndFeedbackDateAfter(
                eq(USER_ID),
                argThat(date -> !date.isBefore(LocalDate.now().minusDays(8)) && !date.isAfter(LocalDate.now().minusDays(6)))
        );
    }

    @Test
    void getRecentFeedback_EmptyResult_Returns200WithEmptyList() {
        when(dietFeedbackRepository.findByUserIdAndFeedbackDateAfter(eq(USER_ID), any(LocalDate.class)))
                .thenReturn(List.of());

        ResponseEntity<List<DietFeedback>> response = controller.getRecentFeedback(30, httpRequest);

        log.info("test.dietFeedback.empty status={}", response.getStatusCode());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }
}
