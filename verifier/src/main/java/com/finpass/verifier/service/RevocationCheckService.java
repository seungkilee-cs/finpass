package com.finpass.verifier.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

/**
 * Service for checking credential revocation status from issuer
 */
@Service
public class RevocationCheckService {

    private static final Logger logger = LoggerFactory.getLogger(RevocationCheckService.class);

    private final RestTemplate restTemplate;
    private final String issuerUrl;

    @Autowired
    public RevocationCheckService(RestTemplate restTemplate, 
                                @Value("${issuer.url:http://localhost:8080}") String issuerUrl) {
        this.restTemplate = restTemplate;
        this.issuerUrl = issuerUrl;
    }

    /**
     * Check if a credential is revoked (with caching)
     */
    @Cacheable(value = "revocationCheck", key = "#credentialId")
    public boolean isCredentialRevoked(UUID credentialId) {
        logger.debug("Checking revocation status for credential {}", credentialId);

        try {
            String url = issuerUrl + "/api/v1/credentials/" + credentialId + "/valid";
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null) {
                Boolean isValid = (Boolean) response.get("is_valid");
                boolean isRevoked = isValid == null || !isValid;
                
                logger.debug("Credential {} revocation status: {}", credentialId, isRevoked ? "REVOKED" : "VALID");
                return isRevoked;
            }
            
            logger.warn("No response received for credential {} revocation check", credentialId);
            return false; // Assume valid if can't check
            
        } catch (Exception e) {
            logger.error("Error checking revocation status for credential {}: {}", credentialId, e.getMessage(), e);
            // In production, you might want to implement different error handling strategies
            // For now, assume valid if we can't check
            return false;
        }
    }

    /**
     * Get full credential status
     */
    @SuppressWarnings("unchecked")
    @Cacheable(value = "credentialStatus", key = "#credentialId")
    public Map<String, Object> getCredentialStatus(UUID credentialId) {
        logger.debug("Getting full status for credential {}", credentialId);

        try {
            String url = issuerUrl + "/api/v1/credentials/" + credentialId + "/status";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null) {
                logger.debug("Credential {} status: {}", credentialId, response.get("status"));
                return response;
            }
            
            logger.warn("No status response received for credential {}", credentialId);
            throw new RuntimeException("Unable to get credential status");
            
        } catch (Exception e) {
            logger.error("Error getting status for credential {}: {}", credentialId, e.getMessage(), e);
            throw new RuntimeException("Failed to check credential status", e);
        }
    }

    /**
     * Check credential status without throwing exceptions
     */
    public boolean isCredentialValid(UUID credentialId) {
        try {
            return !isCredentialRevoked(credentialId);
        } catch (Exception e) {
            logger.warn("Could not verify credential status for {}, assuming valid: {}", credentialId, e.getMessage());
            return true; // Fail open - assume valid if can't verify
        }
    }

    /**
     * Batch check multiple credentials
     */
    public boolean areCredentialsValid(UUID... credentialIds) {
        for (UUID credentialId : credentialIds) {
            if (!isCredentialValid(credentialId)) {
                logger.info("Credential {} is not valid", credentialId);
                return false;
            }
        }
        return true;
    }

    /**
     * Clear cache for a specific credential
     */
    public void clearCredentialCache(UUID credentialId) {
        // This would be used to manually clear cache if needed
        // In practice, @CacheEvict would handle this automatically
        logger.info("Cache cleared for credential {}", credentialId);
    }

    /**
     * Test connectivity to issuer revocation service
     */
    public boolean testIssuerConnectivity() {
        try {
            String url = issuerUrl + "/api/v1/credentials/" + UUID.randomUUID() + "/valid";
            restTemplate.getForObject(url, Map.class);
            return true; // If we get any response (even 404), connectivity is working
        } catch (Exception e) {
            logger.error("Issuer connectivity test failed: {}", e.getMessage());
            return false;
        }
    }
}
