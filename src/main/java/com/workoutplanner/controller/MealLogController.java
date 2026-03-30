package com.workoutplanner.controller;

import com.workoutplanner.dto.MealLogRequest;
import com.workoutplanner.dto.MealLogResponse;
import com.workoutplanner.model.User;
import com.workoutplanner.repository.UserRepository;
import com.workoutplanner.service.JwtService;
import com.workoutplanner.service.MealLogService;
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
@RequestMapping("/api/v1/diet/logs")
@CrossOrigin(origins = "*")
public class MealLogController {

    private static final Logger log = LoggerFactory.getLogger(MealLogController.class);

    private final MealLogService mealLogService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${beta.mode:false}")
    private boolean betaMode;

    @Autowired
    public MealLogController(MealLogService mealLogService,
                              UserRepository userRepository,
                              JwtService jwtService) {
        this.mealLogService = mealLogService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @PostMapping
    public ResponseEntity<MealLogResponse> create(
            @Valid @RequestBody MealLogRequest request,
            HttpServletRequest httpRequest) {

        String userId = getCurrentUserId(httpRequest);
        log.info("meal_log.controller.create userId={}", userId);
        MealLogResponse response = mealLogService.create(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<MealLogResponse>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest httpRequest) {

        String userId = getCurrentUserId(httpRequest);
        log.info("meal_log.controller.list userId={} startDate={} endDate={}", userId, startDate, endDate);
        return ResponseEntity.ok(mealLogService.list(userId, startDate, endDate));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MealLogResponse> getById(
            @PathVariable String id,
            HttpServletRequest httpRequest) {

        String userId = getCurrentUserId(httpRequest);
        log.info("meal_log.controller.get id={} userId={}", id, userId);
        return ResponseEntity.ok(mealLogService.getById(id, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MealLogResponse> update(
            @PathVariable String id,
            @Valid @RequestBody MealLogRequest request,
            HttpServletRequest httpRequest) {

        String userId = getCurrentUserId(httpRequest);
        log.info("meal_log.controller.update id={} userId={}", id, userId);
        return ResponseEntity.ok(mealLogService.update(id, request, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            HttpServletRequest httpRequest) {

        String userId = getCurrentUserId(httpRequest);
        log.info("meal_log.controller.delete id={} userId={}", id, userId);
        mealLogService.delete(id, userId);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Auth helper — mirrors pattern from FoodCatalogController
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
                    log.debug("meal_log.auth.jwt_extract_failed error={}", e.getMessage());
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
