package com.finpass.issuer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * OpenID4VCI Credential Request
 * Implements credential endpoint request per OpenID4VCI specification
 */
public class CredentialRequest {
    
    @JsonProperty("format")
    @NotBlank(message = "Format is required")
    private String format;
    
    @JsonProperty("credential_definition")
    private CredentialDefinition credentialDefinition;
    
    @JsonProperty("proof")
    private Proof proof;
    
    @JsonProperty("issuer")
    private String issuer;
    
    @JsonProperty("subject")
    private String subject;
    
    @JsonProperty("types")
    private java.util.List<String> types;
    
    // Constructors
    public CredentialRequest() {}
    
    public CredentialRequest(String format, CredentialDefinition credentialDefinition, Proof proof) {
        this.format = format;
        this.credentialDefinition = credentialDefinition;
        this.proof = proof;
    }
    
    // Getters and Setters
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    
    public CredentialDefinition getCredentialDefinition() { return credentialDefinition; }
    public void setCredentialDefinition(CredentialDefinition credentialDefinition) { this.credentialDefinition = credentialDefinition; }
    
    public Proof getProof() { return proof; }
    public void setProof(Proof proof) { this.proof = proof; }
    
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public java.util.List<String> getTypes() { return types; }
    public void setTypes(java.util.List<String> types) { this.types = types; }
    
    /**
     * Credential definition for the request
     */
    public static class CredentialDefinition {
        @JsonProperty("type")
        private java.util.List<String> type;
        
        @JsonProperty("credential_context")
        private java.util.List<Object> credentialContext;
        
        public CredentialDefinition() {}
        
        public CredentialDefinition(java.util.List<String> type) {
            this.type = type;
        }
        
        public java.util.List<String> getType() { return type; }
        public void setType(java.util.List<String> type) { this.type = type; }
        
        public java.util.List<Object> getCredentialContext() { return credentialContext; }
        public void setCredentialContext(java.util.List<Object> credentialContext) { this.credentialContext = credentialContext; }
    }
    
    /**
     * Proof for the credential request
     */
    public static class Proof {
        @JsonProperty("proof_type")
        private String proofType;
        
        @JsonProperty("jwt")
        private String jwt;
        
        @JsonProperty("created")
        private String created;
        
        @JsonProperty("proof_purpose")
        private String proofPurpose;
        
        @JsonProperty("verification_method")
        private String verificationMethod;
        
        @JsonProperty("domain")
        private String domain;
        
        @JsonProperty("challenge")
        private String challenge;
        
        @JsonProperty("jws")
        private String jws;
        
        public Proof() {}
        
        public Proof(String proofType, String jwt) {
            this.proofType = proofType;
            this.jwt = jwt;
        }
        
        public String getProofType() { return proofType; }
        public void setProofType(String proofType) { this.proofType = proofType; }
        
        public String getJwt() { return jwt; }
        public void setJwt(String jwt) { this.jwt = jwt; }
        
        public String getCreated() { return created; }
        public void setCreated(String created) { this.created = created; }
        
        public String getProofPurpose() { return proofPurpose; }
        public void setProofPurpose(String proofPurpose) { this.proofPurpose = proofPurpose; }
        
        public String getVerificationMethod() { return verificationMethod; }
        public void setVerificationMethod(String verificationMethod) { this.verificationMethod = verificationMethod; }
        
        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
        
        public String getChallenge() { return challenge; }
        public void setChallenge(String challenge) { this.challenge = challenge; }
        
        public String getJws() { return jws; }
        public void setJws(String jws) { this.jws = jws; }
    }
}
