package com.open.unicorn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/uws-s3")
public class UWSS3Controller {

    @Autowired
    private BucketRepository bucketRepository;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    private final Path uploadPath = Paths.get("uploads");

    public UWSS3Controller() {
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory!", e);
        }
    }

    // Create bucket
    @PostMapping("/buckets")
    public ResponseEntity<Map<String, Object>> createBucket(@RequestBody Map<String, String> request, Authentication authentication) {
        try {
            String bucketName = request.get("name");
            String userEmail = authentication.getName();

            if (bucketName == null || bucketName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Bucket name is required"));
            }

            if (bucketRepository.existsByName(bucketName)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Bucket already exists"));
            }

            Bucket bucket = new Bucket(bucketName, userEmail);
            bucketRepository.save(bucket);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Bucket created successfully");
            response.put("bucket", Map.of(
                "id", bucket.getId(),
                "name", bucket.getName(),
                "owner", bucket.getOwnerEmail(),
                "createdAt", bucket.getCreatedAt()
            ));

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
            List<Bucket> buckets = bucketRepository.findByOwnerEmail(userEmail);

            List<Map<String, Object>> bucketList = buckets.stream()
                .map(bucket -> {
                    Map<String, Object> bucketMap = new HashMap<>();
                    bucketMap.put("id", bucket.getId());
                    bucketMap.put("name", bucket.getName());
                    bucketMap.put("owner", bucket.getOwnerEmail());
                    bucketMap.put("createdAt", bucket.getCreatedAt());
                    bucketMap.put("updatedAt", bucket.getUpdatedAt());
                    return bucketMap;
                })
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("buckets", bucketList);
            response.put("count", bucketList.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to list buckets: " + e.getMessage()));
        }
    }

    // Upload file
    @PostMapping("/buckets/{bucketName}/files")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @PathVariable String bucketName,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();

            // Check if bucket exists and user owns it
            Bucket bucket = bucketRepository.findByName(bucketName);
            if (bucket == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Bucket not found"));
            }

            if (!bucket.getOwnerEmail().equals(userEmail)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied"));
            }

            // Generate unique filename
            String originalFileName = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString() + fileExtension;

            // Create bucket directory if it doesn't exist
            Path bucketPath = uploadPath.resolve(bucketName);
            Files.createDirectories(bucketPath);

            // Save file
            Path filePath = bucketPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);

            // Save metadata
            FileMetadata metadata = new FileMetadata(
                fileName, originalFileName, bucketName, userEmail,
                filePath.toString(), file.getSize(), file.getContentType()
            );
            fileMetadataRepository.save(metadata);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "File uploaded successfully");
            response.put("file", Map.of(
                "id", metadata.getId(),
                "fileName", metadata.getFileName(),
                "originalFileName", metadata.getOriginalFileName(),
                "bucketName", metadata.getBucketName(),
                "fileSize", metadata.getFileSize(),
                "contentType", metadata.getContentType(),
                "uploadedAt", metadata.getUploadedAt()
            ));

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to upload file: " + e.getMessage()));
        }
    }

    // List files in bucket
    @GetMapping("/buckets/{bucketName}/files")
    public ResponseEntity<Map<String, Object>> listFiles(
            @PathVariable String bucketName,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();

            // Check if bucket exists and user owns it
            Bucket bucket = bucketRepository.findByName(bucketName);
            if (bucket == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Bucket not found"));
            }

            if (!bucket.getOwnerEmail().equals(userEmail)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied"));
            }

            List<FileMetadata> files = fileMetadataRepository.findByBucketNameAndOwnerEmail(bucketName, userEmail);

            List<Map<String, Object>> fileList = files.stream()
                .map(file -> {
                    Map<String, Object> fileMap = new HashMap<>();
                    fileMap.put("id", file.getId());
                    fileMap.put("fileName", file.getFileName());
                    fileMap.put("originalFileName", file.getOriginalFileName());
                    fileMap.put("fileSize", file.getFileSize());
                    fileMap.put("contentType", file.getContentType());
                    fileMap.put("uploadedAt", file.getUploadedAt());
                    fileMap.put("downloadUrl", "/api/uws-s3/buckets/" + bucketName + "/files/" + file.getFileName() + "/download");
                    return fileMap;
                })
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("files", fileList);
            response.put("count", fileList.size());
            response.put("bucketName", bucketName);

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

            // Check if file exists and user owns it
            FileMetadata metadata = fileMetadataRepository.findByFileName(fileName);
            if (metadata == null || !metadata.getBucketName().equals(bucketName)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            if (!metadata.getOwnerEmail().equals(userEmail)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Path filePath = Paths.get(metadata.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + metadata.getOriginalFileName() + "\"")
                    .contentType(MediaType.parseMediaType(metadata.getContentType()))
                    .body(resource);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (MalformedURLException e) {
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

            // Check if file exists and user owns it
            FileMetadata metadata = fileMetadataRepository.findByFileName(fileName);
            if (metadata == null || !metadata.getBucketName().equals(bucketName)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "File not found"));
            }

            if (!metadata.getOwnerEmail().equals(userEmail)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied"));
            }

            // Delete physical file
            Path filePath = Paths.get(metadata.getFilePath());
            Files.deleteIfExists(filePath);

            // Delete metadata
            fileMetadataRepository.delete(metadata);

            return ResponseEntity.ok(Map.of("message", "File deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete file: " + e.getMessage()));
        }
    }

    // Delete bucket
    @DeleteMapping("/buckets/{bucketName}")
    public ResponseEntity<Map<String, Object>> deleteBucket(
            @PathVariable String bucketName,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();

            // Check if bucket exists and user owns it
            Bucket bucket = bucketRepository.findByName(bucketName);
            if (bucket == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Bucket not found"));
            }

            if (!bucket.getOwnerEmail().equals(userEmail)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied"));
            }

            // Delete all files in bucket
            List<FileMetadata> files = fileMetadataRepository.findByBucketNameAndOwnerEmail(bucketName, userEmail);
            for (FileMetadata file : files) {
                Path filePath = Paths.get(file.getFilePath());
                Files.deleteIfExists(filePath);
                fileMetadataRepository.delete(file);
            }

            // Delete bucket directory
            Path bucketPath = uploadPath.resolve(bucketName);
            Files.deleteIfExists(bucketPath);

            // Delete bucket
            bucketRepository.delete(bucket);

            return ResponseEntity.ok(Map.of("message", "Bucket and all files deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete bucket: " + e.getMessage()));
        }
    }
} 