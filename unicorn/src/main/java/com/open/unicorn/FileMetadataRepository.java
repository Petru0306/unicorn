package com.open.unicorn;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {
    List<FileMetadata> findByBucketName(String bucketName);
    List<FileMetadata> findByOwnerEmail(String ownerEmail);
    List<FileMetadata> findByBucketNameAndOwnerEmail(String bucketName, String ownerEmail);
    FileMetadata findByFileName(String fileName);
} 