package com.workoutplanner.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.core.sync.RequestBody;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/minio-debug")
public class MinIODebugController {

    /**
     * Simple health check for this controller
     */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "MinIODebugController is working");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        response.put("message", "Controller successfully loaded and responding");
        return ResponseEntity.ok(response);
    }

    /**
     * Test MinIO upload without any Spring dependencies
     */
    @PostMapping("/upload-dummy")
    public ResponseEntity<Map<String, Object>> uploadDummyFile() {
        Map<String, Object> result = new HashMap<>();

        try {
            System.out.println("🧪 MinIODebugController: Starting direct MinIO upload test...");

            // Read environment variables directly
            String endpoint = System.getenv("MINIO_ENDPOINT");
            String rawAccessKey = System.getenv("MINIO_ROOT_USER");
            String rawSecretKey = System.getenv("MINIO_ROOT_PASSWORD");
            String region = System.getenv("MINIO_REGION");

            System.out.println("🔧 MinIODebugController Environment Check:");
            System.out.println("   ENDPOINT: " + (endpoint != null ? endpoint : "NULL"));
            System.out.println("   ACCESS_KEY: " + (rawAccessKey != null ? "[PRESENT]" : "NULL"));
            System.out.println("   SECRET_KEY: " + (rawSecretKey != null ? "[PRESENT]" : "NULL"));
            System.out.println("   REGION: " + (region != null ? region : "NULL"));

            result.put("environmentVariables", Map.of(
                "endpoint", endpoint != null,
                "accessKey", rawAccessKey != null,
                "secretKey", rawSecretKey != null,
                "region", region != null
            ));

            if (endpoint == null || rawAccessKey == null || rawSecretKey == null) {
                result.put("error", "Missing MinIO environment variables");
                result.put("success", false);
                return ResponseEntity.status(500).body(result);
            }

            if (region == null) region = "us-east-1";

            // URL decode credentials
            String accessKey, secretKey;
            try {
                accessKey = java.net.URLDecoder.decode(rawAccessKey, "UTF-8");
                secretKey = java.net.URLDecoder.decode(rawSecretKey, "UTF-8");
                result.put("credentialDecoding", "success");
                System.out.println("✅ MinIODebugController: Credentials decoded successfully");
            } catch (Exception decodeError) {
                accessKey = rawAccessKey;
                secretKey = rawSecretKey;
                result.put("credentialDecoding", "failed - using raw");
                System.out.println("⚠️ MinIODebugController: Using raw credentials");
            }

            // Create S3Client manually
            try {
                AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

                S3Client s3Client = S3Client.builder()
                    .endpointOverride(URI.create(endpoint))
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .region(Region.of(region))
                    .forcePathStyle(true)
                    .build();

                result.put("s3ClientCreation", "success");
                System.out.println("✅ MinIODebugController: S3Client created successfully");

                // Test connectivity
                try {
                    ListBucketsResponse bucketsResponse = s3Client.listBuckets();
                    result.put("connectivity", "success");
                    result.put("bucketCount", bucketsResponse.buckets().size());
                    System.out.println("✅ MinIODebugController: MinIO connectivity confirmed - " + bucketsResponse.buckets().size() + " buckets found");
                } catch (Exception connectError) {
                    result.put("connectivity", "failed");
                    result.put("connectivityError", connectError.getMessage());
                    System.err.println("❌ MinIODebugController: Connectivity failed - " + connectError.getMessage());
                }

                // Test file upload
                String bucketName = "minio-debug-test";
                String testKey = "test-uploads/debug-test-" + System.currentTimeMillis() + ".txt";
                String testContent = "MinIO Debug Controller Test File\n" +
                                   "Timestamp: " + java.time.LocalDateTime.now() + "\n" +
                                   "Test successful!";

                try {
                    // Ensure bucket exists
                    try {
                        HeadBucketRequest headRequest = HeadBucketRequest.builder()
                                .bucket(bucketName)
                                .build();
                        s3Client.headBucket(headRequest);
                        result.put("bucketExists", true);
                        System.out.println("✅ MinIODebugController: Bucket exists - " + bucketName);
                    } catch (NoSuchBucketException e) {
                        CreateBucketRequest createRequest = CreateBucketRequest.builder()
                                .bucket(bucketName)
                                .build();
                        s3Client.createBucket(createRequest);
                        result.put("bucketCreated", true);
                        System.out.println("✅ MinIODebugController: Bucket created - " + bucketName);
                    }

                    // Upload test file
                    byte[] contentBytes = testContent.getBytes("UTF-8");
                    PutObjectRequest putRequest = PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(testKey)
                            .contentType("text/plain")
                            .contentLength((long) contentBytes.length)
                            .build();

                    s3Client.putObject(putRequest, RequestBody.fromBytes(contentBytes));

                    result.put("uploadSuccess", true);
                    result.put("uploadKey", testKey);
                    result.put("uploadSize", contentBytes.length);
                    System.out.println("✅ MinIODebugController: File uploaded successfully - " + testKey);

                    // Verify upload
                    GetObjectRequest getRequest = GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(testKey)
                            .build();
                    byte[] downloadedData = s3Client.getObject(getRequest).readAllBytes();
                    String downloadedContent = new String(downloadedData, "UTF-8");

                    result.put("verificationSuccess", testContent.equals(downloadedContent));
                    System.out.println("✅ MinIODebugController: Upload verified successfully");

                } catch (Exception uploadError) {
                    result.put("uploadError", uploadError.getMessage());
                    result.put("uploadSuccess", false);
                    System.err.println("❌ MinIODebugController: Upload failed - " + uploadError.getMessage());
                    uploadError.printStackTrace();
                }

            } catch (Exception s3Error) {
                result.put("s3ClientCreation", "failed");
                result.put("s3Error", s3Error.getMessage());
                System.err.println("❌ MinIODebugController: S3Client creation failed - " + s3Error.getMessage());
                s3Error.printStackTrace();
            }

            result.put("success", true);
            result.put("message", "MinIO debug test completed");
            result.put("timestamp", java.time.LocalDateTime.now().toString());

            System.out.println("🎉 MinIODebugController: Test completed");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("success", false);
            result.put("criticalError", e.getMessage());
            result.put("errorType", e.getClass().getSimpleName());
            System.err.println("❌ MinIODebugController: Critical error - " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(result);
        }
    }
}