package com.finpass.verifier.service;

import com.finpass.verifier.config.TrustedIssuers;
import com.finpass.verifier.dto.VerifyRequest;
import com.finpass.verifier.dto.VerifyResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for VerifierService with TrustRegistryService
 */
@ExtendWith(MockitoExtension.class)
class VerifierServiceTrustRegistryIntegrationTest {
    
    @Mock
    private TrustedIssuers trustedIssuers;
    
    @Mock
    private VerifierKeyProvider keyProvider;
    
    @Mock
    private ChallengeStore challengeStore;
    
    @Mock
    private BlockchainService blockchainService;
    
    @Mock
    private TrustRegistryService trustRegistryService;
    
    @InjectMocks
    private VerifierService verifierService;
    
    private VerifyRequest validRequest;
    
    @BeforeEach
    void setUp() {
        // Setup valid verification request
        validRequest = new VerifyRequest();
        validRequest.setChallenge("test-challenge-123");
        validRequest.setHolderDid("did:example:holder");
        validRequest.setCommitmentJwt("eyJhbGciOiJFZERTQSIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJkaWQ6ZXhhbXBsZTp0cnVzdGVkLWlzc3VlciIsInN1YiI6ImRpZDpleGFtcGxlOmhvbGRlciIsImp0aSI6InRlc3Qtand0LWlkIiwiY29tbWl0bWVudF9oYXNoIjoiYWJjZGVmMTIzNDU2Nzg5MCIsImlhdCI6MTY0MDk5NTIwMH0.signature");
        validRequest.setProof("mock-proof");
        
        Map<String, Object> publicSignals = new HashMap<>();
        publicSignals.put("challenge", "test-challenge-123");
        publicSignals.put("predicate", "over_18");
        publicSignals.put("result", true);
        validRequest.setPublicSignals(publicSignals);
        
        // Default mock behaviors
        doNothing().when(challengeStore).consumeOrThrow(anyString());
        when(trustedIssuers.isTrusted(anyString())).thenReturn(true);
        when(trustedIssuers.verifierFor(anyString())).thenReturn(mock(com.nimbusds.jose.JWSVerifier.class));
        when(blockchainService.verifyIssuerOnChain(anyString())).thenReturn(true);
        when(keyProvider.getAlgorithm()).thenReturn(com.nimbusds.jose.JWSAlgorithm.EdDSA);
        when(keyProvider.getKeyId()).thenReturn("test-key-id");
    }
    
    @Test
    void testVerify_WithTrustRegistrySuccess() throws Exception {
        // Arrange
        String issuerDID = "did:example:trusted-issuer";
        when(trustRegistryService.isTrustedIssuer(issuerDID)).thenReturn(true);
        
        // Mock JWT verification
        mockJWTVerification();
        
        // Act
        VerifyResponse response = verifierService.verify(validRequest);
        
        // Assert
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getDecisionToken(), "Decision token should not be null");
        assertEquals("LOW", response.getAssuranceLevel(), "Assurance level should be LOW");
        assertEquals(List.of("over_18"), response.getVerifiedClaims(), "Should verify over_18 claim");
        assertTrue(response.getExpiresIn() > 0, "Expires in should be positive");
        
