package com.workoutplanner.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

@Service
public class StorageService {

    private static final Logger logger = LoggerFactory.getLogger(StorageService.class);

    private final Optional<ObjectStorageService> objectStorageService;
    private final Optional<LocalFileStorageService> localFileStorageService;
    private final boolean useLocalStorage;

    @Autowired
    public StorageService(
            Optional<ObjectStorageService> objectStorageService,
            Optional<LocalFileStorageService> localFileStorageService,
            @Value("${storage.use-local:false}") boolean useLocalStorage) {
        this.objectStorageService = objectStorageService;
        this.localFileStorageService = localFileStorageService;
        this.useLocalStorage = useLocalStorage;

        logger.info("Storage mode initialized: {}", useLocalStorage ? "LOCAL FILE STORAGE" : "OBJECT STORAGE (MinIO/S3)");
    }

    public String storeWorkoutPlan(String bucketName, String userId, String planTitle, Object workoutPlan) {
        if (useLocalStorage) {
            return localFileStorageService.orElseThrow(() -> new RuntimeException("Local storage requested but not available"))
                    .storeWorkoutPlan(userId, planTitle, workoutPlan);
        } else {
            return objectStorageService.orElseThrow(() -> new RuntimeException("Object storage requested but not available"))
                    .storeWorkoutPlan(bucketName, userId, planTitle, workoutPlan);
        }
    }

    public String storeDietPlan(String bucketName, String userId, String planTitle, Object dietPlan) {
        if (useLocalStorage) {
            return localFileStorageService.orElseThrow(() -> new RuntimeException("Local storage requested but not available"))
                    .storeDietPlan(userId, planTitle, dietPlan);
        } else {
            return objectStorageService.orElseThrow(() -> new RuntimeException("Object storage requested but not available"))
                    .storeDietPlan(bucketName, userId, planTitle, dietPlan);
        }
    }

    public Map<String, Object> retrieveWorkoutPlan(String bucketName, String userId, String storageKey) {
        if (useLocalStorage) {
            return localFileStorageService.orElseThrow(() -> new RuntimeException("Local storage requested but not available"))
                    .retrieveWorkoutPlan(userId, storageKey);
        } else {
            return objectStorageService.orElseThrow(() -> new RuntimeException("Object storage requested but not available"))
                    .retrieveWorkoutPlan(bucketName, userId, storageKey);
        }
    }

    public Map<String, Object> retrieveDietPlan(String bucketName, String userId, String storageKey) {
        if (useLocalStorage) {
            return localFileStorageService.orElseThrow(() -> new RuntimeException("Local storage requested but not available"))
                    .retrieveDietPlan(userId, storageKey);
        } else {
            return objectStorageService.orElseThrow(() -> new RuntimeException("Object storage requested but not available"))
                    .retrieveDietPlan(bucketName, userId, storageKey);
        }
    }

    public boolean isUsingLocalStorage() {
        return useLocalStorage;
    }
}