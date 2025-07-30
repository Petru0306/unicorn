package com.open.unicorn;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "monitoring_events")
public class MonitoringEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "service_name", nullable = false)
    private String serviceName;
    
    @Column(name = "endpoint_name", nullable = false)
    private String endpointName;
    
    @Column(name = "http_method")
    private String httpMethod;
    
    @Column(name = "request_timestamp", nullable = false)
    private LocalDateTime requestTimestamp;
    
    @Column(name = "response_time_ms")
    private Long responseTimeMs;
    
    @Column(name = "cpu_usage_percent")
    private Double cpuUsagePercent;
    
    @Column(name = "ram_usage_mb")
    private Double ramUsageMb;
    
    @Column(name = "status_code")
    private Integer statusCode;
    
    @Column(name = "request_size_bytes")
    private Long requestSizeBytes;
    
    @Column(name = "response_size_bytes")
    private Long responseSizeBytes;
    
    @Column(name = "user_agent")
    private String userAgent;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    // Constructors
    public MonitoringEvent() {}
    
    public MonitoringEvent(Long userId, String serviceName, String endpointName, String httpMethod, 
                          LocalDateTime requestTimestamp, Long responseTimeMs, Double cpuUsagePercent, 
                          Double ramUsageMb, Integer statusCode) {
        this.userId = userId;
        this.serviceName = serviceName;
        this.endpointName = endpointName;
        this.httpMethod = httpMethod;
        this.requestTimestamp = requestTimestamp;
        this.responseTimeMs = responseTimeMs;
        this.cpuUsagePercent = cpuUsagePercent;
        this.ramUsageMb = ramUsageMb;
        this.statusCode = statusCode;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
    
    public String getEndpointName() {
        return endpointName;
    }
    
    public void setEndpointName(String endpointName) {
        this.endpointName = endpointName;
    }
    
    public String getHttpMethod() {
        return httpMethod;
    }
    
    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }
    
    public LocalDateTime getRequestTimestamp() {
        return requestTimestamp;
    }
    
    public void setRequestTimestamp(LocalDateTime requestTimestamp) {
        this.requestTimestamp = requestTimestamp;
    }
    
    public Long getResponseTimeMs() {
        return responseTimeMs;
    }
    
    public void setResponseTimeMs(Long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }
    
    public Double getCpuUsagePercent() {
        return cpuUsagePercent;
    }
    
    public void setCpuUsagePercent(Double cpuUsagePercent) {
        this.cpuUsagePercent = cpuUsagePercent;
    }
    
    public Double getRamUsageMb() {
        return ramUsageMb;
    }
    
    public void setRamUsageMb(Double ramUsageMb) {
        this.ramUsageMb = ramUsageMb;
    }
    
    public Integer getStatusCode() {
        return statusCode;
    }
    
    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }
    
    public Long getRequestSizeBytes() {
        return requestSizeBytes;
    }
    
    public void setRequestSizeBytes(Long requestSizeBytes) {
        this.requestSizeBytes = requestSizeBytes;
    }
    
    public Long getResponseSizeBytes() {
        return responseSizeBytes;
    }
    
    public void setResponseSizeBytes(Long responseSizeBytes) {
        this.responseSizeBytes = responseSizeBytes;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
} 