package com.open.unicorn;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingAlertRepository extends JpaRepository<BillingAlert, Long> {
    
    // Find active alerts by user
    List<BillingAlert> findByUserIdAndIsActiveTrue(Long userId);
    
    // Find alerts by user and service
    List<BillingAlert> findByUserIdAndServiceName(Long userId, String serviceName);
    
    // Find triggered alerts by user
    List<BillingAlert> findByUserIdAndStatus(Long userId, String status);
    
    // Find alerts by type
    List<BillingAlert> findByUserIdAndAlertType(Long userId, String alertType);
    
    // Find alerts by time period
    List<BillingAlert> findByUserIdAndTimePeriod(Long userId, String timePeriod);
    
    // Get alert statistics by user
    @Query("SELECT ba.alertType, COUNT(ba) as count, SUM(CASE WHEN ba.status = 'TRIGGERED' THEN 1 ELSE 0 END) as triggeredCount " +
           "FROM BillingAlert ba WHERE ba.userId = :userId GROUP BY ba.alertType")
    List<Object[]> getAlertStatisticsByUser(@Param("userId") Long userId);
    
    // Get alerts that need to be checked (active alerts)
    @Query("SELECT ba FROM BillingAlert ba WHERE ba.userId = :userId AND ba.isActive = true AND ba.status = 'ACTIVE'")
    List<BillingAlert> getActiveAlertsForUser(@Param("userId") Long userId);
    
    // Get recently triggered alerts
    @Query("SELECT ba FROM BillingAlert ba WHERE ba.userId = :userId AND ba.status = 'TRIGGERED' AND ba.lastTriggeredAt >= :since ORDER BY ba.lastTriggeredAt DESC")
    List<BillingAlert> getRecentlyTriggeredAlerts(@Param("userId") Long userId, @Param("since") java.time.LocalDateTime since);
} 