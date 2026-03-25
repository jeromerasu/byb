package com.workoutplanner.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

@RestController
@RequestMapping("/api/v1/config")
public class ConfigController {

    private final Environment environment;

    @Value("${storage.use-local:NOT_SET}")
    private String useLocalStorage;

    @Value("${beta.mode:NOT_SET}")
    private String betaMode;

    public ConfigController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/spring-info")
    public ResponseEntity<Map<String, Object>> getSpringInfo() {
        Map<String, Object> info = new HashMap<>();

        // Get active profiles
        String[] activeProfiles = environment.getActiveProfiles();
        info.put("active_profiles", Arrays.asList(activeProfiles));

        // Get default profiles
        String[] defaultProfiles = environment.getDefaultProfiles();
        info.put("default_profiles", Arrays.asList(defaultProfiles));

        // Get key configuration values
        info.put("storage.use-local", useLocalStorage);
        info.put("beta.mode", betaMode);

        // Check if specific environment variables exist
        Map<String, Object> envVars = new HashMap<>();
        envVars.put("SPRING_PROFILES_ACTIVE", System.getenv("SPRING_PROFILES_ACTIVE"));
        envVars.put("MINIO_ENDPOINT", System.getenv("MINIO_ENDPOINT") != null ? "SET" : "NOT_SET");
        envVars.put("MINIO_ROOT_USER", System.getenv("MINIO_ROOT_USER") != null ? "SET" : "NOT_SET");
        envVars.put("MINIO_ROOT_PASSWORD", System.getenv("MINIO_ROOT_PASSWORD") != null ? "SET" : "NOT_SET");
        info.put("environment_variables", envVars);

        return ResponseEntity.ok(info);
    }
}