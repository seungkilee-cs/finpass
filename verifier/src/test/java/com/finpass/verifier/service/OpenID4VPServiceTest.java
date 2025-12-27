package com.finpass.verifier.service;

import com.finpass.verifier.dto.*;
import com.nimbusds.jose.JWSAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OpenID4VP Service
 */
@ExtendWith(MockitoExtension.class)
class OpenID4VPServiceTest {
    
    @Mock
    private VerifierKeyProvider keyProvider;
    
    @Mock
    private VerifierService verifierService;
    
    @InjectMocks
    private OpenID4VPService service;
    
    @BeforeEach
    void setUp() {
        // Mock key provider
        when(keyProvider.getAlgorithm()).thenReturn(JWSAlgorithm.EdDSA);
        when(keyProvider.getKeyId()).thenReturn("test-key-id");
        
        // Mock verifier service
        VerifyResponse verifyResponse = new VerifyResponse();
        verifyResponse.setVerifiedClaims(java.util.List.of("name", "nationality", "birthDate"));
        verifyResponse.setDecisionToken("test_decision_token");
        
        when(verifierService.verify(any(VerifyRequest.class))).thenReturn(verifyResponse);
    }
    
    @Test
    void testGenerateVerifierMetadata() {
        // Act
        VerifierMetadata metadata = service.generateVerifierMetadata();
        
        // Assert
        assertNotNull(metadata, "Metadata should not be null");
        assertEquals("http://localhost:8081/authorize", metadata.getAuthorizationEndpoint(), "Authorization endpoint should match");
        assertEquals("http://localhost:8081/callback", metadata.getResponseEndpoint(), "Response endpoint should match");
        assertEquals("http://localhost:8081/presentation-definition", metadata.getPresentationDefinitionEndpoint(), "Presentation definition endpoint should match");
        
        assertNotNull(metadata.getSupportedCredentialFormats(), "Supported formats should not be null");
        assertTrue(metadata.getSupportedCredentialFormats().contains("jwt_vc"), "Should contain jwt_vc");
        assertTrue(metadata.getSupportedCredentialFormats().contains("jwt_vp"), "Should contain jwt_vp");
        
        assertNotNull(metadata.getSupportedAlgorithms(), "Supported algorithms should not be null");
        assertTrue(metadata.getSupportedAlgorithms().contains("EdDSA"), "Should contain EdDSA");
        
        assertNotNull(metadata.getDisplay(), "Display should not be null");
        assertFalse(metadata.getDisplay().isEmpty(), "Display should have entries");
        assertEquals("FinPass Passport Verifier", metadata.getDisplay().get(0).getName(), "Display name should match");
        
        assertNotNull(metadata.getClientMetadata(), "Client metadata should not be null");
        assertEquals("finpass-verifier", metadata.getClientMetadata().getClientId(), "Client ID should match");
    }
    
