package com.workoutplanner.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

@Configuration
@Profile("prod")
public class DatabaseConfig {

    @Value("${DATABASE_URL:}")
    private String databaseUrl;

    @Bean
    @Primary
    public DataSource dataSource() throws URISyntaxException {
        System.out.println("=== DatabaseConfig Debug Info ===");
        System.out.println("DATABASE_URL: " + (databaseUrl != null ? databaseUrl : "null"));
        System.out.println("DB_HOST: " + System.getenv("DB_HOST"));
        System.out.println("DB_NAME: " + System.getenv("DB_NAME"));

        // Print all database-related environment variables for debugging
        System.out.println("=== All Environment Variables ===");
        System.getenv().entrySet().stream()
                .filter(e -> e.getKey().contains("DATABASE") || e.getKey().contains("DB") || e.getKey().contains("POSTGRES"))
                .forEach(e -> System.out.println(e.getKey() + ": " + e.getValue()));

        // Look for the original DATABASE_URL that Render provides
        String originalDatabaseUrl = System.getenv("DATABASE_URL");

        if (originalDatabaseUrl != null && !originalDatabaseUrl.isEmpty()) {
            // Clean up the URL by removing any line breaks or whitespace
            originalDatabaseUrl = originalDatabaseUrl.replaceAll("\\s+", "").trim();
            System.out.println("Found DATABASE_URL environment variable (cleaned): " + originalDatabaseUrl);

            if (originalDatabaseUrl.startsWith("postgres://") || originalDatabaseUrl.startsWith("postgresql://")) {
                // Parse Render-style DATABASE_URL and convert to JDBC format
                System.out.println("Converting postgres:// URL to JDBC format...");
                URI dbUri = new URI(originalDatabaseUrl);
                String jdbcUrl = "jdbc:postgresql://" + dbUri.getHost() + ":" + dbUri.getPort() + dbUri.getPath();

                String userInfo = dbUri.getUserInfo();
                String username = null;
                String password = null;

                if (userInfo != null) {
                    String[] userInfoParts = userInfo.split(":");
                    username = userInfoParts[0];
                    if (userInfoParts.length > 1) {
                        password = userInfoParts[1];
                    }
                }

                System.out.println("Final JDBC URL: " + jdbcUrl);
                System.out.println("Username: " + username);

                return DataSourceBuilder.create()
                        .driverClassName("org.postgresql.Driver")
                        .url(jdbcUrl)
                        .username(username)
                        .password(password)
                        .build();
            } else if (originalDatabaseUrl.startsWith("jdbc:postgresql://")) {
                // Handle already-JDBC formatted URLs that might have line breaks
                System.out.println("Cleaning malformed JDBC URL...");

                // Try to extract the components from the malformed JDBC URL
                // Remove "jdbc:postgresql://" prefix
                String urlPart = originalDatabaseUrl.substring("jdbc:postgresql://".length());

                // Split on @ to separate credentials from host/db
                String[] parts = urlPart.split("@");
                if (parts.length == 2) {
                    String credentials = parts[0];
                    String hostAndDb = parts[1];

                    // Extract username and password
                    String[] credParts = credentials.split(":");
                    String username = credParts[0];
                    String password = credParts.length > 1 ? credParts[1] : "";

                    // Extract host, port, and database
                    String[] hostDbParts = hostAndDb.split("/");
                    String hostPort = hostDbParts[0];
                    String database = hostDbParts.length > 1 ? hostDbParts[1] : "";

                    // Rebuild the clean JDBC URL
                    String cleanJdbcUrl = "jdbc:postgresql://" + hostPort + "/" + database;

                    System.out.println("Reconstructed JDBC URL: " + cleanJdbcUrl);
                    System.out.println("Username: " + username);

                    return DataSourceBuilder.create()
                            .driverClassName("org.postgresql.Driver")
                            .url(cleanJdbcUrl)
                            .username(username)
                            .password(password)
                            .build();
                }
            }
        }

        // If DATABASE_URL is empty or invalid, use the separate environment variables
        String host = System.getenv("DB_HOST");
        String port = System.getenv("DB_PORT");
        String database = System.getenv("DB_NAME");
        String username = System.getenv("DB_USERNAME");
        String password = System.getenv("DB_PASSWORD");

        if (host != null && database != null) {
            String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s",
                host, port != null ? port : "5432", database);

            System.out.println("Using separate DB environment variables");
            System.out.println("Final JDBC URL: " + jdbcUrl);

            return DataSourceBuilder.create()
                    .driverClassName("org.postgresql.Driver")
                    .url(jdbcUrl)
                    .username(username)
                    .password(password)
                    .build();
        }

        throw new IllegalStateException("No valid database configuration found. Please set DATABASE_URL or DB_HOST/DB_NAME environment variables.");
    }
}