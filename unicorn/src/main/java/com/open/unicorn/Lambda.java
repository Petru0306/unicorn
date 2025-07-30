package com.open.unicorn;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lambdas")
public class Lambda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String language; // "javascript" or "python"

    @Column(columnDefinition = "TEXT", nullable = false)
    private String code;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Resource limits
    @Column(name = "cpu_limit")
    private Integer cpuLimit = 1; // CPU cores (default: 1)

    @Column(name = "memory_limit")
    private Integer memoryLimit = 512; // Memory in MB (default: 512MB)

    @Column(name = "timeout_limit")
    private Integer timeoutLimit = 30; // Timeout in seconds (default: 30s)

    @OneToMany(mappedBy = "lambda", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LambdaExecution> executions = new ArrayList<>();

    // Default constructor
    public Lambda() {
        this.createdAt = LocalDateTime.now();
        this.cpuLimit = 1;
        this.memoryLimit = 512;
        this.timeoutLimit = 30;
    }

    // Constructor with parameters
    public Lambda(User user, String name, String language, String code) {
        this.user = user;
        this.name = name;
        this.language = language;
        this.code = code;
        this.createdAt = LocalDateTime.now();
        this.cpuLimit = 1;
        this.memoryLimit = 512;
        this.timeoutLimit = 30;
    }

    // Constructor with resource limits
    public Lambda(User user, String name, String language, String code, Integer cpuLimit, Integer memoryLimit, Integer timeoutLimit) {
        this.user = user;
        this.name = name;
        this.language = language;
        this.code = code;
        this.createdAt = LocalDateTime.now();
        this.cpuLimit = cpuLimit != null ? cpuLimit : 1;
        this.memoryLimit = memoryLimit != null ? memoryLimit : 512;
        this.timeoutLimit = timeoutLimit != null ? timeoutLimit : 30;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getCpuLimit() {
        return cpuLimit;
    }

    public void setCpuLimit(Integer cpuLimit) {
        this.cpuLimit = cpuLimit != null ? cpuLimit : 1;
    }

    public Integer getMemoryLimit() {
        return memoryLimit;
    }

    public void setMemoryLimit(Integer memoryLimit) {
        this.memoryLimit = memoryLimit != null ? memoryLimit : 512;
    }

    public Integer getTimeoutLimit() {
        return timeoutLimit;
    }

    public void setTimeoutLimit(Integer timeoutLimit) {
        this.timeoutLimit = timeoutLimit != null ? timeoutLimit : 30;
    }

    public List<LambdaExecution> getExecutions() {
        return executions;
    }

    public void setExecutions(List<LambdaExecution> executions) {
        this.executions = executions;
    }
} 