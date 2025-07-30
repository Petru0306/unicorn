package com.open.unicorn;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "file_metadata")
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private String bucketName;

    @Column(nullable = false)
    private String ownerEmail;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Integer version; // File version number

    @Column(nullable = false)
    private Boolean isLatest; // Whether this is the latest version

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private String checksum; // File checksum for integrity

    @Column
    private String tags; // JSON string for file tags

    // Default constructor
    public FileMetadata() {
        this.uploadedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.version = 1;
        this.isLatest = true;
    }

    // Constructor with all fields
    public FileMetadata(String fileName, String originalFileName, String bucketName, 
                       String ownerEmail, String filePath, Long fileSize, String contentType) {
        this();
        this.fileName = fileName;
        this.originalFileName = originalFileName;
        this.bucketName = bucketName;
        this.ownerEmail = ownerEmail;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.contentType = contentType;
    }

    // Constructor with version
    public FileMetadata(String fileName, String originalFileName, String bucketName, 
                       String ownerEmail, String filePath, Long fileSize, String contentType, 
                       Integer version, Boolean isLatest) {
        this();
        this.fileName = fileName;
        this.originalFileName = originalFileName;
        this.bucketName = bucketName;
        this.ownerEmail = ownerEmail;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.version = version;
        this.isLatest = isLatest;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Boolean getIsLatest() {
        return isLatest;
    }

    public void setIsLatest(Boolean isLatest) {
        this.isLatest = isLatest;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }
} 