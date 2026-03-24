package com.workoutplanner.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(ObjectStorageConfig.class);

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

        logger.info("Configuring MinIO S3 Client (Direct Env Variables)");
        logger.info("Endpoint: {}", endpoint);
        logger.info("Raw Access Key: {}", (rawAccessKey != null ? rawAccessKey.substring(0, Math.min(3, rawAccessKey.length())) + "***" : "null"));
        logger.info("Region: {}", region);
        logger.debug("Reading environment variables directly to bypass Spring property issues");

        String decodedAccessKey;
        String decodedSecretKey;

        try {
            // Handle URL-encoded credentials (if they are URL-encoded)
            decodedAccessKey = java.net.URLDecoder.decode(rawAccessKey, java.nio.charset.StandardCharsets.UTF_8);
            decodedSecretKey = java.net.URLDecoder.decode(rawSecretKey, java.nio.charset.StandardCharsets.UTF_8);

            logger.debug("Decoded Access Key: {}", (decodedAccessKey != null ? decodedAccessKey.substring(0, Math.min(3, decodedAccessKey.length())) + "***" : "null"));

            // If the decoded version is the same as raw, they weren't URL-encoded
            if (decodedAccessKey.equals(rawAccessKey)) {
                logger.debug("Credentials were not URL-encoded, using as-is");
            } else {
                logger.info("Credentials were URL-encoded and have been decoded");
            }

        } catch (Exception e) {
            logger.warn("URL decoding failed, using raw credentials: {}", e.getMessage());
            decodedAccessKey = rawAccessKey;
            decodedSecretKey = rawSecretKey;
        }

        S3ClientBuilder clientBuilder = S3Client.builder();

        try {
            // Set credentials for MinIO using decoded values
            AwsBasicCredentials credentials = AwsBasicCredentials.create(decodedAccessKey, decodedSecretKey);
            clientBuilder.credentialsProvider(StaticCredentialsProvider.create(credentials));
            logger.debug("AWS credentials configured");

            // Set region
            clientBuilder.region(Region.of(region));
            logger.debug("Region configured: {}", region);

            // Set MinIO endpoint and force path-style access (required for MinIO)
            clientBuilder.endpointOverride(URI.create(endpoint));
            clientBuilder.forcePathStyle(true);
            logger.debug("Endpoint and path-style access configured");

            S3Client client = clientBuilder.build();
            logger.info("S3Client successfully created for MinIO");
            return client;

        } catch (Exception e) {
            logger.error("Failed to configure S3Client: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to configure MinIO S3Client", e);
        }
    }
}