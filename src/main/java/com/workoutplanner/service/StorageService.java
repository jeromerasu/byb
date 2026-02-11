package com.workoutplanner.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class StorageService {

    private final ObjectStorageService objectStorageService;
    private final LocalFileStorageService localFileStorageService;
    private final boolean useLocalStorage;

    @Autowired
    public StorageService(
            ObjectStorageService objectStorageService,
            LocalFileStorageService localFileStorageService,
            @Value("${storage.use-local:false}") boolean useLocalStorage) {
        this.objectStorageService = objectStorageService;
        this.localFileStorageService = localFileStorageService;
        this.useLocalStorage = useLocalStorage;

        System.out.println("🗄️  Storage mode: " + (useLocalStorage ? "LOCAL FILE STORAGE" : "OBJECT STORAGE (MinIO/S3)"));
    }

    public String storeWorkoutPlan(String userId, String planTitle, Object workoutPlan) {
        if (useLocalStorage) {
            return localFileStorageService.storeWorkoutPlan(userId, planTitle, workoutPlan);
        } else {
            return objectStorageService.storeWorkoutPlan(userId, planTitle, workoutPlan);
        }
    }

    public String storeDietPlan(String userId, String planTitle, Object dietPlan) {
        if (useLocalStorage) {
            return localFileStorageService.storeDietPlan(userId, planTitle, dietPlan);
        } else {
            return objectStorageService.storeDietPlan(userId, planTitle, dietPlan);
        }
    }

    public Map<String, Object> retrieveWorkoutPlan(String userId, String storageKey) {
        if (useLocalStorage) {
            return localFileStorageService.retrieveWorkoutPlan(userId, storageKey);
        } else {
            return objectStorageService.retrieveWorkoutPlan(userId, storageKey);
        }
    }

    public Map<String, Object> retrieveDietPlan(String userId, String storageKey) {
        if (useLocalStorage) {
            return localFileStorageService.retrieveDietPlan(userId, storageKey);
        } else {
            return objectStorageService.retrieveDietPlan(userId, storageKey);
        }
    }

    public boolean isUsingLocalStorage() {
        return useLocalStorage;
    }
}