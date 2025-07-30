package com.open.unicorn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@RestController
@RequestMapping("/api/uws-rdb")
public class UWSRDBController {

    @Autowired
    private DatabaseInstanceRepository databaseInstanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Create a new database instance
    @PostMapping("/instances")
    public ResponseEntity<?> createDatabaseInstance(@RequestBody CreateInstanceRequest request, Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            User user = userRepository.findByEmail(userEmail);
            
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }

            // Validate input
            if (request.getDbName() == null || request.getDbName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Database name is required"));
            }

            if (request.getCpuLimit() == null || request.getCpuLimit() <= 0 || request.getCpuLimit() > 16) {
                return ResponseEntity.badRequest().body(Map.of("error", "CPU limit must be between 1 and 16"));
            }

            if (request.getRamLimit() == null || request.getRamLimit() <= 0 || request.getRamLimit() > 32768) {
                return ResponseEntity.badRequest().body(Map.of("error", "RAM limit must be between 1 and 32768 MB"));
            }

            if (request.getStorageLimit() == null || request.getStorageLimit() <= 0 || request.getStorageLimit() > 1048576) {
                return ResponseEntity.badRequest().body(Map.of("error", "Storage limit must be between 1 and 1048576 MB"));
            }

            // Check if database name already exists for this user
            DatabaseInstance existing = databaseInstanceRepository.findByUserIdAndDbName(user.getId(), request.getDbName());
            if (existing != null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Database name already exists"));
            }

            // Create database instance
            DatabaseInstance instance = new DatabaseInstance();
            instance.setUser(user);
            instance.setDbName(request.getDbName());
            instance.setCpuLimit(request.getCpuLimit());
            instance.setRamLimit(request.getRamLimit());
            instance.setStorageLimit(request.getStorageLimit());
            
            // Generate unique schema name
            String schemaName = "user_" + user.getId() + "_" + request.getDbName().toLowerCase().replaceAll("[^a-z0-9_]", "_");
            instance.setSchemaName(schemaName);
            
            // Create PostgreSQL schema
            createSchema(schemaName);
            
