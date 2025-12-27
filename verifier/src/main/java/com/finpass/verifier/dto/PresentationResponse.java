package com.finpass.verifier.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * OpenID4VP Presentation Response
 * Contains the verifiable presentation and submission data
 */
public class PresentationResponse {
    
    @JsonProperty("vp_token")
    @NotBlank(message = "VP token is required")
    private String vpToken;
    
    @JsonProperty("presentation_submission")
    private PresentationSubmission presentationSubmission;
    
    @JsonProperty("state")
    private String state;
    
    @JsonProperty("id_token")
    private String idToken;
    
    @JsonProperty("error")
    private String error;
    
    @JsonProperty("error_description")
    private String errorDescription;
    
    // Constructors
    public PresentationResponse() {}
    
    public PresentationResponse(String vpToken, PresentationSubmission presentationSubmission) {
        this.vpToken = vpToken;
        this.presentationSubmission = presentationSubmission;
    }
    
    // Static factory methods
    public static PresentationResponse success(String vpToken, PresentationSubmission presentationSubmission) {
        PresentationResponse response = new PresentationResponse();
        response.setVpToken(vpToken);
        response.setPresentationSubmission(presentationSubmission);
        return response;
    }
    
    public static PresentationResponse error(String error, String errorDescription) {
        PresentationResponse response = new PresentationResponse();
        response.setError(error);
        response.setErrorDescription(errorDescription);
        return response;
    }
    
    // Getters and Setters
    public String getVpToken() { return vpToken; }
    public void setVpToken(String vpToken) { this.vpToken = vpToken; }
    
    public PresentationSubmission getPresentationSubmission() { return presentationSubmission; }
    public void setPresentationSubmission(PresentationSubmission presentationSubmission) { this.presentationSubmission = presentationSubmission; }
    
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    
    public String getIdToken() { return idToken; }
    public void setIdToken(String idToken) { this.idToken = idToken; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    
    public String getErrorDescription() { return errorDescription; }
    public void setErrorDescription(String errorDescription) { this.errorDescription = errorDescription; }
    
    public boolean isError() {
        return error != null && !error.trim().isEmpty();
    }
}
