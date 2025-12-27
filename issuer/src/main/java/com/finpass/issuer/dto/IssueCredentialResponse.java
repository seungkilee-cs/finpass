package com.finpass.issuer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response for credential issuance
 * Contains the issued credential JWT
 */
public class IssueCredentialResponse {
    
    @JsonProperty("credential_jwt")
    private String credentialJwt;
    
    @JsonProperty("credential_id")
    private String credentialId;
    
    @JsonProperty("issued_at")
    private String issuedAt;
    
    // Constructors
    public IssueCredentialResponse() {}
    
    public IssueCredentialResponse(String credentialJwt) {
        this.credentialJwt = credentialJwt;
    }
    
    public IssueCredentialResponse(String credentialJwt, String credentialId, String issuedAt) {
        this.credentialJwt = credentialJwt;
        this.credentialId = credentialId;
        this.issuedAt = issuedAt;
    }
    
    // Getters and Setters
    public String getCredentialJwt() { return credentialJwt; }
    public void setCredentialJwt(String credentialJwt) { this.credentialJwt = credentialJwt; }
    
    public String getCredentialId() { return credentialId; }
    public void setCredentialId(String credentialId) { this.credentialId = credentialId; }
    
    public String getIssuedAt() { return issuedAt; }
    public void setIssuedAt(String issuedAt) { this.issuedAt = issuedAt; }
}
