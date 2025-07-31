package com.open.unicorn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class IAMService {
    
    @Autowired
    private IAMRoleRepository roleRepository;
    
    @Autowired
    private UserRoleRepository userRoleRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    // Available services and their resources
    public static final Map<String, List<String>> SERVICE_RESOURCES = new HashMap<>();
    
    static {
        SERVICE_RESOURCES.put("S3", Arrays.asList("buckets", "objects"));
        SERVICE_RESOURCES.put("Lambda", Arrays.asList("functions", "executions"));
        SERVICE_RESOURCES.put("NoSQL", Arrays.asList("tables", "indexes", "entities"));
        SERVICE_RESOURCES.put("DNS", Arrays.asList("zones", "records"));
        SERVICE_RESOURCES.put("Monitoring", Arrays.asList("alerts", "events"));
        SERVICE_RESOURCES.put("Billing", Arrays.asList("events", "alerts"));
        SERVICE_RESOURCES.put("AI", Arrays.asList("functions", "executions"));
        SERVICE_RESOURCES.put("Compute", Arrays.asList("containers", "services"));
        SERVICE_RESOURCES.put("RDB", Arrays.asList("instances", "databases"));
        SERVICE_RESOURCES.put("Secrets", Arrays.asList("secrets"));
        SERVICE_RESOURCES.put("SQS", Arrays.asList("queues", "messages"));
    }
    
    // Create a new role
    public IAMRole createRole(String name, String description, Map<String, String> permissions, User createdBy) {
        if (roleRepository.existsByName(name)) {
            throw new RuntimeException("Role with name '" + name + "' already exists");
        }
        
        IAMRole role = new IAMRole(name, description, createdBy);
        role.setPermissions(permissions);
        return roleRepository.save(role);
    }
    
    // Get all roles for a user
    public List<IAMRole> getRolesByOwner(User owner) {
        return roleRepository.findByCreatedBy(owner);
    }
    
    // Search roles
    public List<IAMRole> searchRoles(String searchTerm) {
        return roleRepository.searchRoles(searchTerm);
    }
    
    // Search roles by owner
    public List<IAMRole> searchRolesByOwner(User owner, String searchTerm) {
        return roleRepository.searchRolesByUser(owner, searchTerm);
    }
    
    // Assign role to user
    public UserRole assignRoleToUser(User user, IAMRole role, User assignedBy) {
        System.out.println("Assigning role '" + role.getName() + "' to user '" + user.getEmail() + "' by '" + assignedBy.getEmail() + "'");
        
        // Remove existing role if any
        userRoleRepository.deleteByUser(user);
        
        UserRole userRole = new UserRole(user, role, assignedBy);
        UserRole savedUserRole = userRoleRepository.save(userRole);
        System.out.println("Role assigned successfully with ID: " + savedUserRole.getId());
        return savedUserRole;
    }
    
    // Get users with a specific role
    public List<UserRole> getUsersWithRole(IAMRole role) {
        return userRoleRepository.findByRole(role);
    }
    
    // Search users for role assignment
    public List<User> searchUsersForRole(String searchTerm) {
        return userRepository.findByEmailContainingIgnoreCase(searchTerm);
    }
    
    // Get user's role
    public Optional<IAMRole> getUserRole(User user) {
        Optional<UserRole> userRole = userRoleRepository.findByUser(user);
        return userRole.map(UserRole::getRole);
    }
    
    // Get all roles assigned to a user
    public List<UserRole> getUserRoles(User user) {
        Optional<UserRole> userRole = userRoleRepository.findByUser(user);
        if (userRole.isPresent()) {
            return List.of(userRole.get());
        }
        return new ArrayList<>();
    }
    
    // Get roles assigned to a user with role details
    public List<Map<String, Object>> getUserRolesWithDetails(User user) {
        System.out.println("Getting user roles for: " + user.getEmail());
        List<UserRole> userRoles = getUserRoles(user);
        System.out.println("Found " + userRoles.size() + " user roles");
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (UserRole userRole : userRoles) {
            System.out.println("Processing role: " + userRole.getRole().getName());
            Map<String, Object> roleInfo = new HashMap<>();
            roleInfo.put("id", userRole.getId());
            roleInfo.put("role", userRole.getRole());
            roleInfo.put("assignedBy", userRole.getAssignedBy());
            roleInfo.put("assignedAt", userRole.getAssignedAt());
            roleInfo.put("permissions", userRole.getRole().getPermissions());
            result.add(roleInfo);
        }
        
        System.out.println("Returning " + result.size() + " role details");
        return result;
    }
    
    // Check if user has permission
    public boolean hasPermission(User user, String service, String resource, String requiredPermission) {
        System.out.println("Checking permission for user: " + user.getEmail() + ", service: " + service + ", resource: " + resource + ", required: " + requiredPermission);
        
        Optional<IAMRole> role = getUserRole(user);
        if (role.isEmpty()) {
            System.out.println("User has no role assigned");
            return false;
        }
        
        IAMRole userRole = role.get();
        System.out.println("User has role: " + userRole.getName());
        System.out.println("Role permissions: " + userRole.getPermissions());
        
        boolean hasPermission = userRole.hasPermission(service, resource, requiredPermission);
        System.out.println("Permission check result: " + hasPermission);
        
        return hasPermission;
    }
    
    // Get all users with roles for an owner
    public List<UserRole> getUsersWithRolesByOwner(User owner) {
        return userRoleRepository.findByRoleOwner(owner);
    }
    
    // Search users with roles by owner
    public List<UserRole> searchUsersWithRolesByOwner(User owner, String searchTerm) {
        return userRoleRepository.findByRoleOwnerAndUserSearch(owner, searchTerm);
    }
    
    // Get role by ID (only if user is the owner)
    public IAMRole getRoleById(Long roleId, User owner) {
        Optional<IAMRole> role = roleRepository.findById(roleId);
        if (role.isPresent() && role.get().getCreatedBy().equals(owner)) {
            return role.get();
        }
        return null; // Role not found or user doesn't have permission
    }

    // Update role
    public IAMRole updateRole(Long roleId, String name, String description, Map<String, String> permissions, User owner) {
        Optional<IAMRole> role = roleRepository.findById(roleId);
        if (role.isPresent() && role.get().getCreatedBy().equals(owner)) {
            IAMRole existingRole = role.get();
            
            // Check if new name conflicts with existing role (excluding current role)
            if (!name.equals(existingRole.getName()) && roleRepository.existsByName(name)) {
                throw new RuntimeException("Role with name '" + name + "' already exists");
            }
            
            // Update role properties
            existingRole.setName(name);
            existingRole.setDescription(description);
            existingRole.setPermissions(permissions);
            
            return roleRepository.save(existingRole);
        }
        return null; // Role not found or user doesn't have permission
    }

    // Delete role
    public void deleteRole(Long roleId, User owner) {
        Optional<IAMRole> role = roleRepository.findById(roleId);
        if (role.isPresent() && role.get().getCreatedBy().equals(owner)) {
            // Remove all user assignments for this role
            List<UserRole> userRoles = userRoleRepository.findByRole(role.get());
            userRoleRepository.deleteAll(userRoles);
            
            // Delete the role
            roleRepository.delete(role.get());
        }
    }
    
    // Remove user from role
    public void removeUserFromRole(User user) {
        userRoleRepository.deleteByUser(user);
    }
    
    // Get available services
    public Set<String> getAvailableServices() {
        return SERVICE_RESOURCES.keySet();
    }
    
    // Get resources for a service
    public List<String> getResourcesForService(String service) {
        return SERVICE_RESOURCES.getOrDefault(service, new ArrayList<>());
    }
} 