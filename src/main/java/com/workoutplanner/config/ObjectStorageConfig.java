package com.workoutplanner.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.net.URI;

@Configuration
public class ObjectStorageConfig {

    @Bean
    public S3Client s3Client() {
        // Read environment variables directly to avoid Spring property resolution issues
        String rawAccessKey = System.getenv("MINIO_ROOT_USER");
        String rawSecretKey = System.getenv("MINIO_ROOT_PASSWORD");
        String endpoint = System.getenv("MINIO_ENDPOINT");
        String region = System.getenv("MINIO_REGION");

        // Set defaults if environment variables are not found
        if (rawAccessKey == null) rawAccessKey = "minioadmin";
        if (rawSecretKey == null) rawSecretKey = "minioadmin";
        if (endpoint == null) endpoint = "http://localhost:9000";
        if (region == null) region = "us-east-1";

        System.out.println("🔧 Configuring MinIO S3 Client (Direct Env Variables):");
        System.out.println("   Endpoint: " + endpoint);
        System.out.println("   Raw Access Key: " + (rawAccessKey != null ? rawAccessKey.substring(0, Math.min(3, rawAccessKey.length())) + "***" : "null"));
        System.out.println("   Region: " + region);
        System.out.println("   📝 Reading environment variables directly to bypass Spring property issues...");

        String decodedAccessKey;
        String decodedSecretKey;

        try {
            // Handle URL-encoded credentials (if they are URL-encoded)
            decodedAccessKey = java.net.URLDecoder.decode(rawAccessKey, java.nio.charset.StandardCharsets.UTF_8);
            decodedSecretKey = java.net.URLDecoder.decode(rawSecretKey, java.nio.charset.StandardCharsets.UTF_8);

            System.out.println("   Decoded Access Key: " + (decodedAccessKey != null ? decodedAccessKey.substring(0, Math.min(3, decodedAccessKey.length())) + "***" : "null"));

            // If the decoded version is the same as raw, they weren't URL-encoded
            if (decodedAccessKey.equals(rawAccessKey)) {
                System.out.println("   ℹ️ Credentials were not URL-encoded, using as-is");
            } else {
                System.out.println("   ✅ Credentials were URL-encoded and have been decoded");
            }

        } catch (Exception e) {
            System.out.println("   ⚠️ URL decoding failed, using raw credentials: " + e.getMessage());
            decodedAccessKey = rawAccessKey;
            decodedSecretKey = rawSecretKey;
        }

        S3ClientBuilder clientBuilder = S3Client.builder();

        try {
            // Set credentials for MinIO using decoded values
            AwsBasicCredentials credentials = AwsBasicCredentials.create(decodedAccessKey, decodedSecretKey);
            clientBuilder.credentialsProvider(StaticCredentialsProvider.create(credentials));
            System.out.println("   ✅ AWS credentials configured");

            // Set region
            clientBuilder.region(Region.of(region));
            System.out.println("   ✅ Region configured: " + region);

            // Set MinIO endpoint and force path-style access (required for MinIO)
            clientBuilder.endpointOverride(URI.create(endpoint));
            clientBuilder.forcePathStyle(true);
            System.out.println("   ✅ Endpoint and path-style access configured");

            S3Client client = clientBuilder.build();
            System.out.println("🚀 S3Client successfully created for MinIO");
            return client;

        } catch (Exception e) {
            System.out.println("❌ Failed to configure S3Client: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to configure MinIO S3Client", e);
        }
    }
}