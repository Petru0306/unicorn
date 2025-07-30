package com.open.unicorn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.CompletableFuture;
import java.util.Map;
import java.util.HashMap;

@Service
public class LambdaService {

    @Autowired
    private LambdaRepository lambdaRepository;

    @Autowired
    private LambdaExecutionRepository executionRepository;

    @Autowired
    private UserRepository userRepository;

    // Rate limiting configuration
    private static final int MAX_REQUESTS_PER_MINUTE = 30;
    private static final int MAX_REQUESTS_PER_HOUR = 1000;
    private static final int MAX_FUNCTIONS_PER_USER = 50;
    
    // Rate limiting storage
    private final ConcurrentHashMap<String, RateLimitInfo> rateLimitMap = new ConcurrentHashMap<>();
    
    // Performance optimization
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final ConcurrentHashMap<String, String> codeCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Lambda> lambdaCache = new ConcurrentHashMap<>();
    
    // Cache configuration
    private static final int MAX_CACHE_SIZE = 1000;
    private static final long CACHE_TTL_MS = 300000; // 5 minutes
    
    // Monitoring and metrics
    private final AtomicInteger totalExecutions = new AtomicInteger(0);
    private final AtomicInteger successfulExecutions = new AtomicInteger(0);
    private final AtomicInteger failedExecutions = new AtomicInteger(0);
    private final ConcurrentHashMap<String, Integer> userExecutionCounts = new ConcurrentHashMap<>();
    
    // Rate limit info class
    private static class RateLimitInfo {
        private final AtomicInteger minuteCount = new AtomicInteger(0);
        private final AtomicInteger hourCount = new AtomicInteger(0);
        private LocalDateTime minuteReset = LocalDateTime.now().plusMinutes(1);
        private LocalDateTime hourReset = LocalDateTime.now().plusHours(1);
        
        public boolean canMakeRequest() {
            LocalDateTime now = LocalDateTime.now();
            
            // Reset minute counter if needed
            if (now.isAfter(minuteReset)) {
                minuteCount.set(0);
                minuteReset = now.plusMinutes(1);
            }
            
            // Reset hour counter if needed
            if (now.isAfter(hourReset)) {
                hourCount.set(0);
                hourReset = now.plusHours(1);
            }
            
            return minuteCount.get() < MAX_REQUESTS_PER_MINUTE && 
                   hourCount.get() < MAX_REQUESTS_PER_HOUR;
        }
        
        public void incrementCounters() {
            minuteCount.incrementAndGet();
            hourCount.incrementAndGet();
        }
        
        public int getMinuteCount() {
            return minuteCount.get();
        }
        
        public int getHourCount() {
            return hourCount.get();
        }
    }

    public List<Lambda> getUserLambdas() {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(userEmail);
        return lambdaRepository.findByUser(user);
    }

    public Optional<Lambda> getUserLambda(Long id) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(userEmail);
        
        // Check cache first
        Lambda cachedLambda = lambdaCache.get(id);
        if (cachedLambda != null && cachedLambda.getUser().getId().equals(user.getId())) {
            return Optional.of(cachedLambda);
        }
        
        Optional<Lambda> lambdaOpt = lambdaRepository.findByUserAndId(user, id);
        
        // Cache the result if found
        if (lambdaOpt.isPresent()) {
            Lambda lambda = lambdaOpt.get();
            lambdaCache.put(id, lambda);
            
            // Clean cache if it gets too large
            if (lambdaCache.size() > MAX_CACHE_SIZE) {
                cleanCache();
            }
        }
        
