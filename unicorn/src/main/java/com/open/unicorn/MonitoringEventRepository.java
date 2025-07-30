package com.open.unicorn;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MonitoringEventRepository extends JpaRepository<MonitoringEvent, Long> {
    
    // Find events by user and time range
    List<MonitoringEvent> findByUserIdAndRequestTimestampBetweenOrderByRequestTimestampDesc(
        Long userId, LocalDateTime startTime, LocalDateTime endTime);
    
    // Find events by service name and user
    List<MonitoringEvent> findByUserIdAndServiceNameOrderByRequestTimestampDesc(Long userId, String serviceName);
    
    // Find events by user and time range for specific service
    List<MonitoringEvent> findByUserIdAndServiceNameAndRequestTimestampBetweenOrderByRequestTimestampDesc(
        Long userId, String serviceName, LocalDateTime startTime, LocalDateTime endTime);
    
    // Get request count by service for a user
    @Query("SELECT me.serviceName, COUNT(me) as count FROM MonitoringEvent me " +
           "WHERE me.userId = :userId AND me.requestTimestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY me.serviceName ORDER BY count DESC")
    List<Object[]> getRequestCountByService(@Param("userId") Long userId, 
                                          @Param("startTime") LocalDateTime startTime, 
                                          @Param("endTime") LocalDateTime endTime);
    
    // Get response time statistics by service
    @Query("SELECT me.serviceName, " +
           "MIN(me.responseTimeMs) as minTime, " +
           "MAX(me.responseTimeMs) as maxTime, " +
           "AVG(me.responseTimeMs) as avgTime, " +
           "SUM(me.responseTimeMs) as sumTime " +
           "FROM MonitoringEvent me " +
           "WHERE me.userId = :userId AND me.requestTimestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY me.serviceName")
    List<Object[]> getResponseTimeStatsByService(@Param("userId") Long userId, 
                                               @Param("startTime") LocalDateTime startTime, 
                                               @Param("endTime") LocalDateTime endTime);
    
    // Get P95 response time by service
    @Query(value = "SELECT service_name, " +
           "PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY response_time_ms) as p95_time " +
           "FROM monitoring_events " +
           "WHERE user_id = :userId AND request_timestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY service_name", nativeQuery = true)
    List<Object[]> getP95ResponseTimeByService(@Param("userId") Long userId, 
                                             @Param("startTime") LocalDateTime startTime, 
                                             @Param("endTime") LocalDateTime endTime);
    
    // Get CPU and RAM usage statistics
    @Query("SELECT me.serviceName, " +
           "AVG(me.cpuUsagePercent) as avgCpu, " +
           "AVG(me.ramUsageMb) as avgRam, " +
           "MAX(me.cpuUsagePercent) as maxCpu, " +
           "MAX(me.ramUsageMb) as maxRam " +
           "FROM MonitoringEvent me " +
           "WHERE me.userId = :userId AND me.requestTimestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY me.serviceName")
    List<Object[]> getResourceUsageStats(@Param("userId") Long userId, 
                                       @Param("startTime") LocalDateTime startTime, 
                                       @Param("endTime") LocalDateTime endTime);
    
    // Get hourly request count for the last 24 hours
    @Query("SELECT HOUR(me.requestTimestamp) as hour, COUNT(me) as count " +
           "FROM MonitoringEvent me " +
           "WHERE me.userId = :userId AND me.requestTimestamp >= :startTime " +
           "GROUP BY HOUR(me.requestTimestamp) ORDER BY hour")
    List<Object[]> getHourlyRequestCount(@Param("userId") Long userId, 
                                       @Param("startTime") LocalDateTime startTime);
    
    // Get most used endpoints
    @Query("SELECT me.endpointName, me.serviceName, COUNT(me) as count " +
           "FROM MonitoringEvent me " +
           "WHERE me.userId = :userId AND me.requestTimestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY me.endpointName, me.serviceName ORDER BY count DESC")
    List<Object[]> getMostUsedEndpoints(@Param("userId") Long userId, 
                                      @Param("startTime") LocalDateTime startTime, 
                                      @Param("endTime") LocalDateTime endTime);
    
    // Get error count by service
    @Query("SELECT me.serviceName, COUNT(me) as errorCount " +
           "FROM MonitoringEvent me " +
           "WHERE me.userId = :userId AND me.requestTimestamp BETWEEN :startTime AND :endTime " +
           "AND me.statusCode >= 400 " +
           "GROUP BY me.serviceName")
    List<Object[]> getErrorCountByService(@Param("userId") Long userId, 
                                        @Param("startTime") LocalDateTime startTime, 
                                        @Param("endTime") LocalDateTime endTime);
    
    // Get total events count for a user
    long countByUserId(Long userId);
    
    // Get events count by user and time range
    long countByUserIdAndRequestTimestampBetween(Long userId, LocalDateTime startTime, LocalDateTime endTime);
    
    // Find events by user and time range (for error rate calculation)
    List<MonitoringEvent> findByUserIdAndRequestTimestampBetween(Long userId, LocalDateTime startTime, LocalDateTime endTime);
} 