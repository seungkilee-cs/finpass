package com.finpass.issuer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * OpenID4VCI Credential Response
 * Implements credential endpoint response per OpenID4VCI specification
 */
public class CredentialResponse {
    
    @JsonProperty("format")
    private String format;
    
    @JsonProperty("credential")
    private String credential;
    
    @JsonProperty("c_nonce")
    private String cNonce;
    
    @JsonProperty("c_nonce_expires_in")
    private Long cNonceExpiresIn;
    
    @JsonProperty("notification_id")
    private String notificationId;
    
    @JsonProperty("acceptance_token")
    private String acceptanceToken;
    
    @JsonProperty("key_proofs")
    private Object keyProofs;
    
    @JsonProperty("error")
    private String error;
    
    @JsonProperty("error_description")
    private String errorDescription;
    
    // Constructors
    public CredentialResponse() {}
    
    public CredentialResponse(String format, String credential) {
        this.format = format;
        this.credential = credential;
    }
    
    // Static factory methods
    public static CredentialResponse success(String format, String credential) {
        CredentialResponse response = new CredentialResponse();
        response.setFormat(format);
        response.setCredential(credential);
        return response;
    }
    
    public static CredentialResponse error(String error, String errorDescription) {
        CredentialResponse response = new CredentialResponse();
        response.setError(error);
        response.setErrorDescription(errorDescription);
        return response;
    }
    
    // Getters and Setters
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    
    public String getCredential() { return credential; }
    public void setCredential(String credential) { this.credential = credential; }
    
    public String getCNonce() { return cNonce; }
    public void setCNonce(String cNonce) { this.cNonce = cNonce; }
    
    public Long getCNonceExpiresIn() { return cNonceExpiresIn; }
    public void setCNonceExpiresIn(Long cNonceExpiresIn) { this.cNonceExpiresIn = cNonceExpiresIn; }
    
    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
    
    public String getAcceptanceToken() { return acceptanceToken; }
    public void setAcceptanceToken(String acceptanceToken) { this.acceptanceToken = acceptanceToken; }
    
    public Object getKeyProofs() { return keyProofs; }
    public void setKeyProofs(Object keyProofs) { this.keyProofs = keyProofs; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    
    public String getErrorDescription() { return errorDescription; }
    public void setErrorDescription(String errorDescription) { this.errorDescription = errorDescription; }
    
    public boolean isError() {
        return error != null && !error.trim().isEmpty();
    }
}
