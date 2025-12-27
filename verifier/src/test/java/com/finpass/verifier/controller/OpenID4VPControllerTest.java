package com.finpass.verifier.controller;

import com.finpass.verifier.dto.*;
import com.finpass.verifier.service.OpenID4VPService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for OpenID4VP Controller
 */
@ExtendWith(MockitoExtension.class)
class OpenID4VPControllerTest {
    
    @Mock
    private OpenID4VPService openID4VPService;
    
    @InjectMocks
    private OpenID4VPController controller;
    
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }
    
    @Test
    void testGetVerifierMetadata_Success() throws Exception {
        // Arrange
        VerifierMetadata metadata = createSampleVerifierMetadata();
        when(openID4VPService.generateVerifierMetadata()).thenReturn(metadata);
        
        // Act & Assert
        mockMvc.perform(get("/.well-known/openid-verifier"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.authorization_endpoint").value("http://localhost:8081/authorize"))
                .andExpect(jsonPath("$.response_endpoint").value("http://localhost:8081/callback"))
                .andExpect(jsonPath("$.supported_credential_formats").isArray())
                .andExpect(jsonPath("$.supported_algorithms").isArray())
                .andExpect(jsonPath("$.display").isArray());
        
        verify(openID4VPService, times(1)).generateVerifierMetadata();
    }
    
    @Test
    void testGetVerifierMetadata_Error() throws Exception {
        // Arrange
        when(openID4VPService.generateVerifierMetadata())
            .thenThrow(new RuntimeException("Service error"));
        
        // Act & Assert
        mockMvc.perform(get("/.well-known/openid-verifier"))
                .andExpect(status().isInternalServerError());
        
        verify(openID4VPService, times(1)).generateVerifierMetadata();
    }
    
    @Test
    void testAuthorize_Success() throws Exception {
        // Arrange
        OpenID4VPService.AuthorizationResponse response = createValidAuthorizationResponse();
        
        when(openID4VPService.processAuthorizationRequest(any(AuthorizationRequest.class)))
            .thenReturn(response);
        
        // Act & Assert
        mockMvc.perform(get("/authorize")
                .param("response_type", "vp_token")
                .param("client_id", "test-client")
                .param("state", "test-state"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.session_id").isNotEmpty())
                .andExpect(jsonPath("$.presentation_definition").isNotEmpty())
                .andExpect(jsonPath("$.nonce").isNotEmpty())
                .andExpect(jsonPath("$.expires_in").value(600))
                .andExpect(jsonPath("$.response_uri").value("http://localhost:8081/callback"));
        
        verify(openID4VPService, times(1)).processAuthorizationRequest(any(AuthorizationRequest.class));
    }
    
    @Test
    void testAuthorize_InvalidResponseType() throws Exception {
        // Arrange
        OpenID4VPService.AuthorizationResponse errorResponse = createErrorAuthorizationResponse();
        
        when(openID4VPService.processAuthorizationRequest(any(AuthorizationRequest.class)))
            .thenReturn(errorResponse);
        
        // Act & Assert
        mockMvc.perform(get("/authorize")
                .param("response_type", "invalid_type")
                .param("client_id", "test-client"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_request"))
                .andExpect(jsonPath("$.error_description").value("Unsupported response type"));
        
        verify(openID4VPService, times(1)).processAuthorizationRequest(any(AuthorizationRequest.class));
    }
    
    @Test
    void testAuthorize_ServerError() throws Exception {
        // Arrange
        when(openID4VPService.processAuthorizationRequest(any(AuthorizationRequest.class)))
            .thenThrow(new RuntimeException("Internal error"));
        
        // Act & Assert
        mockMvc.perform(get("/authorize")
                .param("response_type", "vp_token")
                .param("client_id", "test-client"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("server_error"));
    }
    
    @Test
    void testGetPresentationDefinition_Success() throws Exception {
        // Arrange
        PresentationDefinition definition = createSamplePresentationDefinition();
        when(openID4VPService.generatePassportPresentationDefinition()).thenReturn(definition);
        
        // Act & Assert
        mockMvc.perform(get("/presentation-definition"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("passport_verification_definition"))
                .andExpect(jsonPath("$.name").value("Passport Verification"))
                .andExpect(jsonPath("$.purpose").value("Verify your passport credential to access this service"))
                .andExpect(jsonPath("$.input_descriptors").isArray())
                .andExpect(jsonPath("$.submission_requirements").isArray());
        
        verify(openID4VPService, times(1)).generatePassportPresentationDefinition();
    }
    
    @Test
    void testGetPresentationDefinition_Error() throws Exception {
        // Arrange
        when(openID4VPService.generatePassportPresentationDefinition())
            .thenThrow(new RuntimeException("Service error"));
        
        // Act & Assert
        mockMvc.perform(get("/presentation-definition"))
                .andExpect(status().isInternalServerError());
        
        verify(openID4VPService, times(1)).generatePassportPresentationDefinition();
    }
    
    @Test
    void testCallback_Success() throws Exception {
        // Arrange
        PresentationResponse request = createValidPresentationResponse();
        OpenID4VPService.VerificationResult result = createValidVerificationResult();
        
        when(openID4VPService.processPresentationSubmission(any(PresentationResponse.class)))
            .thenReturn(result);
        
        // Act & Assert
        mockMvc.perform(post("/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id_token").isNotEmpty())
                .andExpect(jsonPath("$.state").value("test-state"));
        
        verify(openID4VPService, times(1)).processPresentationSubmission(any(PresentationResponse.class));
    }
    
    @Test
    void testCallback_InvalidPresentation() throws Exception {
        // Arrange
        PresentationResponse request = createValidPresentationResponse();
        OpenID4VPService.VerificationResult errorResult = createErrorVerificationResult("invalid_presentation");
        
        when(openID4VPService.processPresentationSubmission(any(PresentationResponse.class)))
            .thenReturn(errorResult);
        
        // Act & Assert
        mockMvc.perform(post("/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_presentation"))
                .andExpect(jsonPath("$.error_description").value("VP token is required"));
        
        verify(openID4VPService, times(1)).processPresentationSubmission(any(PresentationResponse.class));
    }
    
    @Test
    void testCallback_VerificationFailed() throws Exception {
        // Arrange
        PresentationResponse request = createValidPresentationResponse();
        OpenID4VPService.VerificationResult errorResult = createErrorVerificationResult("verification_failed");
        
        when(openID4VPService.processPresentationSubmission(any(PresentationResponse.class)))
            .thenReturn(errorResult);
        
        // Act & Assert
        mockMvc.perform(post("/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("verification_failed"));
        
        verify(openID4VPService, times(1)).processPresentationSubmission(any(PresentationResponse.class));
    }
    
    @Test
    void testCallback_ServerError() throws Exception {
        // Arrange
        PresentationResponse request = createValidPresentationResponse();
        
        when(openID4VPService.processPresentationSubmission(any(PresentationResponse.class)))
            .thenThrow(new RuntimeException("Internal error"));
        
        // Act & Assert
        mockMvc.perform(post("/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("server_error"));
    }
    
    @Test
    void testHealthCheck() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/openid4vp/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("OpenID4VP service is healthy"));
    }
    
    // Helper methods
    private VerifierMetadata createSampleVerifierMetadata() {
        VerifierMetadata metadata = new VerifierMetadata();
        metadata.setAuthorizationEndpoint("http://localhost:8081/authorize");
        metadata.setResponseEndpoint("http://localhost:8081/callback");
        metadata.setPresentationDefinitionEndpoint("http://localhost:8081/presentation-definition");
        metadata.setSupportedCredentialFormats(java.util.List.of("jwt_vc", "jwt_vp"));
        metadata.setSupportedAlgorithms(java.util.List.of("EdDSA", "ES256K"));
        
        VerifierMetadata.Display display = new VerifierMetadata.Display(
            "FinPass Passport Verifier", 
            "en",
            "Verify your passport credential"
        );
        metadata.setDisplay(java.util.List.of(display));
        
        return metadata;
    }
    
    private OpenID4VPService.AuthorizationResponse createValidAuthorizationResponse() {
        OpenID4VPService.AuthorizationResponse response = new OpenID4VPService.AuthorizationResponse();
        response.setSessionId("test-session-id");
        response.setNonce("test-nonce");
        response.setExpiresIn(600L);
        response.setResponseUri("http://localhost:8081/callback");
        response.setPresentationDefinition(createSamplePresentationDefinition());
        return response;
    }
    
    private OpenID4VPService.AuthorizationResponse createErrorAuthorizationResponse() {
        OpenID4VPService.AuthorizationResponse response = new OpenID4VPService.AuthorizationResponse();
        response.setError("invalid_request");
        response.setErrorDescription("Unsupported response type");
        return response;
    }
    
    private PresentationDefinition createSamplePresentationDefinition() {
        PresentationDefinition definition = new PresentationDefinition(
            "passport_verification_definition",
            "Passport Verification",
            "Verify your passport credential to access this service"
        );
        
        PresentationDefinition.InputDescriptor inputDescriptor = 
            new PresentationDefinition.InputDescriptor(
                "passport_credential",
                "Passport Credential",
                "Please present your passport credential for verification"
            );
        
        definition.setInputDescriptors(java.util.List.of(inputDescriptor));
        return definition;
    }
    
    private PresentationResponse createValidPresentationResponse() {
        PresentationResponse response = new PresentationResponse();
        response.setVpToken("sample_vp_token");
        response.setPresentationSubmission(createSamplePresentationSubmission());
        response.setState("test-state");
        return response;
    }
    
    private PresentationSubmission createSamplePresentationSubmission() {
        PresentationSubmission submission = new PresentationSubmission();
        submission.setId("test-submission");
        submission.setDefinitionId("passport_verification_definition");
        return submission;
    }
    
    private OpenID4VPService.VerificationResult createValidVerificationResult() {
        OpenID4VPService.VerificationResult result = new OpenID4VPService.VerificationResult();
        result.setSuccess(true);
        result.setDecisionToken("sample_decision_token");
        result.setVerifiedClaims(java.util.List.of("name", "nationality", "birthDate"));
        result.setAssuranceLevel("LOW");
        result.setExpiresIn(300L);
        return result;
    }
    
    private OpenID4VPService.VerificationResult createErrorVerificationResult(String error) {
        return OpenID4VPService.VerificationResult.error(error, "Test error description");
    }
}
