package com.open.unicorn;

import jakarta.persistence.*;

@Entity
@Table(name = "user_roles")
public class UserRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private IAMRole role;
    
    @ManyToOne
    @JoinColumn(name = "assigned_by")
    private User assignedBy;
    
    @Column(name = "assigned_at")
    private java.time.LocalDateTime assignedAt;
    
    // Constructors
    public UserRole() {}
    
    public UserRole(User user, IAMRole role, User assignedBy) {
        this.user = user;
        this.role = role;
        this.assignedBy = assignedBy;
        this.assignedAt = java.time.LocalDateTime.now();
    }
    
    // Getters and Setters
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
    
    public IAMRole getRole() {
        return role;
    }
    
    public void setRole(IAMRole role) {
        this.role = role;
    }
    
    public User getAssignedBy() {
        return assignedBy;
    }
    
    public void setAssignedBy(User assignedBy) {
        this.assignedBy = assignedBy;
    }
    
    public java.time.LocalDateTime getAssignedAt() {
        return assignedAt;
    }
    
    public void setAssignedAt(java.time.LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }
} 