package com.open.unicorn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/uws-iam")
public class UWSIAMController {
    
    @Autowired
    private IAMService iamService;
    
    @Autowired
    private UserRepository userRepository;
    
    // Main IAM page
    @GetMapping
    public String iamPage(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return "redirect:/login";
        }
        
        return "redirect:/uws-iam.html";
    }
    
    // Get roles and user assignments data
    @GetMapping("/data")
    @ResponseBody
    public ResponseEntity<?> getIAMData(HttpServletRequest httpRequest) {
        String userEmail = getCurrentUserEmail(httpRequest);
        
        // Return empty data if user not authenticated
        if (userEmail == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("roles", new ArrayList<>());
            response.put("userRoles", new ArrayList<>());
            return ResponseEntity.ok(response);
        }
        
        User currentUser = userRepository.findByEmail(userEmail);
        if (currentUser == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("roles", new ArrayList<>());
            response.put("userRoles", new ArrayList<>());
            return ResponseEntity.ok(response);
        }
        
        List<IAMRole> roles = iamService.getRolesByOwner(currentUser);
        List<UserRole> userRoles = iamService.getUsersWithRolesByOwner(currentUser);
        
        Map<String, Object> response = new HashMap<>();
        response.put("roles", roles);
        response.put("userRoles", userRoles);
        
        return ResponseEntity.ok(response);
    }
    
    // Create role API
    @PostMapping("/roles")
    @ResponseBody
    public ResponseEntity<?> createRole(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        String userEmail = getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "User not authenticated");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        User currentUser = userRepository.findByEmail(userEmail);
        if (currentUser == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "User not found");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        try {
            String name = (String) request.get("name");
            String description = (String) request.get("description");
            @SuppressWarnings("unchecked")
            Map<String, String> permissions = (Map<String, String>) request.get("permissions");
            
            System.out.println("Creating role: " + name + " for user: " + currentUser.getEmail());
            System.out.println("Permissions: " + permissions);
            
            IAMRole role = iamService.createRole(name, description, permissions, currentUser);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("role", role);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("Error creating role: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Search roles API
    @GetMapping("/roles/search")
    @ResponseBody
    public ResponseEntity<?> searchRoles(@RequestParam String searchTerm, HttpServletRequest httpRequest) {
        String userEmail = getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "User not authenticated");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        User currentUser = userRepository.findByEmail(userEmail);
        if (currentUser == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "User not found");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        List<IAMRole> roles = iamService.searchRolesByOwner(currentUser, searchTerm);
        return ResponseEntity.ok(roles);
    }
    
    // Get role by ID API
    @GetMapping("/roles/{roleId}")
    @ResponseBody
    public ResponseEntity<?> getRoleById(@PathVariable Long roleId, HttpServletRequest httpRequest) {
        String userEmail = getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "User not authenticated");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        User currentUser = userRepository.findByEmail(userEmail);
        if (currentUser == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "User not found");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        try {
            IAMRole role = iamService.getRoleById(roleId, currentUser);
            
            Map<String, Object> response = new HashMap<>();
            if (role != null) {
                response.put("success", true);
                response.put("role", role);
            } else {
                response.put("success", false);
                response.put("error", "Role not found or you don't have permission to access it");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Error getting role: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // Update role API
    @PutMapping("/roles/{roleId}")
    @ResponseBody
    public ResponseEntity<?> updateRole(@PathVariable Long roleId, @RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        String userEmail = getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "User not authenticated");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        User currentUser = userRepository.findByEmail(userEmail);
        if (currentUser == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "User not found");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        try {
            String name = (String) request.get("name");
            String description = (String) request.get("description");
            @SuppressWarnings("unchecked")
            Map<String, String> permissions = (Map<String, String>) request.get("permissions");
            
            System.out.println("Updating role: " + roleId + " for user: " + currentUser.getEmail());
            System.out.println("New name: " + name + ", description: " + description);
            System.out.println("New permissions: " + permissions);
            
            IAMRole updatedRole = iamService.updateRole(roleId, name, description, permissions, currentUser);
            
            Map<String, Object> response = new HashMap<>();
            if (updatedRole != null) {
                response.put("success", true);
                response.put("role", updatedRole);
            } else {
                response.put("success", false);
                response.put("error", "Role not found or you don't have permission to update it");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Error updating role: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // Delete role API
    @DeleteMapping("/roles/{roleId}")
    @ResponseBody
    public ResponseEntity<?> deleteRole(@PathVariable Long roleId, HttpServletRequest httpRequest) {
        String userEmail = getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "User not authenticated");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        User currentUser = userRepository.findByEmail(userEmail);
        if (currentUser == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "User not found");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        try {
            iamService.deleteRole(roleId, currentUser);
            return ResponseEntity.ok().body(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    // Search users for role assignment API
    @GetMapping("/users/search")
    @ResponseBody
    public ResponseEntity<?> searchUsers(@RequestParam String searchTerm, HttpServletRequest httpRequest) {
        String userEmail = getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "User not authenticated");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        User currentUser = userRepository.findByEmail(userEmail);
        if (currentUser == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "User not found");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        List<User> users = iamService.searchUsersForRole(searchTerm);
        // Filter out current user
        users = users.stream()
                   .filter(user -> !user.getId().equals(currentUser.getId()))
                   .collect(Collectors.toList());
        
        return ResponseEntity.ok(users);
    }
    
    // Assign role to user API
    @PostMapping("/users/{userId}/role/{roleId}")
    @ResponseBody
    public ResponseEntity<?> assignRole(@PathVariable Long userId, @PathVariable Long roleId, HttpServletRequest httpRequest) {
        String userEmail = getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "User not authenticated");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        User currentUser = userRepository.findByEmail(userEmail);
        if (currentUser == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "User not found");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            Optional<IAMRole> roleOpt = iamService.getRolesByOwner(currentUser).stream()
                .filter(role -> role.getId().equals(roleId))
                .findFirst();
            
            if (userOpt.isEmpty() || roleOpt.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "User or role not found");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            UserRole userRole = iamService.assignRoleToUser(userOpt.get(), roleOpt.get(), currentUser);
            return ResponseEntity.ok(Map.of("success", true, "userRole", userRole));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    // Remove user from role API
    @DeleteMapping("/users/{userId}/role")
    @ResponseBody
    public ResponseEntity<?> removeUserRole(@PathVariable Long userId, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "User not authenticated");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "User not found");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            iamService.removeUserFromRole(userOpt.get());
            return ResponseEntity.ok(Map.of("success", true));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    // Search users with roles API
    @GetMapping("/users/with-roles/search")
    @ResponseBody
    public ResponseEntity<?> searchUsersWithRoles(@RequestParam String searchTerm, HttpSession session) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "User not authenticated");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        List<UserRole> userRoles = iamService.searchUsersWithRolesByOwner(currentUser, searchTerm);
        return ResponseEntity.ok(userRoles);
    }
    
    // Get services and resources API
    @GetMapping("/services")
    @ResponseBody
    public ResponseEntity<?> getServices() {
        Map<String, Object> response = new HashMap<>();
        response.put("services", iamService.getAvailableServices());
        response.put("serviceResources", IAMService.SERVICE_RESOURCES);
        return ResponseEntity.ok(response);
    }
    
    // Get current user's assigned roles
    @GetMapping("/my-roles")
    @ResponseBody
    public ResponseEntity<?> getMyRoles(HttpServletRequest httpRequest) {
        String userEmail = getCurrentUserEmail(httpRequest);
        if (userEmail == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "User not authenticated");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        User currentUser = userRepository.findByEmail(userEmail);
        if (currentUser == null) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "User not found");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        try {
            System.out.println("Getting roles for user: " + currentUser.getEmail());
            List<Map<String, Object>> userRoles = iamService.getUserRolesWithDetails(currentUser);
            System.out.println("Found " + userRoles.size() + " roles for user");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("userRoles", userRoles);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error getting user roles: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    // Helper method to get current user email from JWT
    private String getCurrentUserEmail(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        
        try {
            String token = header.replace("Bearer ", "");
            String userEmail = io.jsonwebtoken.Jwts.parserBuilder()
                    .setSigningKey(UnicornApplication.getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
            return userEmail;
        } catch (Exception e) {
            return null;
        }
    }
} 