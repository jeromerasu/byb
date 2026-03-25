package com.workoutplanner.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple controller with zero dependencies to test basic functionality
 * when S3Client bean creation might be failing
 */
@RestController
@RequestMapping("/api/v1/simple")
public class SimpleHealthController {

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");
        result.put("timestamp", System.currentTimeMillis());
        result.put("message", "Simple endpoint with no dependencies");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/env-check")
    public ResponseEntity<Map<String, Object>> checkEnvironment() {
        Map<String, Object> result = new HashMap<>();

        // Check environment variables
        String minioEndpoint = System.getenv("MINIO_ENDPOINT");
        String minioUser = System.getenv("MINIO_ROOT_USER");
        String minioPassword = System.getenv("MINIO_ROOT_PASSWORD");
        String minioRegion = System.getenv("MINIO_REGION");
        String port = System.getenv("PORT");
        String javaHome = System.getenv("JAVA_HOME");

        result.put("environment_variables", Map.of(
            "MINIO_ENDPOINT", minioEndpoint != null ? minioEndpoint : "NOT_SET",
            "MINIO_ROOT_USER", minioUser != null ? "SET (length: " + minioUser.length() + ")" : "NOT_SET",
            "MINIO_ROOT_PASSWORD", minioPassword != null ? "SET (length: " + minioPassword.length() + ")" : "NOT_SET",
            "MINIO_REGION", minioRegion != null ? minioRegion : "NOT_SET",
            "PORT", port != null ? port : "NOT_SET",
            "JAVA_HOME", javaHome != null ? "SET" : "NOT_SET"
        ));

        result.put("status", "ok");
        result.put("controller_created_successfully", true);

        return ResponseEntity.ok(result);
    }
}