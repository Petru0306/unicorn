package com.open.unicorn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/uws-s3")
public class UWSS3Controller {

    @Autowired
    private S3Service s3Service;

    // Create bucket
    @PostMapping("/buckets")
    public ResponseEntity<Map<String, Object>> createBucket(@RequestBody Map<String, String> request, Authentication authentication) {
        try {
            String bucketName = request.get("name");
            String userEmail = authentication.getName();

            if (bucketName == null || bucketName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Bucket name is required"));
            }

            Map<String, Object> result = s3Service.createBucket(bucketName, userEmail);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to create bucket: " + e.getMessage()));
        }
    }

    // List buckets
    @GetMapping("/buckets")
    public ResponseEntity<Map<String, Object>> listBuckets(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            List<Map<String, Object>> buckets = s3Service.listBuckets(userEmail);

            Map<String, Object> response = new HashMap<>();
            response.put("buckets", buckets);
            response.put("count", buckets.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to list buckets: " + e.getMessage()));
        }
    }

    // Delete bucket
    @DeleteMapping("/buckets/{bucketName}")
    public ResponseEntity<Map<String, Object>> deleteBucket(@PathVariable String bucketName, Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            Map<String, Object> result = s3Service.deleteBucket(bucketName, userEmail);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete bucket: " + e.getMessage()));
        }
    }

    // Upload file
    @PostMapping("/buckets/{bucketName}/files")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @PathVariable String bucketName,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "customName", required = false) String customName,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            Map<String, Object> result = s3Service.uploadFile(bucketName, file, userEmail, customName);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to upload file: " + e.getMessage()));
        }
    }

    // Multi-file upload
    @PostMapping("/buckets/{bucketName}/files/multiple")
    public ResponseEntity<Map<String, Object>> uploadMultipleFiles(
            @PathVariable String bucketName,
            @RequestParam("files") MultipartFile[] files,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            List<Map<String, Object>> uploadedFiles = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            for (MultipartFile file : files) {
                try {
                    Map<String, Object> result = s3Service.uploadFile(bucketName, file, userEmail);
                    Object fileObj = result.get("file");
                    if (fileObj instanceof Map<?, ?>) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> fileMap = (Map<String, Object>) fileObj;
                        uploadedFiles.add(fileMap);
                    }
                } catch (Exception e) {
                    errors.add(file.getOriginalFilename() + ": " + e.getMessage());
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("uploadedFiles", uploadedFiles);
            response.put("successCount", uploadedFiles.size());
            response.put("errorCount", errors.size());
            response.put("errors", errors);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to upload files: " + e.getMessage()));
        }
    }

    // List files in bucket
    @GetMapping("/buckets/{bucketName}/files")
    public ResponseEntity<Map<String, Object>> listFiles(
            @PathVariable String bucketName,
            @RequestParam(value = "search", required = false) String searchTerm,
            @RequestParam(value = "type", required = false) String fileType,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            List<Map<String, Object>> files = s3Service.listFiles(bucketName, userEmail, searchTerm, fileType);

            Map<String, Object> response = new HashMap<>();
            response.put("files", files);
            response.put("count", files.size());
            response.put("bucketName", bucketName);
            response.put("searchTerm", searchTerm);
            response.put("fileType", fileType);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to list files: " + e.getMessage()));
        }
    }

    // Download file
    @GetMapping("/buckets/{bucketName}/files/{fileName}/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String bucketName,
            @PathVariable String fileName,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            Resource resource = s3Service.downloadFile(bucketName, fileName, userEmail);

            // Get file metadata for headers
            FileMetadata metadata = s3Service.getFileMetadata(fileName);
            if (metadata == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            String encodedFileName = URLEncoder.encode(metadata.getOriginalFileName(), StandardCharsets.UTF_8.toString());

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"" + encodedFileName + "\"")
                .contentType(MediaType.parseMediaType(metadata.getContentType()))
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // File preview
    @GetMapping("/buckets/{bucketName}/files/{fileName}/preview")
    public ResponseEntity<Resource> previewFile(
            @PathVariable String bucketName,
            @PathVariable String fileName,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            Resource resource = s3Service.downloadFile(bucketName, fileName, userEmail);

            // Get file metadata for headers
            FileMetadata metadata = s3Service.getFileMetadata(fileName);
            if (metadata == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            // Set appropriate content type for preview
            MediaType mediaType = MediaType.parseMediaType(metadata.getContentType());
            
            // For images, PDFs, and text files, set inline disposition
            String disposition = "inline";
            if (metadata.getContentType().startsWith("image/") || 
                metadata.getContentType().equals("application/pdf") ||
                metadata.getContentType().startsWith("text/")) {
                disposition = "inline";
            } else {
                disposition = "attachment";
            }

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(mediaType)
                .body(resource);
        } catch (Exception e) {
            System.err.println("Preview error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Delete file
    @DeleteMapping("/buckets/{bucketName}/files/{fileName}")
    public ResponseEntity<Map<String, Object>> deleteFile(
            @PathVariable String bucketName,
            @PathVariable String fileName,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            Map<String, Object> result = s3Service.deleteFile(bucketName, fileName, userEmail);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete file: " + e.getMessage()));
        }
    }

    // Get file versions
    @GetMapping("/buckets/{bucketName}/files/{originalFileName}/versions")
    public ResponseEntity<Map<String, Object>> getFileVersions(
            @PathVariable String bucketName,
            @PathVariable String originalFileName,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            List<Map<String, Object>> versions = s3Service.getFileVersions(bucketName, originalFileName, userEmail);

            Map<String, Object> response = new HashMap<>();
            response.put("versions", versions);
            response.put("count", versions.size());
            response.put("originalFileName", originalFileName);
            response.put("bucketName", bucketName);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get file versions: " + e.getMessage()));
        }
    }

    // Get user storage info
    @GetMapping("/storage/info")
    public ResponseEntity<Map<String, Object>> getUserStorageInfo(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            Map<String, Object> storageInfo = s3Service.getUserStorageInfo(userEmail);
            return ResponseEntity.ok(storageInfo);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get storage info: " + e.getMessage()));
        }
    }

    // Get user activity log
    @GetMapping("/activity/log")
    public ResponseEntity<Map<String, Object>> getUserActivityLog(
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            List<Map<String, Object>> activities = s3Service.getUserActivityLog(userEmail, limit);

            Map<String, Object> response = new HashMap<>();
            response.put("activities", activities);
            response.put("count", activities.size());
            response.put("limit", limit);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get activity log: " + e.getMessage()));
        }
    }

    // Search files across all buckets
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchFiles(
            @RequestParam("query") String query,
            @RequestParam(value = "type", required = false) String fileType,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            List<Map<String, Object>> searchResults = s3Service.searchFiles(userEmail, query, fileType);

            Map<String, Object> response = new HashMap<>();
            response.put("results", searchResults);
            response.put("count", searchResults.size());
            response.put("query", query);
            response.put("fileType", fileType);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to search files: " + e.getMessage()));
        }
    }

    // Get bucket statistics
    @GetMapping("/buckets/{bucketName}/stats")
    public ResponseEntity<Map<String, Object>> getBucketStats(
            @PathVariable String bucketName,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            Map<String, Object> stats = s3Service.getBucketStats(bucketName, userEmail);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get bucket stats: " + e.getMessage()));
        }
    }
} 