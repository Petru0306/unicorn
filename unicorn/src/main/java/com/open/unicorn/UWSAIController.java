package com.open.unicorn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Base64;

@RestController
@RequestMapping("/api/ai")
public class UWSAIController {

    @Autowired
    private AIService aiService;

    @GetMapping("/functions")
    public ResponseEntity<?> getUserFunctions(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            List<AIFunction> functions = aiService.getUserFunctions(userEmail);
            return ResponseEntity.ok(functions);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/functions")
    public ResponseEntity<?> createFunction(
            Authentication authentication,
            @RequestBody Map<String, String> request) {
        try {
            String userEmail = authentication.getName();
            String name = request.get("name");
            String type = request.get("type");
            String model = request.get("model");

            if (name == null || type == null || model == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Name, type, and model are required");
                return ResponseEntity.badRequest().body(error);
            }

            AIFunction function = aiService.createFunction(userEmail, name, type, model);
            return ResponseEntity.ok(function);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/functions/{id}/invoke")
    public ResponseEntity<?> invokeFunction(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String userEmail = authentication.getName();
            String input = request.get("input");

            if (input == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Input is required");
                return ResponseEntity.badRequest().body(error);
            }

            AIExecution execution = aiService.invokeFunction(userEmail, id, input);
            return ResponseEntity.ok(execution);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/functions/{id}/invoke-image")
    public ResponseEntity<?> invokeFunctionWithImage(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam("image") MultipartFile image) {
        try {
            String userEmail = authentication.getName();
            
            // Convert image to base64 string for processing
            String input = Base64.getEncoder().encodeToString(image.getBytes());
            
            AIExecution execution = aiService.invokeFunction(userEmail, id, input);
            return ResponseEntity.ok(execution);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/functions/{id}")
    public ResponseEntity<?> deleteFunction(
            Authentication authentication,
            @PathVariable Long id) {
        try {
            String userEmail = authentication.getName();
            aiService.deleteFunction(userEmail, id);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "AI function deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/functions/{id}/history")
    public ResponseEntity<?> getExecutionHistory(
            Authentication authentication,
            @PathVariable Long id) {
        try {
            String userEmail = authentication.getName();
            List<AIExecution> history = aiService.getExecutionHistory(userEmail, id);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            Map<String, Object> stats = aiService.getStatistics(userEmail);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/models")
    public ResponseEntity<?> getAvailableModels() {
        try {
            Map<String, String> models = aiService.getAvailableModels();
            return ResponseEntity.ok(models);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/types")
    public ResponseEntity<?> getAvailableTypes() {
        try {
            List<String> types = aiService.getAvailableTypes();
            return ResponseEntity.ok(types);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("service", "UWS-AI");
        response.put("version", "1.0.0");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/configuration")
    public ResponseEntity<?> getAIConfiguration() {
        try {
            Map<String, Object> config = aiService.getAIConfiguration();
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
} 