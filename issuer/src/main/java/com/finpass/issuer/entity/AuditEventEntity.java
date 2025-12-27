package com.finpass.issuer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Audit Event Entity for comprehensive audit logging
 */
@Entity
@Table(name = "audit_events", 
       indexes = {
           @Index(name = "idx_event_type", columnList = "event_type"),
           @Index(name = "idx_created_at", columnList = "created_at"),
           @Index(name = "idx_user_id_hash", columnList = "user_id_hash"),
           @Index(name = "idx_session_id", columnList = "session_id"),
           @Index(name = "idx_ip_address", columnList = "ip_address")
       })
public class AuditEventEntity {

    public enum EventType {
        // Credential Events
        CREDENTIAL_ISSUED,
        CREDENTIAL_REVOKED,
        CREDENTIAL_SUSPENDED,
        CREDENTIAL_REINSTATED,
        CREDENTIAL_VERIFIED,
        CREDENTIAL_PRESENTED,
        PRESENTATION_VERIFIED,
        
        // User Events
        USER_REGISTERED,
        USER_LOGIN,
        USER_LOGOUT,
        USER_SUSPENDED,
        USER_TERMINATED,
        USER_KYC_VERIFIED,
        USER_KYC_FAILED,
        
        // Payment Events
        PAYMENT_INITIATED,
        PAYMENT_AUTHORIZED,
        PAYMENT_CAPTURED,
        PAYMENT_COMPLETED,
        PAYMENT_FAILED,
        PAYMENT_REFUNDED,
        PAYMENT_CANCELLED,
        
        // Security Events
        SECURITY_BREACH_ATTEMPT,
        AUTHENTICATION_FAILED,
        AUTHENTICATION_SUCCESS,
        AUTHORIZATION_FAILED,
        TOKEN_ISSUED,
        TOKEN_REVOKED,
        
        // System Events
        SYSTEM_ERROR,
        SYSTEM_STARTUP,
        SYSTEM_SHUTDOWN,
        CONFIGURATION_CHANGED,
        DATA_EXPORTED,
        DATA_IMPORTED,
        
        // Admin Events
        ADMIN_LOGIN,
        ADMIN_LOGOUT,
        ADMIN_ACTION,
        BULK_OPERATION,
        DATA_PURGE
    }

