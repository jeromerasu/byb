package com.workoutplanner.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.core.sync.RequestBody;

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

    @PostMapping("/minio-upload-test")
    public ResponseEntity<Map<String, Object>> testMinIOUploadDirectly() {
        Map<String, Object> result = new HashMap<>();

        try {
            System.out.println("🧪 Starting direct MinIO upload test...");
            logger.info("Direct MinIO upload test started");

            // Create S3Client manually (same as diagnostic test)
            String endpoint = System.getenv("MINIO_ENDPOINT");
            String rawAccessKey = System.getenv("MINIO_ROOT_USER");
            String rawSecretKey = System.getenv("MINIO_ROOT_PASSWORD");
            String region = System.getenv("MINIO_REGION");

            System.out.println("🔧 Environment variables check:");
            System.out.println("   MINIO_ENDPOINT: " + (endpoint != null ? endpoint : "NULL"));
            System.out.println("   MINIO_ROOT_USER: " + (rawAccessKey != null ? rawAccessKey.substring(0, Math.min(10, rawAccessKey.length())) + "..." : "NULL"));
            System.out.println("   MINIO_ROOT_PASSWORD: " + (rawSecretKey != null ? "[" + rawSecretKey.length() + " characters]" : "NULL"));
            System.out.println("   MINIO_REGION: " + (region != null ? region : "NULL"));

            result.put("environment_check", Map.of(
                "endpoint_provided", endpoint != null,
                "access_key_provided", rawAccessKey != null,
                "secret_key_provided", rawSecretKey != null,
                "region_provided", region != null
            ));

            if (endpoint == null || rawAccessKey == null || rawSecretKey == null) {
                System.err.println("❌ Missing MinIO environment variables");
                result.put("error", "Missing MinIO environment variables");
                return ResponseEntity.status(500).body(result);
            }

            if (region == null) region = "us-east-1";

            // URL decode credentials
            String accessKey, secretKey;
            try {
                accessKey = java.net.URLDecoder.decode(rawAccessKey, "UTF-8");
                secretKey = java.net.URLDecoder.decode(rawSecretKey, "UTF-8");
            } catch (Exception decodeError) {
                // Keep original if decode fails
                accessKey = rawAccessKey;
                secretKey = rawSecretKey;
                logger.info("URL decode failed, using original credentials: {}", decodeError.getMessage());
            }

            // Create credentials and S3Client
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

            S3Client s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of(region))
                .serviceConfiguration(S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .build())
                .build();

            result.put("s3_client_created", true);

            // Test bucket creation
            String testBucket = "test-bucket-upload";
            try {
                HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(testBucket)
                    .build();
                s3Client.headBucket(headBucketRequest);
                result.put("bucket_exists", true);
            } catch (NoSuchBucketException e) {
                // Create bucket
                CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                    .bucket(testBucket)
                    .build();
                s3Client.createBucket(createBucketRequest);
                result.put("bucket_created", true);
            }

            // Test file upload
            String testKey = "test-uploads/test-file-" + System.currentTimeMillis() + ".txt";
            String testContent = "MinIO upload test successful!\nTimestamp: " + java.time.LocalDateTime.now() +
                                "\nUser ID: test-user\nTest successful!";
            byte[] contentBytes = testContent.getBytes("UTF-8");

            PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(testBucket)
                .key(testKey)
                .contentType("text/plain")
                .contentLength((long) contentBytes.length)
                .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(contentBytes));

            result.put("upload_success", true);
            result.put("uploaded_key", testKey);
            result.put("uploaded_size", contentBytes.length);
            result.put("bucket_name", testBucket);

            // Verify upload by downloading
            GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(testBucket)
                .key(testKey)
                .build();

            byte[] downloadedData = s3Client.getObject(getRequest).readAllBytes();
            String downloadedContent = new String(downloadedData, "UTF-8");

            result.put("download_success", true);
            result.put("downloaded_size", downloadedData.length);
            result.put("content_matches", testContent.equals(downloadedContent));

            result.put("message", "MinIO upload and download test completed successfully!");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("error_type", e.getClass().getSimpleName());
            result.put("message", "MinIO upload test failed");
            result.put("stack_trace", getStackTraceString(e));
            logger.error("MinIO upload test failed", e);
            return ResponseEntity.status(500).body(result);
        }
    }

    @GetMapping("/test-exception")
    public ResponseEntity<Map<String, Object>> testExceptionHandler() {
        // Deliberately throw an exception to test our GlobalExceptionHandler
        throw new RuntimeException("Test exception to verify GlobalExceptionHandler is working properly - this should show detailed error info");
    }

    private String getStackTraceString(Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}