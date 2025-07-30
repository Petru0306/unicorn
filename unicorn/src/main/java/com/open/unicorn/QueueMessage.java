package com.open.unicorn;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "queue_messages")
public class QueueMessage {
    
    public enum MessageStatus {
        VISIBLE,
        INVISIBLE,
        DELETED
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "queue_id", nullable = false)
    private Queue queue;
    
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MessageStatus status;
    
    @Column(name = "visibility_expiry")
    private LocalDateTime visibilityExpiry;
    
    // Default constructor
    public QueueMessage() {
        this.createdAt = LocalDateTime.now();
        this.status = MessageStatus.VISIBLE;
    }
    
    // Constructor with required fields
    public QueueMessage(Queue queue, String content) {
        this();
        this.queue = queue;
        this.content = content;
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Queue getQueue() {
        return queue;
    }
    
    public void setQueue(Queue queue) {
        this.queue = queue;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public MessageStatus getStatus() {
        return status;
    }
    
    public void setStatus(MessageStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getVisibilityExpiry() {
        return visibilityExpiry;
    }
    
    public void setVisibilityExpiry(LocalDateTime visibilityExpiry) {
        this.visibilityExpiry = visibilityExpiry;
    }
    
    // Helper method to check if message is visible
    public boolean isVisible() {
        if (status == MessageStatus.DELETED) {
            return false;
        }
        
        if (status == MessageStatus.INVISIBLE && visibilityExpiry != null) {
            return LocalDateTime.now().isAfter(visibilityExpiry);
        }
        
        return status == MessageStatus.VISIBLE;
    }
    
    // Helper method to check if message is currently invisible (regardless of expiry)
    public boolean isCurrentlyInvisible() {
        return status == MessageStatus.INVISIBLE;
    }
    
    // Helper method to check if message has expired visibility timeout
    public boolean hasExpiredVisibility() {
        return status == MessageStatus.INVISIBLE && 
               visibilityExpiry != null && 
               LocalDateTime.now().isAfter(visibilityExpiry);
    }
    
    // Helper method to make message invisible for a timeout period
    public void makeInvisible(int timeoutSeconds) {
        this.status = MessageStatus.INVISIBLE;
        this.visibilityExpiry = LocalDateTime.now().plusSeconds(timeoutSeconds);
    }
    
    // Helper method to make message visible again
    public void makeVisible() {
        this.status = MessageStatus.VISIBLE;
        this.visibilityExpiry = null;
    }
} 