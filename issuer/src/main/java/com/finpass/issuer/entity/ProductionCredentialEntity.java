package com.finpass.issuer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Production Credential Entity with enhanced schema
 */
@Entity
@Table(name = "credentials", 
       indexes = {
           @Index(name = "idx_user_id", columnList = "user_id"),
           @Index(name = "idx_status", columnList = "status"),
           @Index(name = "idx_issuer_id", columnList = "issuer_id"),
           @Index(name = "idx_issued_at", columnList = "issued_at"),
           @Index(name = "idx_expires_at", columnList = "expires_at"),
           @Index(name = "idx_revocation_handle", columnList = "revocation_handle")
       })
public class ProductionCredentialEntity {

    @Id
    private UUID id;

    @ManyToOne
    @jakarta.persistence.JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "issuer_id", nullable = false, length = 255)
    private String issuerId;

    @Column(name = "credential_type", nullable = false, length = 100)
    private String credentialType;

    @Column(name = "credential_jwt", nullable = false, columnDefinition = "TEXT")
    private String credentialJwt;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "VALID";

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revocation_handle", length = 255)
    private String revocationHandle;

    // Additional production fields
    @Column(name = "revocation_reason", length = 100)
    private String revocationReason;

    @Column(name = "revoked_by", length = 255)
    private String revokedBy;

    @Column(name = "credential_hash", length = 255, unique = true)
    private String credentialHash;

    @Column(name = "nonce", length = 255)
    private String nonce;

    @Column(name = "subject_did", length = 255)
    private String subjectDid;

    @Column(name = "verification_method", length = 100)
    private String verificationMethod;

    @Column(name = "proof_type", length = 50)
    private String proofType;

    @Column(name = "proof_created_at")
    private Instant proofCreatedAt;

    @Column(name = "proof_purpose", length = 50)
    private String proofPurpose;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Constructors
    public ProductionCredentialEntity() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public ProductionCredentialEntity(UserEntity user, String issuerId, String credentialType) {
        this();
        this.id = UUID.randomUUID();
        this.user = user;
        this.issuerId = issuerId;
        this.credentialType = credentialType;
        this.issuedAt = Instant.now();
        this.status = "VALID";
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
        this.updatedAt = Instant.now();
    }

    public String getIssuerId() {
        return issuerId;
    }

    public void setIssuerId(String issuerId) {
        this.issuerId = issuerId;
        this.updatedAt = Instant.now();
    }

    public String getCredentialType() {
        return credentialType;
    }

    public void setCredentialType(String credentialType) {
        this.credentialType = credentialType;
        this.updatedAt = Instant.now();
    }

    public String getCredentialJwt() {
        return credentialJwt;
    }

    public void setCredentialJwt(String credentialJwt) {
        this.credentialJwt = credentialJwt;
        this.updatedAt = Instant.now();
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
        this.updatedAt = Instant.now();
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
        this.updatedAt = Instant.now();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
        this.updatedAt = Instant.now();
    }

    public String getRevocationHandle() {
        return revocationHandle;
    }

    public void setRevocationHandle(String revocationHandle) {
        this.revocationHandle = revocationHandle;
        this.updatedAt = Instant.now();
    }

    public String getRevocationReason() {
        return revocationReason;
    }

    public void setRevocationReason(String revocationReason) {
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

    public String getCredentialHash() {
        return credentialHash;
    }

    public void setCredentialHash(String credentialHash) {
        this.credentialHash = credentialHash;
        this.updatedAt = Instant.now();
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
        this.updatedAt = Instant.now();
    }

    public String getSubjectDid() {
        return subjectDid;
    }

    public void setSubjectDid(String subjectDid) {
        this.subjectDid = subjectDid;
        this.updatedAt = Instant.now();
    }

    public String getVerificationMethod() {
        return verificationMethod;
    }

    public void setVerificationMethod(String verificationMethod) {
        this.verificationMethod = verificationMethod;
        this.updatedAt = Instant.now();
    }

    public String getProofType() {
        return proofType;
    }

    public void setProofType(String proofType) {
        this.proofType = proofType;
        this.updatedAt = Instant.now();
    }

    public Instant getProofCreatedAt() {
        return proofCreatedAt;
    }

    public void setProofCreatedAt(Instant proofCreatedAt) {
        this.proofCreatedAt = proofCreatedAt;
        this.updatedAt = Instant.now();
    }

    public String getProofPurpose() {
        return proofPurpose;
    }

    public void setProofPurpose(String proofPurpose) {
        this.proofPurpose = proofPurpose;
        this.updatedAt = Instant.now();
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
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

    // Business methods
    public boolean isValid() {
        return "VALID".equals(this.status);
    }

    public boolean isRevoked() {
        return "REVOKED".equals(this.status);
    }

    public boolean isSuspended() {
        return "SUSPENDED".equals(this.status);
    }

    public boolean isExpired() {
        return this.expiresAt != null && Instant.now().isAfter(this.expiresAt);
    }

    public void revoke(String reason, String revokedBy) {
        this.status = "REVOKED";
        this.revokedAt = Instant.now();
        this.revocationReason = reason;
        this.revokedBy = revokedBy;
        this.updatedAt = Instant.now();
    }

    public void suspend() {
        this.status = "SUSPENDED";
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.status = "VALID";
        this.updatedAt = Instant.now();
    }

    @Override
    public String toString() {
        return "ProductionCredentialEntity{" +
                "id=" + id +
                ", issuerId='" + issuerId + '\'' +
                ", credentialType='" + credentialType + '\'' +
                ", status='" + status + '\'' +
                ", issuedAt=" + issuedAt +
                ", expiresAt=" + expiresAt +
                '}';
    }
}
