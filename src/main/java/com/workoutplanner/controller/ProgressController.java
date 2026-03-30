package com.workoutplanner.controller;

import com.workoutplanner.dto.*;
import com.workoutplanner.model.User;
import com.workoutplanner.repository.UserRepository;
import com.workoutplanner.service.JwtService;
import com.workoutplanner.service.OverloadService;
import com.workoutplanner.service.ProgressService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/progress")
@CrossOrigin(origins = "*")
public class ProgressController {

    private static final Logger log = LoggerFactory.getLogger(ProgressController.class);

    private final ProgressService progressService;
    private final OverloadService overloadService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${beta.mode:false}")
    private boolean betaMode;

    @Autowired
    public ProgressController(ProgressService progressService,
                               OverloadService overloadService,
                               UserRepository userRepository,
                               JwtService jwtService) {
        this.progressService = progressService;
        this.overloadService = overloadService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @GetMapping("/exercise-history")
    public ResponseEntity<List<ExerciseHistoryResponse>> exerciseHistory(
            @RequestParam(required = false) String exercise,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest request) {

        String userId = getCurrentUserId(request);
        LocalDate resolvedFrom = from != null ? from : LocalDate.now().minusYears(1);
        LocalDate resolvedTo = to != null ? to : LocalDate.now();
        log.info("progress.exercise_history.request userId={} exercise={} from={} to={}", userId, exercise, resolvedFrom, resolvedTo);
        return ResponseEntity.ok(progressService.getExerciseHistory(userId, exercise, resolvedFrom, resolvedTo));
    }

    @GetMapping("/workout-heatmap")
    public ResponseEntity<List<WorkoutHeatmapResponse>> workoutHeatmap(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest request) {

        String userId = getCurrentUserId(request);
        LocalDate resolvedFrom = from != null ? from : LocalDate.now().minusMonths(3);
        LocalDate resolvedTo = to != null ? to : LocalDate.now();
        log.info("progress.workout_heatmap.request userId={} from={} to={}", userId, resolvedFrom, resolvedTo);
        return ResponseEntity.ok(progressService.getWorkoutHeatmap(userId, resolvedFrom, resolvedTo));
    }

    @GetMapping("/bodyweight")
    public ResponseEntity<List<BodyweightResponse>> bodyweight(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest request) {

        String userId = getCurrentUserId(request);
        log.info("progress.bodyweight.request userId={} from={} to={}", userId, from, to);
        return ResponseEntity.ok(progressService.getBodyweight(userId, from, to));
    }

    @GetMapping("/nutrition-adherence")
    public ResponseEntity<List<NutritionAdherenceResponse>> nutritionAdherence(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest request) {

        String userId = getCurrentUserId(request);
        LocalDate resolvedFrom = from != null ? from : LocalDate.now().minusMonths(1);
        LocalDate resolvedTo = to != null ? to : LocalDate.now();
        log.info("progress.nutrition_adherence.request userId={} from={} to={}", userId, resolvedFrom, resolvedTo);
        return ResponseEntity.ok(progressService.getNutritionAdherence(userId, resolvedFrom, resolvedTo));
    }

    @GetMapping("/volume-trend")
    public ResponseEntity<List<VolumeTrendResponse>> volumeTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest request) {

        String userId = getCurrentUserId(request);
        LocalDate resolvedFrom = from != null ? from : LocalDate.now().minusMonths(1);
        LocalDate resolvedTo = to != null ? to : LocalDate.now();
        log.info("progress.volume_trend.request userId={} from={} to={}", userId, resolvedFrom, resolvedTo);
        return ResponseEntity.ok(progressService.getVolumeTrend(userId, resolvedFrom, resolvedTo));
    }

    @GetMapping("/muscle-balance")
    public ResponseEntity<List<MuscleBalanceResponse>> muscleBalance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest request) {

        String userId = getCurrentUserId(request);
        LocalDate resolvedFrom = from != null ? from : LocalDate.now().minusMonths(1);
        LocalDate resolvedTo = to != null ? to : LocalDate.now();
        log.info("progress.muscle_balance.request userId={} from={} to={}", userId, resolvedFrom, resolvedTo);
        return ResponseEntity.ok(progressService.getMuscleBalance(userId, resolvedFrom, resolvedTo));
    }

    @GetMapping("/weekly-overview")
    public ResponseEntity<WeeklyOverviewResponse> weeklyOverview(HttpServletRequest request) {
        String userId = getCurrentUserId(request);
        log.info("progress.weekly_overview.request userId={}", userId);
        return ResponseEntity.ok(progressService.getWeeklyOverview(userId));
    }

    // P1-011: Progressive overload summary
    @GetMapping("/overload-summary")
    public ResponseEntity<List<OverloadSummaryResponse>> overloadSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest request) {

        String userId = getCurrentUserId(request);
        log.info("progress.overload_summary userId={} from={} to={}", userId, from, to);
        return ResponseEntity.ok(overloadService.getOverloadSummary(userId, from, to));
    }

    // -------------------------------------------------------------------------
    // Auth helper — mirrors pattern from other controllers
    // -------------------------------------------------------------------------

    private String getCurrentUserId(HttpServletRequest request) {
        if (betaMode) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7);
                    String username = jwtService.extractUsername(token);
                    Optional<User> user = userRepository.findByUsername(username);
                    if (user.isPresent()) {
                        return user.get().getId();
                    }
                } catch (Exception e) {
                    log.debug("progress.auth.jwt_extract_failed error={}", e.getMessage());
                }
            }
            return userRepository.findAll().stream()
                    .findFirst()
                    .map(User::getId)
                    .orElseThrow(() -> new RuntimeException("No users found in database for BETA testing"));
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        throw new RuntimeException("User not authenticated");
    }
}
