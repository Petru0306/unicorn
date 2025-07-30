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

@Service
public class LambdaService {

    @Autowired
    private LambdaRepository lambdaRepository;

    @Autowired
    private LambdaExecutionRepository executionRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Lambda> getUserLambdas() {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(userEmail);
        return lambdaRepository.findByUser(user);
    }

    public Optional<Lambda> getUserLambda(Long id) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(userEmail);
        return lambdaRepository.findByUserAndId(user, id);
    }

    public Lambda createLambda(String name, String language, String code) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(userEmail);
        
        Lambda lambda = new Lambda(user, name, language, code);
        return lambdaRepository.save(lambda);
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
            System.out.println("Lambda deleted successfully");
            
        } catch (Exception e) {
            System.err.println("Error deleting lambda " + id + ": " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public LambdaExecution executeLambda(Long lambdaId, String input) {
        Optional<Lambda> lambdaOpt = getUserLambda(lambdaId);
        if (!lambdaOpt.isPresent()) {
            throw new RuntimeException("Lambda not found");
        }

        Lambda lambda = lambdaOpt.get();
        long startTime = System.currentTimeMillis();
        
        try {
            // Simulate Docker container execution with resource limits
            ExecutionResult result = executeInContainer(lambda.getLanguage(), lambda.getCode(), input);
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            LambdaExecution execution = new LambdaExecution(
                lambda, input, result.output, result.error, result.success, executionTime
            );
            
            return executionRepository.save(execution);
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            LambdaExecution execution = new LambdaExecution(
                lambda, input, "", e.getMessage(), false, executionTime
            );
            return executionRepository.save(execution);
        }
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

    private ExecutionResult executeInContainer(String language, String code, String input) {
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
            System.out.println("Input length: " + input.length());
            System.out.println("Input bytes: " + java.util.Arrays.toString(input.getBytes()));
            
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
            
            // Debug: Print the command being executed
            System.out.println("Command: " + String.join(" ", pb.command()));
            System.out.println("Working directory: " + pb.directory());

            // Set resource limits (simulated)
            Process process = pb.start();
            
            // Set timeout (2 seconds)
            boolean completed = process.waitFor(2, TimeUnit.SECONDS);
            
            if (!completed) {
                process.destroyForcibly();
                return new ExecutionResult("", "Execution timeout (2s limit exceeded)", false);
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
} 