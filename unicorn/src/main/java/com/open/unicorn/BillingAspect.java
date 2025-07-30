package com.open.unicorn;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Map;

@Aspect
@Component
public class BillingAspect {
    
    @Autowired
    private BillingService billingService;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Record billing events for all REST controller methods
     */
    @Around("@annotation(org.springframework.web.bind.annotation.RequestMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.GetMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PatchMapping)")
    public Object recordBillingEvent(ProceedingJoinPoint joinPoint) throws Throwable {
        
        String serviceName = "Unknown";
        String resourceType = "api_request";
        String resourceId = "unknown";
        Double usageQuantity = 1.0;
        String usageUnit = "request";
        String description = "API request";
        
        try {
            // Extract request information
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                
                // Determine service name from controller class
                String className = joinPoint.getTarget().getClass().getSimpleName();
                serviceName = determineServiceName(className);
                
                // Get endpoint name for resource ID
                String endpointName = joinPoint.getSignature().getName();
                resourceId = endpointName;
                
                // Determine billing details based on service and operation
                Map<String, Object> billingDetails = determineBillingDetails(serviceName, endpointName, request, joinPoint.getArgs());
                resourceType = (String) billingDetails.get("resourceType");
                usageQuantity = (Double) billingDetails.get("usageQuantity");
                usageUnit = (String) billingDetails.get("usageUnit");
                description = (String) billingDetails.get("description");
            }
            
            // Execute the method
            Object result = joinPoint.proceed();
            
            // Record billing event
            recordBillingEvent(serviceName, resourceType, resourceId, usageQuantity, usageUnit, description);
            
            return result;
            
        } catch (Exception e) {
            // Record billing event even for errors (failed requests still cost)
            recordBillingEvent(serviceName, resourceType, resourceId, usageQuantity, usageUnit, description + " (failed)");
            throw e;
        }
    }
    
    private String determineServiceName(String className) {
        if (className.contains("UWSS3Controller")) return "UWS-S3";
        if (className.contains("UWSLambdaController")) return "UWS-Lambda";
        if (className.contains("UWSComputeController")) return "UWS-Compute";
        if (className.contains("UWSRDBController")) return "RDB";
        if (className.contains("UWSNoSQLController")) return "NoSQL";
        if (className.contains("UWSSQSController")) return "SQS";
        if (className.contains("UWSSecretsController")) return "Secrets Manager";
        if (className.contains("UWSAIController")) return "AI Service";
        if (className.contains("UWSDNSController")) return "DNS Service";
        return "Unknown";
    }
    
    private Map<String, Object> determineBillingDetails(String serviceName, String endpointName, HttpServletRequest request, Object[] args) {
        Map<String, Object> details = Map.of(
            "resourceType", "api_request",
            "usageQuantity", 1.0,
            "usageUnit", "request",
            "description", serviceName + " - " + endpointName
        );
        
        // S3 specific billing
        if ("UWS-S3".equals(serviceName)) {
            if (endpointName.contains("upload") || endpointName.contains("create")) {
                return Map.of(
                    "resourceType", "request_put",
                    "usageQuantity", 1.0,
                    "usageUnit", "request",
                    "description", "S3 PUT request - " + endpointName
                );
            } else if (endpointName.contains("download") || endpointName.contains("get") || endpointName.contains("list")) {
                return Map.of(
                    "resourceType", "request_get",
                    "usageQuantity", 1.0,
                    "usageUnit", "request",
                    "description", "S3 GET request - " + endpointName
                );
            }
        }
        
        // Lambda specific billing
        if ("UWS-Lambda".equals(serviceName)) {
            if (endpointName.contains("execute") || endpointName.contains("invoke")) {
                return Map.of(
                    "resourceType", "request",
                    "usageQuantity", 1.0,
                    "usageUnit", "request",
                    "description", "Lambda execution - " + endpointName
                );
            }
        }
        
        // Compute specific billing
        if ("UWS-Compute".equals(serviceName)) {
            if (endpointName.contains("create") || endpointName.contains("start")) {
                return Map.of(
                    "resourceType", "micro_hour",
                    "usageQuantity", 0.1, // 6 minutes of compute time
                    "usageUnit", "hour",
                    "description", "Compute instance creation - " + endpointName
                );
            }
        }
        
        // RDB specific billing
        if ("RDB".equals(serviceName)) {
            if (endpointName.contains("create") || endpointName.contains("start")) {
                return Map.of(
                    "resourceType", "instance_hour",
                    "usageQuantity", 0.1, // 6 minutes of instance time
                    "usageUnit", "hour",
                    "description", "RDB instance operation - " + endpointName
                );
            }
        }
        
        // NoSQL specific billing
        if ("NoSQL".equals(serviceName)) {
            if (endpointName.contains("read") || endpointName.contains("get")) {
                return Map.of(
                    "resourceType", "read_capacity_unit",
                    "usageQuantity", 1.0,
                    "usageUnit", "unit",
                    "description", "NoSQL read operation - " + endpointName
                );
            } else if (endpointName.contains("write") || endpointName.contains("create") || endpointName.contains("update")) {
                return Map.of(
                    "resourceType", "write_capacity_unit",
                    "usageQuantity", 1.0,
                    "usageUnit", "unit",
                    "description", "NoSQL write operation - " + endpointName
                );
            }
        }
        
        // SQS specific billing
        if ("SQS".equals(serviceName)) {
            if (endpointName.contains("send") || endpointName.contains("publish")) {
                return Map.of(
                    "resourceType", "message",
                    "usageQuantity", 1.0,
                    "usageUnit", "message",
                    "description", "SQS message send - " + endpointName
                );
            } else if (endpointName.contains("receive") || endpointName.contains("consume")) {
                return Map.of(
                    "resourceType", "request",
                    "usageQuantity", 1.0,
                    "usageUnit", "request",
                    "description", "SQS message receive - " + endpointName
                );
            }
        }
        
        // Secrets Manager specific billing
        if ("Secrets Manager".equals(serviceName)) {
            return Map.of(
                "resourceType", "api_call",
                "usageQuantity", 1.0,
                "usageUnit", "call",
                "description", "Secrets Manager API call - " + endpointName
            );
        }
        
        // AI Service specific billing
        if ("AI Service".equals(serviceName)) {
            return Map.of(
                "resourceType", "request",
                "usageQuantity", 1.0,
                "usageUnit", "request",
                "description", "AI Service request - " + endpointName
            );
        }
        
        // DNS Service specific billing
        if ("DNS Service".equals(serviceName)) {
            if (endpointName.contains("query") || endpointName.contains("resolve")) {
                return Map.of(
                    "resourceType", "query",
                    "usageQuantity", 1.0,
                    "usageUnit", "query",
                    "description", "DNS query - " + endpointName
                );
            }
        }
        
        return details;
    }
    
    private void recordBillingEvent(String serviceName, String resourceType, String resourceId, 
                                  Double usageQuantity, String usageUnit, String description) {
        try {
            Long userId = extractUserIdFromAuthentication();
            if (userId != null) {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime periodStart = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
                LocalDateTime periodEnd = periodStart.plusDays(1);
                
                billingService.recordBillingEvent(userId, serviceName, resourceType, resourceId,
                                                usageQuantity, usageUnit, periodStart, periodEnd, description);
            }
        } catch (Exception e) {
            // Log error but don't fail the main operation
            System.err.println("Failed to record billing event: " + e.getMessage());
        }
    }
    
    private Long extractUserIdFromAuthentication() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                String userEmail = authentication.getName();
                User user = userRepository.findByEmail(userEmail);
                return user != null ? user.getId() : null;
            }
        } catch (Exception e) {
            System.err.println("Failed to extract user ID: " + e.getMessage());
        }
        return null;
    }
} 