package com.open.unicorn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/lambdas")
public class UWSLambdaController {

    @Autowired
    private LambdaService lambdaService;

    @PostMapping
    public ResponseEntity<?> createLambda(@RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            String language = request.get("language");
            String code = request.get("code");

            if (name == null || language == null || code == null) {
                return ResponseEntity.badRequest().body("Missing required fields: name, language, code");
            }

            if (!language.equals("javascript") && !language.equals("python")) {
                return ResponseEntity.badRequest().body("Language must be 'javascript' or 'python'");
            }

            Lambda lambda = lambdaService.createLambda(name, language, code);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", lambda.getId());
            response.put("name", lambda.getName());
            response.put("language", lambda.getLanguage());
            response.put("createdAt", lambda.getCreatedAt());
            
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
} 