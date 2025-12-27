package com.finpass.issuer.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Entity for tracking credential revocation status
 */
@Entity
@Table(name = "credential_status")
public class CredentialStatusEntity {

    public enum Status {
        VALID,
        REVOKED,
        SUSPENDED
    }

    public enum RevocationReason {
        FRAUD,
        COMPROMISED,
        ERROR,
        EXPIRED,
        USER_REQUEST,
        ADMIN_DECISION
    }

    @Id
    private java.util.UUID id;

    @OneToOne
    @JoinColumn(name = "credential_id", nullable = false, unique = true)
    private CredentialEntity credential;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revocation_reason")
    @Enumerated(EnumType.STRING)
    private RevocationReason revocationReason;

    @Column(name = "revoked_by", length = 255)
    private String revokedBy;

    @Column(name = "reason_description", length = 1000)
    private String reasonDescription;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Constructors
    public CredentialStatusEntity() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.status = Status.VALID;
    }

    public CredentialStatusEntity(CredentialEntity credential) {
        this();
        this.credential = credential;
        this.id = java.util.UUID.randomUUID();
    }

    // Getters and Setters
    public java.util.UUID getId() {
        return id;
    }

    public void setId(java.util.UUID id) {
        this.id = id;
    }

    public CredentialEntity getCredential() {
        return credential;
    }

    public void setCredential(CredentialEntity credential) {
        this.credential = credential;
        this.updatedAt = Instant.now();
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
        this.updatedAt = Instant.now();
        
        // Set revoked timestamp when status changes to REVOKED
        if (status == Status.REVOKED && this.revokedAt == null) {
            this.revokedAt = Instant.now();
        }
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
        this.updatedAt = Instant.now();
    }

    public RevocationReason getRevocationReason() {
        return revocationReason;
    }

    public void setRevocationReason(RevocationReason revocationReason) {
        this.revocationReason = revocationReason;
        this.updatedAt = Instant.now();
    }

    public String getRevokedBy() {
        return revokedBy;
    }

    public void setRevokedBy(String revokedBy) {
        this.revokedBy = revokedBy;
        this.updatedAt = Instant.now();
    }

    public String getReasonDescription() {
        return reasonDescription;
    }

    public void setReasonDescription(String reasonDescription) {
        this.reasonDescription = reasonDescription;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Revoke this credential
     */
    public void revoke(RevocationReason reason, String revokedBy, String description) {
        this.status = Status.REVOKED;
        this.revocationReason = reason;
        this.revokedBy = revokedBy;
        this.reasonDescription = description;
        this.revokedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Suspend this credential
     */
    public void suspend(String revokedBy, String description) {
        this.status = Status.SUSPENDED;
        this.revokedBy = revokedBy;
        this.reasonDescription = description;
        this.updatedAt = Instant.now();
    }

    /**
     * Reinstate this credential (from suspended)
     */
    public void reinstate(String reinstatedBy) {
        this.status = Status.VALID;
        this.revokedBy = reinstatedBy;
        this.reasonDescription = "Credential reinstated by " + reinstatedBy;
        this.updatedAt = Instant.now();
    }

    /**
     * Check if credential is currently valid
     */
    public boolean isValid() {
        return this.status == Status.VALID;
    }

    /**
     * Check if credential is revoked
     */
    public boolean isRevoked() {
        return this.status == Status.REVOKED;
    }

    /**
     * Check if credential is suspended
     */
    public boolean isSuspended() {
        return this.status == Status.SUSPENDED;
    }
}
