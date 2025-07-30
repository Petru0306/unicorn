package com.open.unicorn;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    
    // Find all activities for a specific user, ordered by timestamp descending
    List<ActivityLog> findByUserEmailOrderByTimestampDesc(String userEmail);
    
    // Find recent activities for a user (latest N activities)
    @Query("SELECT a FROM ActivityLog a WHERE a.userEmail = :userEmail ORDER BY a.timestamp DESC")
    List<ActivityLog> findRecentActivitiesByUser(@Param("userEmail") String userEmail, org.springframework.data.domain.Pageable pageable);
    
    // Find activities by user and action type
    List<ActivityLog> findByUserEmailAndActionOrderByTimestampDesc(String userEmail, String action);
    
    // Find activities by user and resource type
    List<ActivityLog> findByUserEmailAndResourceTypeOrderByTimestampDesc(String userEmail, String resourceType);
    
    // Find activities by user and bucket
    List<ActivityLog> findByUserEmailAndBucketNameOrderByTimestampDesc(String userEmail, String bucketName);
    
    // Count activities by user and action
    long countByUserEmailAndAction(String userEmail, String action);
    
    // Find activities within a date range for a user
    @Query("SELECT a FROM ActivityLog a WHERE a.userEmail = :userEmail AND a.timestamp BETWEEN :startDate AND :endDate ORDER BY a.timestamp DESC")
    List<ActivityLog> findByUserEmailAndTimestampBetween(@Param("userEmail") String userEmail, 
                                                        @Param("startDate") java.time.LocalDateTime startDate, 
                                                        @Param("endDate") java.time.LocalDateTime endDate);
} 