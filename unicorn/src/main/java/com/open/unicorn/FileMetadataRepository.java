package com.open.unicorn;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {
    
    // Basic file operations
    List<FileMetadata> findByBucketName(String bucketName);
    List<FileMetadata> findByOwnerEmail(String ownerEmail);
    List<FileMetadata> findByBucketNameAndOwnerEmail(String bucketName, String ownerEmail);
    FileMetadata findByFileName(String fileName);
    
    // Versioning support
    List<FileMetadata> findByOriginalFileNameAndBucketNameAndOwnerEmailOrderByVersionDesc(
        String originalFileName, String bucketName, String ownerEmail);
    
    // Find latest version of a file
    Optional<FileMetadata> findByOriginalFileNameAndBucketNameAndOwnerEmailAndIsLatestTrue(
        String originalFileName, String bucketName, String ownerEmail);
    
    // Find all versions of a file
    List<FileMetadata> findByOriginalFileNameAndBucketNameAndOwnerEmail(
        String originalFileName, String bucketName, String ownerEmail);
    
    // Find files by content type
    List<FileMetadata> findByBucketNameAndOwnerEmailAndContentTypeContaining(
        String bucketName, String ownerEmail, String contentType);
    
    // Find files by name pattern (search functionality)
    List<FileMetadata> findByBucketNameAndOwnerEmailAndOriginalFileNameContainingIgnoreCase(
        String bucketName, String ownerEmail, String fileName);
    
    // Find files by size range
    @Query("SELECT f FROM FileMetadata f WHERE f.bucketName = :bucketName AND f.ownerEmail = :ownerEmail AND f.fileSize BETWEEN :minSize AND :maxSize")
    List<FileMetadata> findByBucketNameAndOwnerEmailAndFileSizeBetween(
        @Param("bucketName") String bucketName, 
        @Param("ownerEmail") String ownerEmail, 
        @Param("minSize") Long minSize, 
        @Param("maxSize") Long maxSize);
    
    // Get total storage used by user
    @Query("SELECT SUM(f.fileSize) FROM FileMetadata f WHERE f.ownerEmail = :ownerEmail")
    Long getTotalStorageUsedByUser(@Param("ownerEmail") String ownerEmail);
    
    // Get total storage used in a bucket
    @Query("SELECT SUM(f.fileSize) FROM FileMetadata f WHERE f.bucketName = :bucketName AND f.ownerEmail = :ownerEmail")
    Long getTotalStorageUsedInBucket(@Param("bucketName") String bucketName, @Param("ownerEmail") String ownerEmail);
    
    // Count files by user
    @Query("SELECT COUNT(f) FROM FileMetadata f WHERE f.ownerEmail = :ownerEmail")
    Long countFilesByUser(@Param("ownerEmail") String ownerEmail);
    
    // Count files in bucket
    @Query("SELECT COUNT(f) FROM FileMetadata f WHERE f.bucketName = :bucketName AND f.ownerEmail = :ownerEmail")
    Long countFilesInBucket(@Param("bucketName") String bucketName, @Param("ownerEmail") String ownerEmail);
    
    // Find files uploaded in date range
    @Query("SELECT f FROM FileMetadata f WHERE f.ownerEmail = :ownerEmail AND f.uploadedAt BETWEEN :startDate AND :endDate ORDER BY f.uploadedAt DESC")
    List<FileMetadata> findByOwnerEmailAndUploadedAtBetween(
        @Param("ownerEmail") String ownerEmail, 
        @Param("startDate") java.time.LocalDateTime startDate, 
        @Param("endDate") java.time.LocalDateTime endDate);
    
    // Find latest files (recent uploads)
    @Query("SELECT f FROM FileMetadata f WHERE f.ownerEmail = :ownerEmail ORDER BY f.uploadedAt DESC")
    List<FileMetadata> findLatestFilesByUser(@Param("ownerEmail") String ownerEmail, org.springframework.data.domain.Pageable pageable);
    
    // Find files by tags (if tags are stored as JSON string)
    @Query("SELECT f FROM FileMetadata f WHERE f.ownerEmail = :ownerEmail AND f.tags LIKE %:tag%")
    List<FileMetadata> findByOwnerEmailAndTagsContaining(@Param("ownerEmail") String ownerEmail, @Param("tag") String tag);
    
    // Find files by content type across all buckets for a user
    List<FileMetadata> findByOwnerEmailAndContentTypeContaining(String ownerEmail, String contentType);
    
    // Find files by name pattern across all buckets for a user
    List<FileMetadata> findByOwnerEmailAndOriginalFileNameContainingIgnoreCase(String ownerEmail, String fileName);
} 