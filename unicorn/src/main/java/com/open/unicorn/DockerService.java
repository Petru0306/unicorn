package com.open.unicorn;

import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

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
} 