    @Test
    void testGeneratePassportPresentationDefinition() {
        // Act
        PresentationDefinition definition = service.generatePassportPresentationDefinition();
        
        // Assert
        assertNotNull(definition, "Definition should not be null");
        assertEquals("passport_verification_definition", definition.getId(), "Definition ID should match");
        assertEquals("Passport Verification", definition.getName(), "Name should match");
        assertEquals("Verify your passport credential to access this service", definition.getPurpose(), "Purpose should match");
        
        assertNotNull(definition.getFormat(), "Format should not be null");
        assertNotNull(definition.getFormat().getJwtVc(), "JWT VC format should not be null");
        assertNotNull(definition.getFormat().getJwtVp(), "JWT VP format should not be null");
        
        assertNotNull(definition.getInputDescriptors(), "Input descriptors should not be null");
        assertFalse(definition.getInputDescriptors().isEmpty(), "Should have input descriptors");
        
        PresentationDefinition.InputDescriptor inputDescriptor = definition.getInputDescriptors().get(0);
        assertEquals("passport_credential", inputDescriptor.getId(), "Input descriptor ID should match");
        assertEquals("Passport Credential", inputDescriptor.getName(), "Input descriptor name should match");
        
        assertNotNull(inputDescriptor.getConstraints(), "Constraints should not be null");
        assertNotNull(inputDescriptor.getConstraints().getFields(), "Fields should not be null");
        assertFalse(inputDescriptor.getConstraints().getFields().isEmpty(), "Should have fields");
        
        // Check required fields
        java.util.List<PresentationDefinition.Field> fields = inputDescriptor.getConstraints().getFields();
        assertTrue(fields.stream().anyMatch(f -> "name_field".equals(f.getId())), "Should have name field");
        assertTrue(fields.stream().anyMatch(f -> "nationality_field".equals(f.getId())), "Should have nationality field");
        assertTrue(fields.stream().anyMatch(f -> "birth_date_field".equals(f.getId())), "Should have birth date field");
        assertTrue(fields.stream().anyMatch(f -> "passport_number_field".equals(f.getId())), "Should have passport number field");
        
        assertNotNull(definition.getSubmissionRequirements(), "Submission requirements should not be null");
        assertFalse(definition.getSubmissionRequirements().isEmpty(), "Should have submission requirements");
        
        PresentationDefinition.SubmissionRequirement requirement = definition.getSubmissionRequirements().get(0);
        assertEquals("passport_requirement", requirement.getName(), "Requirement name should match");
        assertEquals("all", requirement.getRule(), "Rule should be all");
        assertEquals("passport_credential", requirement.getFrom(), "From should match");
    }
    
    @Test
    void testProcessAuthorizationRequest_Success() {
        // Arrange
        AuthorizationRequest request = createValidAuthorizationRequest();
        
        // Act
        OpenID4VPService.AuthorizationResponse response = service.processAuthorizationRequest(request);
        
        // Assert
        assertNotNull(response, "Response should not be null");
        assertFalse(response.isError(), "Response should not be an error");
        assertNotNull(response.getSessionId(), "Session ID should not be null");
        assertNotNull(response.getNonce(), "Nonce should not be null");
        assertEquals(600L, response.getExpiresIn(), "Expires in should be 600");
        assertEquals("http://localhost:8081/callback", response.getResponseUri(), "Response URI should match");
        
        assertNotNull(response.getPresentationDefinition(), "Presentation definition should not be null");
        assertEquals("passport_verification_definition", response.getPresentationDefinition().getId(), "Definition ID should match");
    }
    
    @Test
    void testProcessAuthorizationRequest_InvalidResponseType() {
        // Arrange
        AuthorizationRequest request = new AuthorizationRequest();
        request.setResponseType("invalid_type");
        request.setClientId("test-client");
        
        // Act
        OpenID4VPService.AuthorizationResponse response = service.processAuthorizationRequest(request);
        
        // Assert
        assertNotNull(response, "Response should not be null");
        assertTrue(response.isError(), "Response should be an error");
        assertEquals("invalid_request", response.getError(), "Error should be invalid_request");
        assertEquals("Unsupported response type", response.getErrorDescription(), "Error description should match");
    }
    
    @Test
    void testProcessAuthorizationRequest_WithCustomPresentationDefinition() {
        // Arrange
        PresentationDefinition customDefinition = new PresentationDefinition(
            "custom_definition",
            "Custom Definition",
            "Custom purpose"
        );
        
        AuthorizationRequest request = new AuthorizationRequest(
            "vp_token", 
            "test-client", 
            customDefinition
        );
        
        // Act
        OpenID4VPService.AuthorizationResponse response = service.processAuthorizationRequest(request);
        
        // Assert
        assertNotNull(response, "Response should not be null");
        assertFalse(response.isError(), "Response should not be an error");
        assertEquals("custom_definition", response.getPresentationDefinition().getId(), "Should use custom definition");
    }
    
