package com.open.unicorn;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/billing")
@CrossOrigin(origins = "*")
public class UWSBillingController {
    
    @Autowired
    private BillingService billingService;
    
    @Autowired
    private UserRepository userRepository;
    
    // Get billing summary
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getBillingSummary(
            @RequestParam(defaultValue = "30days") String timeRange,
            HttpServletRequest request) {
        
        Long userId = extractUserIdFromRequest(request);
        LocalDateTime startTime = getStartTimeFromRange(timeRange);
        LocalDateTime endTime = LocalDateTime.now();
        
        Map<String, Object> summary = billingService.getBillingSummary(userId, startTime, endTime);
        return ResponseEntity.ok(summary);
    }
    
    // Get monthly billing data
    @GetMapping("/monthly")
    public ResponseEntity<Map<String, Object>> getMonthlyBilling(
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "0") int month,
            HttpServletRequest request) {
        
        Long userId = extractUserIdFromRequest(request);
        
        // If year/month not provided, use current month
        if (year == 0 || month == 0) {
            YearMonth current = YearMonth.now();
            year = current.getYear();
            month = current.getMonthValue();
        }
        
        Map<String, Object> monthlyData = billingService.getMonthlyBilling(userId, year, month);
        return ResponseEntity.ok(monthlyData);
    }
    
    // Get cost trends
    @GetMapping("/trends")
    public ResponseEntity<List<Object[]>> getCostTrends(
            @RequestParam(defaultValue = "24hours") String timeRange,
            HttpServletRequest request) {
        
        Long userId = extractUserIdFromRequest(request);
        LocalDateTime startTime = getStartTimeFromRange(timeRange);
        
        List<Object[]> trends = billingService.getCostTrends(userId, startTime);
        return ResponseEntity.ok(trends);
    }
    
    // Get pricing information
    @GetMapping("/pricing")
    public ResponseEntity<Map<String, Object>> getPricingInfo() {
        Map<String, Object> pricingInfo = new HashMap<>();
        
        // Add pricing model information (updated for better visibility in testing)
        pricingInfo.put("UWS-S3", Map.of(
            "storage_gb", "$0.023 per GB per month",
            "request_put", "$0.05 per PUT request",
            "request_get", "$0.04 per GET request",
            "data_transfer_out", "$0.09 per GB"
        ));
        
        pricingInfo.put("UWS-Lambda", Map.of(
            "request", "$0.02 per request",
            "duration_ms", "$0.00021 per 100ms",
            "memory_gb", "$0.00166667 per GB-second"
        ));
        
        pricingInfo.put("UWS-Compute", Map.of(
            "micro_hour", "$0.008 per hour",
            "small_hour", "$0.016 per hour",
            "medium_hour", "$0.032 per hour",
            "storage_gb", "$0.10 per GB per month"
        ));
        
        pricingInfo.put("RDB", Map.of(
            "instance_hour", "$0.017 per hour",
            "storage_gb", "$0.115 per GB per month",
            "backup_gb", "$0.095 per GB per month"
        ));
        
        pricingInfo.put("NoSQL", Map.of(
            "read_capacity_unit", "$0.025 per RCU",
            "write_capacity_unit", "$0.025 per WCU",
            "storage_gb", "$0.25 per GB per month"
        ));
        
        pricingInfo.put("SQS", Map.of(
            "request", "$0.04 per request",
            "message", "$0.04 per message"
        ));
        
        pricingInfo.put("Secrets Manager", Map.of(
            "secret", "$0.40 per secret per month",
            "api_call", "$0.005 per API call"
        ));
        
        pricingInfo.put("AI Service", Map.of(
            "request", "$0.01 per request",
            "token", "$0.0002 per token",
            "model_hour", "$0.10 per hour for model usage"
        ));
        
        pricingInfo.put("DNS Service", Map.of(
            "hosted_zone", "$0.50 per hosted zone per month",
            "query", "$0.04 per query"
        ));
        
        return ResponseEntity.ok(pricingInfo);
    }
    
    // Create billing alert
    @PostMapping("/alerts")
    public ResponseEntity<Map<String, Object>> createBillingAlert(
            @RequestBody Map<String, Object> alertData,
            HttpServletRequest request) {
        
        Long userId = extractUserIdFromRequest(request);
        
        try {
            String alertName = (String) alertData.get("alertName");
            String alertType = (String) alertData.get("alertType");
            BigDecimal thresholdValue = new BigDecimal(alertData.get("thresholdValue").toString());
            String serviceName = (String) alertData.get("serviceName");
            String timePeriod = (String) alertData.get("timePeriod");
            Boolean emailNotification = (Boolean) alertData.get("emailNotification");
            String webhookUrl = (String) alertData.get("webhookUrl");
            
            BillingAlert alert = billingService.createBillingAlert(userId, alertName, alertType, thresholdValue,
                                                                 serviceName, timePeriod, emailNotification, webhookUrl);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("alert", alert);
            response.put("message", "Billing alert created successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Failed to create billing alert: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Get user alerts
    @GetMapping("/alerts")
    public ResponseEntity<List<BillingAlert>> getUserAlerts(HttpServletRequest request) {
        Long userId = extractUserIdFromRequest(request);
        List<BillingAlert> alerts = billingService.getUserAlerts(userId);
        return ResponseEntity.ok(alerts);
    }
    
    // Generate sample billing data
    @PostMapping("/generate-sample-data")
    public ResponseEntity<Map<String, Object>> generateSampleData(HttpServletRequest request) {
        Long userId = extractUserIdFromRequest(request);
        
        try {
            billingService.generateSampleBillingData(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Sample billing data generated successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Failed to generate sample data: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Test endpoint to verify billing integration
    @PostMapping("/test-billing")
    public ResponseEntity<Map<String, Object>> testBilling(HttpServletRequest request) {
        Long userId = extractUserIdFromRequest(request);

        try {
            // This will trigger the billing aspect and record a test event
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Test billing event recorded");
            response.put("userId", userId);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "Failed to test billing: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Helper method to extract user ID from JWT token
    private Long extractUserIdFromRequest(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                
                String userEmail = io.jsonwebtoken.Jwts.parserBuilder()
                    .setSigningKey(UnicornApplication.getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
                
                User user = userRepository.findByEmail(userEmail);
                if (user != null) {
                    return user.getId();
                }
            }
        } catch (Exception e) {
            System.err.println("Error extracting user ID from JWT: " + e.getMessage());
        }
        return 1L; // Fallback
    }
    
    // Helper method to get start time from time range
    private LocalDateTime getStartTimeFromRange(String timeRange) {
        LocalDateTime now = LocalDateTime.now();
        switch (timeRange.toLowerCase()) {
            case "1hour":
                return now.minusHours(1);
            case "24hours":
                return now.minusHours(24);
            case "7days":
                return now.minusDays(7);
            case "30days":
                return now.minusDays(30);
            case "90days":
                return now.minusDays(90);
            default:
                return now.minusDays(30);
        }
    }
} 