package com.finpass.issuer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * OpenID4VCI Token Request
 * Implements token endpoint request for pre-authorized code flow
 */
public class TokenRequest {
    
    @JsonProperty("grant_type")
    @NotBlank(message = "Grant type is required")
    private String grantType;
    
    @JsonProperty("pre-authorized_code")
    private String preAuthorizedCode;
    
    @JsonProperty("code")
    private String code;
    
    @JsonProperty("redirect_uri")
    private String redirectUri;
    
    @JsonProperty("client_id")
    private String clientId;
    
    @JsonProperty("client_secret")
    private String clientSecret;
    
    // Constructors
    public TokenRequest() {}
    
    public TokenRequest(String grantType, String preAuthorizedCode) {
        this.grantType = grantType;
        this.preAuthorizedCode = preAuthorizedCode;
    }
    
    // Getters and Setters
    public String getGrantType() { return grantType; }
    public void setGrantType(String grantType) { this.grantType = grantType; }
    
    public String getPreAuthorizedCode() { return preAuthorizedCode; }
    public void setPreAuthorizedCode(String preAuthorizedCode) { this.preAuthorizedCode = preAuthorizedCode; }
    
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    
    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
    
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    
    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
}
