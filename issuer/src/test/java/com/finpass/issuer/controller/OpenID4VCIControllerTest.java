package com.finpass.issuer.controller;

import com.finpass.issuer.dto.*;
import com.finpass.issuer.service.OpenID4VCIService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for OpenID4VCI Controller
 */
@ExtendWith(MockitoExtension.class)
class OpenID4VCIControllerTest {
    
    @Mock
    private OpenID4VCIService openID4VCIService;
    
    @InjectMocks
    private OpenID4VCIController controller;
    
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }
    
    @Test
    void testGetCredentialIssuerMetadata_Success() throws Exception {
        // Arrange
        CredentialIssuerMetadata metadata = createSampleMetadata();
        when(openID4VCIService.generateIssuerMetadata()).thenReturn(metadata);
        
        // Act & Assert
        mockMvc.perform(get("/.well-known/openid-credential-issuer"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.credential_issuer").value("http://localhost:8080"))
                .andExpect(jsonPath("$.credential_endpoint").value("http://localhost:8080/credential"))
                .andExpect(jsonPath("$.token_endpoint").value("http://localhost:8080/token"))
                .andExpect(jsonPath("$.credentials_supported").isArray())
                .andExpect(jsonPath("$.display").isArray());
        
        verify(openID4VCIService, times(1)).generateIssuerMetadata();
    }
    
    @Test
    void testGetCredentialIssuerMetadata_Error() throws Exception {
        // Arrange
        when(openID4VCIService.generateIssuerMetadata())
            .thenThrow(new RuntimeException("Service error"));
        
        // Act & Assert
        mockMvc.perform(get("/.well-known/openid-credential-issuer"))
                .andExpect(status().isInternalServerError());
        
        verify(openID4VCIService, times(1)).generateIssuerMetadata();
    }
    
    @Test
    void testGetToken_Success() throws Exception {
        // Arrange
        TokenRequest request = createValidTokenRequest();
        TokenResponse response = createValidTokenResponse();
        
        when(openID4VCIService.processTokenRequest(any(TokenRequest.class)))
            .thenReturn(response);
        
        // Act & Assert
        mockMvc.perform(post("/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(3600))
                .andExpect(jsonPath("$.c_nonce").isNotEmpty())
                .andExpect(jsonPath("$.c_nonce_expires_in").value(300));
        
        verify(openID4VCIService, times(1)).processTokenRequest(any(TokenRequest.class));
    }
    
    @Test
    void testGetToken_InvalidGrantType() throws Exception {
        // Arrange
        TokenRequest request = new TokenRequest();
        request.setGrantType("invalid_grant_type");
        
        TokenResponse errorResponse = new TokenResponse();
        errorResponse.setError("unsupported_grant_type");
        errorResponse.setErrorDescription("Unsupported grant type");
        
        when(openID4VCIService.processTokenRequest(any(TokenRequest.class)))
            .thenReturn(errorResponse);
        
        // Act & Assert
        mockMvc.perform(post("/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("unsupported_grant_type"))
                .andExpect(jsonPath("$.error_description").value("Unsupported grant type"));
        
        verify(openID4VCIService, times(1)).processTokenRequest(any(TokenRequest.class));
    }
    
    @Test
    void testGetToken_MissingPreAuthorizedCode() throws Exception {
        // Arrange
        TokenRequest request = new TokenRequest();
        request.setGrantType("urn:ietf:params:oauth:grant-type:pre-authorized_code");
        request.setPreAuthorizedCode("");
        
        TokenResponse errorResponse = new TokenResponse();
        errorResponse.setError("invalid_grant");
        errorResponse.setErrorDescription("Invalid pre-authorized code");
        
        when(openID4VCIService.processTokenRequest(any(TokenRequest.class)))
            .thenReturn(errorResponse);
        
        // Act & Assert
        mockMvc.perform(post("/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid_grant"));
        
        verify(openID4VCIService, times(1)).processTokenRequest(any(TokenRequest.class));
    }
    
    @Test
    void testGetToken_ServerError() throws Exception {
        // Arrange
        TokenRequest request = createValidTokenRequest();
        
        when(openID4VCIService.processTokenRequest(any(TokenRequest.class)))
            .thenThrow(new RuntimeException("Internal error"));
        
        // Act & Assert
        mockMvc.perform(post("/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("server_error"));
    }
    
    @Test
    void testGetCredential_Success() throws Exception {
        // Arrange
        CredentialRequest request = createValidCredentialRequest();
        CredentialResponse response = createValidCredentialResponse();
        
        when(openID4VCIService.processCredentialRequest(any(CredentialRequest.class), anyString()))
            .thenReturn(response);
        
        // Act & Assert
        mockMvc.perform(post("/credential")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer valid_access_token")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.format").value("jwt_vc"))
                .andExpect(jsonPath("$.credential").isNotEmpty())
                .andExpect(jsonPath("$.c_nonce").isNotEmpty())
                .andExpect(jsonPath("$.c_nonce_expires_in").value(300));
        
        verify(openID4VCIService, times(1))
            .processCredentialRequest(any(CredentialRequest.class), eq("valid_access_token"));
    }
    
    @Test
    void testGetCredential_MissingAuthorization() throws Exception {
        // Arrange
        CredentialRequest request = createValidCredentialRequest();
        
        // Act & Assert
        mockMvc.perform(post("/credential")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_token"))
                .andExpect(jsonPath("$.error_description").value("Missing or invalid Authorization header"));
    }
    
    @Test
    void testGetCredential_InvalidToken() throws Exception {
        // Arrange
        CredentialRequest request = createValidCredentialRequest();
        CredentialResponse errorResponse = CredentialResponse.error("invalid_token", "Token expired");
        
        when(openID4VCIService.processCredentialRequest(any(CredentialRequest.class), anyString()))
            .thenReturn(errorResponse);
        
        // Act & Assert
        mockMvc.perform(post("/credential")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer invalid_token")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("invalid_token"));
        
        verify(openID4VCIService, times(1))
            .processCredentialRequest(any(CredentialRequest.class), eq("invalid_token"));
    }
    
    @Test
    void testGetCredential_InvalidProof() throws Exception {
        // Arrange
        CredentialRequest request = createValidCredentialRequest();
        CredentialResponse errorResponse = CredentialResponse.error("invalid_proof", "Proof verification failed");
        
        when(openID4VCIService.processCredentialRequest(any(CredentialRequest.class), anyString()))
            .thenReturn(errorResponse);
        
        // Act & Assert
        mockMvc.perform(post("/credential")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer valid_access_token")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("invalid_proof"));
        
        verify(openID4VCIService, times(1))
            .processCredentialRequest(any(CredentialRequest.class), eq("valid_access_token"));
    }
    
    @Test
    void testGetCredential_ServerError() throws Exception {
        // Arrange
        CredentialRequest request = createValidCredentialRequest();
        
        when(openID4VCIService.processCredentialRequest(any(CredentialRequest.class), anyString()))
            .thenThrow(new RuntimeException("Internal error"));
        
        // Act & Assert
        mockMvc.perform(post("/credential")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer valid_access_token")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("server_error"));
    }
    
    @Test
    void testHealthCheck() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/openid4vci/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("OpenID4VCI service is healthy"));
    }
    
    // Helper methods
    private CredentialIssuerMetadata createSampleMetadata() {
        CredentialIssuerMetadata metadata = new CredentialIssuerMetadata();
        metadata.setCredentialIssuer("http://localhost:8080");
        metadata.setCredentialEndpoint("http://localhost:8080/credential");
        metadata.setTokenEndpoint("http://localhost:8080/token");
        
        CredentialIssuerMetadata.Display display = new CredentialIssuerMetadata.Display(
            "FinPass Passport Issuer", 
            "en"
        );
        metadata.setDisplay(java.util.List.of(display));
        
        CredentialIssuerMetadata.CredentialSupported credential = 
            new CredentialIssuerMetadata.CredentialSupported(
                "jwt_vc", 
                java.util.List.of("VerifiableCredential", "PassportCredential")
            );
        metadata.setCredentialsSupported(java.util.List.of(credential));
        
        return metadata;
    }
    
    private TokenRequest createValidTokenRequest() {
        TokenRequest request = new TokenRequest();
        request.setGrantType("urn:ietf:params:oauth:grant-type:pre-authorized_code");
        request.setPreAuthorizedCode("valid_pre_auth_code");
        return request;
    }
    
    private TokenResponse createValidTokenResponse() {
        TokenResponse response = new TokenResponse();
        response.setAccessToken("sample_access_token");
        response.setTokenType("Bearer");
        response.setExpiresIn(3600L);
        response.setCNonce("sample_nonce");
        response.setCNonceExpiresIn(300L);
        return response;
    }
    
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
    
    private CredentialResponse createValidCredentialResponse() {
        CredentialResponse response = new CredentialResponse();
        response.setFormat("jwt_vc");
        response.setCredential("sample_credential_jwt");
        response.setCNonce("new_nonce");
        response.setCNonceExpiresIn(300L);
        return response;
    }
}
