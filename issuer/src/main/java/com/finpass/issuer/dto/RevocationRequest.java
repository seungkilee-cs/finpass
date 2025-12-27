package com.finpass.issuer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.finpass.issuer.entity.CredentialStatusEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request for credential revocation
 */
public class RevocationRequest {
    
    @NotNull(message = "Revocation reason is required")
    @JsonProperty("revocation_reason")
    private CredentialStatusEntity.RevocationReason revocationReason;
    
    @NotBlank(message = "Revoked by is required")
    @JsonProperty("revoked_by")
    private String revokedBy;
    
    @JsonProperty("reason_description")
    private String reasonDescription;
    
    // Constructors
    public RevocationRequest() {}
    
    public RevocationRequest(CredentialStatusEntity.RevocationReason revocationReason, String revokedBy) {
        this.revocationReason = revocationReason;
        this.revokedBy = revokedBy;
    }
    
    public RevocationRequest(CredentialStatusEntity.RevocationReason revocationReason, String revokedBy, String reasonDescription) {
        this.revocationReason = revocationReason;
        this.revokedBy = revokedBy;
        this.reasonDescription = reasonDescription;
    }
    
    // Getters and Setters
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
}
