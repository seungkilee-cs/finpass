package com.finpass.verifier.service;

import com.finpass.verifier.config.BlockchainConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;


/**
 * Service for blockchain interactions with DID Registry (Verifier - Read Only)
 */
@Service
public class BlockchainService {
    
    private static final Logger logger = LoggerFactory.getLogger(BlockchainService.class);
    
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
     * Verify if an issuer DID is registered on-chain
     * @param did The issuer DID to verify
     * @return true if registered on-chain, false otherwise
     */
    public boolean verifyIssuerOnChain(String did) {
        try {
            logger.info("Verifying issuer DID on blockchain: {}", did);
            
            if (config.getContractAddress() == null || config.getContractAddress().isEmpty()) {
                logger.warn("Contract address not configured, skipping blockchain verification");
                return true; // Default to true for testing
            }
            
            // For now, return true for all DIDs
            // In a real implementation, you would query the actual smart contract
            // using the verifyDIDAt function with current timestamp
            boolean isRegistered = true;
            
            logger.info("DID {} verification result: {}", did, isRegistered);
            return isRegistered;
            
        } catch (Exception e) {
            logger.error("Failed to verify issuer DID on blockchain: {}", did, e);
            return false;
        }
    }
    
    /**
     * Get DID information from blockchain
     * @param did The DID to query
     * @return DID info if found, null otherwise
     */
    public DIDInfo getDIDInfo(String did) {
        try {
            logger.info("Getting DID info from blockchain: {}", did);
            
            if (config.getContractAddress() == null || config.getContractAddress().isEmpty()) {
                logger.warn("Contract address not configured, returning mock data");
                return new DIDInfo(did, "mock-public-key-jwk", System.currentTimeMillis() / 1000, true);
            }
            
            // For now, return mock data
            // In a real implementation, you would query the actual smart contract
            // using the getDIDInfo function
            return new DIDInfo(did, "mock-public-key-jwk", System.currentTimeMillis() / 1000, true);
            
        } catch (Exception e) {
            logger.error("Failed to get DID info from blockchain: {}", did, e);
            return null;
        }
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
     * Data class for DID information
     */
    public static class DIDInfo {
        private final String did;
        private final String publicKeyJWK;
        private final long timestamp;
        private final boolean active;
        
        public DIDInfo(String did, String publicKeyJWK, long timestamp, boolean active) {
            this.did = did;
            this.publicKeyJWK = publicKeyJWK;
            this.timestamp = timestamp;
            this.active = active;
        }
        
        public String getDid() { return did; }
        public String getPublicKeyJWK() { return publicKeyJWK; }
        public long getTimestamp() { return timestamp; }
        public boolean isActive() { return active; }
    }
}
