package com.workoutplanner.controller;

import com.workoutplanner.service.ObjectStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/storage")
@CrossOrigin(origins = "*")
@ConditionalOnProperty(name = "storage.use-local", havingValue = "false", matchIfMissing = false)
public class ObjectStorageController {

    private final ObjectStorageService objectStorageService;

    @Autowired
    public ObjectStorageController(ObjectStorageService objectStorageService) {
        this.objectStorageService = objectStorageService;
    }

    /**
     * Upload a file to object storage
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") String userId,
            @RequestParam("category") String category,
            @RequestParam(value = "metadata", required = false) String metadata) {

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("File is empty"));
            }

            // Validate file size (max 10MB)
            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(createErrorResponse("File size exceeds 10MB limit"));
            }

            // Validate category
            if (!isValidCategory(category)) {
                return ResponseEntity.badRequest().body(createErrorResponse("Invalid category. Allowed: progress-photos, workout-videos, documents"));
            }

            // Upload file
            String storageKey = objectStorageService.uploadFile("files", file, userId, category, metadata);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("storageKey", storageKey);
            response.put("fileName", file.getOriginalFilename());
            response.put("fileSize", file.getSize());
            response.put("contentType", file.getContentType());
            response.put("category", category);
            response.put("userId", userId);
            response.put("uploadedAt", java.time.LocalDateTime.now().toString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Upload failed: " + e.getMessage()));
        }
    }

    /**
     * Download a file from object storage
     */
    @GetMapping("/download/{storageKey}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String storageKey) {
        try {
            Optional<byte[]> fileData = objectStorageService.downloadFile("files", storageKey);

            if (fileData.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Get file metadata to determine content type
            Map<String, String> metadata = objectStorageService.getObjectMetadata("files", storageKey);
            String contentType = metadata.getOrDefault("content-type", "application/octet-stream");

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(fileData.get());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get file information
     */
    @GetMapping("/info/{storageKey}")
    public ResponseEntity<Map<String, Object>> getFileInfo(@PathVariable String storageKey) {
        try {
            if (!objectStorageService.objectExists("files", storageKey)) {
                return ResponseEntity.notFound().build();
            }

            Map<String, String> metadata = objectStorageService.getObjectMetadata("files", storageKey);

            Map<String, Object> response = new HashMap<>();
            response.put("storageKey", storageKey);
            response.put("exists", true);
            response.put("metadata", metadata);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to get file info: " + e.getMessage()));
        }
    }

    /**
     * List user's files by category
     */
    @GetMapping("/list/{userId}")
    public ResponseEntity<Map<String, Object>> listUserFiles(
            @PathVariable String userId,
            @RequestParam(required = false) String category) {

        try {
            String objectType = category != null ? category : "all";
            List<String> files;

            if ("all".equals(objectType)) {
                // List all files for user across all categories
                files = objectStorageService.listAllUserFiles("files", userId);
            } else {
                // List files in specific category
                files = objectStorageService.listUserObjects("files", userId, category);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("category", objectType);
            response.put("files", files);
            response.put("count", files.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to list files: " + e.getMessage()));
        }
    }

    /**
     * Delete a file
     */
    @DeleteMapping("/delete/{storageKey}")
    public ResponseEntity<Map<String, Object>> deleteFile(
            @PathVariable String storageKey,
            @RequestParam String userId) {

        try {
            // Verify the file belongs to the user (basic security check)
            if (!storageKey.contains(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Access denied"));
            }

            if (!objectStorageService.objectExists("files", storageKey)) {
                return ResponseEntity.notFound().build();
            }

            objectStorageService.deleteObject("files", storageKey);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "File deleted successfully");
            response.put("deletedKey", storageKey);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Delete failed: " + e.getMessage()));
        }
    }

    /**
     * Generate presigned URL for direct upload (for large files)
     */
    @PostMapping("/presigned-url")
    public ResponseEntity<Map<String, Object>> generatePresignedUrl(
            @RequestParam String userId,
            @RequestParam String category,
            @RequestParam String fileName,
            @RequestParam(defaultValue = "60") int expirationMinutes) {

        try {
            if (!isValidCategory(category)) {
                return ResponseEntity.badRequest().body(createErrorResponse("Invalid category"));
            }

            String key = generateFileKey(userId, category, fileName);
            String presignedUrl = objectStorageService.generatePresignedUrl("files", key, expirationMinutes);

            Map<String, Object> response = new HashMap<>();
            response.put("presignedUrl", presignedUrl);
            response.put("storageKey", key);
            response.put("expiresIn", expirationMinutes + " minutes");
            response.put("uploadMethod", "PUT");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to generate presigned URL: " + e.getMessage()));
        }
    }

    /**
     * Get storage statistics for a user
     */
    @GetMapping("/stats/{userId}")
    public ResponseEntity<Map<String, Object>> getUserStorageStats(@PathVariable String userId) {
        try {
            Map<String, Object> stats = objectStorageService.getUserStorageStats("files", userId);
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Failed to get storage stats: " + e.getMessage()));
        }
    }

    /**
     * Upload a dummy test file to object storage
     */
    @PostMapping("/test-upload/{userId}")
    public ResponseEntity<Map<String, Object>> uploadDummyFile(@PathVariable String userId) {
        try {
            // Create dummy file content
            String dummyContent = "This is a test file for MinIO object storage validation.\n" +
                    "Timestamp: " + java.time.LocalDateTime.now() + "\n" +
                    "User ID: " + userId + "\n" +
                    "Test successful!";

            byte[] fileContent = dummyContent.getBytes();
            String fileName = "test-file-" + System.currentTimeMillis() + ".txt";

            // Create a mock MultipartFile for testing
            MultipartFile mockFile = new org.springframework.mock.web.MockMultipartFile(
                    "file",
                    fileName,
                    "text/plain",
                    fileContent
            );

            // Upload to MinIO using existing method
            String storageKey = objectStorageService.uploadFile("files", mockFile, userId, "documents", "test upload");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Dummy file uploaded successfully to MinIO");
            response.put("storageKey", storageKey);
            response.put("fileName", fileName);
            response.put("fileSize", fileContent.length);
            response.put("contentType", "text/plain");
            response.put("userId", userId);
            response.put("uploadedAt", java.time.LocalDateTime.now().toString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Dummy file upload failed: " + e.getMessage()));
        }
    }

    // Helper methods

    private boolean isValidCategory(String category) {
        return List.of("progress-photos", "workout-videos", "documents", "diet-plans", "workout-plans", "workout-sessions")
                .contains(category);
    }

    private String generateFileKey(String userId, String category, String fileName) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String sanitizedFileName = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return String.format("%s/%s/%s_%s", category, userId, timestamp, sanitizedFileName);
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        error.put("timestamp", java.time.LocalDateTime.now().toString());
        return error;
    }
}