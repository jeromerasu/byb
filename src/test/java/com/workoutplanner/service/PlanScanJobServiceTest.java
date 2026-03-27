package com.workoutplanner.service;

import com.workoutplanner.model.*;
import com.workoutplanner.repository.DietProfileRepository;
import com.workoutplanner.repository.PlanGenerationQueueRepository;
import com.workoutplanner.repository.UserRepository;
import com.workoutplanner.repository.WorkoutProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TASK-BE-016E: Unit tests for PlanScanJobService.
 */
@ExtendWith(MockitoExtension.class)
class PlanScanJobServiceTest {

    private static final Logger log = LoggerFactory.getLogger(PlanScanJobServiceTest.class);

    @Mock
    private UserRepository userRepository;
    @Mock
    private WorkoutProfileRepository workoutProfileRepository;
    @Mock
    private DietProfileRepository dietProfileRepository;
    @Mock
    private PlanGenerationQueueRepository planGenerationQueueRepository;

    private PlanScanJobService scanJobService;

    @BeforeEach
    void setUp() {
        scanJobService = new PlanScanJobService(
                userRepository, workoutProfileRepository, dietProfileRepository, planGenerationQueueRepository);
        ReflectionTestUtils.setField(scanJobService, "maxAgeDays", 7);
        ReflectionTestUtils.setField(scanJobService, "scanEnabled", true);
    }

