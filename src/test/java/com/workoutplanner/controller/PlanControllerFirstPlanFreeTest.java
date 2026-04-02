package com.workoutplanner.controller;

import com.workoutplanner.model.UserWeekPlan;
import com.workoutplanner.repository.UserWeekPlanRepository;
import com.workoutplanner.service.BillingEntitlementService;
import com.workoutplanner.service.CombinedPlanService;
import com.workoutplanner.service.JwtService;
import com.workoutplanner.service.PlanParsingService;
import com.workoutplanner.service.StorageService;
import com.workoutplanner.repository.WorkoutProfileRepository;
import com.workoutplanner.repository.DietProfileRepository;
import com.workoutplanner.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests for Issue 1: first plan generation is free for new users,
 * regardless of subscription status.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlanControllerFirstPlanFreeTest {

    @Mock private CombinedPlanService combinedPlanService;
    @Mock private PlanParsingService planParsingService;
    @Mock private StorageService storageService;
    @Mock private WorkoutProfileRepository workoutProfileRepository;
    @Mock private DietProfileRepository dietProfileRepository;
    @Mock private UserRepository userRepository;
    @Mock private JwtService jwtService;
    @Mock private BillingEntitlementService billingEntitlementService;
    @Mock private UserWeekPlanRepository userWeekPlanRepository;
    @Mock private HttpServletRequest httpRequest;

    private PlanController controller;

    private static final String TEST_USER_ID = "user-abc";

    @BeforeEach
    void setUp() {
        // Clear any SecurityContext leaked by other tests
        org.springframework.security.core.context.SecurityContextHolder.clearContext();

        controller = new PlanController(
                combinedPlanService, planParsingService, storageService,
                workoutProfileRepository, dietProfileRepository,
                userRepository, jwtService, billingEntitlementService,
                userWeekPlanRepository);

        // production mode — auth via SecurityContext
        ReflectionTestUtils.setField(controller, "betaMode", false);
        ReflectionTestUtils.setField(controller, "billingEnforcementEnabled", true);

        // Seed a valid SecurityContext so getCurrentUserId() works
        com.workoutplanner.model.User mockUser = new com.workoutplanner.model.User();
        mockUser.setId(TEST_USER_ID);
        org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        mockUser, null, Collections.emptyList()));
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void firstPlan_FreeUserNoPlans_Allowed() {
        // User has no prior plans → first plan is free
        when(billingEntitlementService.hasActivePremiumEntitlement(TEST_USER_ID)).thenReturn(false);
        when(userWeekPlanRepository.findByUserIdOrderByWeekStartDesc(TEST_USER_ID))
                .thenReturn(Collections.emptyList());
        when(combinedPlanService.generateCombinedPlan(TEST_USER_ID))
                .thenReturn(new com.workoutplanner.dto.CombinedPlanResponseDto());

        ResponseEntity<?> response = controller.generateCombinedPlan(httpRequest).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(combinedPlanService).generateCombinedPlan(TEST_USER_ID);
    }

    @Test
    void secondPlan_FreeUserHasExistingPlans_Blocked() {
        // User already has a plan → billing gate applies
        when(billingEntitlementService.hasActivePremiumEntitlement(TEST_USER_ID)).thenReturn(false);
        UserWeekPlan existingPlan = new UserWeekPlan();
        when(userWeekPlanRepository.findByUserIdOrderByWeekStartDesc(TEST_USER_ID))
                .thenReturn(List.of(existingPlan));

        ResponseEntity<?> response = controller.generateCombinedPlan(httpRequest).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(combinedPlanService, never()).generateCombinedPlan(anyString());
    }

    @Test
    void premiumUser_WithExistingPlans_Allowed() {
        // Premium users are always allowed regardless of plan count
        when(billingEntitlementService.hasActivePremiumEntitlement(TEST_USER_ID)).thenReturn(true);
        when(combinedPlanService.generateCombinedPlan(TEST_USER_ID))
                .thenReturn(new com.workoutplanner.dto.CombinedPlanResponseDto());

        ResponseEntity<?> response = controller.generateCombinedPlan(httpRequest).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Plan repo should NOT be queried for premium users (billing gate is skipped entirely)
        verify(userWeekPlanRepository, never()).findByUserIdOrderByWeekStartDesc(anyString());
    }

    @Test
    void billingDisabled_FreeUserWithPlans_AlwaysAllowed() {
        ReflectionTestUtils.setField(controller, "billingEnforcementEnabled", false);
        when(combinedPlanService.generateCombinedPlan(TEST_USER_ID))
                .thenReturn(new com.workoutplanner.dto.CombinedPlanResponseDto());

        ResponseEntity<?> response = controller.generateCombinedPlan(httpRequest).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(billingEntitlementService, never()).hasActivePremiumEntitlement(anyString());
    }
}
