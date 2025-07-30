package com.open.unicorn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/dns")
public class UWSDNSController {

    @Autowired
    private DNSZoneRepository dnsZoneRepository;

    @Autowired
    private DNSRecordRepository dnsRecordRepository;

    @Autowired
    private UserRepository userRepository;

    // Helper method to get user ID from authentication
    private Long getUserIdFromAuth(Authentication authentication) {
        String userEmail = authentication.getName();
        User user = userRepository.findByEmail(userEmail);
        return user != null ? user.getId() : null;
    }

    // Create DNS Zone
    @PostMapping("/zones")
    public ResponseEntity<?> createZone(@RequestBody Map<String, String> request, Authentication authentication) {
        try {
            Long userId = getUserIdFromAuth(authentication);
            if (userId == null) {
                return ResponseEntity.status(403).body(Map.of("error", "User not found"));
            }

            String zoneName = request.get("zoneName");
            if (zoneName == null || zoneName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Zone name is required"));
            }

            // Validate zone name format
            if (!zoneName.matches("^[a-zA-Z0-9][a-zA-Z0-9-]*[a-zA-Z0-9]?\\.uws$")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Zone name must end with .uws and contain only letters, numbers, and hyphens"));
            }

            // Check if zone already exists for this user
            if (dnsZoneRepository.existsByUserIdAndZoneName(userId, zoneName)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Zone name already exists"));
            }

            DNSZone zone = new DNSZone(userId, zoneName);
            DNSZone savedZone = dnsZoneRepository.save(zone);

            Map<String, Object> response = new HashMap<>();
            response.put("id", savedZone.getId());
            response.put("zoneName", savedZone.getZoneName());
            response.put("createdAt", savedZone.getCreatedAt());
            response.put("message", "DNS zone created successfully");

            return ResponseEntity.status(201).body(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to create DNS zone: " + e.getMessage()));
        }
    }

    // List user's DNS zones
    @GetMapping("/zones")
    public ResponseEntity<?> listZones(Authentication authentication) {
        try {
            Long userId = getUserIdFromAuth(authentication);
            if (userId == null) {
                return ResponseEntity.status(403).body(Map.of("error", "User not found"));
            }

            List<DNSZone> zones = dnsZoneRepository.findByUserId(userId);
            return ResponseEntity.ok(zones);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to list DNS zones: " + e.getMessage()));
        }
    }

    // Add DNS record to a zone
    @PostMapping("/zones/{zoneId}/records")
    public ResponseEntity<?> addRecord(@PathVariable Long zoneId, @RequestBody Map<String, Object> request, Authentication authentication) {
        try {
            Long userId = getUserIdFromAuth(authentication);
            if (userId == null) {
                return ResponseEntity.status(403).body(Map.of("error", "User not found"));
            }

            // Verify zone belongs to user
            Optional<DNSZone> zoneOpt = dnsZoneRepository.findById(zoneId);
            if (zoneOpt.isEmpty() || !zoneOpt.get().getUserId().equals(userId)) {
                return ResponseEntity.status(404).body(Map.of("error", "DNS zone not found"));
            }

            String type = (String) request.get("type");
            String name = (String) request.get("name");
            String value = (String) request.get("value");
            Integer ttl = (Integer) request.get("ttl");

            // Validate required fields
            if (type == null || name == null || value == null || ttl == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "All fields (type, name, value, ttl) are required"));
            }

            // Validate DNS record type
            if (!List.of("A", "AAAA", "CNAME", "TXT").contains(type.toUpperCase())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid DNS record type. Supported types: A, AAAA, CNAME, TXT"));
            }

            // Validate TTL
            if (ttl < 60 || ttl > 86400) {
                return ResponseEntity.badRequest().body(Map.of("error", "TTL must be between 60 and 86400 seconds"));
            }

            // Check for CNAME conflicts
            if (type.equalsIgnoreCase("CNAME")) {
                List<DNSRecord> existingRecords = dnsRecordRepository.findByZoneIdAndName(zoneId, name);
                if (!existingRecords.isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of("error", "CNAME record conflicts with existing records for this name"));
                }
            } else {
                // Check if CNAME exists for this name
                DNSRecord cnameRecord = dnsRecordRepository.findByZoneIdAndNameAndType(zoneId, name, "CNAME");
                if (cnameRecord != null) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Cannot add record: CNAME already exists for this name"));
                }
            }

            // Check if record already exists
            if (dnsRecordRepository.existsByZoneIdAndNameAndType(zoneId, name, type.toUpperCase())) {
                return ResponseEntity.badRequest().body(Map.of("error", "DNS record already exists for this name and type"));
            }

            DNSRecord record = new DNSRecord(zoneId, type.toUpperCase(), name, value, ttl);
            DNSRecord savedRecord = dnsRecordRepository.save(record);

            Map<String, Object> response = new HashMap<>();
            response.put("id", savedRecord.getId());
            response.put("type", savedRecord.getType());
            response.put("name", savedRecord.getName());
            response.put("value", savedRecord.getValue());
            response.put("ttl", savedRecord.getTtl());
            response.put("createdAt", savedRecord.getCreatedAt());
            response.put("message", "DNS record added successfully");

            return ResponseEntity.status(201).body(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to add DNS record: " + e.getMessage()));
        }
    }

    // List records for a zone
    @GetMapping("/zones/{zoneId}/records")
    public ResponseEntity<?> listRecords(@PathVariable Long zoneId, Authentication authentication) {
        try {
            Long userId = getUserIdFromAuth(authentication);
            if (userId == null) {
                return ResponseEntity.status(403).body(Map.of("error", "User not found"));
            }

            // Verify zone belongs to user
            Optional<DNSZone> zoneOpt = dnsZoneRepository.findById(zoneId);
            if (zoneOpt.isEmpty() || !zoneOpt.get().getUserId().equals(userId)) {
                return ResponseEntity.status(404).body(Map.of("error", "DNS zone not found"));
            }

            List<DNSRecord> records = dnsRecordRepository.findRecordsByZoneIdOrdered(zoneId);
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to list DNS records: " + e.getMessage()));
        }
    }

    // Update DNS record
    @PutMapping("/records/{recordId}")
    public ResponseEntity<?> updateRecord(@PathVariable Long recordId, @RequestBody Map<String, Object> request, Authentication authentication) {
        try {
            Long userId = getUserIdFromAuth(authentication);
            if (userId == null) {
                return ResponseEntity.status(403).body(Map.of("error", "User not found"));
            }

            Optional<DNSRecord> recordOpt = dnsRecordRepository.findById(recordId);
            if (recordOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "DNS record not found"));
            }

            DNSRecord record = recordOpt.get();

            // Verify zone belongs to user
            Optional<DNSZone> zoneOpt = dnsZoneRepository.findById(record.getZoneId());
            if (zoneOpt.isEmpty() || !zoneOpt.get().getUserId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }

            String value = (String) request.get("value");
            Integer ttl = (Integer) request.get("ttl");

            if (value != null) {
                record.setValue(value);
            }
            if (ttl != null) {
                if (ttl < 60 || ttl > 86400) {
                    return ResponseEntity.badRequest().body(Map.of("error", "TTL must be between 60 and 86400 seconds"));
                }
                record.setTtl(ttl);
            }

            record.setUpdatedAt(LocalDateTime.now());
            DNSRecord updatedRecord = dnsRecordRepository.save(record);

            Map<String, Object> response = new HashMap<>();
            response.put("id", updatedRecord.getId());
            response.put("type", updatedRecord.getType());
            response.put("name", updatedRecord.getName());
            response.put("value", updatedRecord.getValue());
            response.put("ttl", updatedRecord.getTtl());
            response.put("updatedAt", updatedRecord.getUpdatedAt());
            response.put("message", "DNS record updated successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to update DNS record: " + e.getMessage()));
        }
    }

    // Delete DNS record
    @DeleteMapping("/records/{recordId}")
    public ResponseEntity<?> deleteRecord(@PathVariable Long recordId, Authentication authentication) {
        try {
            Long userId = getUserIdFromAuth(authentication);
            if (userId == null) {
                return ResponseEntity.status(403).body(Map.of("error", "User not found"));
            }

            Optional<DNSRecord> recordOpt = dnsRecordRepository.findById(recordId);
            if (recordOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "DNS record not found"));
            }

            DNSRecord record = recordOpt.get();

            // Verify zone belongs to user
            Optional<DNSZone> zoneOpt = dnsZoneRepository.findById(record.getZoneId());
            if (zoneOpt.isEmpty() || !zoneOpt.get().getUserId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }

            dnsRecordRepository.delete(record);

            Map<String, String> response = new HashMap<>();
            response.put("message", "DNS record deleted successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to delete DNS record: " + e.getMessage()));
        }
    }

    // Get DNS record by ID
    @GetMapping("/records/{recordId}")
    public ResponseEntity<?> getRecord(@PathVariable Long recordId, Authentication authentication) {
        try {
            Long userId = getUserIdFromAuth(authentication);
            if (userId == null) {
                return ResponseEntity.status(403).body(Map.of("error", "User not found"));
            }

            Optional<DNSRecord> recordOpt = dnsRecordRepository.findById(recordId);
            if (recordOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "DNS record not found"));
            }

            DNSRecord record = recordOpt.get();

            // Verify zone belongs to user
            Optional<DNSZone> zoneOpt = dnsZoneRepository.findById(record.getZoneId());
            if (zoneOpt.isEmpty() || !zoneOpt.get().getUserId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }

            return ResponseEntity.ok(record);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to get DNS record: " + e.getMessage()));
        }
    }
} 