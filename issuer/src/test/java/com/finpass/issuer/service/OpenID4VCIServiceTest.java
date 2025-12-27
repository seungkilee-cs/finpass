package com.finpass.issuer.service;

import com.finpass.issuer.dto.*;
import com.nimbusds.jose.JWSAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OpenID4VCI Service
 */
@ExtendWith(MockitoExtension.class)
class OpenID4VCIServiceTest {
    
    @Mock
    private IssuerKeyProvider keyProvider;
    
    @Mock
    private IssuerService issuerService;
    
    @InjectMocks
    private OpenID4VCIService service;
    
    @BeforeEach
    void setUp() {
        // Mock key provider
        when(keyProvider.getAlgorithm()).thenReturn(JWSAlgorithm.EdDSA);
        when(keyProvider.getKeyId()).thenReturn("test-key-id");
        
        // Mock issuer service
        when(issuerService.issuePassportCredential(anyString(), any()))
            .thenReturn(new com.finpass.issuer.dto.IssueResponse("test_credential_jwt"));
    }
    
    @Test
    void testGenerateIssuerMetadata() {
        // Act
        CredentialIssuerMetadata metadata = service.generateIssuerMetadata();
        
        // Assert
        assertNotNull(metadata, "Metadata should not be null");
        assertEquals("http://localhost:8080", metadata.getCredentialIssuer(), "Credential issuer should match");
        assertEquals("http://localhost:8080/credential", metadata.getCredentialEndpoint(), "Credential endpoint should match");
        assertEquals("http://localhost:8080/token", metadata.getTokenEndpoint(), "Token endpoint should match");
        
        assertNotNull(metadata.getDisplay(), "Display should not be null");
        assertFalse(metadata.getDisplay().isEmpty(), "Display should have entries");
        assertEquals("FinPass Passport Issuer", metadata.getDisplay().get(0).getName(), "Display name should match");
        
        assertNotNull(metadata.getCredentialsSupported(), "Credentials supported should not be null");
        assertFalse(metadata.getCredentialsSupported().isEmpty(), "Credentials supported should have entries");
        
        CredentialIssuerMetadata.CredentialSupported credential = metadata.getCredentialsSupported().get(0);
        assertEquals("jwt_vc", credential.getFormat(), "Format should be jwt_vc");
        assertTrue(credential.getTypes().contains("VerifiableCredential"), "Should contain VerifiableCredential type");
        assertTrue(credential.getTypes().contains("PassportCredential"), "Should contain PassportCredential type");
    }
    
    @Test
    void testProcessTokenRequest_Success() {
        // Arrange
        TokenRequest request = new TokenRequest();
        request.setGrantType("urn:ietf:params:oauth:grant-type:pre-authorized_code");
        request.setPreAuthorizedCode("valid_pre_auth_code");
        
        // Act
        TokenResponse response = service.processTokenRequest(request);
        
        // Assert
        assertNotNull(response, "Response should not be null");
        assertFalse(response.isError(), "Response should not be an error");
        assertNotNull(response.getAccessToken(), "Access token should not be null");
        assertEquals("Bearer", response.getTokenType(), "Token type should be Bearer");
        assertEquals(3600L, response.getExpiresIn(), "Expires in should be 3600");
        assertNotNull(response.getCNonce(), "C nonce should not be null");
        assertEquals(300L, response.getCNonceExpiresIn(), "C nonce expires in should be 300");
    }
    
    @Test
    void testProcessTokenRequest_UnsupportedGrantType() {
        // Arrange
        TokenRequest request = new TokenRequest();
        request.setGrantType("unsupported_grant_type");
        request.setPreAuthorizedCode("valid_pre_auth_code");
        
        // Act
        TokenResponse response = service.processTokenRequest(request);
        
        // Assert
        assertNotNull(response, "Response should not be null");
        assertTrue(response.isError(), "Response should be an error");
        assertEquals("unsupported_grant_type", response.getError(), "Error should be unsupported_grant_type");
        assertEquals("Unsupported grant type", response.getErrorDescription(), "Error description should match");
    }
    
