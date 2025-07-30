package com.open.unicorn;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "queues")
public class Queue {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "queue_name", nullable = false, unique = true)
    private String queueName;
    
    @Column(name = "visibility_timeout")
    private Integer visibilityTimeout; // in seconds
    
    @Column(name = "retention_time")
    private Integer retentionTime; // in seconds
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    // Default constructor
    public Queue() {
        this.createdAt = LocalDateTime.now();
    }
    
    // Constructor with required fields
    public Queue(User user, String queueName) {
        this();
        this.user = user;
        this.queueName = queueName;
        this.visibilityTimeout = 30; // default 30 seconds
        this.retentionTime = 345600; // default 4 days
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public String getQueueName() {
        return queueName;
    }
    
    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }
    
    public Integer getVisibilityTimeout() {
        return visibilityTimeout;
    }
    
    public void setVisibilityTimeout(Integer visibilityTimeout) {
        this.visibilityTimeout = visibilityTimeout;
    }
    
    public Integer getRetentionTime() {
        return retentionTime;
    }
    
    public void setRetentionTime(Integer retentionTime) {
        this.retentionTime = retentionTime;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
} 