        return lambdaOpt;
    }

    public Lambda createLambda(String name, String language, String code, Integer cpuLimit, Integer memoryLimit, Integer timeoutLimit) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(userEmail);
        
        // Check rate limits
        checkRateLimits(userEmail);
        
        // Check function limit
        List<Lambda> userLambdas = lambdaRepository.findByUser(user);
        if (userLambdas.size() >= MAX_FUNCTIONS_PER_USER) {
            throw new RuntimeException("Maximum number of functions (" + MAX_FUNCTIONS_PER_USER + ") reached for this user");
        }
        
        // Validate and sanitize code
        String sanitizedCode = validateAndSanitizeCode(code, language);
        if (sanitizedCode == null) {
            throw new RuntimeException("Code validation failed: Contains prohibited patterns or syntax errors");
        }
        
        Lambda lambda = new Lambda(user, name, language, sanitizedCode, cpuLimit, memoryLimit, timeoutLimit);
        return lambdaRepository.save(lambda);
    }

    // Overloaded method for backward compatibility
    public Lambda createLambda(String name, String language, String code) {
        return createLambda(name, language, code, null, null, null);
    }

    public void deleteLambda(Long id) {
        try {
            String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
            System.out.println("Deleting lambda " + id + " for user: " + userEmail);
            
            User user = userRepository.findByEmail(userEmail);
            if (user == null) {
                throw new RuntimeException("User not found: " + userEmail);
            }
            
            // Get the lambda to ensure it exists and belongs to the user
            Optional<Lambda> lambdaOpt = lambdaRepository.findByUserAndId(user, id);
            if (!lambdaOpt.isPresent()) {
                throw new RuntimeException("Lambda not found or access denied");
            }
            
            Lambda lambda = lambdaOpt.get();
            System.out.println("Found lambda: " + lambda.getName() + " (ID: " + lambda.getId() + ")");
            
            // Delete the lambda (executions will be deleted automatically due to cascade)
            lambdaRepository.delete(lambda);
            
            // Invalidate cache
            invalidateCache(id);
            
            System.out.println("Lambda deleted successfully");
            
        } catch (Exception e) {
            System.err.println("Error deleting lambda " + id + ": " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public LambdaExecution executeLambda(Long lambdaId, String input) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // Check rate limits
        checkRateLimits(userEmail);
        
        Optional<Lambda> lambdaOpt = getUserLambda(lambdaId);
        if (!lambdaOpt.isPresent()) {
            throw new RuntimeException("Lambda not found");
        }

        Lambda lambda = lambdaOpt.get();
        long startTime = System.currentTimeMillis();
        
        // Update metrics
        totalExecutions.incrementAndGet();
        userExecutionCounts.merge(userEmail, 1, Integer::sum);
        
        try {
            // Execute with resource limits from the lambda configuration
            ExecutionResult result = executeInContainer(
                lambda.getLanguage(), 
                lambda.getCode(), 
                input,
                lambda.getCpuLimit(),
                lambda.getMemoryLimit(),
                lambda.getTimeoutLimit()
            );
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            LambdaExecution execution = new LambdaExecution(
                lambda, input, result.output, result.error, result.success, executionTime
            );
            
            // Update success metrics
            if (result.success) {
                successfulExecutions.incrementAndGet();
            } else {
                failedExecutions.incrementAndGet();
            }
            
            return executionRepository.save(execution);
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Update failure metrics
            failedExecutions.incrementAndGet();
            
            // Create detailed error message
            String errorMessage = createDetailedErrorMessage(e, lambda, executionTime);
            
            LambdaExecution execution = new LambdaExecution(
                lambda, input, "", errorMessage, false, executionTime
            );
            return executionRepository.save(execution);
        }
    }
    
    /**
     * Creates detailed error messages for better debugging
     */
    private String createDetailedErrorMessage(Exception e, Lambda lambda, long executionTime) {
        StringBuilder errorMsg = new StringBuilder();
        errorMsg.append("Execution failed after ").append(executionTime).append("ms\n");
        errorMsg.append("Error: ").append(e.getMessage()).append("\n");
        errorMsg.append("Lambda: ").append(lambda.getName()).append(" (ID: ").append(lambda.getId()).append(")\n");
        errorMsg.append("Language: ").append(lambda.getLanguage()).append("\n");
        errorMsg.append("Resource Limits: CPU=").append(lambda.getCpuLimit())
                .append(", Memory=").append(lambda.getMemoryLimit()).append("MB")
                .append(", Timeout=").append(lambda.getTimeoutLimit()).append("s\n");
        
        if (e instanceof RuntimeException) {
            errorMsg.append("Type: Runtime Exception\n");
        } else {
            errorMsg.append("Type: ").append(e.getClass().getSimpleName()).append("\n");
        }
        
        return errorMsg.toString();
    }
    
    /**
     * Get execution statistics
     */
    public Map<String, Object> getExecutionStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalExecutions", totalExecutions.get());
        stats.put("successfulExecutions", successfulExecutions.get());
        stats.put("failedExecutions", failedExecutions.get());
        stats.put("successRate", totalExecutions.get() > 0 ? 
            (double) successfulExecutions.get() / totalExecutions.get() * 100 : 0);
        stats.put("activeUsers", userExecutionCounts.size());
        stats.put("cacheSize", lambdaCache.size());
        stats.put("rateLimitMapSize", rateLimitMap.size());
        return stats;
    }

    public List<LambdaExecution> getLambdaExecutions(Long lambdaId) {
        Optional<Lambda> lambdaOpt = getUserLambda(lambdaId);
        if (!lambdaOpt.isPresent()) {
            throw new RuntimeException("Lambda not found");
        }
        return executionRepository.findByLambdaOrderByTimestampDesc(lambdaOpt.get());
    }

    private static class ExecutionResult {
        String output;
        String error;
        boolean success;
        
        ExecutionResult(String output, String error, boolean success) {
            this.output = output;
            this.error = error;
            this.success = success;
        }
    }

    private ExecutionResult executeInContainer(String language, String code, String input, Integer cpuLimit, Integer memoryLimit, Integer timeoutLimit) {
        try {
            // Create temporary directory for execution
            Path tempDir = Files.createTempDirectory("lambda-execution");
            
            // Write code to file
            String fileName = language.equals("javascript") ? "function.js" : "function.py";
            Path codeFile = tempDir.resolve(fileName);
            Files.write(codeFile, code.getBytes());

            // Write input to file
            Path inputFile = tempDir.resolve("input.json");
            Files.write(inputFile, input.getBytes());

            // Debug: Print what we're passing
            System.out.println("Executing " + language + " with input: '" + input + "'");
            System.out.println("Resource limits - CPU: " + cpuLimit + ", Memory: " + memoryLimit + "MB, Timeout: " + timeoutLimit + "s");
            
            // Execute based on language - use environment variables for both to avoid command line parsing issues
            ProcessBuilder pb;
            if (language.equals("javascript")) {
                pb = new ProcessBuilder("node", fileName);
                pb.environment().put("LAMBDA_INPUT", input);
            } else {
                // Use environment variable to avoid command line parsing issues
                pb = new ProcessBuilder("python", fileName);
                pb.environment().put("LAMBDA_INPUT", input);
            }
            pb.directory(tempDir.toFile());
            
            System.out.println("Command: " + String.join(" ", pb.command()));
            System.out.println("Working directory: " + pb.directory().getAbsolutePath());

            // Start the process
            Process process = pb.start();
            
            // Apply resource limits based on OS
            applyResourceLimits(process, cpuLimit, memoryLimit);
            
            // Set timeout with the configured limit
            boolean completed = process.waitFor(timeoutLimit, TimeUnit.SECONDS);
            
            if (!completed) {
                process.destroyForcibly();
                return new ExecutionResult("", "Execution timeout (" + timeoutLimit + "s limit exceeded)", false);
            }

            // Read output and error
            String output = readStream(process.getInputStream());
            String error = readStream(process.getErrorStream());
            
            // Cleanup
            Files.walk(tempDir).sorted((a, b) -> b.compareTo(a)).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    // Ignore cleanup errors
                }
            });

            boolean success = process.exitValue() == 0;
            return new ExecutionResult(output, error, success);

        } catch (Exception e) {
            return new ExecutionResult("", "Execution failed: " + e.getMessage(), false);
        }
    }

    private void applyResourceLimits(Process process, Integer cpuLimit, Integer memoryLimit) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            
            if (os.contains("linux")) {
                // Linux: Use cgroups or ulimit
                applyLinuxResourceLimits(process, cpuLimit, memoryLimit);
            } else if (os.contains("win")) {
                // Windows: Use job objects (limited support)
                applyWindowsResourceLimits(process, cpuLimit, memoryLimit);
            } else if (os.contains("mac")) {
                // macOS: Use launchctl or similar
                applyMacResourceLimits(process, cpuLimit, memoryLimit);
            } else {
                // Fallback: Log that resource limits are not applied
                System.out.println("Resource limits not applied - unsupported OS: " + os);
            }
        } catch (Exception e) {
            System.err.println("Failed to apply resource limits: " + e.getMessage());
        }
    }

    private void applyLinuxResourceLimits(Process process, Integer cpuLimit, Integer memoryLimit) {
        try {
            // Get the process ID
            long pid = process.pid();
            
            // Apply memory limit using cgroups (if available)
            if (memoryLimit != null && memoryLimit > 0) {
                // Try to set memory limit using cgroups
                String cgroupPath = "/sys/fs/cgroup/memory/lambda-executions";
                File cgroupDir = new File(cgroupPath);
                if (!cgroupDir.exists()) {
                    cgroupDir.mkdirs();
                }
                
                // Write memory limit to cgroup
                try {
                    Files.write(Paths.get(cgroupPath + "/memory.limit_in_bytes"), 
                              String.valueOf(memoryLimit * 1024 * 1024).getBytes());
                    Files.write(Paths.get(cgroupPath + "/tasks"), 
                              String.valueOf(pid).getBytes());
                } catch (Exception e) {
                    System.err.println("Failed to set memory limit via cgroups: " + e.getMessage());
                }
            }
            
            // Apply CPU limit using cgroups (if available)
            if (cpuLimit != null && cpuLimit > 0) {
                String cgroupPath = "/sys/fs/cgroup/cpu/lambda-executions";
                File cgroupDir = new File(cgroupPath);
                if (!cgroupDir.exists()) {
                    cgroupDir.mkdirs();
                }
                
                try {
                    // Set CPU quota (in microseconds)
                    int quota = cpuLimit * 100000; // 1 CPU = 100000 microseconds
                    Files.write(Paths.get(cgroupPath + "/cpu.cfs_quota_us"), 
                              String.valueOf(quota).getBytes());
                    Files.write(Paths.get(cgroupPath + "/cpu.cfs_period_us"), 
                              "100000".getBytes());
                    Files.write(Paths.get(cgroupPath + "/tasks"), 
                              String.valueOf(pid).getBytes());
                } catch (Exception e) {
                    System.err.println("Failed to set CPU limit via cgroups: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to apply Linux resource limits: " + e.getMessage());
        }
    }

    private void applyWindowsResourceLimits(Process process, Integer cpuLimit, Integer memoryLimit) {
        try {
            // Windows resource limiting is more complex and requires JNI
            // For now, we'll log the limits but not enforce them
            System.out.println("Windows resource limits - CPU: " + cpuLimit + ", Memory: " + memoryLimit + "MB");
            System.out.println("Note: Windows resource limits require additional JNI implementation");
        } catch (Exception e) {
            System.err.println("Failed to apply Windows resource limits: " + e.getMessage());
        }
    }

    private void applyMacResourceLimits(Process process, Integer cpuLimit, Integer memoryLimit) {
        try {
            // macOS resource limiting using launchctl
            long pid = process.pid();
            
            if (memoryLimit != null && memoryLimit > 0) {
                // Try to set memory limit using launchctl
                ProcessBuilder pb = new ProcessBuilder("launchctl", "limit", "maxrss", 
                    String.valueOf(memoryLimit * 1024));
                pb.start();
            }
            
            System.out.println("macOS resource limits - CPU: " + cpuLimit + ", Memory: " + memoryLimit + "MB");
        } catch (Exception e) {
            System.err.println("Failed to apply macOS resource limits: " + e.getMessage());
        }
    }

    private String readStream(InputStream stream) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Validates and sanitizes code to prevent malicious execution
     */
    private String validateAndSanitizeCode(String code, String language) {
        if (code == null || code.trim().isEmpty()) {
            System.err.println("Code validation failed: Code is null or empty");
            return null;
        }
        
        String sanitizedCode = code.trim();
        System.err.println("Validating code for language: " + language);
        System.err.println("Code length: " + sanitizedCode.length());
        
        // Check for dangerous patterns
        if (containsDangerousPatterns(sanitizedCode, language)) {
            System.err.println("Code validation failed: Contains dangerous patterns");
            return null;
        }
        
        // Validate syntax (basic validation)
        if (!validateSyntax(sanitizedCode, language)) {
            System.err.println("Code validation failed: Syntax validation failed");
            return null;
        }
        
        // Apply language-specific sanitization
        sanitizedCode = applyLanguageSpecificSanitization(sanitizedCode, language);
        
        System.err.println("Code validation successful");
        return sanitizedCode;
    }
    
    /**
     * Checks for dangerous patterns in code
     */
    private boolean containsDangerousPatterns(String code, String language) {
        String lowerCode = code.toLowerCase();
        
        // Common dangerous patterns
        String[] dangerousPatterns = {
            // File system access - more specific patterns
            "require\\s*\\(\\s*['\"]fs['\"]", "require\\s*\\(\\s*['\"]path['\"]", 
            "import.*fs", "import.*path", "os\\.", "subprocess", "exec\\(", "eval\\(",
            // Network access
            "http\\.", "https\\.", "fetch\\(", "axios", "request\\(", "urllib",
            // Process/system access
            "process\\.", "child_process", "spawn\\(", "exec\\(", "system\\(",
            // Database access
            "mysql", "postgresql", "mongodb", "sqlite", "database",
            // Shell access
            "shell", "bash", "cmd", "powershell", "terminal",
            // File operations
            "readfile", "writefile", "unlink", "rmdir", "mkdir", "fs\\.",
            // Network sockets
            "socket", "net\\.", "tcp", "udp",
            // Environment variables access
            "process\\.env", "os\\.environ",
            // Dynamic imports
            "import\\(", "require\\(",
            // Reflection
            "reflect", "getattr", "setattr"
        };
        
        for (String pattern : dangerousPatterns) {
            if (lowerCode.matches(".*" + pattern + ".*")) {
                System.err.println("Dangerous pattern detected: " + pattern);
                return true;
            }
        }
        
        // Additional specific checks for common dangerous patterns
        if (lowerCode.contains("require(") || lowerCode.contains("import ")) {
            // Check for specific dangerous modules
            String[] dangerousModules = {
                "fs", "path", "child_process", "crypto", "http", "https", 
                "net", "dgram", "cluster", "worker_threads", "vm"
            };
            
            for (String module : dangerousModules) {
                if (lowerCode.contains("require('" + module + "')") || 
                    lowerCode.contains("require(\"" + module + "\")") ||
                    lowerCode.contains("import " + module) ||
                    lowerCode.contains("import * as " + module)) {
                    System.err.println("Dangerous module detected: " + module);
                    return true;
                }
            }
        }
        
        // Language-specific dangerous patterns
        if (language.equals("javascript")) {
            String[] jsDangerous = {
                "global", "window", "document", "localStorage", "sessionStorage",
                "indexeddb", "websocket", "webworker", "sharedworker", "fs.readfile",
                "fs.writefile", "fs.unlink", "fs.rmdir", "fs.mkdir"
            };
            for (String pattern : jsDangerous) {
                if (lowerCode.contains(pattern)) {
                    System.err.println("JavaScript dangerous pattern detected: " + pattern);
                    return true;
                }
            }
        } else if (language.equals("python")) {
            String[] pyDangerous = {
                "import sys", "import subprocess", "import multiprocessing",
                "import threading", "import socket", "import urllib", "import requests",
                "import sqlite3", "import mysql", "import psycopg2", "import pymongo",
                "os.system", "os.popen", "subprocess.call", "subprocess.run"
            };
            for (String pattern : pyDangerous) {
                if (lowerCode.contains(pattern)) {
                    System.err.println("Python dangerous pattern detected: " + pattern);
                    return true;
                }
            }
            
            // Special handling for os import - only block if dangerous os methods are used
            if (lowerCode.contains("import os")) {
                String[] dangerousOsMethods = {
                    "os.system", "os.popen", "os.spawn", "os.exec", "os.kill",
                    "os.remove", "os.unlink", "os.rmdir", "os.makedirs", "os.mkdir",
                    "os.chmod", "os.chown", "os.rename", "os.symlink", "os.link"
                };
                
                boolean hasDangerousOsUsage = false;
                for (String method : dangerousOsMethods) {
                    if (lowerCode.contains(method)) {
                        System.err.println("Python dangerous os method detected: " + method);
                        hasDangerousOsUsage = true;
                        break;
                    }
                }
                
                if (hasDangerousOsUsage) {
                    return true;
                }
                // If only safe os usage (like os.environ), allow it
            }
        }
        
        return false;
    }
    
    /**
     * Basic syntax validation
     */
    private boolean validateSyntax(String code, String language) {
        try {
            if (language.equals("javascript")) {
                return validateJavaScriptSyntax(code);
            } else if (language.equals("python")) {
                return validatePythonSyntax(code);
            }
        } catch (Exception e) {
            System.err.println("Syntax validation error: " + e.getMessage());
            return false;
        }
        return true;
    }
    
    private boolean validateJavaScriptSyntax(String code) {
        // Basic JavaScript syntax validation
        // Check for balanced braces, parentheses, and quotes
        int braces = 0, parentheses = 0, brackets = 0;
        boolean inString = false, inComment = false;
        char stringChar = 0;
        
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            
            if (inComment) {
                if (c == '\n') inComment = false;
                continue;
            }
            
            if (inString) {
                if (c == stringChar) inString = false;
                continue;
            }
            
            switch (c) {
                case '"':
                case '\'':
                    inString = true;
                    stringChar = c;
                    break;
                case '/':
                    if (i + 1 < code.length() && code.charAt(i + 1) == '/') {
                        inComment = true;
                        i++;
                    }
                    break;
                case '{': braces++; break;
                case '}': braces--; break;
                case '(': parentheses++; break;
                case ')': parentheses--; break;
                case '[': brackets++; break;
                case ']': brackets--; break;
            }
        }
        
        return braces == 0 && parentheses == 0 && brackets == 0 && !inString && !inComment;
    }
    
    private boolean validatePythonSyntax(String code) {
        // Basic Python syntax validation
        // Check for balanced parentheses, brackets, and quotes
        int parentheses = 0, brackets = 0, braces = 0;
        boolean inString = false, inComment = false;
        char stringChar = 0;
        
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            
            if (inComment) {
                if (c == '\n') inComment = false;
                continue;
            }
            
            if (inString) {
                if (c == stringChar) inString = false;
                continue;
            }
            
            switch (c) {
                case '"':
                case '\'':
                    inString = true;
                    stringChar = c;
                    break;
                case '#':
                    inComment = true;
                    break;
                case '(': parentheses++; break;
                case ')': parentheses--; break;
                case '[': brackets++; break;
                case ']': brackets--; break;
                case '{': braces++; break;
                case '}': braces--; break;
            }
        }
        
        return parentheses == 0 && brackets == 0 && braces == 0 && !inString && !inComment;
    }
    
    /**
     * Applies language-specific sanitization
     */
    private String applyLanguageSpecificSanitization(String code, String language) {
        if (language.equals("javascript")) {
            return sanitizeJavaScriptCode(code);
        } else if (language.equals("python")) {
            return sanitizePythonCode(code);
        }
        return code;
    }
    
    private String sanitizeJavaScriptCode(String code) {
        // Remove any potential script tags
        code = code.replaceAll("</?script[^>]*>", "");
        
        // Ensure proper input handling
        if (!code.contains("process.env.LAMBDA_INPUT")) {
            // Add input handling if not present
            code = "// Input handling\nconst input = JSON.parse(process.env.LAMBDA_INPUT || '{}');\n\n" + code;
        }
        
        return code;
    }
    
    private String sanitizePythonCode(String code) {
        // Remove any potential script tags
        code = code.replaceAll("</?script[^>]*>", "");
        
        // Ensure proper input handling
        if (!code.contains("os.environ.get('LAMBDA_INPUT'") && !code.contains("LAMBDA_INPUT")) {
            // Add input handling if not present
            code = "# Input handling\nimport json\nimport os\n\ninput_data = json.loads(os.environ.get('LAMBDA_INPUT', '{}'))\n\n" + code;
        }
        
        return code;
    }

    /**
     * Checks rate limits for a user
     */
    private void checkRateLimits(String userEmail) {
        RateLimitInfo rateLimit = rateLimitMap.computeIfAbsent(userEmail, k -> new RateLimitInfo());
        
        if (!rateLimit.canMakeRequest()) {
            throw new RuntimeException("Rate limit exceeded. Please try again later. " +
                "Current usage: " + rateLimit.getMinuteCount() + "/" + MAX_REQUESTS_PER_MINUTE + 
                " per minute, " + rateLimit.getHourCount() + "/" + MAX_REQUESTS_PER_HOUR + " per hour");
        }
        
        rateLimit.incrementCounters();
    }
    
    /**
     * Cleans the cache to prevent memory issues
     */
    private void cleanCache() {
        // Remove oldest entries (simple FIFO approach)
        if (lambdaCache.size() > MAX_CACHE_SIZE / 2) {
            lambdaCache.clear();
        }
        if (codeCache.size() > MAX_CACHE_SIZE / 2) {
            codeCache.clear();
        }
    }
    
    /**
     * Invalidates cache for a specific lambda
     */
    private void invalidateCache(Long lambdaId) {
        lambdaCache.remove(lambdaId);
    }
    
    /**
     * Shutdown executor service (call on application shutdown)
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
} 