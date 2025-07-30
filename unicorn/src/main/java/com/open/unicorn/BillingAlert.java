package com.open.unicorn;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "billing_alerts")
public class BillingAlert {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "alert_name", nullable = false)
    private String alertName;
    
    @Column(name = "alert_type", nullable = false)
    private String alertType; // SPENDING_LIMIT, COST_THRESHOLD, USAGE_THRESHOLD
    
    @Column(name = "threshold_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal thresholdValue;
    
    @Column(name = "current_value", precision = 10, scale = 2)
    private BigDecimal currentValue;
    
    @Column(name = "service_name")
    private String serviceName; // null for all services
    
    @Column(name = "time_period", nullable = false)
    private String timePeriod; // daily, weekly, monthly
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "email_notification", nullable = false)
    private Boolean emailNotification = false;
    
    @Column(name = "webhook_url")
    private String webhookUrl;
    
    @Column(name = "last_triggered_at")
    private LocalDateTime lastTriggeredAt;
    
    @Column(name = "trigger_count")
    private Integer triggerCount = 0;
    
    @Column(name = "status", nullable = false)
    private String status = "ACTIVE"; // ACTIVE, TRIGGERED, DISABLED
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Constructors
    public BillingAlert() {}
    
    public BillingAlert(Long userId, String alertName, String alertType, BigDecimal thresholdValue,
                       String serviceName, String timePeriod, Boolean emailNotification, String webhookUrl) {
        this.userId = userId;
        this.alertName = alertName;
        this.alertType = alertType;
        this.thresholdValue = thresholdValue;
        this.serviceName = serviceName;
        this.timePeriod = timePeriod;
        this.emailNotification = emailNotification;
        this.webhookUrl = webhookUrl;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Helper methods
    public boolean shouldTrigger(BigDecimal currentValue) {
        if (!isActive || "DISABLED".equals(status)) {
            return false;
        }
        
        this.currentValue = currentValue;
        this.updatedAt = LocalDateTime.now();
        
        return currentValue.compareTo(thresholdValue) >= 0;
    }
    
    public void trigger() {
        this.lastTriggeredAt = LocalDateTime.now();
        this.triggerCount++;
        this.status = "TRIGGERED";
        this.updatedAt = LocalDateTime.now();
    }
    
    public void reset() {
        this.status = "ACTIVE";
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getAlertName() { return alertName; }
    public void setAlertName(String alertName) { this.alertName = alertName; }
    
    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
    
    public BigDecimal getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(BigDecimal thresholdValue) { this.thresholdValue = thresholdValue; }
    
    public BigDecimal getCurrentValue() { return currentValue; }
    public void setCurrentValue(BigDecimal currentValue) { this.currentValue = currentValue; }
    
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    
    public String getTimePeriod() { return timePeriod; }
    public void setTimePeriod(String timePeriod) { this.timePeriod = timePeriod; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public Boolean getEmailNotification() { return emailNotification; }
    public void setEmailNotification(Boolean emailNotification) { this.emailNotification = emailNotification; }
    
    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
    
    public LocalDateTime getLastTriggeredAt() { return lastTriggeredAt; }
    public void setLastTriggeredAt(LocalDateTime lastTriggeredAt) { this.lastTriggeredAt = lastTriggeredAt; }
    
    public Integer getTriggerCount() { return triggerCount; }
    public void setTriggerCount(Integer triggerCount) { this.triggerCount = triggerCount; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
} 