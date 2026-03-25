package com.workoutplanner.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/diagnostic")
public class DiagnosticController {

    private static final Logger logger = LoggerFactory.getLogger(DiagnosticController.class);

    @GetMapping("/s3client-test")
    public ResponseEntity<Map<String, Object>> testS3ClientCreation() {
        Map<String, Object> result = new HashMap<>();

        try {
            logger.info("Starting S3Client creation test");

            // Read environment variables directly
            String rawAccessKey = System.getenv("MINIO_ROOT_USER");
            String rawSecretKey = System.getenv("MINIO_ROOT_PASSWORD");
            String endpoint = System.getenv("MINIO_ENDPOINT");
            String region = System.getenv("MINIO_REGION");

            result.put("env_vars_found", Map.of(
                "MINIO_ROOT_USER", rawAccessKey != null,
                "MINIO_ROOT_PASSWORD", rawSecretKey != null,
                "MINIO_ENDPOINT", endpoint != null,
                "MINIO_REGION", region != null
            ));

            // Set defaults if not found
            if (rawAccessKey == null) rawAccessKey = "minioadmin";
            if (rawSecretKey == null) rawSecretKey = "minioadmin";
            if (endpoint == null) endpoint = "http://localhost:9000";
            if (region == null) region = "us-east-1";

            logger.info("Using endpoint: {}", endpoint);
            logger.info("Using region: {}", region);

            // Try URL decoding
            String decodedAccessKey;
            String decodedSecretKey;

            try {
                decodedAccessKey = java.net.URLDecoder.decode(rawAccessKey, java.nio.charset.StandardCharsets.UTF_8);
                decodedSecretKey = java.net.URLDecoder.decode(rawSecretKey, java.nio.charset.StandardCharsets.UTF_8);
                result.put("url_decoding", "success");
            } catch (Exception e) {
                logger.warn("URL decoding failed: {}", e.getMessage());
                decodedAccessKey = rawAccessKey;
                decodedSecretKey = rawSecretKey;
                result.put("url_decoding_error", e.getMessage());
            }

            // Try to create credentials
            try {
                AwsBasicCredentials credentials = AwsBasicCredentials.create(decodedAccessKey, decodedSecretKey);
                result.put("credentials_creation", "success");
                logger.info("Credentials created successfully");
            } catch (Exception e) {
                logger.error("Failed to create credentials: {}", e.getMessage(), e);
                result.put("credentials_error", e.getMessage());
                result.put("credentials_stack_trace", getStackTraceString(e));
                return ResponseEntity.ok(result);
            }

            // Try to create S3Client
            try {
                AwsBasicCredentials credentials = AwsBasicCredentials.create(decodedAccessKey, decodedSecretKey);

                S3Client s3Client = S3Client.builder()
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .region(Region.of(region))
                    .endpointOverride(URI.create(endpoint))
                    .forcePathStyle(true)
                    .build();

                result.put("s3client_creation", "success");
                result.put("s3client_type", s3Client.getClass().getSimpleName());
                logger.info("S3Client created successfully");

                // Try a simple operation
                try {
                    s3Client.listBuckets();
                    result.put("list_buckets", "success");
                } catch (Exception e) {
                    logger.warn("ListBuckets failed: {}", e.getMessage());
                    result.put("list_buckets_error", e.getMessage());
                }

            } catch (Exception e) {
                logger.error("Failed to create S3Client: {}", e.getMessage(), e);
                result.put("s3client_error", e.getMessage());
                result.put("s3client_stack_trace", getStackTraceString(e));
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Diagnostic test failed: {}", e.getMessage(), e);
            result.put("diagnostic_error", e.getMessage());
            result.put("diagnostic_stack_trace", getStackTraceString(e));
            return ResponseEntity.ok(result);
        }
    }

    private String getStackTraceString(Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}