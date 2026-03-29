package com.workoutplanner.controller;

import com.workoutplanner.dto.FoodCatalogRequestDto;
import com.workoutplanner.dto.FoodCatalogResponseDto;
import com.workoutplanner.model.User;
import com.workoutplanner.repository.UserRepository;
import com.workoutplanner.service.FoodCatalogService;
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

@RestController
@RequestMapping("/api/v1/foods")
@CrossOrigin(origins = "*")
public class FoodCatalogController {

    private static final Logger log = LoggerFactory.getLogger(FoodCatalogController.class);

    private final FoodCatalogService foodCatalogService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${beta.mode:false}")
    private boolean betaMode;

    @Autowired
    public FoodCatalogController(FoodCatalogService foodCatalogService,
                                  UserRepository userRepository,
                                  JwtService jwtService) {
        this.foodCatalogService = foodCatalogService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @PostMapping
    public ResponseEntity<FoodCatalogResponseDto> createFood(
            @Valid @RequestBody FoodCatalogRequestDto request,
            HttpServletRequest httpRequest) {

        String userId = getCurrentUserId(httpRequest);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        FoodCatalogResponseDto response;
        if (user.getRole() == User.Role.ADMIN) {
            log.info("food_catalog.controller.create.system userId={}", userId);
            response = foodCatalogService.createSystemFood(request);
        } else {
            log.info("food_catalog.controller.create.user userId={}", userId);
            response = foodCatalogService.createUserFood(request, userId);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FoodCatalogResponseDto> updateFood(
            @PathVariable Long id,
            @Valid @RequestBody FoodCatalogRequestDto request,
            HttpServletRequest httpRequest) {

        String userId = getCurrentUserId(httpRequest);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        FoodCatalogResponseDto response;
        if (user.getRole() == User.Role.ADMIN) {
            log.info("food_catalog.controller.update.admin id={} userId={}", id, userId);
            response = foodCatalogService.updateSystemFood(id, request);
        } else {
            log.info("food_catalog.controller.update.user id={} userId={}", id, userId);
            response = foodCatalogService.updateUserFood(id, request, userId);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<FoodCatalogResponseDto>> listFoods(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            HttpServletRequest httpRequest) {

        String userId = getCurrentUserId(httpRequest);
        log.info("food_catalog.controller.list userId={} name={} category={}", userId, name, category);
        return ResponseEntity.ok(foodCatalogService.listVisibleToUser(userId, name, category));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FoodCatalogResponseDto> getFood(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        String userId = getCurrentUserId(httpRequest);
        log.info("food_catalog.controller.get id={} userId={}", id, userId);
        return ResponseEntity.ok(foodCatalogService.getById(id, userId));
    }

    // -------------------------------------------------------------------------
    // Auth helper — mirrors pattern from WorkoutController / DietController
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
                    log.debug("food_catalog.auth.jwt_extract_failed error={}", e.getMessage());
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
