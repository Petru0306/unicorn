package com.open.unicorn;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "monitoring_alerts")
public class MonitoringAlert {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "alert_name", nullable = false)
    private String alertName;
    
    @Column(name = "service_name")
    private String serviceName;
    
    @Column(name = "metric_type", nullable = false)
    private String metricType; // response_time, cpu_usage, ram_usage, error_rate
    
    @Column(name = "threshold_value", nullable = false)
    private Double thresholdValue;
    
    @Column(name = "threshold_operator", nullable = false)
    private String thresholdOperator; // >, <, >=, <=, ==
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "email_notification")
    private Boolean emailNotification = false;
    
    @Column(name = "webhook_url")
    private String webhookUrl;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Alert trigger fields
    @Column(name = "last_triggered_at")
    private LocalDateTime lastTriggeredAt;
    
    @Column(name = "trigger_count")
    private Integer triggerCount = 0;
    
    @Column(name = "current_value")
    private Double currentValue;
    
    @Column(name = "status")
    private String status = "OK"; // OK, WARNING, CRITICAL
    
    // Constructors
    public MonitoringAlert() {
        this.createdAt = LocalDateTime.now();
    }
    
    public MonitoringAlert(Long userId, String alertName, String serviceName, String metricType, 
                          Double thresholdValue, String thresholdOperator) {
        this();
        this.userId = userId;
        this.alertName = alertName;
        this.serviceName = serviceName;
        this.metricType = metricType;
        this.thresholdValue = thresholdValue;
        this.thresholdOperator = thresholdOperator;
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
    
    public String getAlertName() {
        return alertName;
    }
    
    public void setAlertName(String alertName) {
        this.alertName = alertName;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
    
    public String getMetricType() {
        return metricType;
    }
    
    public void setMetricType(String metricType) {
        this.metricType = metricType;
    }
    
    public Double getThresholdValue() {
        return thresholdValue;
    }
    
    public void setThresholdValue(Double thresholdValue) {
        this.thresholdValue = thresholdValue;
    }
    
    public String getThresholdOperator() {
        return thresholdOperator;
    }
    
    public void setThresholdOperator(String thresholdOperator) {
        this.thresholdOperator = thresholdOperator;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public Boolean getEmailNotification() {
        return emailNotification;
    }
    
    public void setEmailNotification(Boolean emailNotification) {
        this.emailNotification = emailNotification;
    }
    
    public String getWebhookUrl() {
        return webhookUrl;
    }
    
    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public LocalDateTime getLastTriggeredAt() {
        return lastTriggeredAt;
    }
    
    public void setLastTriggeredAt(LocalDateTime lastTriggeredAt) {
        this.lastTriggeredAt = lastTriggeredAt;
    }
    
    public Integer getTriggerCount() {
        return triggerCount;
    }
    
    public void setTriggerCount(Integer triggerCount) {
        this.triggerCount = triggerCount;
    }
    
    public Double getCurrentValue() {
        return currentValue;
    }
    
    public void setCurrentValue(Double currentValue) {
        this.currentValue = currentValue;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    // Helper method to check if alert should be triggered
    public boolean shouldTrigger(Double currentValue) {
        if (!isActive) return false;
        
        switch (thresholdOperator) {
            case ">":
                return currentValue > thresholdValue;
            case "<":
                return currentValue < thresholdValue;
            case ">=":
                return currentValue >= thresholdValue;
            case "<=":
                return currentValue <= thresholdValue;
            case "==":
                return currentValue.equals(thresholdValue);
            default:
                return false;
        }
    }
    
    // Helper method to trigger alert
    public void trigger(Double currentValue) {
        this.currentValue = currentValue;
        this.lastTriggeredAt = LocalDateTime.now();
        this.triggerCount++;
        this.status = "CRITICAL";
        this.updatedAt = LocalDateTime.now();
    }
    
    // Helper method to reset alert
    public void reset() {
        this.status = "OK";
        this.updatedAt = LocalDateTime.now();
    }
} 