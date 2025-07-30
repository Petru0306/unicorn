package com.open.unicorn;

import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class DockerService {

    public boolean isDockerAvailable() {
        try {
            Process process = Runtime.getRuntime().exec("docker --version");
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public String createContainer(String imageName, String containerName, Integer port) {
        try {
            // Pull the image first
            Process pullProcess = Runtime.getRuntime().exec("docker pull " + imageName);
            pullProcess.waitFor();

            // Create and start container
            String dockerCommand = "docker run -d";
            if (port != null) {
                dockerCommand += " -p " + port + ":80";
            }
            dockerCommand += " --name " + containerName + " " + imageName;

            Process process = Runtime.getRuntime().exec(dockerCommand);
            process.waitFor();

            // Get container ID
            Process idProcess = Runtime.getRuntime().exec("docker ps -q --filter name=" + containerName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(idProcess.getInputStream()));
            String containerId = reader.readLine();
            reader.close();

            return containerId;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create container: " + e.getMessage(), e);
        }
    }

    public void startContainer(String containerId) {
        try {
            Process process = Runtime.getRuntime().exec("docker start " + containerId);
            process.waitFor();
        } catch (Exception e) {
            throw new RuntimeException("Failed to start container: " + e.getMessage(), e);
        }
    }

    public void stopContainer(String containerId) {
        try {
            Process process = Runtime.getRuntime().exec("docker stop " + containerId);
            process.waitFor();
        } catch (Exception e) {
            throw new RuntimeException("Failed to stop container: " + e.getMessage(), e);
        }
    }

    public void deleteContainer(String containerId) {
        try {
            Process process = Runtime.getRuntime().exec("docker rm -f " + containerId);
            process.waitFor();
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete container: " + e.getMessage(), e);
        }
    }

    public String getContainerStatus(String containerId) {
        try {
            Process process = Runtime.getRuntime().exec("docker inspect --format='{{.State.Status}}' " + containerId);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String status = reader.readLine();
            reader.close();
            return status != null ? status.toUpperCase() : "UNKNOWN";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    public boolean containerExists(String containerId) {
        try {
            Process process = Runtime.getRuntime().exec("docker ps -a --filter id=" + containerId + " --format '{{.ID}}'");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String result = reader.readLine();
            reader.close();
            return result != null && !result.trim().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public List<String> listContainers() {
        List<String> containers = new ArrayList<>();
        try {
            Process process = Runtime.getRuntime().exec("docker ps -a --format '{{.ID}} {{.Names}} {{.Status}}'");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                containers.add(line);
            }
            reader.close();
        } catch (Exception e) {
            // Return empty list if Docker is not available
        }
        return containers;
    }

    public String getContainerLogs(String containerId, int lines) {
        try {
            Process process = Runtime.getRuntime().exec("docker logs --tail " + lines + " " + containerId);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder logs = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                logs.append(line).append("\n");
            }
            reader.close();
            return logs.toString();
        } catch (Exception e) {
            return "Failed to retrieve logs: " + e.getMessage();
        }
    }

    public Map<String, Object> getContainerStats(String containerId) {
        Map<String, Object> stats = new HashMap<>();
        try {
            // Get CPU and memory usage with simpler format
            Process process = Runtime.getRuntime().exec("docker stats --no-stream --format \"{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}\t{{.BlockIO}}\" " + containerId);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine(); // Get actual stats (no header with this format)
            reader.close();
            
            if (line != null && !line.trim().isEmpty()) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 4) {
                    stats.put("cpuUsage", parts[0]);
                    stats.put("memoryUsage", parts[1]);
                    stats.put("networkIO", parts[2]);
                    stats.put("blockIO", parts[3]);
                } else {
                    // Fallback: try to get stats with default format
                    Process fallbackProcess = Runtime.getRuntime().exec("docker stats --no-stream " + containerId);
                    BufferedReader fallbackReader = new BufferedReader(new InputStreamReader(fallbackProcess.getInputStream()));
                    String fallbackLine = fallbackReader.readLine(); // Skip header
                    fallbackLine = fallbackReader.readLine(); // Get actual stats
                    fallbackReader.close();
                    
                    if (fallbackLine != null && !fallbackLine.trim().isEmpty()) {
                        // Parse the default format: CONTAINER ID   NAME   CPU %   MEM USAGE / LIMIT   MEM %   NET I/O   BLOCK I/O   PIDS
                        String[] fallbackParts = fallbackLine.split("\\s+");
                        if (fallbackParts.length >= 7) {
                            stats.put("cpuUsage", fallbackParts[2]);
                            stats.put("memoryUsage", fallbackParts[3] + " / " + fallbackParts[5]);
                            stats.put("networkIO", fallbackParts[6]);
                            stats.put("blockIO", fallbackParts[7]);
                        }
                    }
                }
            }
            
            // If still no stats, provide default values
            if (stats.isEmpty()) {
                stats.put("cpuUsage", "0.00%");
                stats.put("memoryUsage", "0B / 0B");
                stats.put("networkIO", "0B / 0B");
                stats.put("blockIO", "0B / 0B");
                stats.put("error", "No stats available");
            }
        } catch (Exception e) {
            stats.put("error", "Failed to get stats: " + e.getMessage());
            // Provide fallback values
            stats.put("cpuUsage", "0.00%");
            stats.put("memoryUsage", "0B / 0B");
            stats.put("networkIO", "0B / 0B");
            stats.put("blockIO", "0B / 0B");
        }
        return stats;
    }
} 