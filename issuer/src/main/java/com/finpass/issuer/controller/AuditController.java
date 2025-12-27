package com.finpass.issuer.controller;

import com.finpass.issuer.entity.AuditEventEntity;
import com.finpass.issuer.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for audit event queries and metrics
 */
@RestController
@RequestMapping("/audit")
public class AuditController {

    private static final Logger logger = LoggerFactory.getLogger(AuditController.class);

    private final AuditService auditService;

    @Autowired
    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Get audit events for a specific user
     */
    @GetMapping("/events/{userHash}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<List<AuditEventEntity>> getUserEvents(@PathVariable String userHash) {
        try {
            List<AuditEventEntity> events = auditService.getUserEvents(userHash);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            logger.error("Failed to get events for user hash: {}", userHash, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get filtered audit events
     */
    @GetMapping("/events")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<List<AuditEventEntity>> getFilteredEvents(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String outcome,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        
        try {
            List<AuditEventEntity> events;
            
            if (type != null && from != null && to != null) {
                // Get events by type and date range
                AuditEventEntity.EventType eventType = AuditEventEntity.EventType.valueOf(type.toUpperCase());
                events = auditService.getEventsByTypeAndDateRange(eventType, from, to);
            } else if (from != null && to != null) {
                // Get events by date range only
                events = auditService.getEventsCreatedBetween(from, to);
            } else {
                // Default to recent events (last 24 hours)
                Instant since = Instant.now().minusSeconds(86400);
                events = auditService.getRecentEvents(since);
            }
            
            // Apply additional filters
            if (severity != null) {
                events = events.stream()
                        .filter(event -> severity.equals(event.getSeverity()))
                        .collect(java.util.stream.Collectors.toList());
            }
            
            if (outcome != null) {
                events = events.stream()
                        .filter(event -> outcome.equals(event.getOutcome()))
                        .collect(java.util.stream.Collectors.toList());
            }
            
            // Apply pagination
            int start = page * size;
            int end = Math.min(start + size, events.size());
            
            if (start >= events.size()) {
                events = List.of();
            } else {
                events = events.subList(start, end);
            }
            
            return ResponseEntity.ok(events);
            
        } catch (Exception e) {
            logger.error("Failed to get filtered events", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get audit metrics and statistics
     */
    @GetMapping("/metrics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<Map<String, Object>> getAuditMetrics() {
        try {
            Object[] stats = auditService.getAuditStatistics();
            
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("totalEvents", stats[0]);
            metrics.put("errorEvents", stats[1]);
            metrics.put("warningEvents", stats[2]);
            metrics.put("criticalEvents", stats[3]);
            metrics.put("failedEvents", stats[4]);
            
            // Add additional metrics
            metrics.put("eventTypes", getEventTypeCounts());
            metrics.put("severityBreakdown", getSeverityBreakdown());
            metrics.put("recentActivity", getRecentActivity());
            metrics.put("topUsers", getTopUsers());
            
            return ResponseEntity.ok(metrics);
            
        } catch (Exception e) {
            logger.error("Failed to get audit metrics", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get events by specific event type
     */
    @GetMapping("/events/type/{eventType}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<List<AuditEventEntity>> getEventsByType(
            @PathVariable String eventType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        
        try {
            AuditEventEntity.EventType type = AuditEventEntity.EventType.valueOf(eventType.toUpperCase());
            Instant since = Instant.now().minusSeconds(86400); // Last 24 hours
            List<AuditEventEntity> events = auditService.getEventsByTypeAndDateRange(type, since, Instant.now());
            
            // Apply pagination
            int start = page * size;
            int end = Math.min(start + size, events.size());
            
            if (start >= events.size()) {
                events = List.of();
            } else {
                events = events.subList(start, end);
            }
            
            return ResponseEntity.ok(events);
            
        } catch (Exception e) {
            logger.error("Failed to get events by type: {}", eventType, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get security events
     */
    @GetMapping("/events/security")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY')")
    public ResponseEntity<List<AuditEventEntity>> getSecurityEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        
        try {
            List<AuditEventEntity> events = auditService.findSecurityEvents();
            
            // Apply pagination
            int start = page * size;
            int end = Math.min(start + size, events.size());
            
            if (start >= events.size()) {
                events = List.of();
            } else {
                events = events.subList(start, end);
            }
            
            return ResponseEntity.ok(events);
            
        } catch (Exception e) {
            logger.error("Failed to get security events", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get error events
     */
    @GetMapping("/events/errors")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<List<AuditEventEntity>> getErrorEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        
        try {
            List<AuditEventEntity> events = auditService.findErrorEvents();
            
            // Apply pagination
            int start = page * size;
            int end = Math.min(start + size, events.size());
            
            if (start >= events.size()) {
                events = List.of();
            } else {
                events = events.subList(start, end);
            }
            
            return ResponseEntity.ok(events);
            
        } catch (Exception e) {
            logger.error("Failed to get error events", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get failed authentication attempts
     */
    @GetMapping("/events/auth-failures")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY')")
    public ResponseEntity<List<AuditEventEntity>> getFailedAuthentications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        
        try {
            List<AuditEventEntity> events = auditService.findFailedAuthentications();
            
            // Apply pagination
            int start = page * size;
            int end = Math.min(start + size, events.size());
            
            if (start >= events.size()) {
                events = List.of();
            } else {
                events = events.subList(start, end);
            }
            
            return ResponseEntity.ok(events);
            
        } catch (Exception e) {
            logger.error("Failed to get failed authentications", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get compliance report
     */
    @GetMapping("/compliance/report")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE')")
    public ResponseEntity<Map<String, Object>> getComplianceReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        
        try {
            List<Object[]> reportData = auditService.getComplianceReport(from, to);
            
            Map<String, Object> report = new HashMap<>();
            report.put("period", Map.of("from", from, "to", to));
            report.put("eventsByTypeAndSeverity", reportData);
            report.put("totalEvents", reportData.stream().mapToLong(row -> (Long) row[2]).sum());
            report.put("errorRate", calculateErrorRate(reportData));
            report.put("criticalEvents", reportData.stream()
                    .filter(row -> "CRITICAL".equals(row[1]))
                    .mapToLong(row -> (Long) row[2]).sum());
            
            return ResponseEntity.ok(report);
            
        } catch (Exception e) {
            logger.error("Failed to get compliance report", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get suspicious activities
     */
    @GetMapping("/security/suspicious")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY')")
    public ResponseEntity<List<Object[]>> getSuspiciousActivities(
            @RequestParam(defaultValue = "10") int threshold) {
        
        try {
            Instant since = Instant.now().minusSeconds(86400); // Last 24 hours
            List<Object[]> activities = auditService.findSuspiciousActivities(since, threshold);
            
            return ResponseEntity.ok(activities);
            
        } catch (Exception e) {
            logger.error("Failed to get suspicious activities", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get user activity timeline
     */
    @GetMapping("/users/{userHash}/timeline")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<List<AuditEventEntity>> getUserTimeline(@PathVariable String userHash) {
        try {
            List<AuditEventEntity> timeline = auditService.getUserActivityTimeline(userHash);
            return ResponseEntity.ok(timeline);
        } catch (Exception e) {
            logger.error("Failed to get user timeline: {}", userHash, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Helper methods for metrics

    private Map<String, Long> getEventTypeCounts() {
        // This would typically query the repository for event type counts
        Map<String, Long> counts = new HashMap<>();
        counts.put("CREDENTIAL_ISSUED", 0L);
        counts.put("CREDENTIAL_REVOKED", 0L);
        counts.put("PRESENTATION_VERIFIED", 0L);
        counts.put("PAYMENT_INITIATED", 0L);
        counts.put("PAYMENT_COMPLETED", 0L);
        counts.put("PAYMENT_FAILED", 0L);
        counts.put("USER_LOGIN", 0L);
        counts.put("AUTHENTICATION_FAILED", 0L);
        return counts;
    }

    private Map<String, Long> getSeverityBreakdown() {
        // This would typically query the repository for severity counts
        Map<String, Long> breakdown = new HashMap<>();
        breakdown.put("INFO", 0L);
        breakdown.put("WARNING", 0L);
        breakdown.put("ERROR", 0L);
        breakdown.put("CRITICAL", 0L);
        return breakdown;
    }

    private Map<String, Object> getRecentActivity() {
        // This would typically query for recent activity metrics
        Map<String, Object> activity = new HashMap<>();
        activity.put("lastHour", 0);
        activity.put("last24Hours", 0);
        activity.put("last7Days", 0);
        return activity;
    }

    private List<Map<String, Object>> getTopUsers() {
        // This would typically query for most active users
        return List.of();
    }

    private double calculateErrorRate(List<Object[]> reportData) {
        long totalEvents = reportData.stream().mapToLong(row -> (Long) row[2]).sum();
        long errorEvents = reportData.stream()
                .filter(row -> "ERROR".equals(row[1]) || "CRITICAL".equals(row[1]))
                .mapToLong(row -> (Long) row[2]).sum();
        
        return totalEvents > 0 ? (double) errorEvents / totalEvents * 100 : 0.0;
    }
}
