package com.open.unicorn;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MonitoringAlertRepository extends JpaRepository<MonitoringAlert, Long> {
    
    // Find all alerts for a user
    List<MonitoringAlert> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    // Find active alerts for a user
    List<MonitoringAlert> findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(Long userId);
    
    // Find alerts by service name for a user
    List<MonitoringAlert> findByUserIdAndServiceNameOrderByCreatedAtDesc(Long userId, String serviceName);
    
    // Find alerts by metric type for a user
    List<MonitoringAlert> findByUserIdAndMetricTypeOrderByCreatedAtDesc(Long userId, String metricType);
    
    // Find triggered alerts (status = CRITICAL) for a user
    List<MonitoringAlert> findByUserIdAndStatusOrderByLastTriggeredAtDesc(Long userId, String status);
    
    // Find alerts that were triggered recently
    List<MonitoringAlert> findByUserIdAndLastTriggeredAtBetweenOrderByLastTriggeredAtDesc(
        Long userId, LocalDateTime startTime, LocalDateTime endTime);
    
    // Find alerts by service and metric type
    List<MonitoringAlert> findByUserIdAndServiceNameAndMetricTypeOrderByCreatedAtDesc(
        Long userId, String serviceName, String metricType);
    
    // Count active alerts for a user
    long countByUserIdAndIsActiveTrue(Long userId);
    
    // Count triggered alerts for a user
    long countByUserIdAndStatus(Long userId, String status);
    
    // Get alert statistics by service
    @Query("SELECT ma.serviceName, COUNT(ma) as totalAlerts, " +
           "SUM(CASE WHEN ma.status = 'CRITICAL' THEN 1 ELSE 0 END) as triggeredAlerts " +
           "FROM MonitoringAlert ma " +
           "WHERE ma.userId = :userId " +
           "GROUP BY ma.serviceName")
    List<Object[]> getAlertStatsByService(@Param("userId") Long userId);
    
    // Get alert statistics by metric type
    @Query("SELECT ma.metricType, COUNT(ma) as totalAlerts, " +
           "SUM(CASE WHEN ma.status = 'CRITICAL' THEN 1 ELSE 0 END) as triggeredAlerts " +
           "FROM MonitoringAlert ma " +
           "WHERE ma.userId = :userId " +
           "GROUP BY ma.metricType")
    List<Object[]> getAlertStatsByMetricType(@Param("userId") Long userId);
    
    // Find alerts that need to be checked (active alerts)
    @Query("SELECT ma FROM MonitoringAlert ma " +
           "WHERE ma.userId = :userId AND ma.isActive = true " +
           "ORDER BY ma.createdAt DESC")
    List<MonitoringAlert> findActiveAlertsForUser(@Param("userId") Long userId);
} 