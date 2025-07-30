package com.open.unicorn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/nosql")
public class UWSNoSQLController {

    @Autowired
    private NoSQLTableRepository tableRepository;

    @Autowired
    private NoSQLEntityRepository entityRepository;

    @Autowired
    private NoSQLIndexRepository indexRepository;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Helper method to get current user ID
    private Long getCurrentUserId(Authentication authentication) {
        String userEmail = authentication.getName();
        User user = userRepository.findByEmail(userEmail);
        return user != null ? user.getId() : null;
    }

    // Create a new table
    @PostMapping("/tables")
    public ResponseEntity<?> createTable(@RequestBody Map<String, Object> request, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            if (userId == null) {
                return ResponseEntity.badRequest().body("User not found");
            }

            String name = (String) request.get("name");
            String primaryKey = (String) request.get("primaryKey");
            String description = (String) request.get("description");

            if (name == null || primaryKey == null) {
                return ResponseEntity.badRequest().body("Table name and primary key are required");
            }

            // Check if table name already exists for this user
            if (tableRepository.existsByNameAndUserId(name, userId)) {
                return ResponseEntity.badRequest().body("Table with this name already exists");
            }

            NoSQLTable table = new NoSQLTable(name, primaryKey, userId);
            table.setDescription(description);
            NoSQLTable savedTable = tableRepository.save(table);

            Map<String, Object> response = new HashMap<>();
            response.put("id", savedTable.getId());
            response.put("name", savedTable.getName());
            response.put("primaryKey", savedTable.getPrimaryKey());
            response.put("description", savedTable.getDescription());
            response.put("createdAt", savedTable.getCreatedAt());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating table: " + e.getMessage());
        }
    }

    // List all tables for the current user
    @GetMapping("/tables")
    public ResponseEntity<?> listTables(Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            if (userId == null) {
                return ResponseEntity.badRequest().body("User not found");
            }

            List<NoSQLTable> tables = tableRepository.findByUserIdOrderByCreatedAtDesc(userId);
            return ResponseEntity.ok(tables);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error listing tables: " + e.getMessage());
        }
    }

    // Insert a new entity into a table
    @PostMapping("/tables/{tableId}/entity")
    public ResponseEntity<?> insertEntity(@PathVariable Long tableId, @RequestBody Map<String, Object> request, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            if (userId == null) {
                return ResponseEntity.badRequest().body("User not found");
            }

            // Verify table belongs to user
            Optional<NoSQLTable> tableOpt = tableRepository.findByIdAndUserId(tableId, userId);
            if (tableOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            NoSQLTable table = tableOpt.get();
            String primaryKeyValue = (String) request.get(table.getPrimaryKey());
            
            if (primaryKeyValue == null) {
                return ResponseEntity.badRequest().body("Primary key value is required");
            }

            // Check if entity already exists
            if (entityRepository.existsByTableIdAndPrimaryKeyValue(tableId, primaryKeyValue)) {
                return ResponseEntity.badRequest().body("Entity with this primary key already exists");
            }

            // Convert request to JSON string
            String documentData = objectMapper.writeValueAsString(request);

            NoSQLEntity entity = new NoSQLEntity(tableId, primaryKeyValue, documentData);
            NoSQLEntity savedEntity = entityRepository.save(entity);

            Map<String, Object> response = new HashMap<>();
            response.put("id", savedEntity.getId());
            response.put("primaryKeyValue", savedEntity.getPrimaryKeyValue());
            response.put("createdAt", savedEntity.getCreatedAt());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error inserting entity: " + e.getMessage());
        }
    }

    // Update an existing entity
    @PutMapping("/tables/{tableId}/entity")
    public ResponseEntity<?> updateEntity(@PathVariable Long tableId, @RequestBody Map<String, Object> request, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            if (userId == null) {
                return ResponseEntity.badRequest().body("User not found");
            }

            // Verify table belongs to user
            Optional<NoSQLTable> tableOpt = tableRepository.findByIdAndUserId(tableId, userId);
            if (tableOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            NoSQLTable table = tableOpt.get();
            String primaryKeyValue = (String) request.get(table.getPrimaryKey());
            
            if (primaryKeyValue == null) {
                return ResponseEntity.badRequest().body("Primary key value is required");
            }

            // Find existing entity
            Optional<NoSQLEntity> entityOpt = entityRepository.findByTableIdAndPrimaryKeyValue(tableId, primaryKeyValue);
            if (entityOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            NoSQLEntity entity = entityOpt.get();
            
            // Merge with existing data
            JsonNode existingData = objectMapper.readTree(entity.getDocumentData());
            JsonNode newData = objectMapper.valueToTree(request);
            
            // Merge JSON objects
            var fieldNames = newData.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                ((com.fasterxml.jackson.databind.node.ObjectNode) existingData).set(fieldName, newData.get(fieldName));
            }

            entity.setDocumentData(existingData.toString());
            NoSQLEntity savedEntity = entityRepository.save(entity);

            Map<String, Object> response = new HashMap<>();
            response.put("id", savedEntity.getId());
            response.put("primaryKeyValue", savedEntity.getPrimaryKeyValue());
            response.put("updatedAt", savedEntity.getUpdatedAt());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating entity: " + e.getMessage());
        }
    }

    // Scan table with pagination
    @GetMapping("/tables/{tableId}/scan")
    public ResponseEntity<?> scanTable(@PathVariable Long tableId, 
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "10") int size,
                                     Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            if (userId == null) {
                return ResponseEntity.badRequest().body("User not found");
            }

            // Verify table belongs to user
            Optional<NoSQLTable> tableOpt = tableRepository.findByIdAndUserId(tableId, userId);
            if (tableOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Pageable pageable = PageRequest.of(page, size);
            Page<NoSQLEntity> pageResult = entityRepository.findByTableIdWithPagination(tableId, pageable);
            List<NoSQLEntity> entities = pageResult.getContent();
            long totalCount = pageResult.getTotalElements();

            Map<String, Object> response = new HashMap<>();
            response.put("entities", entities);
            response.put("totalCount", totalCount);
            response.put("page", page);
            response.put("size", size);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error scanning table: " + e.getMessage());
        }
    }

    // Query entities by field
    @GetMapping("/tables/{tableId}/query")
    public ResponseEntity<?> queryEntities(@PathVariable Long tableId,
                                         @RequestParam String field,
                                         @RequestParam String value,
                                         Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            if (userId == null) {
                return ResponseEntity.badRequest().body("User not found");
            }

            // Verify table belongs to user
            Optional<NoSQLTable> tableOpt = tableRepository.findByIdAndUserId(tableId, userId);
            if (tableOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // For H2, we'll do a simple text search in the JSON data
            String searchPattern = "%\"" + field + "\":\"" + value + "\"%";
            List<NoSQLEntity> entities = entityRepository.findByTableIdAndFieldValue(tableId, searchPattern);

            Map<String, Object> response = new HashMap<>();
            response.put("entities", entities);
            response.put("field", field);
            response.put("value", value);
            response.put("count", entities.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error querying entities: " + e.getMessage());
        }
    }

    // Delete an entity by primary key
    @DeleteMapping("/tables/{tableId}/entity")
    public ResponseEntity<?> deleteEntity(@PathVariable Long tableId, @RequestBody Map<String, Object> request, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            if (userId == null) {
                return ResponseEntity.badRequest().body("User not found");
            }

            // Verify table belongs to user
            Optional<NoSQLTable> tableOpt = tableRepository.findByIdAndUserId(tableId, userId);
            if (tableOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            NoSQLTable table = tableOpt.get();
            String primaryKeyValue = (String) request.get(table.getPrimaryKey());
            
            if (primaryKeyValue == null) {
                return ResponseEntity.badRequest().body("Primary key value is required");
            }

            // Find and delete entity
            Optional<NoSQLEntity> entityOpt = entityRepository.findByTableIdAndPrimaryKeyValue(tableId, primaryKeyValue);
            if (entityOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            entityRepository.delete(entityOpt.get());
            return ResponseEntity.ok("Entity deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting entity: " + e.getMessage());
        }
    }

    // Delete entire table
    @DeleteMapping("/tables/{tableId}")
    public ResponseEntity<?> deleteTable(@PathVariable Long tableId, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            if (userId == null) {
                return ResponseEntity.badRequest().body("User not found");
            }

            // Verify table belongs to user
            Optional<NoSQLTable> tableOpt = tableRepository.findByIdAndUserId(tableId, userId);
            if (tableOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Delete all entities first
            entityRepository.deleteByTableId(tableId);
            
            // Delete table
            tableRepository.delete(tableOpt.get());
            
            return ResponseEntity.ok("Table and all entities deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting table: " + e.getMessage());
        }
    }

    // Get table statistics
    @GetMapping("/tables/{tableId}/stats")
    public ResponseEntity<?> getTableStats(@PathVariable Long tableId, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            if (userId == null) {
                return ResponseEntity.badRequest().body("User not found");
            }

            // Verify table belongs to user
            Optional<NoSQLTable> tableOpt = tableRepository.findByIdAndUserId(tableId, userId);
            if (tableOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            NoSQLTable table = tableOpt.get();
            long entityCount = entityRepository.countByTableId(tableId);

            Map<String, Object> response = new HashMap<>();
            response.put("tableId", table.getId());
            response.put("tableName", table.getName());
            response.put("entityCount", entityCount);
            response.put("createdAt", table.getCreatedAt());
            response.put("updatedAt", table.getUpdatedAt());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error getting table stats: " + e.getMessage());
        }
    }

    // Create a new index
    @PostMapping("/tables/{tableId}/indexes")
    public ResponseEntity<?> createIndex(@PathVariable Long tableId, @RequestBody Map<String, Object> request, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            if (userId == null) {
                return ResponseEntity.badRequest().body("User not found");
            }

            // Verify table belongs to user
            Optional<NoSQLTable> tableOpt = tableRepository.findByIdAndUserId(tableId, userId);
            if (tableOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            String indexName = (String) request.get("indexName");
            String fieldName = (String) request.get("fieldName");

            if (indexName == null || fieldName == null) {
                return ResponseEntity.badRequest().body("Index name and field name are required");
            }

            // Check if index name already exists for this table
            if (indexRepository.existsByIndexNameAndTableId(indexName, tableId)) {
                return ResponseEntity.badRequest().body("Index with this name already exists for this table");
            }

            NoSQLIndex index = new NoSQLIndex(tableId, indexName, fieldName, userId);
            NoSQLIndex savedIndex = indexRepository.save(index);

            Map<String, Object> response = new HashMap<>();
            response.put("id", savedIndex.getId());
            response.put("indexName", savedIndex.getIndexName());
            response.put("fieldName", savedIndex.getFieldName());
            response.put("tableId", savedIndex.getTableId());
            response.put("createdAt", savedIndex.getCreatedAt());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating index: " + e.getMessage());
        }
    }

    // List all indexes for a table
    @GetMapping("/tables/{tableId}/indexes")
    public ResponseEntity<?> listIndexes(@PathVariable Long tableId, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            if (userId == null) {
                return ResponseEntity.badRequest().body("User not found");
            }

            // Verify table belongs to user
            Optional<NoSQLTable> tableOpt = tableRepository.findByIdAndUserId(tableId, userId);
            if (tableOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            List<NoSQLIndex> indexes = indexRepository.findByTableIdAndIsActiveTrueOrderByCreatedAtDesc(tableId);
            return ResponseEntity.ok(indexes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error listing indexes: " + e.getMessage());
        }
    }

    // Delete an index
    @DeleteMapping("/tables/{tableId}/indexes/{indexId}")
    public ResponseEntity<?> deleteIndex(@PathVariable Long tableId, @PathVariable Long indexId, Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            if (userId == null) {
                return ResponseEntity.badRequest().body("User not found");
            }

            // Verify table belongs to user
            Optional<NoSQLTable> tableOpt = tableRepository.findByIdAndUserId(tableId, userId);
            if (tableOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Find and delete index
            Optional<NoSQLIndex> indexOpt = indexRepository.findById(indexId);
            if (indexOpt.isEmpty() || !indexOpt.get().getTableId().equals(tableId)) {
                return ResponseEntity.notFound().build();
            }

            indexRepository.delete(indexOpt.get());
            return ResponseEntity.ok("Index deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting index: " + e.getMessage());
        }
    }

    // Enhanced query with index support
    @GetMapping("/tables/{tableId}/query-indexed")
    public ResponseEntity<?> queryWithIndex(@PathVariable Long tableId,
                                          @RequestParam String field,
                                          @RequestParam String value,
                                          Authentication authentication) {
        try {
            Long userId = getCurrentUserId(authentication);
            if (userId == null) {
                return ResponseEntity.badRequest().body("User not found");
            }

            // Verify table belongs to user
            Optional<NoSQLTable> tableOpt = tableRepository.findByIdAndUserId(tableId, userId);
            if (tableOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Check if there's an index for this field
            List<NoSQLIndex> indexes = indexRepository.findByFieldNameAndTableIdAndIsActiveTrue(field, tableId);
            boolean hasIndex = !indexes.isEmpty();

            // For H2, we'll do a simple text search in the JSON data
            String searchPattern = "%\"" + field + "\":\"" + value + "\"%";
            List<NoSQLEntity> entities = entityRepository.findByTableIdAndFieldValue(tableId, searchPattern);

            Map<String, Object> response = new HashMap<>();
            response.put("entities", entities);
            response.put("field", field);
            response.put("value", value);
            response.put("count", entities.size());
            response.put("hasIndex", hasIndex);
            response.put("indexUsed", hasIndex ? indexes.get(0).getIndexName() : null);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error querying entities: " + e.getMessage());
        }
    }
} 