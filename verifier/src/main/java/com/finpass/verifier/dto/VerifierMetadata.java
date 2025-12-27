package com.finpass.verifier.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * OpenID4VP Verifier Metadata
 * Implements the OpenID for Verifiable Credentials Presentation specification
 */
public class VerifierMetadata {
    
    @JsonProperty("authorization_endpoint")
    private String authorizationEndpoint;
    
    @JsonProperty("response_endpoint")
    private String responseEndpoint;
    
    @JsonProperty("presentation_definition_endpoint")
    private String presentationDefinitionEndpoint;
    
    @JsonProperty("supported_credential_formats")
    private List<String> supportedCredentialFormats;
    
    @JsonProperty("supported_algorithms")
    private List<String> supportedAlgorithms;
    
    @JsonProperty("display")
    private List<Display> display;
    
    @JsonProperty("client_metadata")
    private ClientMetadata clientMetadata;
    
    // Constructors
    public VerifierMetadata() {}
    
    public VerifierMetadata(String authorizationEndpoint, String responseEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
        this.responseEndpoint = responseEndpoint;
    }
    
    // Getters and Setters
    public String getAuthorizationEndpoint() { return authorizationEndpoint; }
    public void setAuthorizationEndpoint(String authorizationEndpoint) { this.authorizationEndpoint = authorizationEndpoint; }
    
    public String getResponseEndpoint() { return responseEndpoint; }
    public void setResponseEndpoint(String responseEndpoint) { this.responseEndpoint = responseEndpoint; }
    
    public String getPresentationDefinitionEndpoint() { return presentationDefinitionEndpoint; }
    public void setPresentationDefinitionEndpoint(String presentationDefinitionEndpoint) { this.presentationDefinitionEndpoint = presentationDefinitionEndpoint; }
    
    public List<String> getSupportedCredentialFormats() { return supportedCredentialFormats; }
    public void setSupportedCredentialFormats(List<String> supportedCredentialFormats) { this.supportedCredentialFormats = supportedCredentialFormats; }
    
    public List<String> getSupportedAlgorithms() { return supportedAlgorithms; }
    public void setSupportedAlgorithms(List<String> supportedAlgorithms) { this.supportedAlgorithms = supportedAlgorithms; }
    
    public List<Display> getDisplay() { return display; }
    public void setDisplay(List<Display> display) { this.display = display; }
    
    public ClientMetadata getClientMetadata() { return clientMetadata; }
    public void setClientMetadata(ClientMetadata clientMetadata) { this.clientMetadata = clientMetadata; }
    
    /**
     * Display information for the verifier
     */
    public static class Display {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("locale")
        private String locale;
        
        @JsonProperty("logo")
        private String logo;
        
        @JsonProperty("purpose")
        private String purpose;
        
        public Display() {}
        
        public Display(String name, String locale, String purpose) {
            this.name = name;
            this.locale = locale;
            this.purpose = purpose;
        }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getLocale() { return locale; }
        public void setLocale(String locale) { this.locale = locale; }
        
        public String getLogo() { return logo; }
        public void setLogo(String logo) { this.logo = logo; }
        
        public String getPurpose() { return purpose; }
        public void setPurpose(String purpose) { this.purpose = purpose; }
    }
    
    /**
     * Client metadata for the verifier
     */
    public static class ClientMetadata {
        @JsonProperty("client_id")
        private String clientId;
        
        @JsonProperty("client_name")
        private String clientName;
        
        @JsonProperty("redirect_uris")
        private List<String> redirectUris;
        
        @JsonProperty("jwks_uri")
        private String jwksUri;
        
        @JsonProperty("id_token_signed_response_alg")
        private String idTokenSignedResponseAlg;
        
        @JsonProperty("vp_formats")
        private Map<String, Object> vpFormats;
        
        public ClientMetadata() {}
        
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        
        public String getClientName() { return clientName; }
        public void setClientName(String clientName) { this.clientName = clientName; }
        
        public List<String> getRedirectUris() { return redirectUris; }
        public void setRedirectUris(List<String> redirectUris) { this.redirectUris = redirectUris; }
        
        public String getJwksUri() { return jwksUri; }
        public void setJwksUri(String jwksUri) { this.jwksUri = jwksUri; }
        
        public String getIdTokenSignedResponseAlg() { return idTokenSignedResponseAlg; }
        public void setIdTokenSignedResponseAlg(String idTokenSignedResponseAlg) { this.idTokenSignedResponseAlg = idTokenSignedResponseAlg; }
        
        public Map<String, Object> getVpFormats() { return vpFormats; }
        public void setVpFormats(Map<String, Object> vpFormats) { this.vpFormats = vpFormats; }
    }
}
