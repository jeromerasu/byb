package com.workoutplanner.controller;

import com.workoutplanner.dto.BodyMetricsRequest;
import com.workoutplanner.dto.BodyMetricsResponse;
import com.workoutplanner.model.User;
import com.workoutplanner.repository.UserRepository;
import com.workoutplanner.service.BodyMetricsService;
import com.workoutplanner.service.JwtService;
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
@RequestMapping("/api/v1/progress/body-metrics")
@CrossOrigin(origins = "*")
public class BodyMetricsController {

    private static final Logger log = LoggerFactory.getLogger(BodyMetricsController.class);

    private final BodyMetricsService bodyMetricsService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${beta.mode:false}")
    private boolean betaMode;

    @Autowired
    public BodyMetricsController(BodyMetricsService bodyMetricsService,
                                  UserRepository userRepository,
                                  JwtService jwtService) {
        this.bodyMetricsService = bodyMetricsService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @PostMapping
    public ResponseEntity<BodyMetricsResponse> create(
            @Valid @RequestBody BodyMetricsRequest request,
            HttpServletRequest httpRequest) {

        String userId = getCurrentUserId(httpRequest);
        log.info("body_metrics.controller.create userId={}", userId);
        BodyMetricsResponse response = bodyMetricsService.create(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<BodyMetricsResponse>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletRequest httpRequest) {

        String userId = getCurrentUserId(httpRequest);
        log.info("body_metrics.controller.list userId={} from={} to={}", userId, from, to);
        return ResponseEntity.ok(bodyMetricsService.list(userId, from, to));
    }

    @GetMapping("/latest")
    public ResponseEntity<BodyMetricsResponse> getLatest(HttpServletRequest httpRequest) {
        String userId = getCurrentUserId(httpRequest);
        log.info("body_metrics.controller.getLatest userId={}", userId);
        return ResponseEntity.ok(bodyMetricsService.getLatest(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BodyMetricsResponse> getById(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        String userId = getCurrentUserId(httpRequest);
        log.info("body_metrics.controller.get id={} userId={}", id, userId);
        return ResponseEntity.ok(bodyMetricsService.getById(id, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BodyMetricsResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody BodyMetricsRequest request,
            HttpServletRequest httpRequest) {

        String userId = getCurrentUserId(httpRequest);
        log.info("body_metrics.controller.update id={} userId={}", id, userId);
        return ResponseEntity.ok(bodyMetricsService.update(id, request, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        String userId = getCurrentUserId(httpRequest);
        log.info("body_metrics.controller.delete id={} userId={}", id, userId);
        bodyMetricsService.delete(id, userId);
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
                    log.debug("body_metrics.auth.jwt_extract_failed error={}", e.getMessage());
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
