package com.workoutplanner.controller;

import com.workoutplanner.dto.FoodCatalogRequestDto;
import com.workoutplanner.dto.FoodCatalogResponseDto;
import com.workoutplanner.service.FoodCatalogService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/foods")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class AdminFoodCatalogController {

    private static final Logger log = LoggerFactory.getLogger(AdminFoodCatalogController.class);

    private final FoodCatalogService foodCatalogService;

    @Autowired
    public AdminFoodCatalogController(FoodCatalogService foodCatalogService) {
        this.foodCatalogService = foodCatalogService;
    }

    @PostMapping
    public ResponseEntity<FoodCatalogResponseDto> createSystemFood(
            @Valid @RequestBody FoodCatalogRequestDto request) {
        log.info("food_catalog.admin.create name={}", request.getName());
        FoodCatalogResponseDto response = foodCatalogService.createSystemFood(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FoodCatalogResponseDto> updateSystemFood(
            @PathVariable Long id,
            @Valid @RequestBody FoodCatalogRequestDto request) {
        log.info("food_catalog.admin.update id={}", id);
        FoodCatalogResponseDto response = foodCatalogService.updateSystemFood(id, request);
        return ResponseEntity.ok(response);
    }
}
