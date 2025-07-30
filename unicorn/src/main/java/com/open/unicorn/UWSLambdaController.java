package com.open.unicorn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/lambdas")
public class UWSLambdaController {

    @Autowired
    private LambdaService lambdaService;

    @PostMapping
    public ResponseEntity<?> createLambda(@RequestBody Map<String, Object> request) {
        try {
            String name = (String) request.get("name");
            String language = (String) request.get("language");
            String code = (String) request.get("code");
            
            // Parse resource limits with defaults
            Integer cpuLimit = null;
            Integer memoryLimit = null;
            Integer timeoutLimit = null;
            
            if (request.get("cpuLimit") != null) {
                cpuLimit = Integer.valueOf(request.get("cpuLimit").toString());
            }
            if (request.get("memoryLimit") != null) {
                memoryLimit = Integer.valueOf(request.get("memoryLimit").toString());
            }
            if (request.get("timeoutLimit") != null) {
                timeoutLimit = Integer.valueOf(request.get("timeoutLimit").toString());
            }

            if (name == null || language == null || code == null) {
                return ResponseEntity.badRequest().body("Missing required fields: name, language, code");
            }

            if (!language.equals("javascript") && !language.equals("python")) {
                return ResponseEntity.badRequest().body("Language must be 'javascript' or 'python'");
            }

            // Validate resource limits
            if (cpuLimit != null && (cpuLimit < 1 || cpuLimit > 8)) {
                return ResponseEntity.badRequest().body("CPU limit must be between 1 and 8 cores");
            }
            if (memoryLimit != null && (memoryLimit < 128 || memoryLimit > 8192)) {
                return ResponseEntity.badRequest().body("Memory limit must be between 128 and 8192 MB");
            }
            if (timeoutLimit != null && (timeoutLimit < 1 || timeoutLimit > 300)) {
                return ResponseEntity.badRequest().body("Timeout limit must be between 1 and 300 seconds");
            }

            Lambda lambda = lambdaService.createLambda(name, language, code, cpuLimit, memoryLimit, timeoutLimit);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", lambda.getId());
            response.put("name", lambda.getName());
            response.put("language", lambda.getLanguage());
            response.put("createdAt", lambda.getCreatedAt());
            response.put("cpuLimit", lambda.getCpuLimit());
            response.put("memoryLimit", lambda.getMemoryLimit());
            response.put("timeoutLimit", lambda.getTimeoutLimit());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error creating lambda: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<?> executeLambda(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String input = request.getOrDefault("input", "{}");
            
            LambdaExecution execution = lambdaService.executeLambda(id, input);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", execution.getId());
            response.put("success", execution.getSuccess());
            response.put("output", execution.getOutput());
            response.put("error", execution.getError());
            response.put("executionTime", execution.getExecutionTime());
            response.put("timestamp", execution.getTimestamp());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error executing lambda: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getUserLambdas() {
        try {
            List<Lambda> lambdas = lambdaService.getUserLambdas();
            
            List<Map<String, Object>> response = lambdas.stream()
                .map(lambda -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", lambda.getId());
                    map.put("name", lambda.getName());
                    map.put("language", lambda.getLanguage());
                    map.put("createdAt", lambda.getCreatedAt());
                    map.put("cpuLimit", lambda.getCpuLimit());
                    map.put("memoryLimit", lambda.getMemoryLimit());
                    map.put("timeoutLimit", lambda.getTimeoutLimit());
                    return map;
                })
                .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error retrieving lambdas: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLambda(@PathVariable Long id) {
        try {
            lambdaService.deleteLambda(id);
            return ResponseEntity.ok("Lambda deleted successfully");
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error deleting lambda: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/logs")
    public ResponseEntity<?> getLambdaLogs(@PathVariable Long id) {
        try {
            List<LambdaExecution> executions = lambdaService.getLambdaExecutions(id);
            
            List<Map<String, Object>> response = executions.stream()
                .map(execution -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", execution.getId());
                    map.put("success", execution.getSuccess());
                    map.put("output", execution.getOutput());
                    map.put("error", execution.getError());
                    map.put("executionTime", execution.getExecutionTime());
                    map.put("timestamp", execution.getTimestamp());
                    map.put("input", execution.getInput());
                    return map;
                })
                .toList();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error retrieving logs: " + e.getMessage());
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getExecutionStats() {
        try {
            Map<String, Object> stats = lambdaService.getExecutionStats();
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error retrieving stats: " + e.getMessage());
        }
    }
} 