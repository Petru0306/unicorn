package com.open.unicorn;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lambda_executions")
public class LambdaExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lambda_id", nullable = false)
    private Lambda lambda;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(columnDefinition = "TEXT")
    private String input; // JSON input data

    @Column(columnDefinition = "TEXT")
    private String output; // stdout from execution

    @Column(columnDefinition = "TEXT")
    private String error; // stderr from execution

    @Column(nullable = false)
    private Boolean success;

    @Column(name = "execution_time")
    private Long executionTime; // in milliseconds

    // Default constructor
    public LambdaExecution() {
        this.timestamp = LocalDateTime.now();
    }

    // Constructor with parameters
    public LambdaExecution(Lambda lambda, String input, String output, String error, Boolean success, Long executionTime) {
        this.lambda = lambda;
        this.input = input;
        this.output = output;
        this.error = error;
        this.success = success;
        this.executionTime = executionTime;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Lambda getLambda() {
        return lambda;
    }

    public void setLambda(Lambda lambda) {
        this.lambda = lambda;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
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

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Long getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(Long executionTime) {
        this.executionTime = executionTime;
    }
} 