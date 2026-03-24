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

    @Value("${minio.access-key:minioadmin}")
    private String accessKey;

    @Value("${minio.secret-key:minioadmin}")
    private String secretKey;

    @Value("${minio.region:us-east-1}")
    private String region;

    @Value("${minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Bean
    public S3Client s3Client() {
        System.out.println("🔧 Configuring MinIO S3 Client:");
        System.out.println("   Endpoint: " + endpoint);
        System.out.println("   Access Key: " + (accessKey != null ? accessKey.substring(0, Math.min(3, accessKey.length())) + "***" : "null"));
        System.out.println("   Region: " + region);

        // Handle URL-encoded credentials
        String decodedAccessKey = java.net.URLDecoder.decode(accessKey, java.nio.charset.StandardCharsets.UTF_8);
        String decodedSecretKey = java.net.URLDecoder.decode(secretKey, java.nio.charset.StandardCharsets.UTF_8);

        System.out.println("   Decoded Access Key: " + (decodedAccessKey != null ? decodedAccessKey.substring(0, Math.min(3, decodedAccessKey.length())) + "***" : "null"));

        S3ClientBuilder clientBuilder = S3Client.builder();

        // Set credentials for MinIO using decoded values
        AwsBasicCredentials credentials = AwsBasicCredentials.create(decodedAccessKey, decodedSecretKey);
        clientBuilder.credentialsProvider(StaticCredentialsProvider.create(credentials));

        // Set region
        clientBuilder.region(Region.of(region));

        // Set MinIO endpoint and force path-style access (required for MinIO)
        clientBuilder.endpointOverride(URI.create(endpoint));
        clientBuilder.forcePathStyle(true);

        System.out.println("✅ S3Client configured for MinIO");
        return clientBuilder.build();
    }
}