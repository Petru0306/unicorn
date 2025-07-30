package com.open.unicorn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;

@Service
public class MonitoringService {
    
    @Autowired
    private MonitoringEventRepository monitoringEventRepository;
    
    @Autowired
    private MonitoringAlertRepository monitoringAlertRepository;
    
    // In-memory cache for real-time metrics
    private final Map<String, Object> realTimeMetrics = new ConcurrentHashMap<>();
    
    // Simulated system metrics
    private final Random random = new Random();
    
    /**
     * Record a monitoring event with real metrics
     */
    public void recordEvent(Long userId, String serviceName, String endpointName, String httpMethod,
                           Long responseTimeMs, Integer statusCode, String userAgent, String ipAddress,
                           Double cpuUsage, Double ramUsage, Long requestSizeBytes, Long responseSizeBytes) {
        
        MonitoringEvent event = new MonitoringEvent(
            userId, serviceName, endpointName, httpMethod, 
            LocalDateTime.now(), responseTimeMs, cpuUsage, ramUsage, statusCode
        );
        
        event.setUserAgent(userAgent);
        event.setIpAddress(ipAddress);
        event.setRequestSizeBytes(requestSizeBytes);
        event.setResponseSizeBytes(responseSizeBytes);
        
        monitoringEventRepository.save(event);
        
        // Update real-time metrics
        updateRealTimeMetrics(userId, serviceName, responseTimeMs, cpuUsage, ramUsage);
        
        // Check alerts
        checkAlerts(userId, serviceName, responseTimeMs, cpuUsage, ramUsage);
    }
    
    /**
     * Record a monitoring event (backward compatibility)
     */
    public void recordEvent(Long userId, String serviceName, String endpointName, String httpMethod,
                           Long responseTimeMs, Integer statusCode, String userAgent, String ipAddress) {
        
        // Generate simulated CPU and RAM usage for backward compatibility
        Double cpuUsage = 20.0 + random.nextDouble() * 60.0; // 20-80%
        Double ramUsage = 100.0 + random.nextDouble() * 400.0; // 100-500 MB
        Long requestSize = 200L + random.nextInt(800);
        Long responseSize = 300L + random.nextInt(700);
        
        recordEvent(userId, serviceName, endpointName, httpMethod, responseTimeMs, statusCode, 
                   userAgent, ipAddress, cpuUsage, ramUsage, requestSize, responseSize);
    }
    
    /**
     * Update real-time metrics cache
     */
    private void updateRealTimeMetrics(Long userId, String serviceName, Long responseTime, 
                                     Double cpuUsage, Double ramUsage) {
        String key = userId + "_" + serviceName;
        
        Map<String, Object> serviceMetrics = (Map<String, Object>) realTimeMetrics.get(key);
        if (serviceMetrics == null) {
            serviceMetrics = new HashMap<>();
            serviceMetrics.put("lastUpdate", LocalDateTime.now());
            serviceMetrics.put("requestCount", 0L);
            serviceMetrics.put("avgResponseTime", 0.0);
            serviceMetrics.put("avgCpuUsage", 0.0);
            serviceMetrics.put("avgRamUsage", 0.0);
            serviceMetrics.put("errorCount", 0L);
        }
        
        // Update metrics
        Long requestCount = (Long) serviceMetrics.get("requestCount") + 1;
        Double avgResponseTime = ((Double) serviceMetrics.get("avgResponseTime") * (requestCount - 1) + responseTime) / requestCount;
        Double avgCpuUsage = ((Double) serviceMetrics.get("avgCpuUsage") * (requestCount - 1) + cpuUsage) / requestCount;
        Double avgRamUsage = ((Double) serviceMetrics.get("avgRamUsage") * (requestCount - 1) + ramUsage) / requestCount;
        
        serviceMetrics.put("requestCount", requestCount);
        serviceMetrics.put("avgResponseTime", avgResponseTime);
        serviceMetrics.put("avgCpuUsage", avgCpuUsage);
        serviceMetrics.put("avgRamUsage", avgRamUsage);
        serviceMetrics.put("lastUpdate", LocalDateTime.now());
        
        realTimeMetrics.put(key, serviceMetrics);
    }
    
