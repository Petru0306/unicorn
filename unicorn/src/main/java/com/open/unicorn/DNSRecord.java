package com.open.unicorn;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "dns_records", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"zone_id", "name", "type"})
})
public class DNSRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zone_id", nullable = false)
    private Long zoneId;

    @Column(name = "type", nullable = false)
    private String type; // A, AAAA, CNAME, TXT

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "record_value", nullable = false)
    private String value;

    @Column(name = "ttl", nullable = false)
    private Integer ttl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Default constructor
    public DNSRecord() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Constructor with parameters
    public DNSRecord(Long zoneId, String type, String name, String value, Integer ttl) {
        this.zoneId = zoneId;
        this.type = type;
        this.name = name;
        this.value = value;
        this.ttl = ttl;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Integer getTtl() {
        return ttl;
    }

    public void setTtl(Integer ttl) {
        this.ttl = ttl;
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
} 