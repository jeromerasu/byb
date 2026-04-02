package com.workoutplanner.controller;

import com.workoutplanner.service.AdminResetService;
import com.workoutplanner.service.PlanScanJobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    // TODO(REMOVE-BEFORE-PROD): Delete this entire endpoint and AdminResetService before production release. Test-only utility.
    @PostMapping("/reset-all")
    public ResponseEntity<Map<String, Object>> resetAll() {
        logger.warn("ADMIN RESET initiated via REST endpoint");
        Map<String, Object> result = adminResetService.resetAll();
        return ResponseEntity.ok(result);
    }
}
