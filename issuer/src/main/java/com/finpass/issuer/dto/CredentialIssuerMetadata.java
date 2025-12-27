package com.finpass.issuer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * OpenID4VCI Credential Issuer Metadata
 * Implements the OpenID for Verifiable Credentials Issuance specification
 */
public class CredentialIssuerMetadata {
    
    @JsonProperty("credential_issuer")
    private String credentialIssuer;
    
    @JsonProperty("authorization_servers")
    private List<String> authorizationServers;
    
    @JsonProperty("credential_endpoint")
    private String credentialEndpoint;
    
    @JsonProperty("token_endpoint")
    private String tokenEndpoint;
    
    @JsonProperty("display")
    private List<Display> display;
    
    @JsonProperty("credentials_supported")
    private List<CredentialSupported> credentialsSupported;
    
    // Constructors
    public CredentialIssuerMetadata() {}
    
    public CredentialIssuerMetadata(String credentialIssuer, String credentialEndpoint, 
                                  String tokenEndpoint, List<CredentialSupported> credentialsSupported) {
        this.credentialIssuer = credentialIssuer;
        this.credentialEndpoint = credentialEndpoint;
        this.tokenEndpoint = tokenEndpoint;
        this.credentialsSupported = credentialsSupported;
    }
    
    // Getters and Setters
    public String getCredentialIssuer() { return credentialIssuer; }
    public void setCredentialIssuer(String credentialIssuer) { this.credentialIssuer = credentialIssuer; }
    
    public List<String> getAuthorizationServers() { return authorizationServers; }
    public void setAuthorizationServers(List<String> authorizationServers) { this.authorizationServers = authorizationServers; }
    
    public String getCredentialEndpoint() { return credentialEndpoint; }
    public void setCredentialEndpoint(String credentialEndpoint) { this.credentialEndpoint = credentialEndpoint; }
    
    public String getTokenEndpoint() { return tokenEndpoint; }
    public void setTokenEndpoint(String tokenEndpoint) { this.tokenEndpoint = tokenEndpoint; }
    
    public List<Display> getDisplay() { return display; }
    public void setDisplay(List<Display> display) { this.display = display; }
    
    public List<CredentialSupported> getCredentialsSupported() { return credentialsSupported; }
    public void setCredentialsSupported(List<CredentialSupported> credentialsSupported) { this.credentialsSupported = credentialsSupported; }
    
    /**
     * Display information for the issuer
     */
    public static class Display {
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("locale")
        private String locale;
        
        public Display() {}
        
        public Display(String name, String locale) {
            this.name = name;
            this.locale = locale;
        }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getLocale() { return locale; }
        public void setLocale(String locale) { this.locale = locale; }
    }
    
    /**
     * Credential configuration
     */
    public static class CredentialSupported {
        @JsonProperty("format")
        private String format;
        
        @JsonProperty("types")
        private List<String> types;
        
        @JsonProperty("cryptographic_binding_methods_supported")
        private List<String> cryptographicBindingMethodsSupported;
        
        @JsonProperty("cryptographic_suites_supported")
        private List<String> cryptographicSuitesSupported;
        
        @JsonProperty("display")
        private List<Display> display;
        
        public CredentialSupported() {}
        
        public CredentialSupported(String format, List<String> types) {
            this.format = format;
            this.types = types;
        }
        
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        
        public List<String> getTypes() { return types; }
        public void setTypes(List<String> types) { this.types = types; }
        
        public List<String> getCryptographicBindingMethodsSupported() { return cryptographicBindingMethodsSupported; }
        public void setCryptographicBindingMethodsSupported(List<String> cryptographicBindingMethodsSupported) { 
            this.cryptographicBindingMethodsSupported = cryptographicBindingMethodsSupported; 
        }
        
        public List<String> getCryptographicSuitesSupported() { return cryptographicSuitesSupported; }
        public void setCryptographicSuitesSupported(List<String> cryptographicSuitesSupported) { 
            this.cryptographicSuitesSupported = cryptographicSuitesSupported; 
        }
        
        public List<Display> getDisplay() { return display; }
        public void setDisplay(List<Display> display) { this.display = display; }
    }
}