    /**
     * Check and trigger alerts
     */
    private void checkAlerts(Long userId, String serviceName, Long responseTime, 
                           Double cpuUsage, Double ramUsage) {
        
        List<MonitoringAlert> activeAlerts = monitoringAlertRepository.findActiveAlertsForUser(userId);
        
        for (MonitoringAlert alert : activeAlerts) {
            if (alert.getServiceName() != null && !alert.getServiceName().equals(serviceName)) {
                continue; // Skip if alert is for specific service and doesn't match
            }
            
            Double currentValue = null;
            switch (alert.getMetricType()) {
                case "response_time":
                    currentValue = responseTime.doubleValue();
                    break;
                case "cpu_usage":
                    currentValue = cpuUsage;
                    break;
                case "ram_usage":
                    currentValue = ramUsage;
                    break;
                case "error_rate":
                    // Calculate error rate from recent events
                    LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
                    long totalRequests = monitoringEventRepository.countByUserIdAndRequestTimestampBetween(
                        userId, oneHourAgo, LocalDateTime.now());
                    long errorRequests = monitoringEventRepository.findByUserIdAndRequestTimestampBetween(
                        userId, oneHourAgo, LocalDateTime.now())
                        .stream()
                        .filter(e -> e.getStatusCode() >= 400)
                        .count();
                    currentValue = totalRequests > 0 ? (double) errorRequests / totalRequests * 100 : 0.0;
                    break;
            }
            
            if (currentValue != null && alert.shouldTrigger(currentValue)) {
                alert.trigger(currentValue);
                monitoringAlertRepository.save(alert);
                
                // Simulate email notification
                if (alert.getEmailNotification()) {
                    simulateEmailNotification(alert, currentValue);
                }
                
                // Simulate webhook
                if (alert.getWebhookUrl() != null && !alert.getWebhookUrl().isEmpty()) {
                    simulateWebhookNotification(alert, currentValue);
                }
            }
        }
    }
    
    /**
     * Simulate email notification
     */
    private void simulateEmailNotification(MonitoringAlert alert, Double currentValue) {
        // In a real implementation, this would send an actual email
        System.out.println("📧 ALERT EMAIL: " + alert.getAlertName() + 
                          " triggered! Current value: " + currentValue + 
                          " " + alert.getThresholdOperator() + " " + alert.getThresholdValue());
    }
    
    /**
     * Simulate webhook notification
     */
    private void simulateWebhookNotification(MonitoringAlert alert, Double currentValue) {
        // In a real implementation, this would send an HTTP POST to the webhook URL
        System.out.println("🔗 WEBHOOK: POST to " + alert.getWebhookUrl() + 
                          " - Alert: " + alert.getAlertName() + 
                          " triggered! Current value: " + currentValue);
    }
    
    /**
     * Get monitoring dashboard data
     */
    public Map<String, Object> getDashboardData(Long userId, String timeRange) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = getStartTimeForRange(timeRange);
        
        Map<String, Object> dashboardData = new HashMap<>();
        
        // Overall statistics
        dashboardData.put("totalRequests", monitoringEventRepository.countByUserIdAndRequestTimestampBetween(userId, startTime, endTime));
        dashboardData.put("totalEvents", monitoringEventRepository.countByUserId(userId));
        
        // Request count by service
        List<Object[]> requestCounts = monitoringEventRepository.getRequestCountByService(userId, startTime, endTime);
        dashboardData.put("requestCountByService", requestCounts);
        
        // Response time statistics
        List<Object[]> responseTimeStats = monitoringEventRepository.getResponseTimeStatsByService(userId, startTime, endTime);
        dashboardData.put("responseTimeStats", responseTimeStats);
        
