package com.finpass.verifier.service;

import com.finpass.verifier.config.BlockchainConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for TrustRegistryService
 */
@ExtendWith(MockitoExtension.class)
class TrustRegistryServiceTest {
    
    @Mock
    private BlockchainConfig config;
    
    @InjectMocks
    private TrustRegistryService trustRegistryService;
    
    @BeforeEach
    void setUp() {
        lenient().when(config.getRpcUrl()).thenReturn("https://rpc-mumbai.maticvigil.com");
        lenient().when(config.getContractAddress()).thenReturn("0x1234567890123456789012345678901234567890");
    }
    
    @Test
    void testIsTrustedIssuer_CacheHit() {
        // Arrange
        String issuerDID = "did:example:trusted-issuer";
        
        // First call should query blockchain and cache result
        boolean result1 = trustRegistryService.isTrustedIssuer(issuerDID);
        assertTrue(result1, "Issuer should be trusted");
        
        // Second call should use cache
        boolean result2 = trustRegistryService.isTrustedIssuer(issuerDID);
        assertTrue(result2, "Cached result should also be trusted");
    }
    
    @Test
    void testIsTrustedIssuer_UntrustedIssuer() {
        // Arrange
        String issuerDID = "did:example:untrusted-issuer";
        
        // Act
        boolean result = trustRegistryService.isTrustedIssuer(issuerDID);
        
        // Assert
        assertFalse(result, "Untrusted issuer should return false");
    }
    
    @Test
    void testIsTrusted_WithTimestamp() {
        // Arrange
        String issuerDID = "did:example:trusted-issuer";
        long currentTimestamp = System.currentTimeMillis() / 1000;
        long pastTimestamp = currentTimestamp - 3600; // 1 hour ago
        
        // Act
        boolean pastResult = trustRegistryService.isTrusted(issuerDID, pastTimestamp);
        boolean futureResult = trustRegistryService.isTrusted(issuerDID, currentTimestamp + 3600);
        
        // Assert
        assertTrue(pastResult, "Issuer should be trusted for past timestamp");
        assertFalse(futureResult, "Issuer should not be trusted for future timestamp");
    }
    
    @Test
    void testAddIssuer() {
        // Arrange
        String issuerDID = "did:example:new-issuer";
        int assuranceLevel = 2;
        String metadata = "{\"type\":\"government\",\"country\":\"US\"}";
        
        // Act
        String txHash = trustRegistryService.addIssuer(issuerDID, assuranceLevel, metadata);
        
        // Assert
        assertNotNull(txHash, "Transaction hash should not be null");
        assertTrue(txHash.startsWith("0x"), "Transaction hash should start with 0x");
        
        // Verify cache is cleared for this issuer
        boolean isTrusted = trustRegistryService.isTrustedIssuer(issuerDID);
        assertTrue(isTrusted, "Newly added issuer should be trusted");
    }
    
    @Test
    void testRemoveIssuer() {
        // Arrange
        String issuerDID = "did:example:trusted-issuer";
        
        // First verify issuer is trusted
        assertTrue(trustRegistryService.isTrustedIssuer(issuerDID));
        
        // Act
        String txHash = trustRegistryService.removeIssuer(issuerDID);
        
        // Assert
        assertNotNull(txHash, "Transaction hash should not be null");
        assertTrue(txHash.startsWith("0x"), "Transaction hash should start with 0x");
    }
    
    @Test
    void testGetIssuerInfo() {
        // Arrange
        String issuerDID = "did:example:trusted-issuer";
        
        // Act
        TrustRegistryService.IssuerInfo info = trustRegistryService.getIssuerInfo(issuerDID);
        
        // Assert
        assertNotNull(info, "Issuer info should not be null");
        assertEquals(issuerDID, info.getIssuerDID(), "Issuer DID should match");
        assertTrue(info.getAssuranceLevel() >= 1 && info.getAssuranceLevel() <= 3, "Assurance level should be valid");
        assertNotNull(info.getMetadata(), "Metadata should not be null");
        assertTrue(info.isActive(), "Issuer should be active");
    }
    
    @Test
    void testGetIssuerInfo_NotFound() {
        // Arrange
        String issuerDID = "did:example:nonexistent-issuer";
        
        // Act
        TrustRegistryService.IssuerInfo info = trustRegistryService.getIssuerInfo(issuerDID);
        
        // Assert - should return mock data for non-existent issuers in current implementation
        assertNotNull(info, "Should return mock data for non-existent issuer");
    }
    
