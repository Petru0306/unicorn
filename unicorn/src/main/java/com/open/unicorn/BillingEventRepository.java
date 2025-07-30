package com.open.unicorn;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BillingEventRepository extends JpaRepository<BillingEvent, Long> {
    
    // Find billing events by user and time range
    List<BillingEvent> findByUserIdAndEventTimestampBetween(Long userId, LocalDateTime startTime, LocalDateTime endTime);
    
    // Find billing events by user, service and time range
    List<BillingEvent> findByUserIdAndServiceNameAndEventTimestampBetween(Long userId, String serviceName, LocalDateTime startTime, LocalDateTime endTime);
    
    // Get total cost for user in time range
    @Query("SELECT SUM(be.totalCost) FROM BillingEvent be WHERE be.userId = :userId AND be.eventTimestamp BETWEEN :startTime AND :endTime")
    BigDecimal getTotalCostByUserIdAndTimeRange(@Param("userId") Long userId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    // Get total cost by service for user in time range
    @Query("SELECT be.serviceName, SUM(be.totalCost) FROM BillingEvent be WHERE be.userId = :userId AND be.eventTimestamp BETWEEN :startTime AND :endTime GROUP BY be.serviceName")
    List<Object[]> getTotalCostByServiceAndTimeRange(@Param("userId") Long userId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    // Get monthly billing summary
    @Query("SELECT YEAR(be.eventTimestamp) as year, MONTH(be.eventTimestamp) as month, SUM(be.totalCost) as totalCost, COUNT(be) as eventCount " +
           "FROM BillingEvent be WHERE be.userId = :userId GROUP BY YEAR(be.eventTimestamp), MONTH(be.eventTimestamp) ORDER BY year DESC, month DESC")
    List<Object[]> getMonthlyBillingSummary(@Param("userId") Long userId);
    
    // Get daily billing for current month
    @Query("SELECT DAY(be.eventTimestamp) as day, SUM(be.totalCost) as totalCost, COUNT(be) as eventCount " +
           "FROM BillingEvent be WHERE be.userId = :userId AND YEAR(be.eventTimestamp) = :year AND MONTH(be.eventTimestamp) = :month " +
           "GROUP BY DAY(be.eventTimestamp) ORDER BY day")
    List<Object[]> getDailyBillingForMonth(@Param("userId") Long userId, @Param("year") int year, @Param("month") int month);
    
    // Get top services by cost
    @Query("SELECT be.serviceName, SUM(be.totalCost) as totalCost, COUNT(be) as eventCount " +
           "FROM BillingEvent be WHERE be.userId = :userId AND be.eventTimestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY be.serviceName ORDER BY totalCost DESC")
    List<Object[]> getTopServicesByCost(@Param("userId") Long userId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    // Get resource usage by type
    @Query("SELECT be.resourceType, SUM(be.usageQuantity) as totalUsage, be.usageUnit, SUM(be.totalCost) as totalCost " +
           "FROM BillingEvent be WHERE be.userId = :userId AND be.eventTimestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY be.resourceType, be.usageUnit ORDER BY totalCost DESC")
    List<Object[]> getResourceUsageByType(@Param("userId") Long userId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    // Get cost trends (hourly for last 24 hours)
    @Query("SELECT HOUR(be.eventTimestamp) as hour, SUM(be.totalCost) as totalCost " +
           "FROM BillingEvent be WHERE be.userId = :userId AND be.eventTimestamp >= :startTime " +
           "GROUP BY HOUR(be.eventTimestamp) ORDER BY hour")
    List<Object[]> getHourlyCostTrends(@Param("userId") Long userId, @Param("startTime") LocalDateTime startTime);
    
    // Get current month total cost
    @Query("SELECT SUM(be.totalCost) FROM BillingEvent be WHERE be.userId = :userId AND YEAR(be.eventTimestamp) = :year AND MONTH(be.eventTimestamp) = :month")
    BigDecimal getCurrentMonthTotalCost(@Param("userId") Long userId, @Param("year") int year, @Param("month") int month);
    
    // Get previous month total cost
    @Query("SELECT SUM(be.totalCost) FROM BillingEvent be WHERE be.userId = :userId AND YEAR(be.eventTimestamp) = :year AND MONTH(be.eventTimestamp) = :month")
    BigDecimal getPreviousMonthTotalCost(@Param("userId") Long userId, @Param("year") int year, @Param("month") int month);
} 