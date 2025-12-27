package com.finpass.issuer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * Production User Entity with enhanced schema
 */
@Entity
@Table(name = "users", 
       indexes = {
           @Index(name = "idx_did", columnList = "did"),
           @Index(name = "idx_created_at", columnList = "created_at"),
           @Index(name = "idx_last_seen", columnList = "last_seen")
       },
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_did", columnNames = {"did"})
       })
public class ProductionUserEntity {

    @Id
    private UUID id;

    @Column(name = "did", nullable = false, unique = true, length = 255)
    private String did;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_seen")
    private Instant lastSeen;

    // Additional fields for production use
    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "kyc_verified", nullable = false)
    private Boolean kycVerified = false;

    @Column(name = "kyc_verified_at")
    private Instant kycVerifiedAt;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Constructors
    public ProductionUserEntity() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public ProductionUserEntity(String did) {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.id = UUID.randomUUID();
        this.did = did;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getDid() {
        return did;
    }

    public void setDid(String did) {
        this.did = did;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
        this.updatedAt = Instant.now();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
        this.updatedAt = Instant.now();
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
        this.updatedAt = Instant.now();
    }

    public Boolean getKycVerified() {
        return kycVerified;
    }

    public void setKycVerified(Boolean kycVerified) {
        this.kycVerified = kycVerified;
        if (kycVerified != null && kycVerified) {
            this.kycVerifiedAt = Instant.now();
        }
        this.updatedAt = Instant.now();
    }

    public Instant getKycVerifiedAt() {
        return kycVerifiedAt;
    }

    public void setKycVerifiedAt(Instant kycVerifiedAt) {
        this.kycVerifiedAt = kycVerifiedAt;
        this.updatedAt = Instant.now();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
        this.updatedAt = Instant.now();
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Business methods
    public void updateLastSeen() {
        this.lastSeen = Instant.now();
        this.updatedAt = Instant.now();
    }

    public boolean isActive() {
        return "ACTIVE".equals(this.status);
    }

    public boolean isSuspended() {
        return "SUSPENDED".equals(this.status);
    }

    public boolean isTerminated() {
        return "TERMINATED".equals(this.status);
    }

    public void suspend() {
        this.status = "SUSPENDED";
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.status = "ACTIVE";
        this.updatedAt = Instant.now();
    }

    public void terminate() {
        this.status = "TERMINATED";
        this.updatedAt = Instant.now();
    }

    @Override
    public String toString() {
        return "UserEntity{" +
                "id=" + id +
                ", did='" + did + '\'' +
                ", createdAt=" + createdAt +
                ", lastSeen=" + lastSeen +
                ", status='" + status + '\'' +
                ", kycVerified=" + kycVerified +
                '}';
    }
}
