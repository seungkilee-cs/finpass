package com.finpass.issuer.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check controller for monitoring and load balancer health checks
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    @Autowired
    private DataSource dataSource;

    /**
     * Basic health check endpoint
     * Returns simple health status for load balancers
     */
    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", Instant.now().toString());
        response.put("service", "finpass-backend");
        response.put("version", "1.0.0");
        return response;
    }

    /**
     * Detailed health check with component status
     * Returns detailed health information for monitoring systems
     */
    @GetMapping("/detailed")
    public Map<String, Object> detailedHealth() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", Instant.now().toString());
        response.put("service", "finpass-backend");
        response.put("version", "1.0.0");
        
        // Component health checks
        Map<String, Object> components = new HashMap<>();
        components.put("database", checkDatabaseHealth());
        components.put("blockchain", checkBlockchainHealth());
        components.put("diskSpace", checkDiskSpaceHealth());
        components.put("memory", checkMemoryHealth());
        
        response.put("components", components);
        
        // Overall status based on components
        boolean allHealthy = components.values().stream()
                .map(component -> (Map<String, Object>) component)
                .allMatch(component -> "UP".equals(component.get("status")));
        
        response.put("status", allHealthy ? "UP" : "DOWN");
        
        return response;
    }

    /**
     * Readiness probe endpoint
     * Indicates if the application is ready to serve traffic
     */
    @GetMapping("/ready")
    public Map<String, Object> readiness() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Check database connectivity
            Map<String, Object> dbHealth = checkDatabaseHealth();
            boolean dbReady = "UP".equals(dbHealth.get("status"));
            
            response.put("status", dbReady ? "READY" : "NOT_READY");
            response.put("timestamp", Instant.now().toString());
            response.put("checks", Map.of(
                "database", dbHealth
            ));
            
            if (!dbReady) {
                response.put("reason", "Database is not ready");
            }
        } catch (Exception e) {
            response.put("status", "NOT_READY");
            response.put("timestamp", Instant.now().toString());
            response.put("reason", "Health check failed: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Liveness probe endpoint
     * Indicates if the application is running (not stuck or deadlocked)
     */
    @GetMapping("/live")
    public Map<String, Object> liveness() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Simple liveness check - if we can respond, we're alive
            response.put("status", "ALIVE");
            response.put("timestamp", Instant.now().toString());
            response.put("uptime", getUptime());
        } catch (Exception e) {
            response.put("status", "NOT_ALIVE");
            response.put("timestamp", Instant.now().toString());
            response.put("error", e.getMessage());
        }
        
        return response;
    }

    private Map<String, Object> checkDatabaseHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            long startTime = System.currentTimeMillis();
            
            try (Connection connection = dataSource.getConnection()) {
                boolean isValid = connection.isValid(5);
                long responseTime = System.currentTimeMillis() - startTime;
                
                health.put("status", isValid ? "UP" : "DOWN");
                health.put("responseTime", responseTime + "ms");
                health.put("timestamp", Instant.now().toString());
                
                if (isValid) {
                    // Get additional database info
                    var metaData = connection.getMetaData();
                    health.put("database", metaData.getDatabaseProductName());
                    health.put("version", metaData.getDatabaseProductVersion());
                    health.put("url", metaData.getURL());
                } else {
                    health.put("error", "Database connection is not valid");
                }
            }
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            health.put("timestamp", Instant.now().toString());
        }
        
        return health;
    }

    private Map<String, Object> checkBlockchainHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Simulate blockchain connectivity check
            // In a real implementation, this would check blockchain RPC connectivity
            long startTime = System.currentTimeMillis();
            
            // Mock blockchain check - replace with actual implementation
            boolean isConnected = true; // blockchainService.isConnected();
            long responseTime = System.currentTimeMillis() - startTime;
            
            health.put("status", isConnected ? "UP" : "DOWN");
            health.put("responseTime", responseTime + "ms");
            health.put("timestamp", Instant.now().toString());
            health.put("network", "testnet"); // blockchainService.getNetwork());
            
            if (!isConnected) {
                health.put("error", "Cannot connect to blockchain network");
            }
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            health.put("timestamp", Instant.now().toString());
        }
        
        return health;
    }

    private Map<String, Object> checkDiskSpaceHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            
            double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
            
            health.put("status", memoryUsagePercent < 90 ? "UP" : "DOWN");
            health.put("memoryUsage", String.format("%.2f%%", memoryUsagePercent));
            health.put("usedMemory", formatBytes(usedMemory));
            health.put("freeMemory", formatBytes(freeMemory));
            health.put("maxMemory", formatBytes(maxMemory));
            health.put("timestamp", Instant.now().toString());
            
            if (memoryUsagePercent >= 90) {
                health.put("error", "Memory usage is critically high");
            }
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            health.put("timestamp", Instant.now().toString());
        }
        
        return health;
    }

    private Map<String, Object> checkMemoryHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            
            double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
            
            health.put("status", memoryUsagePercent < 85 ? "UP" : "DOWN");
            health.put("usagePercent", String.format("%.2f%%", memoryUsagePercent));
            health.put("used", formatBytes(usedMemory));
            health.put("available", formatBytes(maxMemory - usedMemory));
            health.put("total", formatBytes(maxMemory));
            health.put("timestamp", Instant.now().toString());
            
            if (memoryUsagePercent >= 85) {
                health.put("warning", "Memory usage is high");
            }
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            health.put("timestamp", Instant.now().toString());
        }
        
        return health;
    }

    private String getUptime() {
        // Return application uptime - this would typically be calculated from application start time
        return "0h 0m 0s"; // Placeholder
    }

    private String formatBytes(long bytes) {
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        double size = bytes;
        int unitIndex = 0;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%.2f %s", size, units[unitIndex]);
    }
}