    private User makeUser(String id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private WorkoutProfile makeWorkoutProfile(String userId, String storageKey, LocalDateTime createdAt) {
        WorkoutProfile wp = new WorkoutProfile();
        wp.setUserId(userId);
        wp.setCurrentPlanStorageKey(storageKey);
        wp.setCurrentPlanCreatedAt(createdAt);
        return wp;
    }

    private DietProfile makeDietProfile(String userId, String storageKey) {
        DietProfile dp = new DietProfile();
        dp.setUserId(userId);
        dp.setCurrentPlanStorageKey(storageKey);
        return dp;
    }

    // -- triggerManualScan() / executeScan() - disabled ------------------

    @Test
    void scan_WhenDisabled_ShouldReturnZeroCountsAndSkip() {
        ReflectionTestUtils.setField(scanJobService, "scanEnabled", false);

        Map<String, Object> result = scanJobService.triggerManualScan();

        log.info("test.scan.disabled result={}", result);
        assertEquals(0, result.get("users_scanned"));
        assertEquals(0, result.get("enqueued"));
        verifyNoInteractions(userRepository);
    }

    // -- triggerManualScan() - no users ----------------------------------

    @Test
    void scan_NoUsers_ShouldReturnZeroEnqueued() {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        Map<String, Object> result = scanJobService.triggerManualScan();

        log.info("test.scan.noUsers result={}", result);
        assertEquals(0, result.get("users_scanned"));
        assertEquals(0, result.get("enqueued"));
        verify(planGenerationQueueRepository, never()).save(any());
    }

    // -- triggerManualScan() - missing profiles --------------------------

    @Test
    void scan_UserMissingWorkoutProfile_ShouldSkip() {
        User user = makeUser("user-no-wp");
        when(userRepository.findAll()).thenReturn(List.of(user));
        when(workoutProfileRepository.findByUserId("user-no-wp")).thenReturn(Optional.empty());
        when(dietProfileRepository.findByUserId("user-no-wp")).thenReturn(Optional.empty());

        Map<String, Object> result = scanJobService.triggerManualScan();

        log.info("test.scan.missingWorkout skipped={}", result.get("skipped_no_profiles"));
        assertEquals(1, result.get("skipped_no_profiles"));
        assertEquals(0, result.get("enqueued"));
        verify(planGenerationQueueRepository, never()).save(any());
    }

    @Test
    void scan_UserMissingDietProfile_ShouldSkip() {
        User user = makeUser("user-no-dp");
        WorkoutProfile wp = makeWorkoutProfile("user-no-dp", "key", LocalDateTime.now().minusDays(1));
        when(userRepository.findAll()).thenReturn(List.of(user));
        when(workoutProfileRepository.findByUserId("user-no-dp")).thenReturn(Optional.of(wp));
        when(dietProfileRepository.findByUserId("user-no-dp")).thenReturn(Optional.empty());

        Map<String, Object> result = scanJobService.triggerManualScan();

        log.info("test.scan.missingDiet skipped={}", result.get("skipped_no_profiles"));
        assertEquals(1, result.get("skipped_no_profiles"));
        assertEquals(0, result.get("enqueued"));
    }

    // -- triggerManualScan() - needs plan --------------------------------

    @Test
    void scan_UserWithNoPlan_ShouldEnqueue() {
        User user = makeUser("user-needs-plan");
        WorkoutProfile wp = makeWorkoutProfile("user-needs-plan", null, null);
        DietProfile dp = makeDietProfile("user-needs-plan", null);

        when(userRepository.findAll()).thenReturn(List.of(user));
        when(workoutProfileRepository.findByUserId("user-needs-plan")).thenReturn(Optional.of(wp));
        when(dietProfileRepository.findByUserId("user-needs-plan")).thenReturn(Optional.of(dp));
        when(planGenerationQueueRepository.findByUserIdAndStatusIn(eq("user-needs-plan"), any()))
                .thenReturn(Collections.emptyList());
        when(planGenerationQueueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = scanJobService.triggerManualScan();

        log.info("test.scan.noPlan enqueued={}", result.get("enqueued"));
        assertEquals(1, result.get("enqueued"));
        verify(planGenerationQueueRepository).save(any(PlanGenerationQueue.class));
    }

    @Test
    void scan_UserWithExpiredPlan_ShouldEnqueue() {
        User user = makeUser("user-expired");
        WorkoutProfile wp = makeWorkoutProfile("user-expired", "old-key",
                LocalDateTime.now().minusDays(10)); // older than 7 days
        DietProfile dp = makeDietProfile("user-expired", "old-diet-key");

        when(userRepository.findAll()).thenReturn(List.of(user));
        when(workoutProfileRepository.findByUserId("user-expired")).thenReturn(Optional.of(wp));
        when(dietProfileRepository.findByUserId("user-expired")).thenReturn(Optional.of(dp));
        when(planGenerationQueueRepository.findByUserIdAndStatusIn(eq("user-expired"), any()))
                .thenReturn(Collections.emptyList());
        when(planGenerationQueueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = scanJobService.triggerManualScan();

        log.info("test.scan.expiredPlan enqueued={}", result.get("enqueued"));
        assertEquals(1, result.get("enqueued"));
    }

    // -- triggerManualScan() - already queued ----------------------------

    @Test
    void scan_UserAlreadyQueued_ShouldSkip() {
        User user = makeUser("user-queued");
        WorkoutProfile wp = makeWorkoutProfile("user-queued", null, null);
        DietProfile dp = makeDietProfile("user-queued", null);
        PlanGenerationQueue existing = new PlanGenerationQueue();
        existing.setStatus(QueueStatus.PENDING);

        when(userRepository.findAll()).thenReturn(List.of(user));
        when(workoutProfileRepository.findByUserId("user-queued")).thenReturn(Optional.of(wp));
        when(dietProfileRepository.findByUserId("user-queued")).thenReturn(Optional.of(dp));
        when(planGenerationQueueRepository.findByUserIdAndStatusIn(eq("user-queued"), any()))
                .thenReturn(List.of(existing));

        Map<String, Object> result = scanJobService.triggerManualScan();

        log.info("test.scan.alreadyQueued skipped={}", result.get("skipped_already_queued"));
        assertEquals(1, result.get("skipped_already_queued"));
        assertEquals(0, result.get("enqueued"));
        verify(planGenerationQueueRepository, never()).save(any());
    }

    // -- triggerManualScan() - user with fresh plan ----------------------

    @Test
    void scan_UserWithFreshPlan_ShouldNotEnqueue() {
        User user = makeUser("user-fresh");
        WorkoutProfile wp = makeWorkoutProfile("user-fresh", "fresh-key",
                LocalDateTime.now().minusDays(2)); // within 7 days
        DietProfile dp = makeDietProfile("user-fresh", "fresh-diet-key");

        when(userRepository.findAll()).thenReturn(List.of(user));
        when(workoutProfileRepository.findByUserId("user-fresh")).thenReturn(Optional.of(wp));
        when(dietProfileRepository.findByUserId("user-fresh")).thenReturn(Optional.of(dp));

        Map<String, Object> result = scanJobService.triggerManualScan();

        log.info("test.scan.freshPlan enqueued={}", result.get("enqueued"));
        assertEquals(0, result.get("enqueued"));
        verify(planGenerationQueueRepository, never()).save(any());
    }

    // -- triggerManualScan() - enqueued entry fields ---------------------

    @Test
    void scan_EnqueuedEntry_ShouldHaveCorrectFields() {
        User user = makeUser("user-enqueue-check");
        WorkoutProfile wp = makeWorkoutProfile("user-enqueue-check", null, null);
        DietProfile dp = makeDietProfile("user-enqueue-check", null);

        when(userRepository.findAll()).thenReturn(List.of(user));
        when(workoutProfileRepository.findByUserId("user-enqueue-check")).thenReturn(Optional.of(wp));
        when(dietProfileRepository.findByUserId("user-enqueue-check")).thenReturn(Optional.of(dp));
        when(planGenerationQueueRepository.findByUserIdAndStatusIn(any(), any()))
                .thenReturn(Collections.emptyList());
        when(planGenerationQueueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        scanJobService.triggerManualScan();

        ArgumentCaptor<PlanGenerationQueue> captor = ArgumentCaptor.forClass(PlanGenerationQueue.class);
        verify(planGenerationQueueRepository).save(captor.capture());
        PlanGenerationQueue saved = captor.getValue();

        log.info("test.scan.enqueuedFields userId={} status={}", saved.getUserId(), saved.getStatus());
        assertEquals("user-enqueue-check", saved.getUserId());
        assertEquals(QueueStatus.PENDING, saved.getStatus());
        assertNotNull(saved.getScheduledAt());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    // -- triggerManualScan() - return map keys ---------------------------

    @Test
    void scan_ShouldReturnAllExpectedMetricKeys() {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        Map<String, Object> result = scanJobService.triggerManualScan();

        log.info("test.scan.metricKeys keys={}", result.keySet());
        assertTrue(result.containsKey("users_scanned"));
        assertTrue(result.containsKey("skipped_no_profiles"));
        assertTrue(result.containsKey("skipped_already_queued"));
        assertTrue(result.containsKey("enqueued"));
    }

    // -- triggerManualScan() - multiple users ----------------------------

    @Test
    void scan_MultipleUsers_ShouldProcessAll() {
        User u1 = makeUser("multi-1");
        User u2 = makeUser("multi-2");
        User u3 = makeUser("multi-3");

        // u1: no profiles (both repos queried before isEmpty check)
        when(workoutProfileRepository.findByUserId("multi-1")).thenReturn(Optional.empty());
        when(dietProfileRepository.findByUserId("multi-1")).thenReturn(Optional.empty());

        // u2: has fresh plan
        WorkoutProfile wp2 = makeWorkoutProfile("multi-2", "key2", LocalDateTime.now().minusDays(1));
        DietProfile dp2 = makeDietProfile("multi-2", "dkey2");
        when(workoutProfileRepository.findByUserId("multi-2")).thenReturn(Optional.of(wp2));
        when(dietProfileRepository.findByUserId("multi-2")).thenReturn(Optional.of(dp2));

        // u3: needs plan, not queued
        WorkoutProfile wp3 = makeWorkoutProfile("multi-3", null, null);
        DietProfile dp3 = makeDietProfile("multi-3", null);
        when(workoutProfileRepository.findByUserId("multi-3")).thenReturn(Optional.of(wp3));
        when(dietProfileRepository.findByUserId("multi-3")).thenReturn(Optional.of(dp3));
        when(planGenerationQueueRepository.findByUserIdAndStatusIn(eq("multi-3"), any()))
                .thenReturn(Collections.emptyList());
        when(planGenerationQueueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        when(userRepository.findAll()).thenReturn(List.of(u1, u2, u3));

        Map<String, Object> result = scanJobService.triggerManualScan();

        log.info("test.scan.multipleUsers scanned={} enqueued={} skippedNoProfiles={}",
                result.get("users_scanned"), result.get("enqueued"), result.get("skipped_no_profiles"));
        assertEquals(3, result.get("users_scanned"));
        assertEquals(1, result.get("enqueued"));
        assertEquals(1, result.get("skipped_no_profiles"));
        assertEquals(0, result.get("skipped_already_queued"));
    }
}
