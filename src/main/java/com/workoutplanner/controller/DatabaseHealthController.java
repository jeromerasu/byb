package com.workoutplanner.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@CrossOrigin(origins = "*")
public class DatabaseHealthController {

    private final DataSource dataSource;

    @Autowired
    public DatabaseHealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/database")
    public Mono<ResponseEntity<Map<String, Object>>> checkDatabaseHealth() {
        return Mono.fromCallable(() -> {
            Map<String, Object> healthStatus = new HashMap<>();

            try (Connection connection = dataSource.getConnection()) {
                // Test database connection
                boolean isValid = connection.isValid(5); // 5 second timeout

                if (isValid) {
                    String databaseProductName = connection.getMetaData().getDatabaseProductName();
                    String databaseProductVersion = connection.getMetaData().getDatabaseProductVersion();
                    String databaseUrl = connection.getMetaData().getURL();

                    healthStatus.put("status", "UP");
                    healthStatus.put("database", Map.of(
                        "product_name", databaseProductName,
                        "product_version", databaseProductVersion,
                        "url", maskPassword(databaseUrl),
                        "connection_valid", true
                    ));
                    healthStatus.put("timestamp", LocalDateTime.now());

                    return ResponseEntity.ok(healthStatus);
                } else {
                    healthStatus.put("status", "DOWN");
                    healthStatus.put("error", "Database connection is not valid");
                    healthStatus.put("timestamp", LocalDateTime.now());

                    return ResponseEntity.status(503).body(healthStatus);
                }

            } catch (SQLException e) {
                healthStatus.put("status", "DOWN");
                healthStatus.put("error", "Database connection failed: " + e.getMessage());
                healthStatus.put("timestamp", LocalDateTime.now());

                return ResponseEntity.status(503).body(healthStatus);
            }
        });
    }

    @GetMapping("/database/detailed")
    public Mono<ResponseEntity<Map<String, Object>>> checkDetailedDatabaseHealth() {
        return Mono.fromCallable(() -> {
            Map<String, Object> detailedHealth = new HashMap<>();

            try (Connection connection = dataSource.getConnection()) {
                boolean isValid = connection.isValid(5);

                if (isValid) {
                    var metaData = connection.getMetaData();

                    detailedHealth.put("status", "UP");
                    detailedHealth.put("database_info", Map.of(
                        "product_name", metaData.getDatabaseProductName(),
                        "product_version", metaData.getDatabaseProductVersion(),
                        "driver_name", metaData.getDriverName(),
                        "driver_version", metaData.getDriverVersion(),
                        "url", maskPassword(metaData.getURL()),
                        "username", metaData.getUserName(),
                        "max_connections", metaData.getMaxConnections(),
                        "catalog_term", metaData.getCatalogTerm(),
                        "schema_term", metaData.getSchemaTerm()
                    ));

                    // Test a simple query
                    try (var stmt = connection.createStatement()) {
                        var rs = stmt.executeQuery("SELECT 1 as test_query");
                        boolean queryWorked = rs.next() && rs.getInt("test_query") == 1;
                        detailedHealth.put("query_test", queryWorked ? "PASS" : "FAIL");
                    }

                    detailedHealth.put("timestamp", LocalDateTime.now());

                    return ResponseEntity.ok(detailedHealth);
                } else {
                    detailedHealth.put("status", "DOWN");
                    detailedHealth.put("error", "Database connection is not valid");
                    detailedHealth.put("timestamp", LocalDateTime.now());

                    return ResponseEntity.status(503).body(detailedHealth);
                }

            } catch (SQLException e) {
                detailedHealth.put("status", "DOWN");
                detailedHealth.put("error", "Database connection failed: " + e.getMessage());
                detailedHealth.put("timestamp", LocalDateTime.now());

                return ResponseEntity.status(503).body(detailedHealth);
            }
        });
    }

    /**
     * Mask password in database URL for security
     */
    private String maskPassword(String url) {
        if (url == null) return null;

        // Mask password in JDBC URL
        return url.replaceAll("password=[^&;]*", "password=***");
    }
}