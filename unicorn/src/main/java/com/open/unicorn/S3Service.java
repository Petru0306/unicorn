package com.open.unicorn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class S3Service {

    @Autowired
    private BucketRepository bucketRepository;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private UserStorageQuotaRepository quotaRepository;

    private final Path uploadPath = Paths.get("uploads");

    public S3Service() {
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory!", e);
        }
    }

    // Bucket operations
    public Map<String, Object> createBucket(String bucketName, String userEmail) {
        if (bucketRepository.existsByName(bucketName)) {
            throw new RuntimeException("Bucket already exists");
        }

        Bucket bucket = new Bucket(bucketName, userEmail);
        bucketRepository.save(bucket);

        // Log activity
        ActivityLog activity = new ActivityLog(userEmail, "CREATE_BUCKET", bucketName, "BUCKET", 
            "Created new bucket: " + bucketName);
        activityLogRepository.save(activity);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Bucket created successfully");
        response.put("bucket", Map.of(
            "id", bucket.getId(),
            "name", bucket.getName(),
            "owner", bucket.getOwnerEmail(),
            "createdAt", bucket.getCreatedAt()
        ));
        return response;
    }

    public List<Map<String, Object>> listBuckets(String userEmail) {
        List<Bucket> buckets = bucketRepository.findByOwnerEmail(userEmail);
        
        return buckets.stream()
            .map(bucket -> {
                Map<String, Object> bucketMap = new HashMap<>();
                bucketMap.put("id", bucket.getId());
                bucketMap.put("name", bucket.getName());
                bucketMap.put("owner", bucket.getOwnerEmail());
                bucketMap.put("createdAt", bucket.getCreatedAt());
                bucketMap.put("updatedAt", bucket.getUpdatedAt());
                
                // Get bucket statistics
                Long fileCount = fileMetadataRepository.countFilesInBucket(bucket.getName(), userEmail);
                Long totalSize = fileMetadataRepository.getTotalStorageUsedInBucket(bucket.getName(), userEmail);
                bucketMap.put("fileCount", fileCount != null ? fileCount : 0);
                bucketMap.put("totalSize", totalSize != null ? totalSize : 0);
                
                return bucketMap;
            })
            .collect(Collectors.toList());
    }

    public Map<String, Object> deleteBucket(String bucketName, String userEmail) {
        Bucket bucket = bucketRepository.findByName(bucketName);
        if (bucket == null) {
            throw new RuntimeException("Bucket not found");
        }

        if (!bucket.getOwnerEmail().equals(userEmail)) {
            throw new RuntimeException("Access denied");
        }

        // Check if bucket is empty
        Long fileCount = fileMetadataRepository.countFilesInBucket(bucketName, userEmail);
        if (fileCount != null && fileCount > 0) {
            throw new RuntimeException("Cannot delete bucket: bucket is not empty");
        }

        bucketRepository.delete(bucket);

        // Log activity
        ActivityLog activity = new ActivityLog(userEmail, "DELETE_BUCKET", bucketName, "BUCKET", 
            "Deleted bucket: " + bucketName);
        activityLogRepository.save(activity);

        return Map.of("message", "Bucket deleted successfully");
    }

    // File operations with versioning
    public Map<String, Object> uploadFile(String bucketName, MultipartFile file, String userEmail) {
        return uploadFile(bucketName, file, userEmail, null);
    }

    public Map<String, Object> uploadFile(String bucketName, MultipartFile file, String userEmail, String customName) {
        // Check bucket ownership
        Bucket bucket = bucketRepository.findByName(bucketName);
        if (bucket == null) {
            throw new RuntimeException("Bucket not found");
        }
        if (!bucket.getOwnerEmail().equals(userEmail)) {
            throw new RuntimeException("Access denied");
        }

        // Check storage quota
        UserStorageQuota quota = getOrCreateQuota(userEmail);
        if (quota.isQuotaExceeded()) {
            throw new RuntimeException("Storage quota exceeded");
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            throw new RuntimeException("Invalid file name");
        }

        // Use custom name if provided, otherwise use original name
        String finalFileName = (customName != null && !customName.trim().isEmpty()) ? 
            customName.trim() : originalFileName;

        // Check if file already exists and get next version
        Integer nextVersion = 1;
        List<FileMetadata> existingVersions = fileMetadataRepository
            .findByOriginalFileNameAndBucketNameAndOwnerEmailOrderByVersionDesc(finalFileName, bucketName, userEmail);
        
        if (!existingVersions.isEmpty()) {
            nextVersion = existingVersions.get(0).getVersion() + 1;
            // Mark previous versions as not latest
            for (FileMetadata existing : existingVersions) {
                existing.setIsLatest(false);
                fileMetadataRepository.save(existing);
            }
        }

        // Generate unique filename
        String fileExtension = "";
        if (originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString() + fileExtension;

        // Create bucket directory
        Path bucketPath = uploadPath.resolve(bucketName);
        try {
            Files.createDirectories(bucketPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create bucket directory", e);
        }

        // Save file
        Path filePath = bucketPath.resolve(fileName);
        try {
            Files.copy(file.getInputStream(), filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save file", e);
        }

        // Generate checksum
        String checksum = generateChecksum(file);

        // Save metadata
        FileMetadata metadata = new FileMetadata(
            fileName, finalFileName, bucketName, userEmail,
            filePath.toString(), file.getSize(), file.getContentType(),
            nextVersion, true
        );
        metadata.setChecksum(checksum);
        fileMetadataRepository.save(metadata);

        // Update storage quota
        quota.addStorage(file.getSize());
        quotaRepository.save(quota);

        // Log activity
        ActivityLog activity = new ActivityLog(userEmail, "UPLOAD", finalFileName, "FILE", 
            bucketName, file.getSize(), file.getContentType(), 
            "Uploaded file version " + nextVersion);
        activityLogRepository.save(activity);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "File uploaded successfully");
        response.put("file", Map.of(
            "id", metadata.getId(),
            "fileName", metadata.getFileName(),
            "originalFileName", metadata.getOriginalFileName(),
            "bucketName", metadata.getBucketName(),
            "fileSize", metadata.getFileSize(),
            "contentType", metadata.getContentType(),
            "version", metadata.getVersion(),
            "uploadedAt", metadata.getUploadedAt(),
            "checksum", metadata.getChecksum()
        ));
        return response;
    }

    public List<Map<String, Object>> listFiles(String bucketName, String userEmail, String searchTerm, String fileType) {
        // Check bucket ownership
        Bucket bucket = bucketRepository.findByName(bucketName);
        if (bucket == null) {
            throw new RuntimeException("Bucket not found");
        }
        if (!bucket.getOwnerEmail().equals(userEmail)) {
            throw new RuntimeException("Access denied");
        }

        List<FileMetadata> files;
        
        // Apply combined filters
        if (searchTerm != null && !searchTerm.trim().isEmpty() && fileType != null && !fileType.trim().isEmpty()) {
            // Both search term and file type filter
            files = fileMetadataRepository.findByBucketNameAndOwnerEmailAndOriginalFileNameContainingIgnoreCase(
                bucketName, userEmail, searchTerm.trim());
            // Filter by content type in memory
            files = files.stream()
                .filter(file -> file.getContentType().toLowerCase().contains(fileType.trim().toLowerCase()))
                .collect(Collectors.toList());
        } else if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            // Only search term filter
            files = fileMetadataRepository.findByBucketNameAndOwnerEmailAndOriginalFileNameContainingIgnoreCase(
                bucketName, userEmail, searchTerm.trim());
        } else if (fileType != null && !fileType.trim().isEmpty()) {
            // Only file type filter
            files = fileMetadataRepository.findByBucketNameAndOwnerEmailAndContentTypeContaining(
                bucketName, userEmail, fileType.trim());
        } else {
            // No filters
            files = fileMetadataRepository.findByBucketNameAndOwnerEmail(bucketName, userEmail);
        }

        return files.stream()
            .map(file -> {
                Map<String, Object> fileMap = new HashMap<>();
                fileMap.put("id", file.getId());
                fileMap.put("fileName", file.getFileName());
                fileMap.put("originalFileName", file.getOriginalFileName());
                fileMap.put("bucketName", file.getBucketName()); // Add bucketName for frontend
                fileMap.put("fileSize", file.getFileSize());
                fileMap.put("contentType", file.getContentType());
                fileMap.put("version", file.getVersion());
                fileMap.put("isLatest", file.getIsLatest());
                fileMap.put("uploadedAt", file.getUploadedAt());
                fileMap.put("downloadUrl", "/api/uws-s3/buckets/" + bucketName + "/files/" + file.getFileName() + "/download");
                fileMap.put("previewUrl", "/api/uws-s3/buckets/" + bucketName + "/files/" + file.getFileName() + "/preview");
                fileMap.put("canPreview", isPreviewable(file.getContentType()));
                return fileMap;
            })
            .collect(Collectors.toList());
    }

    public Resource downloadFile(String bucketName, String fileName, String userEmail) {
        FileMetadata metadata = fileMetadataRepository.findByFileName(fileName);
        if (metadata == null) {
            throw new RuntimeException("File not found");
        }
        
        if (!metadata.getBucketName().equals(bucketName)) {
            throw new RuntimeException("File not found in specified bucket");
        }
        
        if (!metadata.getOwnerEmail().equals(userEmail)) {
            throw new RuntimeException("Access denied");
        }

        Path filePath = Paths.get(metadata.getFilePath());
        Resource resource;
        try {
            resource = new UrlResource(filePath.toUri());
        } catch (MalformedURLException e) {
            throw new RuntimeException("File not found", e);
        }

        if (!resource.exists() || !resource.isReadable()) {
            throw new RuntimeException("File not found or not readable");
        }

        // Log activity
        ActivityLog activity = new ActivityLog(userEmail, "DOWNLOAD", metadata.getOriginalFileName(), "FILE", 
            bucketName, metadata.getFileSize(), metadata.getContentType(), 
            "Downloaded file version " + metadata.getVersion());
        activityLogRepository.save(activity);

        return resource;
    }

    public Map<String, Object> deleteFile(String bucketName, String fileName, String userEmail) {
        FileMetadata metadata = fileMetadataRepository.findByFileName(fileName);
        if (metadata == null) {
            throw new RuntimeException("File not found");
        }
        
        // Check if file belongs to the specified bucket
        if (!metadata.getBucketName().equals(bucketName)) {
            throw new RuntimeException("File not found in specified bucket");
        }
        
        if (!metadata.getOwnerEmail().equals(userEmail)) {
            throw new RuntimeException("Access denied");
        }

        // Delete physical file
        Path filePath = Paths.get(metadata.getFilePath());
        try {
            if (!Files.exists(filePath)) {
                // File doesn't exist physically, but we can still delete the metadata
                System.out.println("Warning: Physical file not found, deleting metadata only: " + filePath);
            } else {
                Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete physical file: " + e.getMessage());
        }

        // Update storage quota
        UserStorageQuota quota = quotaRepository.findByUserEmail(userEmail).orElse(null);
        if (quota != null) {
            quota.removeStorage(metadata.getFileSize());
            quotaRepository.save(quota);
        }

        // Log activity
        ActivityLog activity = new ActivityLog(userEmail, "DELETE", metadata.getOriginalFileName(), "FILE", 
            bucketName, metadata.getFileSize(), metadata.getContentType(), 
            "Deleted file version " + metadata.getVersion());
        activityLogRepository.save(activity);

        // Delete metadata
        fileMetadataRepository.delete(metadata);

        return Map.of("message", "File deleted successfully");
    }

    // Storage quota operations
    public Map<String, Object> getUserStorageInfo(String userEmail) {
        UserStorageQuota quota = getOrCreateQuota(userEmail);
        
        Map<String, Object> info = new HashMap<>();
        info.put("usedStorage", quota.getUsedStorage());
        info.put("maxStorage", quota.getMaxStorage());
        info.put("remainingStorage", quota.getRemainingStorage());
        info.put("usagePercentage", quota.getUsagePercentage());
        info.put("isQuotaExceeded", quota.isQuotaExceeded());
        info.put("lastUpdated", quota.getLastUpdated());
        
        return info;
    }

    // Activity log operations
    public List<Map<String, Object>> getUserActivityLog(String userEmail, int limit) {
        List<ActivityLog> activities = activityLogRepository.findRecentActivitiesByUser(
            userEmail, org.springframework.data.domain.PageRequest.of(0, limit));
        
        return activities.stream()
            .map(activity -> {
                Map<String, Object> activityMap = new HashMap<>();
                activityMap.put("id", activity.getId());
                activityMap.put("action", activity.getAction());
                activityMap.put("resourceName", activity.getResourceName());
                activityMap.put("resourceType", activity.getResourceType());
                activityMap.put("bucketName", activity.getBucketName());
                activityMap.put("fileSize", activity.getFileSize());
                activityMap.put("contentType", activity.getContentType());
                activityMap.put("timestamp", activity.getTimestamp());
                activityMap.put("details", activity.getDetails());
                return activityMap;
            })
            .collect(Collectors.toList());
    }

    // File versioning operations
    public List<Map<String, Object>> getFileVersions(String bucketName, String originalFileName, String userEmail) {
        List<FileMetadata> versions = fileMetadataRepository
            .findByOriginalFileNameAndBucketNameAndOwnerEmailOrderByVersionDesc(originalFileName, bucketName, userEmail);
        
        return versions.stream()
            .map(file -> {
                Map<String, Object> versionMap = new HashMap<>();
                versionMap.put("id", file.getId());
                versionMap.put("fileName", file.getFileName());
                versionMap.put("version", file.getVersion());
                versionMap.put("isLatest", file.getIsLatest());
                versionMap.put("fileSize", file.getFileSize());
                versionMap.put("uploadedAt", file.getUploadedAt());
                versionMap.put("downloadUrl", "/api/uws-s3/buckets/" + bucketName + "/files/" + file.getFileName() + "/download");
                return versionMap;
            })
            .collect(Collectors.toList());
    }

    // Helper methods
    private UserStorageQuota getOrCreateQuota(String userEmail) {
        return quotaRepository.findByUserEmail(userEmail)
            .orElseGet(() -> {
                UserStorageQuota newQuota = new UserStorageQuota(userEmail);
                return quotaRepository.save(newQuota);
            });
    }

    // Get file metadata by fileName
    public FileMetadata getFileMetadata(String fileName) {
        return fileMetadataRepository.findByFileName(fileName);
    }

    // Search files across all buckets
    public List<Map<String, Object>> searchFiles(String userEmail, String query, String fileType) {
        List<FileMetadata> files;
        
        if (fileType != null && !fileType.trim().isEmpty()) {
            files = fileMetadataRepository.findByOwnerEmailAndContentTypeContaining(userEmail, fileType.trim());
        } else {
            files = fileMetadataRepository.findByOwnerEmailAndOriginalFileNameContainingIgnoreCase(userEmail, query.trim());
        }
        
        return files.stream()
            .map(file -> {
                Map<String, Object> fileMap = new HashMap<>();
                fileMap.put("id", file.getId());
                fileMap.put("fileName", file.getFileName());
                fileMap.put("originalFileName", file.getOriginalFileName());
                fileMap.put("fileSize", file.getFileSize());
                fileMap.put("contentType", file.getContentType());
                fileMap.put("version", file.getVersion());
                fileMap.put("isLatest", file.getIsLatest());
                fileMap.put("uploadedAt", file.getUploadedAt());
                fileMap.put("bucketName", file.getBucketName()); // Add bucketName for frontend
                fileMap.put("downloadUrl", "/api/uws-s3/buckets/" + file.getBucketName() + "/files/" + file.getFileName() + "/download");
                fileMap.put("previewUrl", "/api/uws-s3/buckets/" + file.getBucketName() + "/files/" + file.getFileName() + "/preview");
                fileMap.put("canPreview", isPreviewable(file.getContentType()));
                return fileMap;
            })
            .collect(Collectors.toList());
    }

    // Get bucket statistics
    public Map<String, Object> getBucketStats(String bucketName, String userEmail) {
        // Check bucket ownership
        Bucket bucket = bucketRepository.findByName(bucketName);
        if (bucket == null) {
            throw new RuntimeException("Bucket not found");
        }
        if (!bucket.getOwnerEmail().equals(userEmail)) {
            throw new RuntimeException("Access denied");
        }

        Long fileCount = fileMetadataRepository.countFilesInBucket(bucketName, userEmail);
        Long totalSize = fileMetadataRepository.getTotalStorageUsedInBucket(bucketName, userEmail);
        List<FileMetadata> latestFiles = fileMetadataRepository.findLatestFilesByUser(userEmail, 
            org.springframework.data.domain.PageRequest.of(0, 5));

        Map<String, Object> stats = new HashMap<>();
        stats.put("bucketName", bucketName);
        stats.put("fileCount", fileCount != null ? fileCount : 0);
        stats.put("totalSize", totalSize != null ? totalSize : 0);
        stats.put("createdAt", bucket.getCreatedAt());
        stats.put("latestFiles", latestFiles.stream()
            .filter(file -> file.getBucketName().equals(bucketName))
            .map(file -> {
                Map<String, Object> fileMap = new HashMap<>();
                fileMap.put("originalFileName", file.getOriginalFileName());
                fileMap.put("fileSize", file.getFileSize());
                fileMap.put("uploadedAt", file.getUploadedAt());
                return fileMap;
            })
            .collect(Collectors.toList()));

        return stats;
    }

    // Generate signed URL (simplified implementation)
    public String generateSignedUrl(String bucketName, String fileName, String userEmail, Integer expirationMinutes) {
        // In a real implementation, this would generate a proper signed URL
        // For now, we'll return a simple URL with a timestamp
        long expirationTime = System.currentTimeMillis() + (expirationMinutes * 60 * 1000);
        return "/api/uws-s3/buckets/" + bucketName + "/files/" + fileName + "/download?expires=" + expirationTime;
    }

    private String generateChecksum(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | IOException e) {
            return null;
        }
    }

    private boolean isPreviewable(String contentType) {
        if (contentType == null) return false;
        return contentType.startsWith("image/") || 
               contentType.equals("application/pdf") ||
               contentType.startsWith("text/");
    }
} 