    @Test
    void testProcessTokenRequest_MissingPreAuthorizedCode() {
        // Arrange
        TokenRequest request = new TokenRequest();
        request.setGrantType("urn:ietf:params:oauth:grant-type:pre-authorized_code");
        request.setPreAuthorizedCode("");
        
        // Act
        TokenResponse response = service.processTokenRequest(request);
        
        // Assert
        assertNotNull(response, "Response should not be null");
        assertTrue(response.isError(), "Response should be an error");
        assertEquals("invalid_grant", response.getError(), "Error should be invalid_grant");
        assertEquals("Invalid pre-authorized code", response.getErrorDescription(), "Error description should match");
    }
    
    @Test
    void testProcessTokenRequest_NullPreAuthorizedCode() {
        // Arrange
        TokenRequest request = new TokenRequest();
        request.setGrantType("urn:ietf:params:oauth:grant-type:pre-authorized_code");
        request.setPreAuthorizedCode(null);
        
        // Act
        TokenResponse response = service.processTokenRequest(request);
        
        // Assert
        assertNotNull(response, "Response should not be null");
        assertTrue(response.isError(), "Response should be an error");
        assertEquals("invalid_grant", response.getError(), "Error should be invalid_grant");
    }
    
    @Test
    void testProcessTokenRequest_ServiceException() {
        // Arrange
        TokenRequest request = new TokenRequest();
        request.setGrantType("urn:ietf:params:oauth:grant-type:pre-authorized_code");
        request.setPreAuthorizedCode("valid_pre_auth_code");
        
        // Mock service to throw exception
        OpenID4VCIService spyService = spy(service);
        doThrow(new RuntimeException("Service error")).when(spyService).generateAccessToken(anyString());
        
        // Act
        TokenResponse response = spyService.processTokenRequest(request);
        
        // Assert
        assertNotNull(response, "Response should not be null");
        assertTrue(response.isError(), "Response should be an error");
        assertEquals("server_error", response.getError(), "Error should be server_error");
    }
    
    @Test
    void testProcessCredentialRequest_Success() {
        // Arrange
        CredentialRequest request = createValidCredentialRequest();
        String accessToken = "valid_access_token";
        
        // Mock validation methods
        OpenID4VCIService spyService = spy(service);
        doReturn(true).when(spyService).validateAccessToken(accessToken);
        doReturn(true).when(spyService).validateProof(any(CredentialRequest.Proof.class));
        doReturn("did:example:subject").when(spyService).extractSubjectFromProof(any(CredentialRequest.Proof.class));
        
        // Act
        CredentialResponse response = spyService.processCredentialRequest(request, accessToken);
        
        // Assert
        assertNotNull(response, "Response should not be null");
        assertFalse(response.isError(), "Response should not be an error");
        assertEquals("jwt_vc", response.getFormat(), "Format should be jwt_vc");
        assertNotNull(response.getCredential(), "Credential should not be null");
        assertNotNull(response.getCNonce(), "C nonce should not be null");
        assertEquals(300L, response.getCNonceExpiresIn(), "C nonce expires in should be 300");
    }
    
    @Test
    void testProcessCredentialRequest_InvalidToken() {
        // Arrange
        CredentialRequest request = createValidCredentialRequest();
        String invalidToken = "invalid_token";
        
        // Mock validation to return false
        OpenID4VCIService spyService = spy(service);
        doReturn(false).when(spyService).validateAccessToken(invalidToken);
        
        // Act
        CredentialResponse response = spyService.processCredentialRequest(request, invalidToken);
        
        // Assert
        assertNotNull(response, "Response should not be null");
        assertTrue(response.isError(), "Response should be an error");
        assertEquals("invalid_token", response.getError(), "Error should be invalid_token");
        assertEquals("Invalid or expired access token", response.getErrorDescription(), "Error description should match");
    }
    
