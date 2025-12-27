package com.finpass.verifier.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for blockchain integration
 */
@Configuration
public class BlockchainConfig {
    
    @Value("${blockchain.rpc.url:https://rpc-mumbai.maticvigil.com}")
    private String rpcUrl;
    
    @Value("${blockchain.contract.address:}")
    private String contractAddress;
    
    @Value("${blockchain.gas.limit:200000}")
    private Long gasLimit;
    
    @Value("${blockchain.gas.price:20000000000}")
    private Long gasPrice;
    
    public String getRpcUrl() {
        return rpcUrl;
    }
    
    public String getContractAddress() {
        return contractAddress;
    }
    
    public Long getGasLimit() {
        return gasLimit;
    }
    
    public Long getGasPrice() {
        return gasPrice;
    }
}
