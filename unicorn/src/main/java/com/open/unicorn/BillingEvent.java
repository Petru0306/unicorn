package com.open.unicorn;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "billing_events")
public class BillingEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "service_name", nullable = false)
    private String serviceName;
    
    @Column(name = "resource_type", nullable = false)
    private String resourceType;
    
    @Column(name = "resource_id")
    private String resourceId;
    
    @Column(name = "usage_quantity", nullable = false)
    private Double usageQuantity;
    
    @Column(name = "usage_unit", nullable = false)
    private String usageUnit; // requests, hours, GB, etc.
    
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 6)
    private BigDecimal unitPrice;
    
    @Column(name = "total_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalCost;
    
    @Column(name = "billing_period_start", nullable = false)
    private LocalDateTime billingPeriodStart;
    
    @Column(name = "billing_period_end", nullable = false)
    private LocalDateTime billingPeriodEnd;
    
    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime eventTimestamp;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON string for additional data
    
    // Constructors
    public BillingEvent() {}
    
    public BillingEvent(Long userId, String serviceName, String resourceType, String resourceId,
                       Double usageQuantity, String usageUnit, BigDecimal unitPrice, 
                       LocalDateTime billingPeriodStart, LocalDateTime billingPeriodEnd, String description) {
        this.userId = userId;
        this.serviceName = serviceName;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.usageQuantity = usageQuantity;
        this.usageUnit = usageUnit;
        this.unitPrice = unitPrice;
        this.billingPeriodStart = billingPeriodStart;
        this.billingPeriodEnd = billingPeriodEnd;
        this.eventTimestamp = LocalDateTime.now();
        this.description = description;
        this.totalCost = unitPrice.multiply(BigDecimal.valueOf(usageQuantity));
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    
    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    
    public Double getUsageQuantity() { return usageQuantity; }
    public void setUsageQuantity(Double usageQuantity) { 
        this.usageQuantity = usageQuantity;
        this.totalCost = unitPrice.multiply(BigDecimal.valueOf(usageQuantity));
    }
    
    public String getUsageUnit() { return usageUnit; }
    public void setUsageUnit(String usageUnit) { this.usageUnit = usageUnit; }
    
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { 
        this.unitPrice = unitPrice;
        this.totalCost = unitPrice.multiply(BigDecimal.valueOf(usageQuantity));
    }
    
    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    
    public LocalDateTime getBillingPeriodStart() { return billingPeriodStart; }
    public void setBillingPeriodStart(LocalDateTime billingPeriodStart) { this.billingPeriodStart = billingPeriodStart; }
    
    public LocalDateTime getBillingPeriodEnd() { return billingPeriodEnd; }
    public void setBillingPeriodEnd(LocalDateTime billingPeriodEnd) { this.billingPeriodEnd = billingPeriodEnd; }
    
    public LocalDateTime getEventTimestamp() { return eventTimestamp; }
    public void setEventTimestamp(LocalDateTime eventTimestamp) { this.eventTimestamp = eventTimestamp; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
} 