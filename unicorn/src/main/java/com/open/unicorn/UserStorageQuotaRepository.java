package com.open.unicorn;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UserStorageQuotaRepository extends JpaRepository<UserStorageQuota, Long> {
    
    // Find quota by user email
    Optional<UserStorageQuota> findByUserEmail(String userEmail);
    
    // Check if quota exists for user
    boolean existsByUserEmail(String userEmail);
    
    // Get total storage used by all users
    @Query("SELECT SUM(u.usedStorage) FROM UserStorageQuota u")
    Long getTotalStorageUsed();
    
    // Get total storage capacity
    @Query("SELECT SUM(u.maxStorage) FROM UserStorageQuota u")
    Long getTotalStorageCapacity();
    
    // Find users exceeding their quota
    @Query("SELECT u FROM UserStorageQuota u WHERE u.usedStorage > u.maxStorage")
    java.util.List<UserStorageQuota> findUsersExceedingQuota();
    
    // Find users with high usage (above 80%)
    @Query("SELECT u FROM UserStorageQuota u WHERE (u.usedStorage * 100.0 / u.maxStorage) > 80")
    java.util.List<UserStorageQuota> findUsersWithHighUsage();
    
    // Update storage usage for a user
    @Query("UPDATE UserStorageQuota u SET u.usedStorage = :usedStorage, u.lastUpdated = CURRENT_TIMESTAMP WHERE u.userEmail = :userEmail")
    void updateStorageUsage(@Param("userEmail") String userEmail, @Param("usedStorage") Long usedStorage);
} 