            instance.setStatus(DatabaseInstance.DatabaseStatus.RUNNING);
            DatabaseInstance savedInstance = databaseInstanceRepository.save(instance);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Database instance created successfully");
            response.put("instance", Map.of(
                "id", savedInstance.getId(),
                "dbName", savedInstance.getDbName(),
                "cpuLimit", savedInstance.getCpuLimit(),
                "ramLimit", savedInstance.getRamLimit(),
                "storageLimit", savedInstance.getStorageLimit(),
                "status", savedInstance.getStatus(),
                "createdAt", savedInstance.getCreatedAt()
            ));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to create database instance: " + e.getMessage()));
        }
    }

    // List all database instances for the authenticated user
    @GetMapping("/instances")
    public ResponseEntity<?> listDatabaseInstances(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            User user = userRepository.findByEmail(userEmail);
            
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }

            List<DatabaseInstance> instances = databaseInstanceRepository.findByUserId(user.getId());
            List<Map<String, Object>> response = new ArrayList<>();

            for (DatabaseInstance instance : instances) {
                Map<String, Object> instanceMap = new HashMap<>();
                instanceMap.put("id", instance.getId());
                instanceMap.put("dbName", instance.getDbName());
                instanceMap.put("cpuLimit", instance.getCpuLimit());
                instanceMap.put("ramLimit", instance.getRamLimit());
                instanceMap.put("storageLimit", instance.getStorageLimit());
                instanceMap.put("status", instance.getStatus());
                instanceMap.put("createdAt", instance.getCreatedAt());
                response.add(instanceMap);
            }

            return ResponseEntity.ok(Map.of("instances", response));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to list database instances: " + e.getMessage()));
        }
    }

    // Delete a database instance
    @DeleteMapping("/instances/{instanceId}")
    public ResponseEntity<?> deleteDatabaseInstance(@PathVariable Long instanceId, Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            User user = userRepository.findByEmail(userEmail);
            
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }

            Optional<DatabaseInstance> instanceOpt = databaseInstanceRepository.findById(instanceId);
            if (instanceOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            DatabaseInstance instance = instanceOpt.get();
            if (!instance.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied"));
            }

            // Drop the schema
            dropSchema(instance.getSchemaName());
            
            // Delete the instance
            databaseInstanceRepository.delete(instance);

            return ResponseEntity.ok(Map.of("message", "Database instance deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to delete database instance: " + e.getMessage()));
        }
    }

    // Execute SQL query on a specific database instance
    @PostMapping("/instances/{instanceId}/query")
    public ResponseEntity<?> executeQuery(@PathVariable Long instanceId, @RequestBody QueryRequest request, Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            User user = userRepository.findByEmail(userEmail);
            
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }

            Optional<DatabaseInstance> instanceOpt = databaseInstanceRepository.findById(instanceId);
            if (instanceOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            DatabaseInstance instance = instanceOpt.get();
            if (!instance.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Access denied"));
            }

            if (instance.getStatus() != DatabaseInstance.DatabaseStatus.RUNNING) {
                return ResponseEntity.badRequest().body(Map.of("error", "Database instance is not running"));
            }

            String sql = request.getSql();
            if (sql == null || sql.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "SQL query is required"));
            }

            // Validate and sanitize SQL
            String sanitizedSql = sanitizeAndValidateSql(sql, instance.getSchemaName());
            if (sanitizedSql == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid or unsupported SQL query"));
            }

            // Execute query
            System.out.println("Executing SQL: " + sanitizedSql);
            QueryResult result = executeSqlQuery(sanitizedSql, instance.getSchemaName());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Query executed successfully");
            response.put("sql", sanitizedSql);
            response.put("result", result);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to execute query: " + e.getMessage()));
        }
    }

    // Helper methods
    private void createSchema(String schemaName) {
        try {
            // For H2, we'll use a different approach since H2 doesn't support schemas like PostgreSQL
            // We'll create a unique table prefix instead
            System.out.println("Creating schema namespace: " + schemaName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create schema: " + e.getMessage());
        }
    }

    private void dropSchema(String schemaName) {
        try {
            // For H2, we'll drop only tables with the schema prefix
            String tablePrefix = schemaName + "_";
            
            // Get all tables with this prefix
            List<Map<String, Object>> tables = jdbcTemplate.queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME LIKE ?",
                tablePrefix + "%"
            );
            
            // Drop each table
            for (Map<String, Object> table : tables) {
                String tableName = (String) table.get("TABLE_NAME");
                jdbcTemplate.execute("DROP TABLE IF EXISTS " + tableName);
                System.out.println("Dropped table: " + tableName);
            }
            
            System.out.println("Dropped schema namespace: " + schemaName);
        } catch (Exception e) {
            // Log error but don't throw - schema might not exist
            System.err.println("Warning: Failed to drop schema " + schemaName + ": " + e.getMessage());
        }
    }

    private String sanitizeAndValidateSql(String sql, String schemaName) {
        String upperSql = sql.trim().toUpperCase();
        
        // Check for dangerous operations
        if (upperSql.contains("DROP DATABASE") || upperSql.contains("DROP SCHEMA") || 
            upperSql.contains("CREATE DATABASE") || upperSql.contains("CREATE SCHEMA") ||
            upperSql.contains("ALTER DATABASE") || upperSql.contains("ALTER SCHEMA")) {
            return null;
        }

        // For H2, we'll use table prefixes instead of schemas
        String sanitizedSql = sql;
        
        // Convert PostgreSQL syntax to H2 syntax
        sanitizedSql = sanitizedSql.replaceAll("(?i)SERIAL", "IDENTITY");
        sanitizedSql = sanitizedSql.replaceAll("(?i)AUTO_INCREMENT", "IDENTITY");
        sanitizedSql = sanitizedSql.replaceAll("(?i)TIMESTAMP\\s+DEFAULT\\s+CURRENT_TIMESTAMP", "TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
        
        // Simple pattern matching for table names (this is a basic implementation)
        // In production, you'd want a more sophisticated SQL parser
        
        // Handle CREATE TABLE
        Pattern createTablePattern = Pattern.compile("\\bCREATE TABLE\\s+([a-zA-Z_][a-zA-Z0-9_]*)", Pattern.CASE_INSENSITIVE);
        Matcher createMatcher = createTablePattern.matcher(sanitizedSql);
        if (createMatcher.find()) {
            String tableName = createMatcher.group(1);
            if (!tableName.startsWith(schemaName + "_")) {
                sanitizedSql = sanitizedSql.replaceFirst(
                    "\\bCREATE TABLE\\s+" + tableName + "\\b",
                    "CREATE TABLE " + schemaName + "_" + tableName
                );
            }
        }
        
        // Handle other operations (FROM, INTO, UPDATE, JOIN)
        Pattern tablePattern = Pattern.compile("\\b(FROM|INTO|UPDATE|JOIN)\\s+([a-zA-Z_][a-zA-Z0-9_]*)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = tablePattern.matcher(sanitizedSql);
        
        while (matcher.find()) {
            String tableName = matcher.group(2);
            // Skip if already has prefix
            if (!tableName.startsWith(schemaName + "_")) {
                sanitizedSql = sanitizedSql.replaceFirst(
                    "\\b" + matcher.group(1) + "\\s+" + tableName + "\\b",
                    matcher.group(1) + " " + schemaName + "_" + tableName
                );
            }
        }

        return sanitizedSql;
    }

    @Transactional
    private QueryResult executeSqlQuery(String sql, String schemaName) {
        try {
            String upperSql = sql.trim().toUpperCase();
            System.out.println("Executing SQL query: " + sql);
            System.out.println("Schema name: " + schemaName);
            
            if (upperSql.startsWith("SELECT")) {
                // Handle SELECT queries
                List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
                System.out.println("SELECT query returned " + results.size() + " rows");
                return new QueryResult("SELECT", results, results.size());
            } else if (upperSql.startsWith("INSERT")) {
                // Handle INSERT queries
                int affectedRows = jdbcTemplate.update(sql);
                System.out.println("INSERT query affected " + affectedRows + " rows");
                return new QueryResult("INSERT", null, affectedRows);
            } else if (upperSql.startsWith("UPDATE")) {
                // Handle UPDATE queries
                int affectedRows = jdbcTemplate.update(sql);
                System.out.println("UPDATE query affected " + affectedRows + " rows");
                return new QueryResult("UPDATE", null, affectedRows);
            } else if (upperSql.startsWith("DELETE")) {
                // Handle DELETE queries
                int affectedRows = jdbcTemplate.update(sql);
                System.out.println("DELETE query affected " + affectedRows + " rows");
                return new QueryResult("DELETE", null, affectedRows);
            } else if (upperSql.startsWith("CREATE TABLE")) {
                // Handle CREATE TABLE queries
                jdbcTemplate.execute(sql);
                System.out.println("CREATE TABLE query executed successfully");
                return new QueryResult("CREATE TABLE", null, 0);
            } else if (upperSql.startsWith("DROP TABLE")) {
                // Handle DROP TABLE queries
                jdbcTemplate.execute(sql);
                System.out.println("DROP TABLE query executed successfully");
                return new QueryResult("DROP TABLE", null, 0);
            } else {
                // Handle other queries
                jdbcTemplate.execute(sql);
                System.out.println("Other query executed successfully");
                return new QueryResult("EXECUTE", null, 0);
            }
        } catch (Exception e) {
            System.err.println("Error executing SQL query: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // Request/Response classes
    public static class CreateInstanceRequest {
        private String dbName;
        private Integer cpuLimit;
        private Integer ramLimit;
        private Long storageLimit;

        // Getters and setters
        public String getDbName() { return dbName; }
        public void setDbName(String dbName) { this.dbName = dbName; }
        
        public Integer getCpuLimit() { return cpuLimit; }
        public void setCpuLimit(Integer cpuLimit) { this.cpuLimit = cpuLimit; }
        
        public Integer getRamLimit() { return ramLimit; }
        public void setRamLimit(Integer ramLimit) { this.ramLimit = ramLimit; }
        
        public Long getStorageLimit() { return storageLimit; }
        public void setStorageLimit(Long storageLimit) { this.storageLimit = storageLimit; }
    }

    public static class QueryRequest {
        private String sql;

        public String getSql() { return sql; }
        public void setSql(String sql) { this.sql = sql; }
    }

    public static class QueryResult {
        private String operation;
        private List<Map<String, Object>> data;
        private int affectedRows;

        public QueryResult(String operation, List<Map<String, Object>> data, int affectedRows) {
            this.operation = operation;
            this.data = data;
            this.affectedRows = affectedRows;
        }

        // Getters
        public String getOperation() { return operation; }
        public List<Map<String, Object>> getData() { return data; }
        public int getAffectedRows() { return affectedRows; }
    }
} 