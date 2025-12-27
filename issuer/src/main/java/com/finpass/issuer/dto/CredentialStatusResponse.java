package com.finpass.issuer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.finpass.issuer.entity.CredentialStatusEntity;

import java.time.Instant;
import java.util.UUID;

/**
 * Response for credential status queries
 */
public class CredentialStatusResponse {
    
    @JsonProperty("credential_id")
    private UUID credentialId;
    
    @JsonProperty("status")
    private CredentialStatusEntity.Status status;
    
    @JsonProperty("is_valid")
    private boolean isValid;
    
    @JsonProperty("revoked_at")
    private Instant revokedAt;
    
    @JsonProperty("revocation_reason")
    private CredentialStatusEntity.RevocationReason revocationReason;
    
    @JsonProperty("revoked_by")
    private String revokedBy;
    
    @JsonProperty("reason_description")
    private String reasonDescription;
    
    @JsonProperty("created_at")
    private Instant createdAt;
    
    @JsonProperty("updated_at")
    private Instant updatedAt;
    
    // Constructors
    public CredentialStatusResponse() {}
    
    public static CredentialStatusResponse valid(UUID credentialId) {
        CredentialStatusResponse response = new CredentialStatusResponse();
        response.credentialId = credentialId;
        response.status = CredentialStatusEntity.Status.VALID;
        response.isValid = true;
        response.createdAt = Instant.now();
        response.updatedAt = Instant.now();
        return response;
    }
    
    public static CredentialStatusResponse revoked(UUID credentialId, Instant revokedAt, 
                                                   CredentialStatusEntity.RevocationReason reason, 
                                                   String revokedBy, String description) {
        CredentialStatusResponse response = new CredentialStatusResponse();
        response.credentialId = credentialId;
        response.status = CredentialStatusEntity.Status.REVOKED;
        response.isValid = false;
        response.revokedAt = revokedAt;
        response.revocationReason = reason;
        response.revokedBy = revokedBy;
        response.reasonDescription = description;
        response.createdAt = Instant.now();
        response.updatedAt = Instant.now();
        return response;
    }
    
    public static CredentialStatusResponse suspended(UUID credentialId, String revokedBy, String description) {
        CredentialStatusResponse response = new CredentialStatusResponse();
        response.credentialId = credentialId;
        response.status = CredentialStatusEntity.Status.SUSPENDED;
        response.isValid = false;
        response.revokedBy = revokedBy;
        response.reasonDescription = description;
        response.createdAt = Instant.now();
        response.updatedAt = Instant.now();
        return response;
    }
    
    // Getters and Setters
    public UUID getCredentialId() {
        return credentialId;
    }
    
    public void setCredentialId(UUID credentialId) {
        this.credentialId = credentialId;
    }
    
    public CredentialStatusEntity.Status getStatus() {
        return status;
    }
    
    public void setStatus(CredentialStatusEntity.Status status) {
        this.status = status;
        this.isValid = (status == CredentialStatusEntity.Status.VALID);
    }
    
    public boolean isValid() {
        return isValid;
    }
    
    public void setValid(boolean valid) {
        isValid = valid;
    }
    
    public Instant getRevokedAt() {
        return revokedAt;
    }
    
    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }
    
    public CredentialStatusEntity.RevocationReason getRevocationReason() {
        return revocationReason;
    }
    
    public void setRevocationReason(CredentialStatusEntity.RevocationReason revocationReason) {
        this.revocationReason = revocationReason;
    }
    
    public String getRevokedBy() {
        return revokedBy;
    }
    
    public void setRevokedBy(String revokedBy) {
        this.revokedBy = revokedBy;
    }
    
    public String getReasonDescription() {
        return reasonDescription;
    }
    
    public void setReasonDescription(String reasonDescription) {
        this.reasonDescription = reasonDescription;
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
}
