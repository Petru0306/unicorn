package com.open.unicorn;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "containers")
public class Container {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String instanceId;

    @Column(nullable = false)
    private String ownerEmail;

    @Column(nullable = false)
    private String imageName;

    @Column(nullable = false)
    private Integer cpu;

    @Column(nullable = false)
    private Integer memory;

    @Column(nullable = false)
    private Integer port;

    @Column(nullable = false)
    private String instanceSize;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private String schedule;

    @Column
    private LocalDateTime lastScheduledRun;

    // Default constructor
    public Container() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = "STOPPED";
    }

    // Constructor with all fields
    public Container(String instanceId, String ownerEmail, String imageName, 
                    Integer cpu, Integer memory, Integer port, String instanceSize) {
        this();
        this.instanceId = instanceId;
        this.ownerEmail = ownerEmail;
        this.imageName = imageName;
        this.cpu = cpu;
        this.memory = memory;
        this.port = port;
        this.instanceSize = instanceSize;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public Integer getCpu() {
        return cpu;
    }

    public void setCpu(Integer cpu) {
        this.cpu = cpu;
    }

    public Integer getMemory() {
        return memory;
    }

    public void setMemory(Integer memory) {
        this.memory = memory;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getInstanceSize() {
        return instanceSize;
    }

    public void setInstanceSize(String instanceSize) {
        this.instanceSize = instanceSize;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public LocalDateTime getLastScheduledRun() {
        return lastScheduledRun;
    }

    public void setLastScheduledRun(LocalDateTime lastScheduledRun) {
        this.lastScheduledRun = lastScheduledRun;
    }
} 