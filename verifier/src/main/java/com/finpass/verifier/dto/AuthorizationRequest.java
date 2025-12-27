package com.finpass.verifier.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * OpenID4VP Authorization Request
 * Implements authorization request for presentation
 */
public class AuthorizationRequest {
    
    @JsonProperty("response_type")
    @NotBlank(message = "Response type is required")
    private String responseType;
    
    @JsonProperty("client_id")
    @NotBlank(message = "Client ID is required")
    private String clientId;
    
    @JsonProperty("redirect_uri")
    private String redirectUri;
    
    @JsonProperty("scope")
    private String scope;
    
    @JsonProperty("state")
    private String state;
    
    @JsonProperty("nonce")
    private String nonce;
    
    @JsonProperty("response_mode")
    private String responseMode;
    
    @JsonProperty("presentation_definition")
    private PresentationDefinition presentationDefinition;
    
    @JsonProperty("response_uri")
    private String responseUri;
    
    // Constructors
    public AuthorizationRequest() {}
    
    public AuthorizationRequest(String responseType, String clientId, PresentationDefinition presentationDefinition) {
        this.responseType = responseType;
        this.clientId = clientId;
        this.presentationDefinition = presentationDefinition;
    }
    
    // Getters and Setters
    public String getResponseType() { return responseType; }
    public void setResponseType(String responseType) { this.responseType = responseType; }
    
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    
    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
    
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    
    public String getNonce() { return nonce; }
    public void setNonce(String nonce) { this.nonce = nonce; }
    
    public String getResponseMode() { return responseMode; }
    public void setResponseMode(String responseMode) { this.responseMode = responseMode; }
    
    public PresentationDefinition getPresentationDefinition() { return presentationDefinition; }
    public void setPresentationDefinition(PresentationDefinition presentationDefinition) { this.presentationDefinition = presentationDefinition; }
    
    public String getResponseUri() { return responseUri; }
    public void setResponseUri(String responseUri) { this.responseUri = responseUri; }
}
