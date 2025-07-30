package com.open.unicorn;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_executions")
public class AIExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_function_id", nullable = false)
    private AIFunction aiFunction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String input;

    @Column(columnDefinition = "TEXT")
    private String output;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    @Column(name = "status", nullable = false)
    private String status; // success, error, timeout

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "input_size_bytes")
    private Long inputSizeBytes;

    @Column(name = "output_size_bytes")
    private Long outputSizeBytes;

    // Constructors
    public AIExecution() {
        this.executedAt = LocalDateTime.now();
        this.status = "success";
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AIFunction getAiFunction() {
        return aiFunction;
    }

    public void setAiFunction(AIFunction aiFunction) {
        this.aiFunction = aiFunction;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getInputSizeBytes() {
        return inputSizeBytes;
    }

    public void setInputSizeBytes(Long inputSizeBytes) {
        this.inputSizeBytes = inputSizeBytes;
    }

    public Long getOutputSizeBytes() {
        return outputSizeBytes;
    }

    public void setOutputSizeBytes(Long outputSizeBytes) {
        this.outputSizeBytes = outputSizeBytes;
    }
} 