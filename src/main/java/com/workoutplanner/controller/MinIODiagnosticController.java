package com.workoutplanner.controller;

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
@RequestMapping("/api/v1/minio-diagnostic")
public class MinIODiagnosticController {

    @GetMapping("/direct-test")
    public ResponseEntity<Map<String, Object>> testMinIODirectly() {
        Map<String, Object> result = new HashMap<>();

        try {
            // Read environment variables directly (same as ObjectStorageConfig)
            String rawAccessKey = System.getenv("MINIO_ROOT_USER");
            String rawSecretKey = System.getenv("MINIO_ROOT_PASSWORD");
            String endpoint = System.getenv("MINIO_ENDPOINT");
            String region = System.getenv("MINIO_REGION");

            result.put("env_check", Map.of(
                "MINIO_ROOT_USER", rawAccessKey != null ? "SET" : "NOT_SET",
                "MINIO_ROOT_PASSWORD", rawSecretKey != null ? "SET" : "NOT_SET",
                "MINIO_ENDPOINT", endpoint != null ? endpoint : "NOT_SET",
                "MINIO_REGION", region != null ? region : "NOT_SET"
            ));

            // Set defaults if not found
            if (rawAccessKey == null) rawAccessKey = "minioadmin";
            if (rawSecretKey == null) rawSecretKey = "minioadmin";
            if (endpoint == null) endpoint = "http://localhost:9000";
            if (region == null) region = "us-east-1";

            // Try URL decoding (same logic as ObjectStorageConfig)
            String decodedAccessKey;
            String decodedSecretKey;

            try {
                decodedAccessKey = java.net.URLDecoder.decode(rawAccessKey, java.nio.charset.StandardCharsets.UTF_8);
                decodedSecretKey = java.net.URLDecoder.decode(rawSecretKey, java.nio.charset.StandardCharsets.UTF_8);
                result.put("url_decoding", "success");

                // Show first 3 characters of decoded credentials
                result.put("decoded_access_key_preview", decodedAccessKey.substring(0, Math.min(3, decodedAccessKey.length())) + "***");

            } catch (Exception e) {
                decodedAccessKey = rawAccessKey;
                decodedSecretKey = rawSecretKey;
                result.put("url_decoding_error", e.getMessage());
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

                result.put("s3client_creation", "SUCCESS");
                result.put("s3client_class", s3Client.getClass().getSimpleName());

                // Try a simple operation
                try {
                    s3Client.listBuckets();
                    result.put("list_buckets_test", "SUCCESS");
                } catch (Exception e) {
                    result.put("list_buckets_error", e.getMessage());
                    result.put("list_buckets_error_class", e.getClass().getSimpleName());
                }

            } catch (Exception e) {
                result.put("s3client_creation_error", e.getMessage());
                result.put("s3client_error_class", e.getClass().getSimpleName());

                // Get the stack trace
                java.io.StringWriter sw = new java.io.StringWriter();
                java.io.PrintWriter pw = new java.io.PrintWriter(sw);
                e.printStackTrace(pw);
                result.put("s3client_stack_trace", sw.toString());
            }

        } catch (Exception e) {
            result.put("general_error", e.getMessage());
            result.put("general_error_class", e.getClass().getSimpleName());
        }

        return ResponseEntity.ok(result);
    }
}