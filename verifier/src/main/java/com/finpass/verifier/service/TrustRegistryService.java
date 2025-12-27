package com.finpass.verifier.service;

import com.finpass.verifier.config.BlockchainConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Service for interacting with the Trust Registry smart contract
 * Provides caching and fallback mechanisms for trust registry operations
 */
@Service
public class TrustRegistryService {
    
    private static final Logger logger = LoggerFactory.getLogger(TrustRegistryService.class);
    
    // Cache configuration
    private static final long CACHE_TTL_HOURS = 1;
    private static final long CACHE_TTL_SECONDS = TimeUnit.HOURS.toSeconds(CACHE_TTL_HOURS);
    
    // In-memory cache for trust registry entries
    private final ConcurrentHashMap<String, CachedTrustEntry> trustCache = new ConcurrentHashMap<>();
    
    @Autowired
    private BlockchainConfig config;
    
    private Web3j web3j;
    
    // Initialize Web3j connection
    private Web3j getWeb3j() {
        if (web3j == null) {
            web3j = Web3j.build(new HttpService(config.getRpcUrl()));
        }
        return web3j;
    }
    
    /**
     * Add an issuer to the trust registry (admin function)
     * @param issuerDID The DID of the issuer to add
     * @param assuranceLevel The assurance level (1-3)
     * @param metadata Additional metadata
     * @return Transaction hash if successful, null otherwise
     */
    public String addIssuer(String issuerDID, int assuranceLevel, String metadata) {
        try {
            logger.info("Adding issuer {} to trust registry with level {}: {}", issuerDID, assuranceLevel, metadata);
            
            if (config.getContractAddress() == null || config.getContractAddress().isEmpty()) {
                logger.warn("Trust registry contract address not configured, using mock implementation");
                return mockAddIssuer(issuerDID, assuranceLevel, metadata);
            }
            
            // For now, return mock transaction hash
            // In a real implementation, you would interact with the actual smart contract
            String mockTxHash = "0xtrustregistry" + System.currentTimeMillis();
            
            // Invalidate cache for this issuer
            trustCache.remove(issuerDID);
            
            logger.info("Added issuer {} to trust registry with transaction hash: {}", issuerDID, mockTxHash);
            return mockTxHash;
            
        } catch (Exception e) {
            logger.error("Failed to add issuer to trust registry: {}", issuerDID, e);
            return null;
        }
    }
    
    /**
     * Remove an issuer from the trust registry (admin function)
     * @param issuerDID The DID of the issuer to remove
     * @return Transaction hash if successful, null otherwise
     */
    public String removeIssuer(String issuerDID) {
        try {
            logger.info("Removing issuer {} from trust registry", issuerDID);
            
            if (config.getContractAddress() == null || config.getContractAddress().isEmpty()) {
                logger.warn("Trust registry contract address not configured, using mock implementation");
                return mockRemoveIssuer(issuerDID);
            }
            
            // For now, return mock transaction hash
            // In a real implementation, you would interact with the actual smart contract
            String mockTxHash = "0xtrustregistry" + System.currentTimeMillis();
            
            // Invalidate cache for this issuer
            trustCache.remove(issuerDID);
            
            logger.info("Removed issuer {} from trust registry with transaction hash: {}", issuerDID, mockTxHash);
            return mockTxHash;
            
        } catch (Exception e) {
            logger.error("Failed to remove issuer from trust registry: {}", issuerDID, e);
            return null;
        }
    }
    
    /**
     * Check if an issuer is trusted at a specific timestamp
     * @param issuerDID The DID of the issuer to check
     * @param timestamp The timestamp to check against
     * @return True if issuer was trusted at the given timestamp
     */
    public boolean isTrusted(String issuerDID, long timestamp) {
        try {
            logger.debug("Checking if issuer {} is trusted at timestamp {}", issuerDID, timestamp);
            
            // Check cache first
            CachedTrustEntry cached = trustCache.get(issuerDID);
            if (cached != null && !cached.isExpired()) {
                logger.debug("Using cached trust status for issuer {}: {}", issuerDID, cached.isTrusted);
                return cached.isTrusted && cached.addedAt <= timestamp;
            }
            
            // Cache miss or expired, query blockchain
            boolean isTrusted = queryBlockchainTrustStatus(issuerDID);
            
            // Cache the result
            long currentTime = System.currentTimeMillis() / 1000;
            trustCache.put(issuerDID, new CachedTrustEntry(isTrusted, currentTime));
            
            logger.debug("Queried blockchain trust status for issuer {}: {}", issuerDID, isTrusted);
            return isTrusted && currentTime <= timestamp;
            
        } catch (Exception e) {
            logger.error("Failed to check trust status for issuer: {}", issuerDID, e);
            return false;
        }
    }
    
    /**
     * Check if an issuer is currently trusted
     * @param issuerDID The DID of the issuer to check
     * @return True if issuer is currently trusted
     */
    public boolean isTrustedIssuer(String issuerDID) {
        try {
            logger.debug("Checking if issuer {} is currently trusted", issuerDID);
            
            // Check cache first
            CachedTrustEntry cached = trustCache.get(issuerDID);
            if (cached != null && !cached.isExpired()) {
                logger.debug("Using cached trust status for issuer {}: {}", issuerDID, cached.isTrusted);
                return cached.isTrusted;
            }
            
            // Cache miss or expired, query blockchain
            boolean isTrusted = queryBlockchainTrustStatus(issuerDID);
            
            // Cache the result
            long currentTime = System.currentTimeMillis() / 1000;
            trustCache.put(issuerDID, new CachedTrustEntry(isTrusted, currentTime));
            
            logger.debug("Queried blockchain trust status for issuer {}: {}", issuerDID, isTrusted);
            return isTrusted;
            
        } catch (Exception e) {
            logger.error("Failed to check trust status for issuer: {}", issuerDID, e);
            return false;
        }
    }
    