        // Verify trust registry was called
        verify(trustRegistryService, times(1)).isTrustedIssuer(issuerDID);
    }
    
    @Test
    void testVerify_TrustRegistryFailure() throws Exception {
        // Arrange
        String issuerDID = "did:example:untrusted-issuer";
        when(trustRegistryService.isTrustedIssuer(issuerDID)).thenReturn(false);
        
        // Mock JWT verification
        mockJWTVerification();
        
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> verifierService.verify(validRequest),
            "Should throw exception for untrusted issuer"
        );
        
        assertTrue(exception.getMessage().contains("not found in trust registry"), 
                  "Exception message should mention trust registry");
        
        // Verify trust registry was called
        verify(trustRegistryService, times(1)).isTrustedIssuer(issuerDID);
    }
    
    @Test
    void testVerify_TrustRegistryUnavailable() throws Exception {
        // Arrange
        String issuerDID = "did:example:trusted-issuer";
        when(trustRegistryService.isTrustedIssuer(issuerDID))
            .thenThrow(new RuntimeException("Blockchain unavailable"));
        
        // Mock JWT verification
        mockJWTVerification();
        
        // Act
        VerifyResponse response = verifierService.verify(validRequest);
        
        // Assert - should proceed with verification despite trust registry failure
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getDecisionToken(), "Decision token should not be null");
        
        // Verify trust registry was called
        verify(trustRegistryService, times(1)).isTrustedIssuer(issuerDID);
    }
    
    @Test
    void testVerify_MultipleTrustChecks() throws Exception {
        // Arrange
        String issuerDID = "did:example:trusted-issuer";
        when(trustRegistryService.isTrustedIssuer(issuerDID)).thenReturn(true);
        
        // Mock JWT verification
        mockJWTVerification();
        
        // Act - verify multiple times
        verifierService.verify(validRequest);
        verifierService.verify(validRequest);
        verifierService.verify(validRequest);
        
        // Assert - trust registry should be checked each time
        verify(trustRegistryService, times(3)).isTrustedIssuer(issuerDID);
    }
    
    @Test
    void testVerify_CachingBehavior() throws Exception {
        // Arrange
        String issuerDID = "did:example:cached-issuer";
        when(trustRegistryService.isTrustedIssuer(issuerDID)).thenReturn(true);
        
        // Mock JWT verification
        mockJWTVerification();
        
        // Act - first verification
        VerifyResponse response1 = verifierService.verify(validRequest);
        assertNotNull(response1);
        
        // Act - second verification (should potentially use cache)
        VerifyResponse response2 = verifierService.verify(validRequest);
        assertNotNull(response2);
        
        // Assert - both responses should be valid
        assertNotNull(response1.getDecisionToken());
        assertNotNull(response2.getDecisionToken());
        
        // Trust registry service should be called for each verification
        verify(trustRegistryService, times(2)).isTrustedIssuer(issuerDID);
    }
    
    @Test
    void testVerify_TrustRegistryWithDifferentIssuers() throws Exception {
        // Arrange - setup different issuers with different trust levels
        String trustedIssuer = "did:example:trusted";
        String untrustedIssuer = "did:example:untrusted";
        
        when(trustRegistryService.isTrustedIssuer(trustedIssuer)).thenReturn(true);
        when(trustRegistryService.isTrustedIssuer(untrustedIssuer)).thenReturn(false);
        
        // Mock JWT verification for trusted issuer
        mockJWTVerification();
        
        // Create request for trusted issuer
        VerifyRequest trustedRequest = createRequestForIssuer(trustedIssuer);
        
        // Create request for untrusted issuer
        VerifyRequest untrustedRequest = createRequestForIssuer(untrustedIssuer);
        
        // Act & Assert - trusted issuer should succeed
        assertDoesNotThrow(() -> verifierService.verify(trustedRequest));
        
        // Act & Assert - untrusted issuer should fail
        assertThrows(IllegalArgumentException.class, 
                   () -> verifierService.verify(untrustedRequest));
        
        // Verify both issuers were checked
        verify(trustRegistryService, times(1)).isTrustedIssuer(trustedIssuer);
        verify(trustRegistryService, times(1)).isTrustedIssuer(untrustedIssuer);
    }
    
    @Test
    void testVerify_TrustRegistryPerformance() throws Exception {
        // Arrange
        String issuerDID = "did:example:performance-test";
        when(trustRegistryService.isTrustedIssuer(issuerDID)).thenReturn(true);
        
        // Mock JWT verification
        mockJWTVerification();
        
        // Act - perform multiple verifications
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 10; i++) {
            VerifyResponse response = verifierService.verify(validRequest);
            assertNotNull(response);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Assert - should complete within reasonable time (less than 5 seconds for 10 verifications)
        assertTrue(duration < 5000, "10 verifications should complete in less than 5 seconds, took: " + duration + "ms");
        
        // Verify trust registry was called for each verification
        verify(trustRegistryService, times(10)).isTrustedIssuer(issuerDID);
    }
    
    private void mockJWTVerification() throws Exception {
        // Mock the JWT verification process
        // This is a simplified mock - in real tests, you'd need to properly mock the JWT parsing and verification
        doNothing().when(challengeStore).consumeOrThrow(anyString());
    }
    
    private VerifyRequest createRequestForIssuer(String issuerDID) {
        VerifyRequest request = new VerifyRequest();
        request.setChallenge("test-challenge-" + issuerDID.hashCode());
        request.setHolderDid("did:example:holder");
        request.setCommitmentJwt("mock.jwt.for." + issuerDID);
        request.setProof("mock-proof");
        
        Map<String, Object> publicSignals = new HashMap<>();
        publicSignals.put("challenge", request.getChallenge());
        publicSignals.put("predicate", "over_18");
        publicSignals.put("result", true);
        request.setPublicSignals(publicSignals);
        
        return request;
    }
}