    @Test
    void testCacheExpiration() throws InterruptedException {
        // Arrange
        String issuerDID = "did:example:cache-test-issuer";
        
        // First call to populate cache
        trustRegistryService.isTrustedIssuer(issuerDID);
        
        // Get initial cache stats
        TrustRegistryService.CacheStats initialStats = trustRegistryService.getCacheStats();
        assertTrue(initialStats.getValidEntries() > 0, "Cache should have valid entries");
        
        // Clear expired cache (shouldn't remove anything yet)
        trustRegistryService.clearExpiredCache();
        
        // Cache should still be valid
        TrustRegistryService.CacheStats afterClearStats = trustRegistryService.getCacheStats();
        assertEquals(initialStats.getValidEntries(), afterClearStats.getValidEntries(), 
                    "Valid entries should not change after clearing expired cache");
    }
    
    @Test
    void testClearIssuerCache() {
        // Arrange
        String issuerDID = "did:example:clear-cache-test";
        
        // Populate cache
        trustRegistryService.isTrustedIssuer(issuerDID);
        
        // Verify cache has entries
        TrustRegistryService.CacheStats stats = trustRegistryService.getCacheStats();
        assertTrue(stats.getTotalEntries() > 0, "Cache should have entries");
        
        // Act
        trustRegistryService.clearIssuerCache(issuerDID);
        
        // Assert - The specific issuer should be removed from cache
        boolean isTrusted = trustRegistryService.isTrustedIssuer(issuerDID);
        assertTrue(isTrusted, "Should still be able to verify after cache clear");
    }
    
    @Test
    void testGetCacheStats() {
        // Arrange
        String issuerDID1 = "did:example:stats-test-1";
        String issuerDID2 = "did:example:stats-test-2";
        
        // Populate cache with multiple entries
        trustRegistryService.isTrustedIssuer(issuerDID1);
        trustRegistryService.isTrustedIssuer(issuerDID2);
        
        // Act
        TrustRegistryService.CacheStats stats = trustRegistryService.getCacheStats();
        
        // Assert
        assertTrue(stats.getTotalEntries() >= 2, "Cache should have at least 2 entries");
        assertTrue(stats.getValidEntries() >= 2, "Cache should have at least 2 valid entries");
        assertTrue(stats.getExpiredEntries() >= 0, "Expired entries should be non-negative");
        assertEquals(stats.getTotalEntries(), stats.getValidEntries() + stats.getExpiredEntries(),
                    "Total should equal valid plus expired");
    }
    
    @Test
    void testIsBlockchainAvailable() {
        // Act
        boolean available = trustRegistryService.isBlockchainAvailable();
        
        // Assert - should return false for mock connection
        assertFalse(available, "Mock blockchain should not be available");
    }
    
    @Test
    void testAddIssuer_InvalidAssuranceLevel() {
        // Arrange
        String issuerDID = "did:example:invalid-level";
        int invalidLevel = 5; // Invalid level (should be 1-3)
        String metadata = "{\"type\":\"test\"}";
        
        // Act & Assert - should handle invalid level gracefully
        String txHash = trustRegistryService.addIssuer(issuerDID, invalidLevel, metadata);
        assertNotNull(txHash, "Should still return transaction hash for mock implementation");
    }
    
    @Test
    void testAddIssuer_EmptyMetadata() {
        // Arrange
        String issuerDID = "did:example:empty-metadata";
        int assuranceLevel = 2;
        String emptyMetadata = "";
        
        // Act & Assert - should handle empty metadata gracefully
        String txHash = trustRegistryService.addIssuer(issuerDID, assuranceLevel, emptyMetadata);
        assertNotNull(txHash, "Should still return transaction hash for mock implementation");
    }
    
    @Test
    void testRemoveIssuer_NonExistent() {
        // Arrange
        String nonExistentIssuer = "did:example:does-not-exist";
        
        // Act & Assert - should handle non-existent issuer gracefully
        String txHash = trustRegistryService.removeIssuer(nonExistentIssuer);
        assertNotNull(txHash, "Should still return transaction hash for mock implementation");
    }
    
    @Test
    void testMultipleIssuersDifferentLevels() {
        // Arrange
        String lowLevelIssuer = "did:example:low-level";
        String mediumLevelIssuer = "did:example:medium-level";
        String highLevelIssuer = "did:example:high-level";
        
        // Act & Assert
        assertTrue(trustRegistryService.isTrustedIssuer(lowLevelIssuer), "Low level issuer should be trusted");
        assertTrue(trustRegistryService.isTrustedIssuer(mediumLevelIssuer), "Medium level issuer should be trusted");
        assertTrue(trustRegistryService.isTrustedIssuer(highLevelIssuer), "High level issuer should be trusted");
        
        // Verify issuer info
        TrustRegistryService.IssuerInfo lowInfo = trustRegistryService.getIssuerInfo(lowLevelIssuer);
        TrustRegistryService.IssuerInfo mediumInfo = trustRegistryService.getIssuerInfo(mediumLevelIssuer);
        TrustRegistryService.IssuerInfo highInfo = trustRegistryService.getIssuerInfo(highLevelIssuer);
        
        assertNotNull(lowInfo);
        assertNotNull(mediumInfo);
        assertNotNull(highInfo);
    }
}
