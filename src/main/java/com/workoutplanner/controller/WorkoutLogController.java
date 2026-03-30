package com.workoutplanner.controller;

import com.workoutplanner.dto.WorkoutLogRequest;
import com.workoutplanner.dto.WorkoutLogResponse;
import com.workoutplanner.model.User;
import com.workoutplanner.repository.UserRepository;
import com.workoutplanner.service.JwtService;
import com.workoutplanner.service.WorkoutLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/workout/logs")
@CrossOrigin(origins = "*")
public class WorkoutLogController {

    private static final Logger log = LoggerFactory.getLogger(WorkoutLogController.class);

    private final WorkoutLogService workoutLogService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${beta.mode:false}")
    private boolean betaMode;

    @Autowired
    public WorkoutLogController(WorkoutLogService workoutLogService,
                                UserRepository userRepository,
                                JwtService jwtService) {
        this.workoutLogService = workoutLogService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @PostMapping
    public ResponseEntity<WorkoutLogResponse> create(
            @Valid @RequestBody WorkoutLogRequest request,
            HttpServletRequest httpRequest) {

        String userId = getCurrentUserId(httpRequest);
        log.info("workout_log.controller.create userId={}", userId);
        WorkoutLogResponse response = workoutLogService.create(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<WorkoutLogResponse>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest httpRequest) {

        String userId = getCurrentUserId(httpRequest);
        log.info("workout_log.controller.list userId={} from={} to={}", userId, from, to);
        return ResponseEntity.ok(workoutLogService.list(userId, from, to));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkoutLogResponse> getById(
            @PathVariable String id,
            HttpServletRequest httpRequest) {

        String userId = getCurrentUserId(httpRequest);
        log.info("workout_log.controller.get id={} userId={}", id, userId);
        return ResponseEntity.ok(workoutLogService.getById(id, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WorkoutLogResponse> update(
            @PathVariable String id,
            @Valid @RequestBody WorkoutLogRequest request,
            HttpServletRequest httpRequest) {

        String userId = getCurrentUserId(httpRequest);
        log.info("workout_log.controller.update id={} userId={}", id, userId);
        return ResponseEntity.ok(workoutLogService.update(id, request, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            HttpServletRequest httpRequest) {

        String userId = getCurrentUserId(httpRequest);
        log.info("workout_log.controller.delete id={} userId={}", id, userId);
        workoutLogService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Auth helper — mirrors pattern from MealLogController
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
                    log.debug("workout_log.auth.jwt_extract_failed error={}", e.getMessage());
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
