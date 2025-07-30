package com.open.unicorn;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs")
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private String action; // UPLOAD, DOWNLOAD, DELETE, CREATE_BUCKET, DELETE_BUCKET

    @Column(nullable = false)
    private String resourceName; // file name or bucket name

    @Column(nullable = false)
    private String resourceType; // FILE, BUCKET

    @Column
    private String bucketName; // for file operations

    @Column
    private Long fileSize; // for file operations

    @Column
    private String contentType; // for file operations

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column
    private String details; // additional details about the action

    // Default constructor
    public ActivityLog() {
        this.timestamp = LocalDateTime.now();
    }

    // Constructor for file operations
    public ActivityLog(String userEmail, String action, String resourceName, String resourceType, 
                      String bucketName, Long fileSize, String contentType, String details) {
        this();
        this.userEmail = userEmail;
        this.action = action;
        this.resourceName = resourceName;
        this.resourceType = resourceType;
        this.bucketName = bucketName;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.details = details;
    }

    // Constructor for bucket operations
    public ActivityLog(String userEmail, String action, String resourceName, String resourceType, String details) {
        this();
        this.userEmail = userEmail;
        this.action = action;
        this.resourceName = resourceName;
        this.resourceType = resourceType;
        this.details = details;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
} 