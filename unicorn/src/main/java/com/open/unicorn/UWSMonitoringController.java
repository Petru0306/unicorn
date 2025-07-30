package com.open.unicorn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitoring")
@CrossOrigin(origins = "*")
public class UWSMonitoringController {
    
    @Autowired
    private MonitoringService monitoringService;
    
    @Autowired
    private MonitoringAlertRepository monitoringAlertRepository;
    
    @Autowired
    private MonitoringEventRepository monitoringEventRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Get monitoring dashboard data
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics(
            @RequestParam(defaultValue = "24hours") String timeRange,
            HttpServletRequest request) {
        
        try {
            // Extract user ID from JWT token (simplified for demo)
            Long userId = extractUserIdFromRequest(request);
            
            Map<String, Object> dashboardData = monitoringService.getDashboardData(userId, timeRange);
            return ResponseEntity.ok(dashboardData);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to fetch monitoring metrics: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * Get real-time metrics for all services
     */
    @GetMapping("/realtime")
    public ResponseEntity<Map<String, Object>> getRealTimeMetrics(HttpServletRequest request) {
        try {
            Long userId = extractUserIdFromRequest(request);
            Map<String, Object> realTimeData = monitoringService.getAllRealTimeMetrics(userId);
            return ResponseEntity.ok(realTimeData);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to fetch real-time metrics: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * Get real-time metrics for a specific service
     */
    @GetMapping("/realtime/{serviceName}")
    public ResponseEntity<Map<String, Object>> getServiceRealTimeMetrics(
            @PathVariable String serviceName,
            HttpServletRequest request) {
        
        try {
            Long userId = extractUserIdFromRequest(request);
            Map<String, Object> serviceMetrics = monitoringService.getServiceMetrics(userId, serviceName);
            return ResponseEntity.ok(serviceMetrics);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to fetch service metrics: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * Create a new alert
     */
    @PostMapping("/alerts")
    public ResponseEntity<Map<String, Object>> createAlert(
            @RequestBody Map<String, Object> alertData,
            HttpServletRequest request) {
        
        try {
            Long userId = extractUserIdFromRequest(request);
            
            String alertName = (String) alertData.get("alertName");
            String serviceName = (String) alertData.get("serviceName");
            String metricType = (String) alertData.get("metricType");
            Double thresholdValue = Double.valueOf(alertData.get("thresholdValue").toString());
            String thresholdOperator = (String) alertData.get("thresholdOperator");
            Boolean emailNotification = (Boolean) alertData.get("emailNotification");
            String webhookUrl = (String) alertData.get("webhookUrl");
            
            MonitoringAlert alert = new MonitoringAlert(userId, alertName, serviceName, metricType, 
                                                      thresholdValue, thresholdOperator);
            
            if (emailNotification != null) {
                alert.setEmailNotification(emailNotification);
            }
            
            if (webhookUrl != null && !webhookUrl.isEmpty()) {
                alert.setWebhookUrl(webhookUrl);
            }
            
            MonitoringAlert savedAlert = monitoringAlertRepository.save(alert);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Alert created successfully");
            response.put("alert", savedAlert);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to create alert: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * Get all alerts for the user
     */
    @GetMapping("/alerts")
    public ResponseEntity<Map<String, Object>> getAlerts(HttpServletRequest request) {
        try {
            Long userId = extractUserIdFromRequest(request);
            
            List<MonitoringAlert> alerts = monitoringAlertRepository.findByUserIdOrderByCreatedAtDesc(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("alerts", alerts);
            response.put("totalAlerts", alerts.size());
            response.put("activeAlerts", alerts.stream().filter(a -> a.getIsActive()).count());
            response.put("triggeredAlerts", alerts.stream().filter(a -> "CRITICAL".equals(a.getStatus())).count());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to fetch alerts: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * Update an alert
     */
    @PutMapping("/alerts/{alertId}")
    public ResponseEntity<Map<String, Object>> updateAlert(
            @PathVariable Long alertId,
            @RequestBody Map<String, Object> alertData,
            HttpServletRequest request) {
        
        try {
            Long userId = extractUserIdFromRequest(request);
            
            MonitoringAlert alert = monitoringAlertRepository.findById(alertId).orElse(null);
            if (alert == null || !alert.getUserId().equals(userId)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Alert not found or access denied");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Update fields
            if (alertData.containsKey("alertName")) {
                alert.setAlertName((String) alertData.get("alertName"));
            }
            if (alertData.containsKey("serviceName")) {
                alert.setServiceName((String) alertData.get("serviceName"));
            }
            if (alertData.containsKey("metricType")) {
                alert.setMetricType((String) alertData.get("metricType"));
            }
            if (alertData.containsKey("thresholdValue")) {
                alert.setThresholdValue(Double.valueOf(alertData.get("thresholdValue").toString()));
            }
            if (alertData.containsKey("thresholdOperator")) {
                alert.setThresholdOperator((String) alertData.get("thresholdOperator"));
            }
            if (alertData.containsKey("isActive")) {
                alert.setIsActive((Boolean) alertData.get("isActive"));
            }
            if (alertData.containsKey("emailNotification")) {
                alert.setEmailNotification((Boolean) alertData.get("emailNotification"));
            }
            if (alertData.containsKey("webhookUrl")) {
                alert.setWebhookUrl((String) alertData.get("webhookUrl"));
            }
            
            alert.setUpdatedAt(java.time.LocalDateTime.now());
            MonitoringAlert savedAlert = monitoringAlertRepository.save(alert);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Alert updated successfully");
            response.put("alert", savedAlert);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to update alert: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * Delete an alert
     */
    @DeleteMapping("/alerts/{alertId}")
    public ResponseEntity<Map<String, Object>> deleteAlert(
            @PathVariable Long alertId,
            HttpServletRequest request) {
        
        try {
            Long userId = extractUserIdFromRequest(request);
            
            MonitoringAlert alert = monitoringAlertRepository.findById(alertId).orElse(null);
            if (alert == null || !alert.getUserId().equals(userId)) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Alert not found or access denied");
                return ResponseEntity.badRequest().body(error);
            }
            
            monitoringAlertRepository.delete(alert);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Alert deleted successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to delete alert: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * Get monitoring logs/events
     */
    @GetMapping("/logs")
    public ResponseEntity<Map<String, Object>> getLogs(
            @RequestParam(defaultValue = "24hours") String timeRange,
            @RequestParam(required = false) String serviceName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            HttpServletRequest request) {
        
        try {
            Long userId = extractUserIdFromRequest(request);
            
            // For simplicity, we'll return recent events without pagination
            List<MonitoringEvent> events;
            if (serviceName != null && !serviceName.isEmpty()) {
                events = monitoringEventRepository.findByUserIdAndServiceNameOrderByRequestTimestampDesc(userId, serviceName);
            } else {
                events = monitoringEventRepository.findByUserIdAndRequestTimestampBetweenOrderByRequestTimestampDesc(
                    userId, getStartTimeForRange(timeRange), java.time.LocalDateTime.now());
            }
            
            // Apply simple pagination
            int start = page * size;
            int end = Math.min(start + size, events.size());
            List<MonitoringEvent> paginatedEvents = events.subList(start, end);
            
            Map<String, Object> response = new HashMap<>();
            response.put("events", paginatedEvents);
            response.put("totalEvents", events.size());
            response.put("page", page);
            response.put("size", size);
            response.put("hasMore", end < events.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to fetch logs: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * Generate sample data for testing
     */
    @PostMapping("/generate-sample-data")
    public ResponseEntity<Map<String, Object>> generateSampleData(HttpServletRequest request) {
        try {
            Long userId = extractUserIdFromRequest(request);
            monitoringService.generateSampleData(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Sample data generated successfully");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to generate sample data: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * Export metrics as CSV
     */
    @GetMapping("/export")
    public ResponseEntity<String> exportMetrics(
            @RequestParam(defaultValue = "24hours") String timeRange,
            HttpServletRequest request) {
        
        try {
            Long userId = extractUserIdFromRequest(request);
            
            List<MonitoringEvent> events = monitoringEventRepository.findByUserIdAndRequestTimestampBetweenOrderByRequestTimestampDesc(
                userId, getStartTimeForRange(timeRange), java.time.LocalDateTime.now());
            
            StringBuilder csv = new StringBuilder();
            csv.append("Timestamp,Service,Endpoint,Method,ResponseTime(ms),CPU(%),RAM(MB),StatusCode\n");
            
            for (MonitoringEvent event : events) {
                csv.append(String.format("%s,%s,%s,%s,%d,%.2f,%.2f,%d\n",
                    event.getRequestTimestamp(),
                    event.getServiceName(),
                    event.getEndpointName(),
                    event.getHttpMethod(),
                    event.getResponseTimeMs(),
                    event.getCpuUsagePercent(),
                    event.getRamUsageMb(),
                    event.getStatusCode()));
            }
            
            return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=monitoring_metrics.csv")
                .body(csv.toString());
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to export metrics: " + e.getMessage());
        }
    }
    
    /**
     * Extract user ID from JWT token in request
     */
    private Long extractUserIdFromRequest(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                
                // Parse JWT token to get user email
                String userEmail = io.jsonwebtoken.Jwts.parserBuilder()
                    .setSigningKey(com.open.unicorn.UnicornApplication.getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
                
                // Find user by email
                User user = userRepository.findByEmail(userEmail);
                if (user != null) {
                    return user.getId();
                }
            }
        } catch (Exception e) {
            System.err.println("Error extracting user ID from JWT: " + e.getMessage());
        }
        
        // Fallback to default user ID if extraction fails
        return 1L;
    }
    
    /**
     * Get start time based on time range
     */
    private java.time.LocalDateTime getStartTimeForRange(String timeRange) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
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
                return now.minusDays(1);
        }
    }
} 