    @Test
    void testProcessAuthorizationRequest_ServiceException() {
        // Arrange
        AuthorizationRequest request = createValidAuthorizationRequest();
        
        // Mock service to throw exception
        OpenID4VPService spyService = spy(service);
        doThrow(new RuntimeException("Service error")).when(spyService).generatePassportPresentationDefinition();
        
        // Act
        OpenID4VPService.AuthorizationResponse response = spyService.processAuthorizationRequest(request);
        
        // Assert
        assertNotNull(response, "Response should not be null");
        assertTrue(response.isError(), "Response should be an error");
        assertEquals("server_error", response.getError(), "Error should be server_error");
    }
    
    @Test
    void testProcessPresentationSubmission_Success() {
        // Arrange
        PresentationResponse request = createValidPresentationResponse();
        
        // Act
        OpenID4VPService.VerificationResult result = service.processPresentationSubmission(request);
        
        // Assert
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isError(), "Result should not be an error");
        assertTrue(result.isSuccess(), "Result should be successful");
        assertNotNull(result.getDecisionToken(), "Decision token should not be null");
        assertNotNull(result.getVerifiedClaims(), "Verified claims should not be null");
        assertFalse(result.getVerifiedClaims().isEmpty(), "Should have verified claims");
        assertEquals("LOW", result.getAssuranceLevel(), "Assurance level should be LOW");
        assertEquals(300L, result.getExpiresIn(), "Expires in should be 300");
    }
    
    @Test
    void testProcessPresentationSubmission_MissingVPToken() {
        // Arrange
        PresentationResponse request = new PresentationResponse();
        request.setPresentationSubmission(createSamplePresentationSubmission());
        request.setVpToken("");
        
        // Act
        OpenID4VPService.VerificationResult result = service.processPresentationSubmission(request);
        
        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isError(), "Result should be an error");
        assertEquals("invalid_presentation", result.getError(), "Error should be invalid_presentation");
        assertEquals("VP token is required", result.getErrorDescription(), "Error description should match");
    }
    
    @Test
    void testProcessPresentationSubmission_MissingSubmission() {
        // Arrange
        PresentationResponse request = new PresentationResponse();
        request.setVpToken("sample_vp_token");
        request.setPresentationSubmission(null);
        
        // Act
        OpenID4VPService.VerificationResult result = service.processPresentationSubmission(request);
        
        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isError(), "Result should be an error");
        assertEquals("invalid_submission", result.getError(), "Error should be invalid_submission");
        assertEquals("Presentation submission is required", result.getErrorDescription(), "Error description should match");
    }
    
    @Test
    void testProcessPresentationSubmission_VerificationFailed() {
        // Arrange
        PresentationResponse request = createValidPresentationResponse();
        
        // Mock verifier service to throw exception
        when(verifierService.verify(any(VerifyRequest.class)))
            .thenThrow(new RuntimeException("Verification failed"));
        
        // Act
        OpenID4VPService.VerificationResult result = service.processPresentationSubmission(request);
        
        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isError(), "Result should be an error");
        assertEquals("verification_failed", result.getError(), "Error should be verification_failed");
    }
    
    @Test
    void testProcessPresentationSubmission_ServiceException() {
        // Arrange
        PresentationResponse request = createValidPresentationResponse();
        
        // Mock service to throw exception
        OpenID4VPService spyService = spy(service);
        doThrow(new RuntimeException("Service error")).when(spyService).verifyVPSignature(any(com.nimbusds.jwt.SignedJWT.class));
        
        // Act
        OpenID4VPService.VerificationResult result = spyService.processPresentationSubmission(request);
        
        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.isError(), "Result should be an error");
        assertEquals("server_error", result.getError(), "Error should be server_error");
    }
    
    // Helper methods
    private AuthorizationRequest createValidAuthorizationRequest() {
        AuthorizationRequest request = new AuthorizationRequest();
        request.setResponseType("vp_token");
        request.setClientId("test-client");
        request.setState("test-state");
        return request;
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
}
