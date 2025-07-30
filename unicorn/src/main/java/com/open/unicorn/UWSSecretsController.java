package com.open.unicorn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/secrets")
public class UWSSecretsController {

    @Autowired
    private SecretRepository secretRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EncryptionService encryptionService;

    // Create a new secret
    @PostMapping
    public ResponseEntity<?> createSecret(@RequestBody Map<String, Object> request, Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            User user = userRepository.findByEmail(userEmail);
            
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not found"));
            }

            String name = (String) request.get("name");
            String value = (String) request.get("value");
            String description = (String) request.get("description");
            String expiryDateStr = (String) request.get("expiresAt");

            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Secret name is required"));
            }

            if (value == null || value.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Secret value is required"));
            }

            // Check if secret name already exists for this user
            Optional<Secret> existingSecret = secretRepository.findByUserIdAndName(user.getId(), name);
            if (existingSecret.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "A secret with this name already exists"));
            }

            // Encrypt the secret value
            String encryptedValue = encryptionService.encrypt(value);

            // Create new secret
            Secret secret = new Secret(user.getId(), name, encryptedValue, description);
            
            // Set expiration date if provided
            if (expiryDateStr != null && !expiryDateStr.trim().isEmpty()) {
                try {
                    LocalDateTime expiryDate = LocalDateTime.parse(expiryDateStr);
                    secret.setExpiresAt(expiryDate);
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Invalid expiration date format"));
                }
            }

            Secret savedSecret = secretRepository.save(secret);

            Map<String, Object> response = new HashMap<>();
            response.put("id", savedSecret.getId());
            response.put("name", savedSecret.getName());
            response.put("description", savedSecret.getDescription());
            response.put("createdAt", savedSecret.getCreatedAt());
            response.put("updatedAt", savedSecret.getUpdatedAt());
            response.put("expiresAt", savedSecret.getExpiresAt());
            response.put("version", savedSecret.getVersion());
            response.put("message", "Secret created successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to create secret: " + e.getMessage()));
        }
    }

    // Get all secrets for the authenticated user (metadata only)
    @GetMapping
    public ResponseEntity<?> getAllSecrets(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            User user = userRepository.findByEmail(userEmail);
            
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not found"));
            }

            List<Secret> secrets = secretRepository.findByUserIdOrderByUpdatedAtDesc(user.getId());
            
            List<Map<String, Object>> secretsList = secrets.stream()
                .map(secret -> {
                    Map<String, Object> secretMap = new HashMap<>();
                    secretMap.put("id", secret.getId());
                    secretMap.put("name", secret.getName());
                    secretMap.put("description", secret.getDescription());
                    secretMap.put("createdAt", secret.getCreatedAt());
                    secretMap.put("updatedAt", secret.getUpdatedAt());
                    secretMap.put("expiresAt", secret.getExpiresAt());
                    secretMap.put("version", secret.getVersion());
                    secretMap.put("isExpired", secret.isExpired());
                    return secretMap;
                })
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("secrets", secretsList);
            response.put("total", secretsList.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to retrieve secrets: " + e.getMessage()));
        }
    }

    // Get a specific secret by ID (with decrypted value)
    @GetMapping("/{id}")
    public ResponseEntity<?> getSecret(@PathVariable Long id, Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            User user = userRepository.findByEmail(userEmail);
            
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not found"));
            }

            Optional<Secret> secretOpt = secretRepository.findByUserIdAndId(user.getId(), id);
            
            if (secretOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Secret not found"));
            }

            Secret secret = secretOpt.get();
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", secret.getId());
            response.put("name", secret.getName());
            response.put("description", secret.getDescription());
            response.put("createdAt", secret.getCreatedAt());
            response.put("updatedAt", secret.getUpdatedAt());
            response.put("expiresAt", secret.getExpiresAt());
            response.put("version", secret.getVersion());
            response.put("isExpired", secret.isExpired());

            // Only decrypt and return value if secret is not expired
            if (secret.isExpired()) {
                response.put("value", null);
                response.put("error", "This secret has expired and its value can no longer be accessed");
            } else {
                // Decrypt the secret value
                String decryptedValue = encryptionService.decrypt(secret.getValueEncrypted());
                response.put("value", decryptedValue);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to retrieve secret: " + e.getMessage()));
        }
    }

    // Update a secret
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSecret(@PathVariable Long id, @RequestBody Map<String, Object> request, Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            User user = userRepository.findByEmail(userEmail);
            
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not found"));
            }

            Optional<Secret> secretOpt = secretRepository.findByUserIdAndId(user.getId(), id);
            
            if (secretOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Secret not found"));
            }

            Secret secret = secretOpt.get();
            
            // Prevent editing expired secrets
            if (secret.isExpired()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Cannot edit expired secrets"));
            }
            
            // Update fields if provided
            if (request.containsKey("name")) {
                String newName = (String) request.get("name");
                if (newName != null && !newName.trim().isEmpty()) {
                    // Check if new name conflicts with existing secret
                    Optional<Secret> existingSecret = secretRepository.findByUserIdAndName(user.getId(), newName);
                    if (existingSecret.isPresent() && !existingSecret.get().getId().equals(id)) {
                        return ResponseEntity.badRequest().body(Map.of("error", "A secret with this name already exists"));
                    }
                    secret.setName(newName);
                }
            }

            if (request.containsKey("value")) {
                String newValue = (String) request.get("value");
                // Only update value if a new non-empty value is provided
                if (newValue != null && !newValue.trim().isEmpty()) {
                    String encryptedValue = encryptionService.encrypt(newValue);
                    secret.setValueEncrypted(encryptedValue);
                    secret.setVersion(secret.getVersion() + 1); // Increment version
                }
                // If value is empty or null, keep the existing value (do nothing)
            }

            if (request.containsKey("description")) {
                secret.setDescription((String) request.get("description"));
            }

            if (request.containsKey("expiresAt")) {
                String expiryDateStr = (String) request.get("expiresAt");
                if (expiryDateStr != null && !expiryDateStr.trim().isEmpty()) {
                    try {
                        LocalDateTime expiryDate = LocalDateTime.parse(expiryDateStr);
                        secret.setExpiresAt(expiryDate);
                    } catch (Exception e) {
                        return ResponseEntity.badRequest().body(Map.of("error", "Invalid expiration date format"));
                    }
                } else {
                    secret.setExpiresAt(null);
                }
            }

            secret.setUpdatedAt(LocalDateTime.now());
            Secret updatedSecret = secretRepository.save(secret);

            Map<String, Object> response = new HashMap<>();
            response.put("id", updatedSecret.getId());
            response.put("name", updatedSecret.getName());
            response.put("description", updatedSecret.getDescription());
            response.put("createdAt", updatedSecret.getCreatedAt());
            response.put("updatedAt", updatedSecret.getUpdatedAt());
            response.put("expiresAt", updatedSecret.getExpiresAt());
            response.put("version", updatedSecret.getVersion());
            response.put("message", "Secret updated successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to update secret: " + e.getMessage()));
        }
    }

    // Delete a secret
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSecret(@PathVariable Long id, Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            User user = userRepository.findByEmail(userEmail);
            
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not found"));
            }

            Optional<Secret> secretOpt = secretRepository.findByUserIdAndId(user.getId(), id);
            
            if (secretOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Secret not found"));
            }

            Secret secret = secretOpt.get();
            secret.setIsActive(false); // Soft delete
            secret.setUpdatedAt(LocalDateTime.now());
            secretRepository.save(secret);

            return ResponseEntity.ok(Map.of("message", "Secret deleted successfully"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to delete secret: " + e.getMessage()));
        }
    }

    // Get secret statistics
    @GetMapping("/stats")
    public ResponseEntity<?> getSecretStats(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            User user = userRepository.findByEmail(userEmail);
            
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not found"));
            }

            long totalSecrets = secretRepository.countByUserId(user.getId());
            
            // Get expired secrets
            List<Secret> expiredSecrets = secretRepository.findExpiredSecrets(user.getId(), LocalDateTime.now());
            long expiredCount = expiredSecrets.size();

            Map<String, Object> response = new HashMap<>();
            response.put("totalSecrets", totalSecrets);
            response.put("expiredSecrets", expiredCount);
            response.put("activeSecrets", totalSecrets - expiredCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to retrieve statistics: " + e.getMessage()));
        }
    }
} 