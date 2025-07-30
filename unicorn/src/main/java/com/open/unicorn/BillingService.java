package com.open.unicorn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BillingService {
    
    @Autowired
    private BillingEventRepository billingEventRepository;
    
    @Autowired
    private BillingAlertRepository billingAlertRepository;
    
    // AWS-like pricing model (prices per unit)
    private static final Map<String, Map<String, BigDecimal>> PRICING_MODEL = new ConcurrentHashMap<>();
    
    static {
        // UWS-S3 Pricing (increased for better visibility in testing)
        Map<String, BigDecimal> s3Pricing = new HashMap<>();
        s3Pricing.put("storage_gb", new BigDecimal("0.023")); // $0.023 per GB per month
        s3Pricing.put("request_put", new BigDecimal("0.05")); // $0.05 per PUT request (increased from $0.0005)
        s3Pricing.put("request_get", new BigDecimal("0.04")); // $0.04 per GET request (increased from $0.0004)
        s3Pricing.put("data_transfer_out", new BigDecimal("0.09")); // $0.09 per GB
        PRICING_MODEL.put("UWS-S3", s3Pricing);
        
        // UWS-Lambda Pricing (increased for better visibility in testing)
        Map<String, BigDecimal> lambdaPricing = new HashMap<>();
        lambdaPricing.put("request", new BigDecimal("0.02")); // $0.02 per request (increased from $0.0000002)
        lambdaPricing.put("duration_ms", new BigDecimal("0.00021")); // $0.00021 per 100ms (increased)
        lambdaPricing.put("memory_gb", new BigDecimal("0.00166667")); // $0.00166667 per GB-second (increased)
        PRICING_MODEL.put("UWS-Lambda", lambdaPricing);
        
        // UWS-Compute Pricing
        Map<String, BigDecimal> computePricing = new HashMap<>();
        computePricing.put("micro_hour", new BigDecimal("0.008")); // $0.008 per hour
        computePricing.put("small_hour", new BigDecimal("0.016")); // $0.016 per hour
        computePricing.put("medium_hour", new BigDecimal("0.032")); // $0.032 per hour
        computePricing.put("storage_gb", new BigDecimal("0.10")); // $0.10 per GB per month
        PRICING_MODEL.put("UWS-Compute", computePricing);
        
        // RDB Pricing
        Map<String, BigDecimal> rdbPricing = new HashMap<>();
        rdbPricing.put("instance_hour", new BigDecimal("0.017")); // $0.017 per hour
        rdbPricing.put("storage_gb", new BigDecimal("0.115")); // $0.115 per GB per month
        rdbPricing.put("backup_gb", new BigDecimal("0.095")); // $0.095 per GB per month
        PRICING_MODEL.put("RDB", rdbPricing);
        
        // NoSQL Pricing (increased for better visibility in testing)
        Map<String, BigDecimal> nosqlPricing = new HashMap<>();
        nosqlPricing.put("read_capacity_unit", new BigDecimal("0.025")); // $0.025 per RCU (increased from $0.00025)
        nosqlPricing.put("write_capacity_unit", new BigDecimal("0.025")); // $0.025 per WCU (increased from $0.00025)
        nosqlPricing.put("storage_gb", new BigDecimal("0.25")); // $0.25 per GB per month
        PRICING_MODEL.put("NoSQL", nosqlPricing);
        
        // SQS Pricing (increased for better visibility in testing)
        Map<String, BigDecimal> sqsPricing = new HashMap<>();
        sqsPricing.put("request", new BigDecimal("0.04")); // $0.04 per request (increased from $0.0000004)
        sqsPricing.put("message", new BigDecimal("0.04")); // $0.04 per message (increased from $0.0000004)
        PRICING_MODEL.put("SQS", sqsPricing);
        
        // Secrets Manager Pricing
        Map<String, BigDecimal> secretsPricing = new HashMap<>();
        secretsPricing.put("secret", new BigDecimal("0.40")); // $0.40 per secret per month
        secretsPricing.put("api_call", new BigDecimal("0.005")); // $0.005 per API call (increased from $0.00005)
        PRICING_MODEL.put("Secrets Manager", secretsPricing);
        
        // AI Service Pricing (increased for better visibility in testing)
        Map<String, BigDecimal> aiPricing = new HashMap<>();
        aiPricing.put("request", new BigDecimal("0.01")); // $0.01 per request (increased from $0.0001)
        aiPricing.put("token", new BigDecimal("0.0002")); // $0.0002 per token (increased from $0.000002)
        aiPricing.put("model_hour", new BigDecimal("0.10")); // $0.10 per hour for model usage
        PRICING_MODEL.put("AI Service", aiPricing);
        
        // DNS Service Pricing (increased for better visibility in testing)
        Map<String, BigDecimal> dnsPricing = new HashMap<>();
        dnsPricing.put("hosted_zone", new BigDecimal("0.50")); // $0.50 per hosted zone per month
        dnsPricing.put("query", new BigDecimal("0.04")); // $0.04 per query (increased from $0.0000004)
        PRICING_MODEL.put("DNS Service", dnsPricing);
    }
    
    // Record billing event
    public void recordBillingEvent(Long userId, String serviceName, String resourceType, String resourceId,
                                 Double usageQuantity, String usageUnit, LocalDateTime billingPeriodStart,
                                 LocalDateTime billingPeriodEnd, String description) {
        
        BigDecimal unitPrice = getUnitPrice(serviceName, resourceType);
        if (unitPrice == null) {
            unitPrice = BigDecimal.ZERO; // Default price if not found
        }
        
        BillingEvent event = new BillingEvent(userId, serviceName, resourceType, resourceId,
                                            usageQuantity, usageUnit, unitPrice, billingPeriodStart, billingPeriodEnd, description);
        
        billingEventRepository.save(event);
        
        // Check alerts after recording billing event
        checkBillingAlerts(userId, serviceName);
    }
    
    // Get unit price for service and resource type
    public BigDecimal getUnitPrice(String serviceName, String resourceType) {
        Map<String, BigDecimal> servicePricing = PRICING_MODEL.get(serviceName);
        if (servicePricing != null) {
            return servicePricing.get(resourceType);
        }
        return BigDecimal.ZERO;
    }
    
    // Get billing summary for user
    public Map<String, Object> getBillingSummary(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> summary = new HashMap<>();
        
        // Total cost
        BigDecimal totalCost = billingEventRepository.getTotalCostByUserIdAndTimeRange(userId, startTime, endTime);
        summary.put("totalCost", totalCost != null ? totalCost : BigDecimal.ZERO);
        
        // Cost by service
        List<Object[]> serviceCosts = billingEventRepository.getTotalCostByServiceAndTimeRange(userId, startTime, endTime);
        Map<String, BigDecimal> costByService = new HashMap<>();
        for (Object[] row : serviceCosts) {
            costByService.put((String) row[0], (BigDecimal) row[1]);
        }
        summary.put("costByService", costByService);
        
        // Top services by cost
        List<Object[]> topServices = billingEventRepository.getTopServicesByCost(userId, startTime, endTime);
        summary.put("topServices", topServices);
        
        // Resource usage by type
        List<Object[]> resourceUsage = billingEventRepository.getResourceUsageByType(userId, startTime, endTime);
        summary.put("resourceUsage", resourceUsage);
        
        return summary;
    }
    
    // Get monthly billing data
    public Map<String, Object> getMonthlyBilling(Long userId, int year, int month) {
        Map<String, Object> monthlyData = new HashMap<>();
        
        // Monthly summary by service for current month
        List<Object[]> monthlySummary = billingEventRepository.getMonthlyBillingSummaryByService(userId, year, month);
        monthlyData.put("monthlySummary", monthlySummary);
        
        // Daily breakdown for the month
        List<Object[]> dailyBilling = billingEventRepository.getDailyBillingForMonth(userId, year, month);
        monthlyData.put("dailyBilling", dailyBilling);
        
        // Current month total
        BigDecimal currentMonthTotal = billingEventRepository.getCurrentMonthTotalCost(userId, year, month);
        monthlyData.put("currentMonthTotal", currentMonthTotal != null ? currentMonthTotal : BigDecimal.ZERO);
        
        // Previous month total
        YearMonth prevMonth = YearMonth.of(year, month).minusMonths(1);
        BigDecimal prevMonthTotal = billingEventRepository.getPreviousMonthTotalCost(userId, prevMonth.getYear(), prevMonth.getMonthValue());
        monthlyData.put("previousMonthTotal", prevMonthTotal != null ? prevMonthTotal : BigDecimal.ZERO);
        
        return monthlyData;
    }
    
    // Get cost trends
    public List<Object[]> getCostTrends(Long userId, LocalDateTime startTime) {
        return billingEventRepository.getHourlyCostTrends(userId, startTime);
    }
    
    // Create billing alert
    public BillingAlert createBillingAlert(Long userId, String alertName, String alertType, BigDecimal thresholdValue,
                                         String serviceName, String timePeriod, Boolean emailNotification, String webhookUrl) {
        
        BillingAlert alert = new BillingAlert(userId, alertName, alertType, thresholdValue,
                                            serviceName, timePeriod, emailNotification, webhookUrl);
        
        return billingAlertRepository.save(alert);
    }
    
    // Get user alerts
    public List<BillingAlert> getUserAlerts(Long userId) {
        return billingAlertRepository.findByUserIdAndIsActiveTrue(userId);
    }
    
    // Check billing alerts
    private void checkBillingAlerts(Long userId, String serviceName) {
        List<BillingAlert> activeAlerts = billingAlertRepository.getActiveAlertsForUser(userId);
        
        for (BillingAlert alert : activeAlerts) {
            // Skip if alert is for specific service and current service doesn't match
            if (alert.getServiceName() != null && !alert.getServiceName().equals(serviceName)) {
                continue;
            }
            
            BigDecimal currentValue = getCurrentValueForAlert(userId, alert);
            
            if (alert.shouldTrigger(currentValue)) {
                alert.trigger();
                billingAlertRepository.save(alert);
                
                // Simulate notifications
                if (alert.getEmailNotification()) {
                    simulateEmailNotification(alert, currentValue);
                }
                
                if (alert.getWebhookUrl() != null && !alert.getWebhookUrl().isEmpty()) {
                    simulateWebhookNotification(alert, currentValue);
                }
            }
        }
    }
    
    // Get current value for alert checking
    private BigDecimal getCurrentValueForAlert(Long userId, BillingAlert alert) {
        LocalDateTime startTime = getStartTimeForPeriod(alert.getTimePeriod());
        LocalDateTime endTime = LocalDateTime.now();
        
        if (alert.getServiceName() != null) {
            // Service-specific cost
            List<Object[]> serviceCosts = billingEventRepository.getTotalCostByServiceAndTimeRange(userId, startTime, endTime);
            for (Object[] row : serviceCosts) {
                if (alert.getServiceName().equals(row[0])) {
                    return (BigDecimal) row[1];
                }
            }
            return BigDecimal.ZERO;
        } else {
            // Total cost across all services
            BigDecimal totalCost = billingEventRepository.getTotalCostByUserIdAndTimeRange(userId, startTime, endTime);
            return totalCost != null ? totalCost : BigDecimal.ZERO;
        }
    }
    
    // Get start time for alert period
    private LocalDateTime getStartTimeForPeriod(String timePeriod) {
        LocalDateTime now = LocalDateTime.now();
        switch (timePeriod.toLowerCase()) {
            case "daily":
                return now.toLocalDate().atStartOfDay();
            case "weekly":
                return now.minusWeeks(1);
            case "monthly":
                return now.minusMonths(1);
            default:
                return now.minusDays(1);
        }
    }
    
    // Simulate email notification
    private void simulateEmailNotification(BillingAlert alert, BigDecimal currentValue) {
        System.out.println("📧 EMAIL ALERT: " + alert.getAlertName() + 
                          " - Current value: $" + currentValue.setScale(2, RoundingMode.HALF_UP) + 
                          " exceeds threshold: $" + alert.getThresholdValue().setScale(2, RoundingMode.HALF_UP));
    }
    
    // Simulate webhook notification
    private void simulateWebhookNotification(BillingAlert alert, BigDecimal currentValue) {
        System.out.println("🔗 WEBHOOK ALERT: " + alert.getAlertName() + 
                          " sent to " + alert.getWebhookUrl() + 
                          " - Current value: $" + currentValue.setScale(2, RoundingMode.HALF_UP));
    }
    
    // Generate sample billing data
    public void generateSampleBillingData(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        Random random = new Random();
        
        // Generate data for the last 30 days
        for (int i = 0; i < 30; i++) {
            LocalDateTime date = now.minusDays(i);
            
            // Generate events for each service
            generateServiceBillingEvents(userId, "UWS-S3", date, random);
            generateServiceBillingEvents(userId, "UWS-Lambda", date, random);
            generateServiceBillingEvents(userId, "UWS-Compute", date, random);
            generateServiceBillingEvents(userId, "RDB", date, random);
            generateServiceBillingEvents(userId, "NoSQL", date, random);
            generateServiceBillingEvents(userId, "SQS", date, random);
            generateServiceBillingEvents(userId, "Secrets Manager", date, random);
            generateServiceBillingEvents(userId, "AI Service", date, random);
            generateServiceBillingEvents(userId, "DNS Service", date, random);
        }
    }
    
    // Generate billing events for a specific service
    private void generateServiceBillingEvents(Long userId, String serviceName, LocalDateTime date, Random random) {
        LocalDateTime startOfDay = date.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        
        switch (serviceName) {
            case "UWS-S3":
                // Storage usage
                double storageGB = 10 + random.nextDouble() * 100; // 10-110 GB
                recordBillingEvent(userId, serviceName, "storage_gb", "bucket-" + random.nextInt(5),
                                 storageGB, "GB", startOfDay, endOfDay, "S3 Storage Usage");
                
                // Request counts
                int putRequests = 50 + random.nextInt(200);
                recordBillingEvent(userId, serviceName, "request_put", "bucket-" + random.nextInt(5),
                                 (double) putRequests, "requests", startOfDay, endOfDay, "S3 PUT Requests");
                
                int getRequests = 100 + random.nextInt(500);
                recordBillingEvent(userId, serviceName, "request_get", "bucket-" + random.nextInt(5),
                                 (double) getRequests, "requests", startOfDay, endOfDay, "S3 GET Requests");
                break;
                
            case "UWS-Lambda":
                // Function invocations
                int invocations = 100 + random.nextInt(1000);
                recordBillingEvent(userId, serviceName, "request", "function-" + random.nextInt(10),
                                 (double) invocations, "requests", startOfDay, endOfDay, "Lambda Invocations");
                
                // Duration
                double durationMs = 100 + random.nextDouble() * 5000; // 100-5100ms
                recordBillingEvent(userId, serviceName, "duration_ms", "function-" + random.nextInt(10),
                                 durationMs, "ms", startOfDay, endOfDay, "Lambda Duration");
                break;
                
            case "UWS-Compute":
                // Instance hours
                double instanceHours = 2 + random.nextDouble() * 22; // 2-24 hours
                String[] sizes = {"micro", "small", "medium"};
                String size = sizes[random.nextInt(sizes.length)];
                recordBillingEvent(userId, serviceName, size + "_hour", "container-" + random.nextInt(5),
                                 instanceHours, "hours", startOfDay, endOfDay, "Compute Instance Hours");
                break;
                
            case "RDB":
                // Instance hours
                double rdbHours = 24; // Always running
                recordBillingEvent(userId, serviceName, "instance_hour", "db-" + random.nextInt(3),
                                 rdbHours, "hours", startOfDay, endOfDay, "RDB Instance Hours");
                
                // Storage
                double rdbStorage = 20 + random.nextDouble() * 80; // 20-100 GB
                recordBillingEvent(userId, serviceName, "storage_gb", "db-" + random.nextInt(3),
                                 rdbStorage, "GB", startOfDay, endOfDay, "RDB Storage");
                break;
                
            case "NoSQL":
                // Capacity units
                int readUnits = 10 + random.nextInt(50);
                recordBillingEvent(userId, serviceName, "read_capacity_unit", "table-" + random.nextInt(5),
                                 (double) readUnits, "RCU", startOfDay, endOfDay, "NoSQL Read Capacity");
                
                int writeUnits = 5 + random.nextInt(20);
                recordBillingEvent(userId, serviceName, "write_capacity_unit", "table-" + random.nextInt(5),
                                 (double) writeUnits, "WCU", startOfDay, endOfDay, "NoSQL Write Capacity");
                break;
                
            case "SQS":
                // Messages
                int messages = 500 + random.nextInt(2000);
                recordBillingEvent(userId, serviceName, "message", "queue-" + random.nextInt(3),
                                 (double) messages, "messages", startOfDay, endOfDay, "SQS Messages");
                break;
                
            case "Secrets Manager":
                // Secrets
                int secrets = 1 + random.nextInt(5);
                recordBillingEvent(userId, serviceName, "secret", "secret-" + random.nextInt(10),
                                 (double) secrets, "secrets", startOfDay, endOfDay, "Secrets Manager");
                break;
                
            case "AI Service":
                // AI requests
                int aiRequests = 50 + random.nextInt(200);
                recordBillingEvent(userId, serviceName, "request", "model-" + random.nextInt(3),
                                 (double) aiRequests, "requests", startOfDay, endOfDay, "AI Service Requests");
                break;
                
            case "DNS Service":
                // DNS queries
                int dnsQueries = 1000 + random.nextInt(5000);
                recordBillingEvent(userId, serviceName, "query", "zone-" + random.nextInt(2),
                                 (double) dnsQueries, "queries", startOfDay, endOfDay, "DNS Queries");
                break;
        }
    }
    
    // Scheduled task to check alerts every hour
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void checkBillingAlertsScheduled() {
        // This would check all active alerts for all users
        // For now, we'll just log that the task ran
        System.out.println("🕐 Scheduled billing alert check completed at " + LocalDateTime.now());
    }
} 