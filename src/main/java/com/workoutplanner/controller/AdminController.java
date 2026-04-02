package com.workoutplanner.controller;

import com.workoutplanner.service.AdminResetService;
import com.workoutplanner.service.PlanScanJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final PlanScanJobService planScanJobService;
    private final AdminResetService adminResetService;

    @Value("${beta.mode:false}")
    private boolean betaMode;

    @Value("${admin.secret:}")
    private String adminSecret;

    @Autowired
    public AdminController(PlanScanJobService planScanJobService,
                           AdminResetService adminResetService) {
        this.planScanJobService = planScanJobService;
        this.adminResetService = adminResetService;
    }

    @PostMapping("/plan-scan/trigger")
    public ResponseEntity<Map<String, Object>> triggerScan() {
        logger.info("Manual plan scan trigger requested");
        Map<String, Object> result = planScanJobService.triggerManualScan();
        return ResponseEntity.ok(result);
    }

    /**
     * Full environment reset: wipes all user-generated data, clears MinIO, re-seeds test coach.
     * Preserves exercise_catalog and food_catalog.
     *
     * Protected by X-Admin-Secret header — must match admin.secret property.
     * Set admin.secret in application properties or as ADMIN_SECRET env var on Render.
     *
     * Usage: POST /api/v1/admin/reset-all
     *        Header: X-Admin-Secret: <your-secret>
     */
    @PostMapping("/reset-all")
    public ResponseEntity<?> resetAll(@RequestHeader(value = "X-Admin-Secret", required = false) String providedSecret) {
        if (adminSecret == null || adminSecret.isBlank()) {
            logger.error("ADMIN RESET blocked: admin.secret is not configured");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "admin.secret is not configured — set it before using this endpoint"));
        }
        if (!adminSecret.equals(providedSecret)) {
            logger.warn("ADMIN RESET blocked: invalid X-Admin-Secret header");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Invalid or missing X-Admin-Secret header"));
        }

        logger.warn("ADMIN RESET initiated via REST endpoint");
        Map<String, Object> result = adminResetService.resetAll();
        return ResponseEntity.ok(result);
    }
}
