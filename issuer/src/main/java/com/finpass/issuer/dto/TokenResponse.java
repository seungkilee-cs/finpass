package com.finpass.issuer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * OpenID4VCI Token Response
 * Implements token endpoint response with access token and metadata
 */
public class TokenResponse {
    
    @JsonProperty("access_token")
    private String accessToken;
    
    @JsonProperty("token_type")
    private String tokenType;
    
    @JsonProperty("expires_in")
    private Long expiresIn;
    
    @JsonProperty("refresh_token")
    private String refreshToken;
    
    @JsonProperty("scope")
    private String scope;
    
    @JsonProperty("c_nonce")
    private String cNonce;
    
    @JsonProperty("c_nonce_expires_in")
    private Long cNonceExpiresIn;
    
    @JsonProperty("authorization_pending")
    private Boolean authorizationPending;
    
    @JsonProperty("interval")
    private Long interval;
    
    @JsonProperty("error")
    private String error;
    
    @JsonProperty("error_description")
    private String errorDescription;
    
    // Constructors
    public TokenResponse() {}
    
    public TokenResponse(String accessToken, String tokenType, Long expiresIn) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
    }
    
    // Getters and Setters
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    
    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
    
    public Long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(Long expiresIn) { this.expiresIn = expiresIn; }
    
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    
    public String getCNonce() { return cNonce; }
    public void setCNonce(String cNonce) { this.cNonce = cNonce; }
    
    public Long getCNonceExpiresIn() { return cNonceExpiresIn; }
    public void setCNonceExpiresIn(Long cNonceExpiresIn) { this.cNonceExpiresIn = cNonceExpiresIn; }
    
    public Boolean getAuthorizationPending() { return authorizationPending; }
    public void setAuthorizationPending(Boolean authorizationPending) { this.authorizationPending = authorizationPending; }
    
    public Long getInterval() { return interval; }
    public void setInterval(Long interval) { this.interval = interval; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    
    public String getErrorDescription() { return errorDescription; }
    public void setErrorDescription(String errorDescription) { this.errorDescription = errorDescription; }
    
    public boolean isError() {
        return error != null && !error.trim().isEmpty();
    }
}
