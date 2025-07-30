package com.open.unicorn;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_storage_quotas")
public class UserStorageQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userEmail;

    @Column(nullable = false)
    private Long usedStorage; // in bytes

    @Column(nullable = false)
    private Long maxStorage; // in bytes (default 1GB = 1,073,741,824 bytes)

    @Column(nullable = false)
    private LocalDateTime lastUpdated;

    // Default constructor
    public UserStorageQuota() {
        this.usedStorage = 0L;
        this.maxStorage = 1073741824L; // 1GB default
        this.lastUpdated = LocalDateTime.now();
    }

    // Constructor with user email
    public UserStorageQuota(String userEmail) {
        this();
        this.userEmail = userEmail;
    }

    // Constructor with custom quota
    public UserStorageQuota(String userEmail, Long maxStorage) {
        this();
        this.userEmail = userEmail;
        this.maxStorage = maxStorage;
    }

    // Helper method to add storage
    public void addStorage(Long bytes) {
        this.usedStorage += bytes;
        this.lastUpdated = LocalDateTime.now();
    }

    // Helper method to remove storage
    public void removeStorage(Long bytes) {
        this.usedStorage = Math.max(0, this.usedStorage - bytes);
        this.lastUpdated = LocalDateTime.now();
    }

    // Helper method to check if quota is exceeded
    public boolean isQuotaExceeded() {
        return this.usedStorage > this.maxStorage;
    }

    // Helper method to get usage percentage
    public double getUsagePercentage() {
        return (double) this.usedStorage / this.maxStorage * 100;
    }

    // Helper method to get remaining storage
    public Long getRemainingStorage() {
        return Math.max(0, this.maxStorage - this.usedStorage);
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

    public Long getUsedStorage() {
        return usedStorage;
    }

    public void setUsedStorage(Long usedStorage) {
        this.usedStorage = usedStorage;
    }

    public Long getMaxStorage() {
        return maxStorage;
    }

    public void setMaxStorage(Long maxStorage) {
        this.maxStorage = maxStorage;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
} 