    @Id
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "user_id_hash", length = 64)
    private String userIdHash;

    @Column(name = "session_id", length = 255)
    private String sessionId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "tenant_id", length = 255)
    private String tenantId;

    @Column(name = "resource_id", length = 255)
    private String resourceId;

    @Column(name = "resource_type", length = 100)
    private String resourceType;

    @Column(name = "action", length = 100)
    private String action;

    @Column(name = "outcome", length = 20)
    private String outcome;

    @Column(name = "severity", length = 20)
    private String severity = "INFO";

    @Column(name = "description", length = 1000)
    private String description;

    @Lob
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "previous_values", columnDefinition = "TEXT")
    private String previousValues;

    @Column(name = "new_values", columnDefinition = "TEXT")
    private String newValues;

    @Column(name = "performed_by", length = 255)
    private String performedBy;

    @Column(name = "performed_by_role", length = 100)
    private String performedByRole;

    @Column(name = "source_system", length = 100)
    private String sourceSystem;

    @Column(name = "correlation_id", length = 255)
    private String correlationId;

    @Column(name = "request_id", length = 255)
    private String requestId;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Column(name = "client_version", length = 50)
    private String clientVersion;

    @Column(name = "api_version", length = 50)
    private String apiVersion;

    @Column(name = "geolocation", length = 255)
    private String geolocation;

    @Column(name = "device_fingerprint", length = 255)
    private String deviceFingerprint;

    @Column(name = "compliance_flags", length = 255)
    private String complianceFlags;

    @Column(name = "retention_days")
    private Integer retentionDays;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Constructors
    public AuditEventEntity() {
        this.createdAt = Instant.now();
    }

    public AuditEventEntity(EventType eventType) {
        this();
        this.id = UUID.randomUUID();
        this.eventType = eventType.name();
    }

    public AuditEventEntity(EventType eventType, String userIdHash) {
        this(eventType);
        this.userIdHash = userIdHash;
    }

    // Static factory methods
    public static AuditEventEntity credentialIssued(String userIdHash, String credentialId) {
        AuditEventEntity event = new AuditEventEntity(EventType.CREDENTIAL_ISSUED, userIdHash);
        event.setResourceId(credentialId);
        event.setResourceType("CREDENTIAL");
        event.setAction("ISSUE");
        event.setOutcome("SUCCESS");
        event.setDescription("Credential issued successfully");
        return event;
    }

    public static AuditEventEntity credentialRevoked(String userIdHash, String credentialId, String reason) {
        AuditEventEntity event = new AuditEventEntity(EventType.CREDENTIAL_REVOKED, userIdHash);
        event.setResourceId(credentialId);
        event.setResourceType("CREDENTIAL");
        event.setAction("REVOKE");
        event.setOutcome("SUCCESS");
        event.setDescription("Credential revoked: " + reason);
        event.setSeverity("WARNING");
        return event;
    }

    public static AuditEventEntity userLogin(String userIdHash, String ipAddress, String userAgent) {
        AuditEventEntity event = new AuditEventEntity(EventType.USER_LOGIN, userIdHash);
        event.setIpAddress(ipAddress);
        event.setUserAgent(userAgent);
        event.setAction("LOGIN");
        event.setOutcome("SUCCESS");
        event.setDescription("User logged in successfully");
        return event;
    }

    public static AuditEventEntity authenticationFailed(String ipAddress, String reason) {
        AuditEventEntity event = new AuditEventEntity(EventType.AUTHENTICATION_FAILED);
        event.setIpAddress(ipAddress);
        event.setAction("AUTHENTICATE");
        event.setOutcome("FAILURE");
        event.setDescription("Authentication failed: " + reason);
        event.setSeverity("WARNING");
        return event;
    }

    public static AuditEventEntity paymentInitiated(String userIdHash, String paymentId, BigDecimal amount) {
        AuditEventEntity event = new AuditEventEntity(EventType.PAYMENT_INITIATED, userIdHash);
        event.setResourceId(paymentId);
        event.setResourceType("PAYMENT");
        event.setAction("INITIATE");
        event.setOutcome("SUCCESS");
        event.setDescription("Payment initiated: " + amount);
        return event;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getUserIdHash() {
        return userIdHash;
    }

    public void setUserIdHash(String userIdHash) {
        this.userIdHash = userIdHash;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getPreviousValues() {
        return previousValues;
    }

    public void setPreviousValues(String previousValues) {
        this.previousValues = previousValues;
    }

    public String getNewValues() {
        return newValues;
    }

    public void setNewValues(String newValues) {
        this.newValues = newValues;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public String getPerformedByRole() {
        return performedByRole;
    }

    public void setPerformedByRole(String performedByRole) {
        this.performedByRole = performedByRole;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getClientVersion() {
        return clientVersion;
    }

    public void setClientVersion(String clientVersion) {
        this.clientVersion = clientVersion;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getGeolocation() {
        return geolocation;
    }

    public void setGeolocation(String geolocation) {
        this.geolocation = geolocation;
    }

    public String getDeviceFingerprint() {
        return deviceFingerprint;
    }

    public void setDeviceFingerprint(String deviceFingerprint) {
        this.deviceFingerprint = deviceFingerprint;
    }

    public String getComplianceFlags() {
        return complianceFlags;
    }

    public void setComplianceFlags(String complianceFlags) {
        this.complianceFlags = complianceFlags;
    }

    public Integer getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(Integer retentionDays) {
        this.retentionDays = retentionDays;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    // Business methods
    public boolean isError() {
        return "ERROR".equals(this.severity);
    }

    public boolean isWarning() {
        return "WARNING".equals(this.severity);
    }

    public boolean isInfo() {
        return "INFO".equals(this.severity);
    }

    public boolean isCritical() {
        return "CRITICAL".equals(this.severity);
    }

    public void markAsError(String errorCode, String errorMessage, String stackTrace) {
        this.severity = "ERROR";
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.stackTrace = stackTrace;
    }

    public void markAsWarning() {
        this.severity = "WARNING";
    }

    public void markAsCritical() {
        this.severity = "CRITICAL";
    }

    @Override
    public String toString() {
        return "AuditEventEntity{" +
                "id=" + id +
                ", eventType='" + eventType + '\'' +
                ", userIdHash='" + userIdHash + '\'' +
                ", action='" + action + '\'' +
                ", outcome='" + outcome + '\'' +
                ", severity='" + severity + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
