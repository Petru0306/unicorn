package com.open.unicorn;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "dns_zones", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "zone_name"})
})
public class DNSZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "zone_name", nullable = false)
    private String zoneName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Default constructor
    public DNSZone() {
        this.createdAt = LocalDateTime.now();
    }

    // Constructor with parameters
    public DNSZone(Long userId, String zoneName) {
        this.userId = userId;
        this.zoneName = zoneName;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and setters
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

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
} 