package com.open.unicorn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/uws-compute")
public class UWSComputeController {

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DockerService dockerService;

    // Instance size configurations
    private static final Map<String, Map<String, Integer>> INSTANCE_SIZES = Map.of(
        "micro", Map.of("cpu", 1, "memory", 512, "maxInstances", 5),
        "small", Map.of("cpu", 2, "memory", 1024, "maxInstances", 3),
        "medium", Map.of("cpu", 4, "memory", 2048, "maxInstances", 2)
    );

    // Create container
    @PostMapping("/containers")
    public ResponseEntity<Map<String, Object>> createContainer(@RequestBody Map<String, Object> request, Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            
            // Extract request parameters
            String imageName = (String) request.get("imageName");
            String instanceSize = (String) request.get("instanceSize");
            Integer customCpu = (Integer) request.get("cpu");
            Integer customMemory = (Integer) request.get("memory");
            Integer port = (Integer) request.get("port");
            String schedule = (String) request.get("schedule");

            // Validate required fields
            if (imageName == null || imageName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Image name is required"));
            }

            if (instanceSize == null || !INSTANCE_SIZES.containsKey(instanceSize)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid instance size. Must be micro, small, or medium"));
            }

            // Check user's running container limit
            long runningContainers = containerRepository.findByOwnerEmailAndStatus(userEmail, "RUNNING").size();
            int maxInstances = INSTANCE_SIZES.get(instanceSize).get("maxInstances");
            
            if (runningContainers >= maxInstances) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Maximum number of running containers reached for " + instanceSize + " instances"));
            }

            // Determine CPU and memory based on instance size or custom values
            int cpu = customCpu != null ? customCpu : INSTANCE_SIZES.get(instanceSize).get("cpu");
            int memory = customMemory != null ? customMemory : INSTANCE_SIZES.get(instanceSize).get("memory");

            // Validate custom values
            if (customCpu != null && (customCpu < 1 || customCpu > 8)) {
                return ResponseEntity.badRequest().body(Map.of("error", "CPU must be between 1 and 8"));
            }

            if (customMemory != null && (customMemory < 256 || customMemory > 8192)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Memory must be between 256 and 8192 MB"));
            }

            if (port != null && (port < 1 || port > 65535)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Port must be between 1 and 65535"));
            }

            // Generate unique instance ID
            String instanceId = "uws-" + UUID.randomUUID().toString().substring(0, 8);

            // Create container
            Container container = new Container(instanceId, userEmail, imageName, cpu, memory, port, instanceSize);
            if (schedule != null && !schedule.trim().isEmpty()) {
                container.setSchedule(schedule);
            }
            
            // Create real Docker container
            try {
                if (dockerService.isDockerAvailable()) {
                    String dockerContainerId = dockerService.createContainer(imageName, instanceId, port);
                    container.setDockerContainerId(dockerContainerId);
                    dockerService.startContainer(dockerContainerId);
                    container.setStatus("RUNNING");
                } else {
                    // Fallback to simulation if Docker is not available
                    container.setStatus("RUNNING");
                }
            } catch (Exception e) {
                // Fallback to simulation if Docker fails
                container.setStatus("RUNNING");
            }
            
