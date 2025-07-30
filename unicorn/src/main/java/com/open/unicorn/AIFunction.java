package com.open.unicorn;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_functions")
public class AIFunction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type; // text, image, code

    @Column(nullable = false)
    private String model; // sentiment-analysis, image-labeling, code-explainer

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "api_endpoint")
    private String apiEndpoint;

    @Column(name = "max_executions_per_day", nullable = false)
    private Integer maxExecutionsPerDay = 1000;

    @Column(name = "current_executions_today", nullable = false)
    private Integer currentExecutionsToday = 0;

    @Column(name = "last_reset_date")
    private LocalDateTime lastResetDate;

    // Constructors
    public AIFunction() {
        this.createdAt = LocalDateTime.now();
        this.lastResetDate = LocalDateTime.now();
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getApiEndpoint() {
        return apiEndpoint;
    }

    public void setApiEndpoint(String apiEndpoint) {
        this.apiEndpoint = apiEndpoint;
    }

    public Integer getMaxExecutionsPerDay() {
        return maxExecutionsPerDay;
    }

    public void setMaxExecutionsPerDay(Integer maxExecutionsPerDay) {
        this.maxExecutionsPerDay = maxExecutionsPerDay;
    }

    public Integer getCurrentExecutionsToday() {
        return currentExecutionsToday;
    }

    public void setCurrentExecutionsToday(Integer currentExecutionsToday) {
        this.currentExecutionsToday = currentExecutionsToday;
    }

    public LocalDateTime getLastResetDate() {
        return lastResetDate;
    }

    public void setLastResetDate(LocalDateTime lastResetDate) {
        this.lastResetDate = lastResetDate;
    }
} 