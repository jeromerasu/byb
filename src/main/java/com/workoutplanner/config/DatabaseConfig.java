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
        if (databaseUrl != null && !databaseUrl.isEmpty() && !databaseUrl.startsWith("jdbc:")) {
            // Parse Render-style DATABASE_URL and convert to JDBC format
            URI dbUri = new URI(databaseUrl);
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

            return DataSourceBuilder.create()
                    .driverClassName("org.postgresql.Driver")
                    .url(jdbcUrl)
                    .username(username)
                    .password(password)
                    .build();
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