    /**
     * Get issuer information from the trust registry
     * @param issuerDID The DID of the issuer
     * @return Issuer information if found, null otherwise
     */
    public IssuerInfo getIssuerInfo(String issuerDID) {
        try {
            logger.debug("Getting issuer info for: {}", issuerDID);
            
            if (config.getContractAddress() == null || config.getContractAddress().isEmpty()) {
                logger.warn("Trust registry contract address not configured, returning mock data");
                return new IssuerInfo(issuerDID, 2, "mock-metadata", System.currentTimeMillis() / 1000, true);
            }
            
            // For now, return mock data
            // In a real implementation, you would query the actual smart contract
            return new IssuerInfo(issuerDID, 2, "mock-metadata", System.currentTimeMillis() / 1000, true);
            
        } catch (Exception e) {
            logger.error("Failed to get issuer info: {}", issuerDID, e);
            return null;
        }
    }
    
    /**
     * Clear expired cache entries
     */
    public void clearExpiredCache() {
        try {
            trustCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
            logger.debug("Cleared expired cache entries");
        } catch (Exception e) {
            logger.error("Failed to clear expired cache entries", e);
        }
    }
    
    /**
     * Clear cache for a specific issuer
     * @param issuerDID The issuer DID to clear from cache
     */
    public void clearIssuerCache(String issuerDID) {
        trustCache.remove(issuerDID);
        logger.debug("Cleared cache for issuer: {}", issuerDID);
    }
    
    /**
     * Get cache statistics
     * @return Cache statistics
     */
    public CacheStats getCacheStats() {
        long expiredCount = trustCache.values().stream()
            .mapToLong(entry -> entry.isExpired() ? 1 : 0)
            .sum();
        
        return new CacheStats(trustCache.size(), (int) expiredCount);
    }
    
    /**
     * Query blockchain for trust status (mock implementation)
     * @param issuerDID The issuer DID to query
     * @return True if trusted, false otherwise
     */
    private boolean queryBlockchainTrustStatus(String issuerDID) {
        // For now, return true for all DIDs with "trusted" in the name, false otherwise
        // In a real implementation, you would query the actual smart contract
        return issuerDID.contains("trusted") || issuerDID.startsWith("did:example:");
    }
    
    /**
     * Mock implementation for adding issuer
     */
    private String mockAddIssuer(String issuerDID, int assuranceLevel, String metadata) {
        logger.info("Mock: Added issuer {} with level {} and metadata: {}", issuerDID, assuranceLevel, metadata);
        return "0xmocktx" + System.currentTimeMillis();
    }
    
    /**
     * Mock implementation for removing issuer
     */
    private String mockRemoveIssuer(String issuerDID) {
        logger.info("Mock: Removed issuer {}", issuerDID);
        return "0xmocktx" + System.currentTimeMillis();
    }
    
    /**
     * Check if blockchain connection is available
     * @return true if connected, false otherwise
     */
    public boolean isBlockchainAvailable() {
        try {
            getWeb3j().ethBlockNumber().send();
            return true;
        } catch (Exception e) {
            logger.error("Blockchain connection unavailable", e);
            return false;
        }
    }
    
    /**
     * Cached trust entry with TTL
     */
    private static class CachedTrustEntry {
        final boolean isTrusted;
        final long addedAt;
        final long expiresAt;
        
        CachedTrustEntry(boolean isTrusted, long addedAt) {
            this.isTrusted = isTrusted;
            this.addedAt = addedAt;
            this.expiresAt = addedAt + CACHE_TTL_SECONDS;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() / 1000 > expiresAt;
        }
    }
    
    /**
     * Data class for issuer information
     */
    public static class IssuerInfo {
        private final String issuerDID;
        private final int assuranceLevel;
        private final String metadata;
        private final long addedAt;
        private final boolean active;
        
        public IssuerInfo(String issuerDID, int assuranceLevel, String metadata, long addedAt, boolean active) {
            this.issuerDID = issuerDID;
            this.assuranceLevel = assuranceLevel;
            this.metadata = metadata;
            this.addedAt = addedAt;
            this.active = active;
        }
        
        public String getIssuerDID() { return issuerDID; }
        public int getAssuranceLevel() { return assuranceLevel; }
        public String getMetadata() { return metadata; }
        public long getAddedAt() { return addedAt; }
        public boolean isActive() { return active; }
    }
    
    /**
     * Data class for cache statistics
     */
    public static class CacheStats {
        private final int totalEntries;
        private final int expiredEntries;
        
        public CacheStats(int totalEntries, int expiredEntries) {
            this.totalEntries = totalEntries;
            this.expiredEntries = expiredEntries;
        }
        
        public int getTotalEntries() { return totalEntries; }
        public int getExpiredEntries() { return expiredEntries; }
        public int getValidEntries() { return totalEntries - expiredEntries; }
    }
}
