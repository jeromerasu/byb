package com.workoutplanner.controller;

import com.workoutplanner.dto.ExerciseCatalogRequestDto;
import com.workoutplanner.dto.ExerciseCatalogResponseDto;
import com.workoutplanner.service.ExerciseCatalogService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin-only endpoints for the exercise catalog.
 * <ul>
 *   <li>POST /api/v1/admin/exercises       — create a system exercise entry</li>
 *   <li>PUT  /api/v1/admin/exercises/{id}  — update a system exercise entry</li>
 * </ul>
 * Protected by ROLE_ADMIN in production (beta mode bypasses Spring Security).
 */
@RestController
@RequestMapping("/api/v1/admin/exercises")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class AdminExerciseCatalogController {

    private static final Logger log = LoggerFactory.getLogger(AdminExerciseCatalogController.class);

    private final ExerciseCatalogService service;

    @Autowired
    public AdminExerciseCatalogController(ExerciseCatalogService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ExerciseCatalogResponseDto> create(
            @Valid @RequestBody ExerciseCatalogRequestDto dto) {

        log.info("admin.catalog.create name={}", dto.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createSystemEntry(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExerciseCatalogResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody ExerciseCatalogRequestDto dto) {

        log.info("admin.catalog.update id={}", id);
        return ResponseEntity.ok(service.updateSystemEntry(id, dto));
    }
}
