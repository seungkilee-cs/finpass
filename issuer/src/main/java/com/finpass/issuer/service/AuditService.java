package com.finpass.issuer.service;

import com.finpass.issuer.entity.AuditEventEntity;
import com.finpass.issuer.repository.AuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Map;

/**
 * Service for comprehensive audit logging with privacy compliance
 */
@Service
@Transactional
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    
    private final AuditEventRepository auditEventRepository;

    @Autowired
    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    /**
     * Log a general audit event
     */
    public void logEvent(AuditEventEntity.EventType eventType, String userId, Map<String, Object> details) {
        logEvent(eventType, userId, details, null, null);
    }

    /**
     * Log an audit event with HTTP context
     */
    public void logEvent(AuditEventEntity.EventType eventType, String userId, Map<String, Object> details,
                        HttpServletRequest request) {
        logEvent(eventType, userId, details, request, null);
    }

    /**
     * Log an audit event with duration tracking
     */
    public void logEvent(AuditEventEntity.EventType eventType, String userId, Map<String, Object> details,
                        Long durationMs) {
        logEvent(eventType, userId, details, null, durationMs);
    }

    /**
     * Log an audit event with full context
     */
    public void logEvent(AuditEventEntity.EventType eventType, String userId, Map<String, Object> details,
                        HttpServletRequest request, Long durationMs) {
        try {
            AuditEventEntity auditEvent = new AuditEventEntity(eventType, hashUserId(userId));
            
            // Set basic event details
            auditEvent.setOutcome(details.containsKey("outcome") ? (String) details.get("outcome") : "SUCCESS");
            auditEvent.setSeverity(details.containsKey("severity") ? (String) details.get("severity") : "INFO");
            auditEvent.setDescription(details.containsKey("description") ? (String) details.get("description") : eventType.name());
            
            // Set HTTP context if available
            if (request != null) {
                auditEvent.setIpAddress(getClientIpAddress(request));
                auditEvent.setUserAgent(request.getHeader("User-Agent"));
                auditEvent.setSessionId(request.getSession().getId());
                auditEvent.setRequestId(request.getHeader("X-Request-ID"));
            }
            
            // Set performance metrics
            if (durationMs != null) {
                auditEvent.setDurationMs(durationMs);
            }
            
            // Set resource information
            if (details.containsKey("resourceId")) {
                auditEvent.setResourceId((String) details.get("resourceId"));
            }
            if (details.containsKey("resourceType")) {
                auditEvent.setResourceType((String) details.get("resourceType"));
            }
            
            // Set action and performer
            if (details.containsKey("action")) {
                auditEvent.setAction((String) details.get("action"));
            }
            if (details.containsKey("performedBy")) {
                auditEvent.setPerformedBy((String) details.get("performedBy"));
            }
            if (details.containsKey("performedByRole")) {
                auditEvent.setPerformedByRole((String) details.get("performedByRole"));
            }
            
            // Set error information if present
            if (details.containsKey("errorCode")) {
                auditEvent.setErrorCode((String) details.get("errorCode"));
            }
            if (details.containsKey("errorMessage")) {
                auditEvent.setErrorMessage((String) details.get("errorMessage"));
            }
            if (details.containsKey("stackTrace")) {
                auditEvent.setStackTrace((String) details.get("stackTrace"));
            }
            
            // Convert details map to JSON string
            auditEvent.setDetails(convertDetailsToJson(details));
            
            // Save the audit event
            auditEventRepository.save(auditEvent);
            
            logger.debug("Audit event logged: {} for user: {}", eventType, userId);
            
        } catch (Exception e) {
            logger.error("Failed to log audit event: {} for user: {}", eventType, userId, e);
            // Don't re-throw to avoid breaking main business flow
        }
    }

    /**
     * Log credential issuance event
     */
    public void logCredentialIssued(String userId, String credentialId, String credentialType, String issuerId) {
        Map<String, Object> details = Map.of(
            "action", "ISSUE",
            "resourceId", credentialId,
            "resourceType", "CREDENTIAL",
            "credentialType", credentialType,
            "issuerId", issuerId,
            "description", "Credential issued successfully"
        );
        
        logEvent(AuditEventEntity.EventType.CREDENTIAL_ISSUED, userId, details);
    }

    /**
     * Log credential revocation event
     */
    public void logCredentialRevoked(String userId, String credentialId, String reason, String revokedBy) {
        Map<String, Object> details = Map.of(
            "action", "REVOKE",
            "resourceId", credentialId,
            "resourceType", "CREDENTIAL",
            "revocationReason", reason,
            "performedBy", revokedBy,
            "severity", "WARNING",
            "description", "Credential revoked: " + reason
        );
        
        logEvent(AuditEventEntity.EventType.CREDENTIAL_REVOKED, userId, details);
    }

    /**
     * Log credential suspension event
     */
    public void logCredentialSuspended(String userId, String credentialId, String reason, String suspendedBy) {
        Map<String, Object> details = Map.of(
            "action", "SUSPEND",
            "resourceId", credentialId,
            "resourceType", "CREDENTIAL",
            "suspensionReason", reason,
            "performedBy", suspendedBy,
            "severity", "WARNING",
            "description", "Credential suspended: " + reason
        );
        
        logEvent(AuditEventEntity.EventType.CREDENTIAL_SUSPENDED, userId, details);
    }

    /**
     * Log credential reinstatement event
     */
    public void logCredentialReinstated(String userId, String credentialId, String reinstatedBy) {
        Map<String, Object> details = Map.of(
            "action", "REINSTATE",
            "resourceId", credentialId,
            "resourceType", "CREDENTIAL",
            "performedBy", reinstatedBy,
            "description", "Credential reinstated successfully"
        );
        
        logEvent(AuditEventEntity.EventType.CREDENTIAL_REINSTATED, userId, details);
    }

    /**
     * Log presentation verification event
     */
    public void logPresentationVerified(String userId, String verifierId, String credentialId, 
                                       boolean decision, String reason) {
        Map<String, Object> details = Map.of(
            "action", "VERIFY",
            "resourceId", credentialId,
            "resourceType", "PRESENTATION",
            "verifierId", verifierId,
            "decision", decision,
            "reason", reason != null ? reason : "",
            "outcome", decision ? "SUCCESS" : "FAILURE",
            "description", "Presentation " + (decision ? "verified" : "rejected") + 
                          (reason != null ? ": " + reason : "")
        );
        
        logEvent(AuditEventEntity.EventType.PRESENTATION_VERIFIED, userId, details);
    }

    /**
     * Log payment initiation event
     */
    public void logPaymentInitiated(String payerDid, String payeeDid, String paymentId, 
                                   BigDecimal amount, String currency) {
        Map<String, Object> details = Map.of(
            "action", "INITIATE",
            "resourceId", paymentId,
            "resourceType", "PAYMENT",
            "payerDid", payerDid,
            "payeeDid", payeeDid,
            "amount", amount.toString(),
            "currency", currency,
            "description", "Payment initiated: " + amount + " " + currency
        );
        
        logEvent(AuditEventEntity.EventType.PAYMENT_INITIATED, payerDid, details);
    }

    /**
     * Log payment completion event
     */
    public void logPaymentCompleted(String payerDid, String paymentId, String status, BigDecimal amount) {
        Map<String, Object> details = Map.of(
            "action", "COMPLETE",
            "resourceId", paymentId,
            "resourceType", "PAYMENT",
            "status", status,
            "amount", amount.toString(),
            "outcome", "SUCCESS".equals(status) ? "SUCCESS" : "FAILURE",
            "description", "Payment " + status.toLowerCase() + ": " + amount
        );
        
        logEvent(AuditEventEntity.EventType.PAYMENT_COMPLETED, payerDid, details);
    }

    /**
     * Log payment failure event
     */
    public void logPaymentFailed(String payerDid, String paymentId, String reason, String errorCode) {
        Map<String, Object> details = Map.of(
            "action", "FAIL",
            "resourceId", paymentId,
            "resourceType", "PAYMENT",
            "failureReason", reason,
            "errorCode", errorCode,
            "outcome", "FAILURE",
            "severity", "WARNING",
            "description", "Payment failed: " + reason
        );
        
        logEvent(AuditEventEntity.EventType.PAYMENT_FAILED, payerDid, details);
    }

    /**
     * Log user authentication event
     */
    public void logUserLogin(String userId, String ipAddress, boolean success, String reason) {
        AuditEventEntity.EventType eventType = success ? 
            AuditEventEntity.EventType.USER_LOGIN : 
            AuditEventEntity.EventType.AUTHENTICATION_FAILED;
            
        Map<String, Object> details = Map.of(
            "action", "LOGIN",
            "outcome", success ? "SUCCESS" : "FAILURE",
            "severity", success ? "INFO" : "WARNING",
            "description", success ? "User logged in successfully" : "Authentication failed: " + reason
        );
        
        logEvent(eventType, userId, details);
    }

    /**
     * Log user registration event
     */
    public void logUserRegistered(String userId, String did) {
        Map<String, Object> details = Map.of(
            "action", "REGISTER",
            "resourceId", userId,
            "resourceType", "USER",
            "userDid", did,
            "description", "User registered successfully"
        );
        
        logEvent(AuditEventEntity.EventType.USER_REGISTERED, userId, details);
    }

    /**
     * Log security event
     */
    public void logSecurityEvent(String userId, String eventType, String description, String severity) {
        Map<String, Object> details = Map.of(
            "action", "SECURITY",
            "severity", severity != null ? severity : "WARNING",
            "description", description,
            "complianceFlags", "SECURITY"
        );
        
        // Use SECURITY_BREACH_ATTEMPT as default security event type
        logEvent(AuditEventEntity.EventType.SECURITY_BREACH_ATTEMPT, userId, details);
    }

    /**
     * Log system event
     */
    public void logSystemEvent(String eventType, String description, Map<String, Object> details) {
        Map<String, Object> systemDetails = Map.of(
            "action", "SYSTEM",
            "sourceSystem", "finpass-issuer",
            "description", description,
            "performedBy", "system"
        );
        
        // Merge with provided details
        if (details != null && !details.isEmpty()) {
            systemDetails = Map.of(
                "action", "SYSTEM",
                "sourceSystem", "finpass-issuer",
                "description", description,
                "performedBy", "system",
                "additionalDetails", convertDetailsToJson(details)
            );
        }
        
        logEvent(AuditEventEntity.EventType.SYSTEM_ERROR, null, systemDetails);
    }

    /**
     * Hash user ID for privacy compliance
     */
    private String hashUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(userId.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            logger.error("Failed to hash user ID", e);
            return userId; // Fallback to plain ID (should not happen)
        }
    }

    /**
     * Get client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Convert details map to JSON string
     */
    private String convertDetailsToJson(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        
        try {
            StringBuilder json = new StringBuilder("{");
            boolean first = true;
            
            for (Map.Entry<String, Object> entry : details.entrySet()) {
                if (!first) {
                    json.append(",");
                }
                json.append("\"").append(entry.getKey()).append("\":");
                
                Object value = entry.getValue();
                if (value instanceof String) {
                    json.append("\"").append(value.toString().replace("\"", "\\\"")).append("\"");
                } else {
                    json.append(value.toString());
                }
                
                first = false;
            }
            
            json.append("}");
            return json.toString();
            
        } catch (Exception e) {
            logger.error("Failed to convert details to JSON", e);
            return details.toString();
        }
    }

    /**
     * Get events created between two timestamps
     */
    public java.util.List<AuditEventEntity> getEventsCreatedBetween(Instant start, Instant end) {
        return auditEventRepository.findEventsCreatedBetween(start, end);
    }

    /**
     * Get recent events since a timestamp
     */
    public java.util.List<AuditEventEntity> getRecentEvents(Instant since) {
        return auditEventRepository.findEventsCreatedBetween(since, Instant.now());
    }

    /**
     * Find security events
     */
    public java.util.List<AuditEventEntity> findSecurityEvents() {
        return auditEventRepository.findByEventType("SECURITY_BREACH_ATTEMPT");
    }

    /**
     * Find error events
     */
    public java.util.List<AuditEventEntity> findErrorEvents() {
        return auditEventRepository.findBySeverity("ERROR");
    }

    /**
     * Find failed authentications
     */
    public java.util.List<AuditEventEntity> findFailedAuthentications() {
        return auditEventRepository.findByEventType("AUTHENTICATION_FAILED");
    }

    /**
     * Get compliance report data
     */
    public java.util.List<Object[]> getComplianceReport(Instant start, Instant end) {
        return auditEventRepository.getComplianceReport(start, end);
    }

    /**
     * Find suspicious activities
     */
    public java.util.List<Object[]> findSuspiciousActivities(Instant since, int threshold) {
        return auditEventRepository.findSuspiciousActivities(since, threshold);
    }

    /**
     * Get user activity timeline
     */
    public java.util.List<AuditEventEntity> getUserActivityTimeline(String userIdHash) {
        return auditEventRepository.findByUserIdHashOrderByCreatedAtDesc(userIdHash);
    }

    /**
     * Get audit statistics
     */
    public Object[] getAuditStatistics() {
        return auditEventRepository.getAuditStatistics();
    }

    /**
     * Get events for a specific user
     */
    public java.util.List<AuditEventEntity> getUserEvents(String userIdHash) {
        return auditEventRepository.findByUserIdHash(userIdHash);
    }

    /**
     * Get events by type and date range
     */
    public java.util.List<AuditEventEntity> getEventsByTypeAndDateRange(
            AuditEventEntity.EventType eventType, Instant start, Instant end) {
        return auditEventRepository.findEventsCreatedBetween(start, end).stream()
                .filter(event -> eventType.name().equals(event.getEventType()))
                .collect(java.util.stream.Collectors.toList());
    }
}
