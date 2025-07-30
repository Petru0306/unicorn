package com.open.unicorn;

import jakarta.persistence.*;
import java.util.Map;
import java.util.HashMap;

@Entity
@Table(name = "iam_roles")
public class IAMRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String name;
    
    @Column(length = 1000)
    private String description;
    
    @ElementCollection
    @CollectionTable(name = "iam_role_permissions", 
                     joinColumns = @JoinColumn(name = "role_id"))
    @MapKeyColumn(name = "service_resource")
    @Column(name = "permission")
    private Map<String, String> permissions = new HashMap<>(); // service:resource -> READ/WRITE
    
    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;
    
    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;
    
    // Constructors
    public IAMRole() {}
    
    public IAMRole(String name, String description, User createdBy) {
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
        this.createdAt = java.time.LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Map<String, String> getPermissions() {
        return permissions;
    }
    
    public void setPermissions(Map<String, String> permissions) {
        this.permissions = permissions;
    }
    
    public User getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }
    
    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    // Helper methods
    public void addPermission(String service, String resource, String permission) {
        this.permissions.put(service + ":" + resource, permission);
    }
    
    public String getPermission(String service, String resource) {
        return this.permissions.get(service + ":" + resource);
    }
    
    public boolean hasPermission(String service, String resource, String requiredPermission) {
        String permission = getPermission(service, resource);
        if (permission == null) return false;
        
        if ("WRITE".equals(requiredPermission)) {
            return "WRITE".equals(permission);
        } else if ("READ".equals(requiredPermission)) {
            return "READ".equals(permission) || "WRITE".equals(permission);
        }
        return false;
    }
} 