            containerRepository.save(container);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Container started successfully");
            response.put("container", Map.of(
                "id", container.getId(),
                "instanceId", container.getInstanceId(),
                "imageName", container.getImageName(),
                "cpu", container.getCpu(),
                "memory", container.getMemory(),
                "port", container.getPort(),
                "instanceSize", container.getInstanceSize(),
                "status", container.getStatus(),
                "createdAt", container.getCreatedAt(),
                "schedule", container.getSchedule()
            ));

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to create container: " + e.getMessage()));
        }
    }

    // List containers
    @GetMapping("/containers")
    public ResponseEntity<Map<String, Object>> listContainers(Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            List<Container> containers = containerRepository.findByOwnerEmail(userEmail);

            List<Map<String, Object>> containerList = containers.stream()
                .map(container -> {
                    Map<String, Object> containerMap = new HashMap<>();
                    containerMap.put("id", container.getId());
                    containerMap.put("instanceId", container.getInstanceId());
                    containerMap.put("imageName", container.getImageName());
                    containerMap.put("cpu", container.getCpu());
                    containerMap.put("memory", container.getMemory());
                    containerMap.put("port", container.getPort());
                    containerMap.put("instanceSize", container.getInstanceSize());
                    containerMap.put("status", container.getStatus());
                    containerMap.put("createdAt", container.getCreatedAt());
                    containerMap.put("updatedAt", container.getUpdatedAt());
                    containerMap.put("schedule", container.getSchedule());
                    containerMap.put("lastScheduledRun", container.getLastScheduledRun());
                    return containerMap;
                })
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("containers", containerList);
            response.put("count", containerList.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to list containers: " + e.getMessage()));
        }
    }

    // Get container by ID
    @GetMapping("/containers/{instanceId}")
    public ResponseEntity<Map<String, Object>> getContainer(@PathVariable String instanceId, Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            Container container = containerRepository.findByInstanceId(instanceId);

            if (container == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Container not found"));
            }

            if (!container.getOwnerEmail().equals(userEmail)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied"));
            }

            Map<String, Object> response = new HashMap<>();
            Map<String, Object> containerMap = new HashMap<>();
            containerMap.put("id", container.getId());
            containerMap.put("instanceId", container.getInstanceId());
            containerMap.put("imageName", container.getImageName());
            containerMap.put("cpu", container.getCpu());
            containerMap.put("memory", container.getMemory());
            containerMap.put("port", container.getPort());
            containerMap.put("instanceSize", container.getInstanceSize());
            containerMap.put("status", container.getStatus());
            containerMap.put("createdAt", container.getCreatedAt());
            containerMap.put("updatedAt", container.getUpdatedAt());
            containerMap.put("schedule", container.getSchedule());
            containerMap.put("lastScheduledRun", container.getLastScheduledRun());
            response.put("container", containerMap);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get container: " + e.getMessage()));
        }
    }

    // Stop container
    @PostMapping("/containers/{instanceId}/stop")
    public ResponseEntity<Map<String, Object>> stopContainer(@PathVariable String instanceId, Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            Container container = containerRepository.findByInstanceId(instanceId);

            if (container == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Container not found"));
            }

            if (!container.getOwnerEmail().equals(userEmail)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied"));
            }

            if ("STOPPED".equals(container.getStatus())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Container is already stopped"));
            }

            // Stop real Docker container
            try {
                if (dockerService.isDockerAvailable() && container.getDockerContainerId() != null) {
                    dockerService.stopContainer(container.getDockerContainerId());
                }
            } catch (Exception e) {
                // Continue even if Docker operation fails
            }

            // Update status
            container.setStatus("STOPPED");
            container.setUpdatedAt(LocalDateTime.now());
            containerRepository.save(container);

            return ResponseEntity.ok(Map.of("message", "Container stopped successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to stop container: " + e.getMessage()));
        }
    }

    // Start container
    @PostMapping("/containers/{instanceId}/start")
    public ResponseEntity<Map<String, Object>> startContainer(@PathVariable String instanceId, Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            Container container = containerRepository.findByInstanceId(instanceId);

            if (container == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Container not found"));
            }

            if (!container.getOwnerEmail().equals(userEmail)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied"));
            }

            if ("RUNNING".equals(container.getStatus())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Container is already running"));
            }

            // Check user's running container limit
            long runningContainers = containerRepository.findByOwnerEmailAndStatus(userEmail, "RUNNING").size();
            int maxInstances = INSTANCE_SIZES.get(container.getInstanceSize()).get("maxInstances");
            
            if (runningContainers >= maxInstances) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Maximum number of running containers reached for " + container.getInstanceSize() + " instances"));
            }

            // Start real Docker container
            try {
                if (dockerService.isDockerAvailable() && container.getDockerContainerId() != null) {
                    dockerService.startContainer(container.getDockerContainerId());
                }
            } catch (Exception e) {
                // Continue even if Docker operation fails
            }

            // Update status
            container.setStatus("RUNNING");
            container.setUpdatedAt(LocalDateTime.now());
            containerRepository.save(container);

            return ResponseEntity.ok(Map.of("message", "Container started successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to start container: " + e.getMessage()));
        }
    }

    // Delete container
    @DeleteMapping("/containers/{instanceId}")
    public ResponseEntity<Map<String, Object>> deleteContainer(@PathVariable String instanceId, Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            Container container = containerRepository.findByInstanceId(instanceId);

            if (container == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Container not found"));
            }

            if (!container.getOwnerEmail().equals(userEmail)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied"));
            }

            // Delete real Docker container
            try {
                if (dockerService.isDockerAvailable() && container.getDockerContainerId() != null) {
                    dockerService.deleteContainer(container.getDockerContainerId());
                }
            } catch (Exception e) {
                // Continue even if Docker operation fails
            }

            containerRepository.delete(container);

            return ResponseEntity.ok(Map.of("message", "Container deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete container: " + e.getMessage()));
        }
    }

    // Get instance sizes
    @GetMapping("/instance-sizes")
    public ResponseEntity<Map<String, Object>> getInstanceSizes() {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("instanceSizes", INSTANCE_SIZES);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get instance sizes: " + e.getMessage()));
        }
    }

    // Update container schedule
    @PutMapping("/containers/{instanceId}/schedule")
    public ResponseEntity<Map<String, Object>> updateSchedule(
            @PathVariable String instanceId, 
            @RequestBody Map<String, String> request, 
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            String schedule = request.get("schedule");
            
            Container container = containerRepository.findByInstanceId(instanceId);

            if (container == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Container not found"));
            }

            if (!container.getOwnerEmail().equals(userEmail)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied"));
            }

            container.setSchedule(schedule);
            container.setUpdatedAt(LocalDateTime.now());
            containerRepository.save(container);

            return ResponseEntity.ok(Map.of("message", "Schedule updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to update schedule: " + e.getMessage()));
        }
    }

    // Get Docker status
    @GetMapping("/docker-status")
    public ResponseEntity<Map<String, Object>> getDockerStatus() {
        try {
            boolean isAvailable = dockerService.isDockerAvailable();
            Map<String, Object> response = new HashMap<>();
            response.put("dockerAvailable", isAvailable);
            response.put("message", isAvailable ? "Docker is available" : "Docker is not available - using simulation mode");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to check Docker status: " + e.getMessage()));
        }
    }

    // Get container logs
    @GetMapping("/containers/{instanceId}/logs")
    public ResponseEntity<Map<String, Object>> getContainerLogs(
            @PathVariable String instanceId,
            @RequestParam(value = "lines", defaultValue = "50") int lines,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            Container container = containerRepository.findByInstanceId(instanceId);

            if (container == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Container not found"));
            }

            if (!container.getOwnerEmail().equals(userEmail)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied"));
            }

            String logs = "";
            if (dockerService.isDockerAvailable() && container.getDockerContainerId() != null) {
                logs = dockerService.getContainerLogs(container.getDockerContainerId(), lines);
            } else {
                logs = "Docker not available - logs not accessible in simulation mode";
            }

            Map<String, Object> response = new HashMap<>();
            response.put("logs", logs);
            response.put("containerId", instanceId);
            response.put("lines", lines);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get container logs: " + e.getMessage()));
        }
    }

    // Get container stats
    @GetMapping("/containers/{instanceId}/stats")
    public ResponseEntity<Map<String, Object>> getContainerStats(
            @PathVariable String instanceId,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            Container container = containerRepository.findByInstanceId(instanceId);

            if (container == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Container not found"));
            }

            if (!container.getOwnerEmail().equals(userEmail)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied"));
            }

            Map<String, Object> stats = new HashMap<>();
            if (dockerService.isDockerAvailable() && container.getDockerContainerId() != null) {
                System.out.println("Getting stats for container: " + container.getDockerContainerId());
                stats = dockerService.getContainerStats(container.getDockerContainerId());
                System.out.println("Stats received: " + stats);
            } else {
                // Simulated stats for demo
                stats.put("cpuUsage", "0.5%");
                stats.put("memoryUsage", "128MB / 512MB");
                stats.put("networkIO", "1.2MB / 2.1MB");
                stats.put("blockIO", "0B / 0B");
                stats.put("simulated", true);
                System.out.println("Using simulated stats for container: " + instanceId);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("stats", stats);
            response.put("containerId", instanceId);
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error getting container stats: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get container stats: " + e.getMessage()));
        }
    }
} 