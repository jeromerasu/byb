package com.workoutplanner.controller;

import com.workoutplanner.dto.ExerciseCatalogRequestDto;
import com.workoutplanner.dto.ExerciseCatalogResponseDto;
import com.workoutplanner.model.User;
import com.workoutplanner.repository.UserRepository;
import com.workoutplanner.service.ExerciseCatalogService;
import com.workoutplanner.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * User-facing endpoints for the exercise catalog.
 * <ul>
 *   <li>POST   /api/v1/exercises           — create (admin: system entry; user: custom entry)</li>
 *   <li>PUT    /api/v1/exercises/{id}       — update own custom entry</li>
 *   <li>GET    /api/v1/exercises            — list (system + own custom), with optional filters</li>
 *   <li>GET    /api/v1/exercises/{id}       — get single</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/exercises")
@CrossOrigin(origins = "*")
public class ExerciseCatalogController {

    private static final Logger log = LoggerFactory.getLogger(ExerciseCatalogController.class);

    private final ExerciseCatalogService service;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Value("${beta.mode:false}")
    private boolean betaMode;

    @Autowired
    public ExerciseCatalogController(ExerciseCatalogService service,
                                     JwtService jwtService,
                                     UserRepository userRepository) {
        this.service = service;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<ExerciseCatalogResponseDto> create(
            @Valid @RequestBody ExerciseCatalogRequestDto dto,
            HttpServletRequest request) {

        String userId = getCurrentUserId(request);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        ExerciseCatalogResponseDto response;
        if (User.Role.ADMIN.equals(user.getRole())) {
            log.info("catalog.create.admin userId={}", userId);
            response = service.createSystemEntry(dto);
        } else {
            log.info("catalog.create.user userId={}", userId);
            response = service.createForUser(dto, userId);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExerciseCatalogResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody ExerciseCatalogRequestDto dto,
            HttpServletRequest request) {

        String userId = getCurrentUserId(request);
        log.info("catalog.update userId={} id={}", userId, id);
        return ResponseEntity.ok(service.updateForUser(id, dto, userId));
    }

    @GetMapping
    public ResponseEntity<List<ExerciseCatalogResponseDto>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String muscleGroup,
            @RequestParam(required = false) String equipment) {

        return ResponseEntity.ok(service.listForUser(name, type, muscleGroup, equipment));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExerciseCatalogResponseDto> getById(
            @PathVariable Long id,
            HttpServletRequest request) {

        String userId = getCurrentUserId(request);
        return ResponseEntity.ok(service.getForUser(id, userId));
    }

    // ---------------------------------------------------------------
    // Auth helper — mirrors WorkoutController pattern
    // ---------------------------------------------------------------

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
                    log.debug("catalog.auth.betaJwtFail reason={}", e.getMessage());
                }
            }
            try {
                Optional<User> firstUser = userRepository.findAll().stream().findFirst();
                if (firstUser.isPresent()) {
                    log.debug("catalog.auth.betaFallback username={}", firstUser.get().getUsername());
                    return firstUser.get().getId();
                }
            } catch (Exception e) {
                log.debug("catalog.auth.betaFallbackFail reason={}", e.getMessage());
            }
            throw new RuntimeException("No users found in database for BETA testing");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return ((User) authentication.getPrincipal()).getId();
        }
        throw new RuntimeException("User not authenticated");
    }
}
