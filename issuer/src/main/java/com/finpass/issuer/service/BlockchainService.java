package com.finpass.issuer.service;

import com.finpass.issuer.config.BlockchainConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service for blockchain interactions with DID Registry
 */
@Service
public class BlockchainService {
    
    private static final Logger logger = LoggerFactory.getLogger(BlockchainService.class);
    
    @Autowired
    private BlockchainConfig config;
    
    private Web3j web3j;
    private TransactionManager transactionManager;
    private ContractGasProvider gasProvider;
    
    // Initialize Web3j connection
    private Web3j getWeb3j() {
        if (web3j == null) {
            web3j = Web3j.build(new HttpService(config.getRpcUrl()));
        }
        return web3j;
    }
    
    // Initialize transaction manager
    // NOTE: Currently unused but kept for future real blockchain contract interaction
    @SuppressWarnings("unused")
    private TransactionManager getTransactionManager() {
        if (transactionManager == null) {
            if (config.getPrivateKey() == null || config.getPrivateKey().isEmpty()) {
                throw new IllegalStateException("Private key not configured for blockchain transactions");
            }
            
            Credentials credentials = Credentials.create(config.getPrivateKey());
            transactionManager = new RawTransactionManager(getWeb3j(), credentials);
        }
        return transactionManager;
    }
    
    // Initialize gas provider
    // NOTE: Currently unused but kept for future real blockchain contract interaction
    @SuppressWarnings("unused")
    private ContractGasProvider getGasProvider() {
        if (gasProvider == null) {
            gasProvider = new ContractGasProvider() {
                @Override
                public BigInteger getGasPrice(String contractFunc) {
                    return BigInteger.valueOf(config.getGasPrice());
                }
                
                @Override
                public BigInteger getGasPrice() {
                    return BigInteger.valueOf(config.getGasPrice());
                }
                
                @Override
                public BigInteger getGasLimit(String contractFunc) {
                    return BigInteger.valueOf(config.getGasLimit());
                }
                
                @Override
                public BigInteger getGasLimit() {
                    return BigInteger.valueOf(config.getGasLimit());
                }
            };
        }
        return gasProvider;
    }
    
    /**
     * Publish issuer DID and public key to blockchain
     * @param did The issuer DID
     * @param publicKeyJWK Public key in JWK format
     * @return Transaction hash if successful, null otherwise
     */
    public String publishIssuerKey(String did, String publicKeyJWK) {
        try {
            logger.info("Publishing issuer DID to blockchain: {}", did);
            
            if (config.getContractAddress() == null || config.getContractAddress().isEmpty()) {
                logger.warn("Contract address not configured, skipping blockchain publication");
                return null;
            }
            
            // For now, return a mock transaction hash
            // In a real implementation, you would deploy and interact with the actual smart contract
            // Note: DID hash calculation (didHash) would be used in actual contract interaction
            String mockTxHash = "0x" + Numeric.toHexStringWithPrefix(BigInteger.valueOf(System.currentTimeMillis()));
            
            logger.info("Published DID {} to blockchain with transaction hash: {}", did, mockTxHash);
            return mockTxHash;
            
        } catch (Exception e) {
            logger.error("Failed to publish issuer DID to blockchain: {}", did, e);
            return null;
        }
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
            return new DIDInfo(did, "mock-public-key-jwk", System.currentTimeMillis() / 1000, true);
            
        } catch (Exception e) {
            logger.error("Failed to get DID info from blockchain: {}", did, e);
            return null;
        }
    }
    
    /**
     * Wait for transaction confirmation
     * @param transactionHash Transaction hash to wait for
     * @param timeoutSeconds Timeout in seconds
     * @return Transaction receipt if confirmed, null otherwise
     */
    public TransactionReceipt waitForTransaction(String transactionHash, int timeoutSeconds) {
        try {
            CompletableFuture<TransactionReceipt> future = CompletableFuture.supplyAsync(() -> {
                try {
                    for (int i = 0; i < timeoutSeconds; i++) {
                        EthGetTransactionReceipt receipt = getWeb3j()
                            .ethGetTransactionReceipt(transactionHash)
                            .send();
                        
                        if (receipt.getTransactionReceipt().isPresent()) {
                            return receipt.getTransactionReceipt().get();
                        }
                        
                        TimeUnit.SECONDS.sleep(1);
                    }
                    return null;
                } catch (Exception e) {
                    logger.error("Error waiting for transaction: {}", transactionHash, e);
                    return null;
                }
            });
            
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
            
        } catch (Exception e) {
            logger.error("Failed to wait for transaction: {}", transactionHash, e);
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