        // P95 response times
        List<Object[]> p95ResponseTimes = monitoringEventRepository.getP95ResponseTimeByService(userId, startTime, endTime);
        dashboardData.put("p95ResponseTimes", p95ResponseTimes);
        
        // Resource usage statistics
        List<Object[]> resourceUsage = monitoringEventRepository.getResourceUsageStats(userId, startTime, endTime);
        dashboardData.put("resourceUsage", resourceUsage);
        
        // Error statistics
        List<Object[]> errorCounts = monitoringEventRepository.getErrorCountByService(userId, startTime, endTime);
        dashboardData.put("errorCounts", errorCounts);
        
        // Most used endpoints
        List<Object[]> mostUsedEndpoints = monitoringEventRepository.getMostUsedEndpoints(userId, startTime, endTime);
        dashboardData.put("mostUsedEndpoints", mostUsedEndpoints);
        
        // Hourly request count for charts
        List<Object[]> hourlyRequests = monitoringEventRepository.getHourlyRequestCount(userId, startTime);
        dashboardData.put("hourlyRequests", hourlyRequests);
        
        // Alert statistics
        List<Object[]> alertStats = monitoringAlertRepository.getAlertStatsByService(userId);
        dashboardData.put("alertStats", alertStats);
        
        // Active alerts
        List<MonitoringAlert> activeAlerts = monitoringAlertRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId);
        dashboardData.put("activeAlerts", activeAlerts);
        
        // Triggered alerts
        List<MonitoringAlert> triggeredAlerts = monitoringAlertRepository.findByUserIdAndStatusOrderByLastTriggeredAtDesc(userId, "CRITICAL");
        dashboardData.put("triggeredAlerts", triggeredAlerts);
        
        // System health (simulated)
        dashboardData.put("systemHealth", getSystemHealth());
        
        return dashboardData;
    }
    
    /**
     * Get system health metrics (simulated)
     */
    private Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("uptime", "99.95%");
        health.put("totalServices", 9);
        health.put("activeServices", 9);
        health.put("overallStatus", "HEALTHY");
        health.put("lastCheck", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return health;
    }
    
    /**
     * Get start time based on time range
     */
    private LocalDateTime getStartTimeForRange(String timeRange) {
        LocalDateTime now = LocalDateTime.now();
        switch (timeRange.toLowerCase()) {
            case "10min":
                return now.minusMinutes(10);
            case "1hour":
                return now.minusHours(1);
            case "24hours":
                return now.minusHours(24);
            case "7days":
                return now.minusDays(7);
            case "30days":
                return now.minusDays(30);
            default:
                return now.minusDays(1); // Default to 24 hours
        }
    }
    
    /**
     * Get real-time metrics for a specific service
     */
    public Map<String, Object> getServiceMetrics(Long userId, String serviceName) {
        String key = userId + "_" + serviceName;
        Map<String, Object> metrics = (Map<String, Object>) realTimeMetrics.get(key);
        
        if (metrics == null) {
            metrics = new HashMap<>();
            metrics.put("lastUpdate", LocalDateTime.now());
            metrics.put("requestCount", 0L);
            metrics.put("avgResponseTime", 0.0);
            metrics.put("avgCpuUsage", 0.0);
            metrics.put("avgRamUsage", 0.0);
            metrics.put("errorCount", 0L);
        }
        
        return metrics;
    }
    
    /**
     * Get all real-time metrics for a user
     */
    public Map<String, Object> getAllRealTimeMetrics(Long userId) {
        Map<String, Object> allMetrics = new HashMap<>();
        
        String[] services = {"UWS-S3", "UWS-Compute", "UWS-Lambda", "UWS-RDB", "UWS-NoSQL", 
                           "UWS-SQS", "UWS-AI", "UWS-Secrets", "UWS-DNS"};
        
        for (String service : services) {
            allMetrics.put(service, getServiceMetrics(userId, service));
        }
        
        return allMetrics;
    }
    
    /**
     * Clean up old metrics (scheduled task)
     */
    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM daily
    public void cleanupOldMetrics() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(90); // Keep 90 days of data
        // In a real implementation, you would delete old records
        System.out.println("🧹 Cleaning up metrics older than " + cutoffDate);
    }
    
    /**
     * Generate realistic sample data based on actual service usage patterns
     */
    public void generateSampleData(Long userId) {
        // Define realistic service patterns
        Map<String, ServicePattern> servicePatterns = new HashMap<>();
        servicePatterns.put("UWS-S3", new ServicePattern("uploadFile", "downloadFile", "listBuckets", "deleteFile", 80, 300));
        servicePatterns.put("UWS-Compute", new ServicePattern("createContainer", "startContainer", "stopContainer", "getStatus", 120, 800));
        servicePatterns.put("UWS-Lambda", new ServicePattern("invokeFunction", "createFunction", "updateFunction", "deleteFunction", 200, 1500));
        servicePatterns.put("UWS-RDB", new ServicePattern("executeQuery", "createTable", "insertData", "updateData", 150, 600));
        servicePatterns.put("UWS-NoSQL", new ServicePattern("putItem", "getItem", "queryItems", "deleteItem", 100, 400));
        servicePatterns.put("UWS-SQS", new ServicePattern("sendMessage", "receiveMessage", "deleteMessage", "getQueueAttributes", 50, 200));
        servicePatterns.put("UWS-AI", new ServicePattern("processImage", "analyzeText", "generateResponse", "trainModel", 500, 3000));
        servicePatterns.put("UWS-Secrets", new ServicePattern("getSecret", "createSecret", "updateSecret", "deleteSecret", 80, 250));
        servicePatterns.put("UWS-DNS", new ServicePattern("createRecord", "updateRecord", "deleteRecord", "listRecords", 60, 180));
        
        LocalDateTime now = LocalDateTime.now();
        String[] userAgents = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36",
            "PostmanRuntime/7.32.3",
            "curl/7.68.0"
        };
        
        String[] ipAddresses = {
            "192.168.1.100", "192.168.1.101", "192.168.1.102",
            "10.0.0.50", "10.0.0.51", "10.0.0.52",
            "172.16.0.10", "172.16.0.11", "172.16.0.12"
        };
        
        // Generate realistic traffic patterns
        for (int i = 0; i < 150; i++) {
            String service = getRandomServiceWithWeight();
            ServicePattern pattern = servicePatterns.get(service);
            
            String endpoint = pattern.getRandomEndpoint();
            Long responseTime = pattern.getRandomResponseTime();
            Integer statusCode = getRealisticStatusCode(service, endpoint);
            
            String userAgent = userAgents[random.nextInt(userAgents.length)];
            String ipAddress = ipAddresses[random.nextInt(ipAddresses.length)];
            
            // Get real system metrics
            double cpuUsage = getRealCpuUsage();
            double ramUsage = getRealRamUsage();
            long requestSize = estimateRequestSize(endpoint);
            long responseSize = estimateResponseSize(statusCode);
            
            recordEvent(userId, service, endpoint, "HTTP", responseTime, statusCode, 
                       userAgent, ipAddress, cpuUsage, ramUsage, requestSize, responseSize);
            
            // Add realistic time variation
            try {
                Thread.sleep(random.nextInt(50) + 10); // 10-60ms between requests
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    /**
     * Get random service with realistic weight distribution
     */
    private String getRandomServiceWithWeight() {
        double rand = random.nextDouble();
        if (rand < 0.25) return "UWS-S3";        // 25% - most used
        if (rand < 0.40) return "UWS-Compute";   // 15%
        if (rand < 0.55) return "UWS-Lambda";    // 15%
        if (rand < 0.70) return "UWS-RDB";       // 15%
        if (rand < 0.80) return "UWS-NoSQL";     // 10%
        if (rand < 0.87) return "UWS-SQS";       // 7%
        if (rand < 0.93) return "UWS-AI";        // 6%
        if (rand < 0.97) return "UWS-Secrets";   // 4%
        return "UWS-DNS";                        // 3% - least used
    }
    
    /**
     * Get realistic status codes based on service and endpoint
     */
    private Integer getRealisticStatusCode(String service, String endpoint) {
        double rand = random.nextDouble();
        
        // Different services have different error rates
        double errorRate = 0.02; // 2% base error rate
        
        switch (service) {
            case "UWS-AI":
                errorRate = 0.05; // 5% - AI services are more complex
                break;
            case "UWS-Lambda":
                errorRate = 0.04; // 4% - Lambda can have cold starts
                break;
            case "UWS-Compute":
                errorRate = 0.03; // 3% - Compute can have resource issues
                break;
        }
        
        // Certain endpoints are more prone to errors
        if (endpoint.contains("delete") || endpoint.contains("update")) {
            errorRate *= 1.5; // 50% higher error rate for destructive operations
        }
        
        if (rand < errorRate) {
            // Return realistic error codes
            double errorRand = random.nextDouble();
            if (errorRand < 0.6) return 400;      // 60% - Bad Request
            if (errorRand < 0.8) return 404;      // 20% - Not Found
            if (errorRand < 0.9) return 500;      // 10% - Internal Server Error
            return 503;                           // 10% - Service Unavailable
        }
        
        return 200; // Success
    }
    
    /**
     * Get real CPU usage
     */
    private double getRealCpuUsage() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean = (com.sun.management.OperatingSystemMXBean) osBean;
                return Math.round(sunOsBean.getCpuLoad() * 100.0 * 100.0) / 100.0;
            }
        } catch (Exception e) {
            // Fallback to realistic simulation
        }
        return 15.0 + random.nextDouble() * 45.0; // 15-60%
    }
    
    /**
     * Get real RAM usage
     */
    private double getRealRamUsage() {
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
            return Math.round((double) usedMemory / (1024 * 1024) * 100.0) / 100.0;
        } catch (Exception e) {
            // Fallback to realistic simulation
        }
        return 80.0 + random.nextDouble() * 320.0; // 80-400 MB
    }
    
    /**
     * Estimate request size based on endpoint
     */
    private long estimateRequestSize(String endpoint) {
        if (endpoint.contains("upload") || endpoint.contains("create")) {
            return 1000 + random.nextInt(5000); // Larger for uploads/creates
        } else if (endpoint.contains("download") || endpoint.contains("get")) {
            return 200 + random.nextInt(300); // Smaller for downloads/gets
        } else {
            return 300 + random.nextInt(700); // Medium for others
        }
    }
    
    /**
     * Estimate response size based on status code
     */
    private long estimateResponseSize(Integer statusCode) {
        if (statusCode >= 400) {
            return 100 + random.nextInt(200); // Error responses are small
        } else {
            return 200 + random.nextInt(800); // Success responses vary
        }
    }
    
    /**
     * Helper class for service patterns
     */
    private static class ServicePattern {
        private final String[] endpoints;
        private final int minResponseTime;
        private final int maxResponseTime;
        
        public ServicePattern(String... endpoints) {
            this.endpoints = endpoints;
            this.minResponseTime = 50;
            this.maxResponseTime = 500;
        }
        
        public ServicePattern(String endpoint1, String endpoint2, String endpoint3, String endpoint4, 
                            int minResponseTime, int maxResponseTime) {
            this.endpoints = new String[]{endpoint1, endpoint2, endpoint3, endpoint4};
            this.minResponseTime = minResponseTime;
            this.maxResponseTime = maxResponseTime;
        }
        
        public String getRandomEndpoint() {
            return endpoints[new Random().nextInt(endpoints.length)];
        }
        
        public Long getRandomResponseTime() {
            return (long) (minResponseTime + new Random().nextDouble() * (maxResponseTime - minResponseTime));
        }
    }
} 