    @Test
    void testProcessCredentialRequest_InvalidProof() {
        // Arrange
        CredentialRequest request = createValidCredentialRequest();
        String accessToken = "valid_access_token";
        
        // Mock validation methods
        OpenID4VCIService spyService = spy(service);
        doReturn(true).when(spyService).validateAccessToken(accessToken);
        doReturn(false).when(spyService).validateProof(any(CredentialRequest.Proof.class));
        
        // Act
        CredentialResponse response = spyService.processCredentialRequest(request, accessToken);
        
        // Assert
        assertNotNull(response, "Response should not be null");
        assertTrue(response.isError(), "Response should be an error");
        assertEquals("invalid_proof", response.getError(), "Error should be invalid_proof");
        assertEquals("Invalid proof", response.getErrorDescription(), "Error description should match");
    }
    
    @Test
    void testProcessCredentialRequest_NullProof() {
        // Arrange
        CredentialRequest request = new CredentialRequest();
        request.setFormat("jwt_vc");
        request.setProof(null);
        
        String accessToken = "valid_access_token";
        
        // Mock validation
        OpenID4VCIService spyService = spy(service);
        doReturn(true).when(spyService).validateAccessToken(accessToken);
        
        // Act
        CredentialResponse response = spyService.processCredentialRequest(request, accessToken);
        
        // Assert
        assertNotNull(response, "Response should not be null");
        assertTrue(response.isError(), "Response should be an error");
        assertEquals("invalid_proof", response.getError(), "Error should be invalid_proof");
    }
    
    @Test
    void testProcessCredentialRequest_MissingSubject() {
        // Arrange
        CredentialRequest request = createValidCredentialRequest();
        String accessToken = "valid_access_token";
        
        // Mock validation methods
        OpenID4VCIService spyService = spy(service);
        doReturn(true).when(spyService).validateAccessToken(accessToken);
        doReturn(true).when(spyService).validateProof(any(CredentialRequest.Proof.class));
        doReturn(null).when(spyService).extractSubjectFromProof(any(CredentialRequest.Proof.class));
        
        // Act
        CredentialResponse response = spyService.processCredentialRequest(request, accessToken);
        
        // Assert
        assertNotNull(response, "Response should not be null");
        assertTrue(response.isError(), "Response should be an error");
        assertEquals("invalid_subject", response.getError(), "Error should be invalid_subject");
        assertEquals("Cannot extract subject from proof", response.getErrorDescription(), "Error description should match");
    }
    
    @Test
    void testProcessCredentialRequest_ServiceException() {
        // Arrange
        CredentialRequest request = createValidCredentialRequest();
        String accessToken = "valid_access_token";
        
        // Mock service to throw exception
        OpenID4VCIService spyService = spy(service);
        doThrow(new RuntimeException("Service error")).when(spyService).validateAccessToken(accessToken);
        
        // Act
        CredentialResponse response = spyService.processCredentialRequest(request, accessToken);
        
        // Assert
        assertNotNull(response, "Response should not be null");
        assertTrue(response.isError(), "Response should be an error");
        assertEquals("server_error", response.getError(), "Error should be server_error");
    }
    
    @Test
    void testGenerateCNonce() {
        // Act
        String nonce1 = service.generateCNonce();
        String nonce2 = service.generateCNonce();
        
        // Assert
        assertNotNull(nonce1, "Nonce should not be null");
        assertNotNull(nonce2, "Nonce should not be null");
        assertNotEquals(nonce1, nonce2, "Nonces should be unique");
        assertTrue(nonce1.matches("[0-9a-f-]+"), "Nonce should be valid UUID format");
        assertTrue(nonce2.matches("[0-9a-f-]+"), "Nonce should be valid UUID format");
    }
    
    // Helper methods
    private CredentialRequest createValidCredentialRequest() {
        CredentialRequest request = new CredentialRequest();
        request.setFormat("jwt_vc");
        
        CredentialRequest.CredentialDefinition definition = 
            new CredentialRequest.CredentialDefinition(
                java.util.List.of("VerifiableCredential", "PassportCredential")
            );
        request.setCredentialDefinition(definition);
        
        CredentialRequest.Proof proof = new CredentialRequest.Proof("jwt", "sample_proof_jwt");
        request.setProof(proof);
        
        return request;
    }
}
