package com.finpass.issuer.service;

import com.finpass.issuer.dto.CredentialStatusResponse;
import com.finpass.issuer.dto.RevocationRequest;
import com.finpass.issuer.entity.CredentialEntity;
import com.finpass.issuer.entity.CredentialStatusEntity;
import com.finpass.issuer.repository.CredentialRepository;
import com.finpass.issuer.repository.CredentialStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for credential revocation and status management
 */
@Service
public class RevocationService {

    private static final Logger logger = LoggerFactory.getLogger(RevocationService.class);

    private final CredentialStatusRepository credentialStatusRepository;
    private final CredentialRepository credentialRepository;

    @Autowired
    public RevocationService(CredentialStatusRepository credentialStatusRepository, 
                           CredentialRepository credentialRepository) {
        this.credentialStatusRepository = credentialStatusRepository;
        this.credentialRepository = credentialRepository;
    }

    /**
     * Revoke a credential
     */
    @Transactional
    @CacheEvict(value = "credentialStatus", key = "#credentialId")
    public CredentialStatusResponse revokeCredential(UUID credentialId, RevocationRequest request) {
        logger.info("Revoking credential {} with reason {} by {}", 
                   credentialId, request.getRevocationReason(), request.getRevokedBy());

        // Find credential
        CredentialEntity credential = credentialRepository.findById(credentialId)
            .orElseThrow(() -> new IllegalArgumentException("Credential not found: " + credentialId));

        // Find or create status record
        CredentialStatusEntity status = credentialStatusRepository.findByCredentialId(credentialId)
            .orElseGet(() -> {
                CredentialStatusEntity newStatus = new CredentialStatusEntity(credential);
                return credentialStatusRepository.save(newStatus);
            });

        // Check if already revoked
        if (status.isRevoked()) {
            logger.warn("Credential {} is already revoked", credentialId);
            throw new IllegalStateException("Credential is already revoked");
        }

        // Revoke the credential
        status.revoke(request.getRevocationReason(), request.getRevokedBy(), request.getReasonDescription());
        credentialStatusRepository.save(status);

        logger.info("Successfully revoked credential {}", credentialId);

        return CredentialStatusResponse.revoked(
            credentialId,
            status.getRevokedAt(),
            status.getRevocationReason(),
            status.getRevokedBy(),
            status.getReasonDescription()
        );
    }

    /**
     * Suspend a credential
     */
    @Transactional
    @CacheEvict(value = "credentialStatus", key = "#credentialId")
    public CredentialStatusResponse suspendCredential(UUID credentialId, String suspendedBy, String reason) {
        logger.info("Suspending credential {} by {}", credentialId, suspendedBy);

        CredentialEntity credential = credentialRepository.findById(credentialId)
            .orElseThrow(() -> new IllegalArgumentException("Credential not found: " + credentialId));

        CredentialStatusEntity status = credentialStatusRepository.findByCredentialId(credentialId)
            .orElseGet(() -> {
                CredentialStatusEntity newStatus = new CredentialStatusEntity(credential);
                return credentialStatusRepository.save(newStatus);
            });

        if (status.isRevoked()) {
            throw new IllegalStateException("Cannot suspend a revoked credential");
        }

        status.suspend(suspendedBy, reason);
        credentialStatusRepository.save(status);

        logger.info("Successfully suspended credential {}", credentialId);

        return CredentialStatusResponse.suspended(credentialId, suspendedBy, reason);
    }

    /**
     * Reinstate a suspended credential
     */
    @Transactional
    @CacheEvict(value = "credentialStatus", key = "#credentialId")
    public CredentialStatusResponse reinstateCredential(UUID credentialId, String reinstatedBy) {
        logger.info("Reinstating credential {} by {}", credentialId, reinstatedBy);

        CredentialStatusEntity status = credentialStatusRepository.findByCredentialId(credentialId)
            .orElseThrow(() -> new IllegalArgumentException("Credential status not found: " + credentialId));

        if (status.isRevoked()) {
            throw new IllegalStateException("Cannot reinstate a revoked credential");
        }

        if (!status.isSuspended()) {
            throw new IllegalStateException("Credential is not suspended");
        }

        status.reinstate(reinstatedBy);
        credentialStatusRepository.save(status);

        logger.info("Successfully reinstated credential {}", credentialId);

        return CredentialStatusResponse.valid(credentialId);
    }

    /**
     * Get credential status with caching
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "credentialStatus", key = "#credentialId")
    public CredentialStatusResponse getCredentialStatus(UUID credentialId) {
        logger.debug("Getting status for credential {}", credentialId);

        // First check if credential exists
        if (!credentialRepository.existsById(credentialId)) {
            throw new IllegalArgumentException("Credential not found: " + credentialId);
        }

        return credentialStatusRepository.findByCredentialId(credentialId)
            .map(status -> {
                switch (status.getStatus()) {
                    case VALID:
                        return CredentialStatusResponse.valid(credentialId);
                    case REVOKED:
                        return CredentialStatusResponse.revoked(
                            credentialId,
                            status.getRevokedAt(),
                            status.getRevocationReason(),
                            status.getRevokedBy(),
                            status.getReasonDescription()
                        );
                    case SUSPENDED:
                        return CredentialStatusResponse.suspended(
                            credentialId,
                            status.getRevokedBy(),
                            status.getReasonDescription()
                        );
                    default:
                        throw new IllegalStateException("Unknown credential status: " + status.getStatus());
                }
            })
            .orElseGet(() -> {
                // If no status record exists, assume valid
                logger.debug("No status record found for credential {}, assuming valid", credentialId);
                return CredentialStatusResponse.valid(credentialId);
            });
    }

    /**
     * Check if credential is valid (for verification)
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "credentialValidity", key = "#credentialId")
    public boolean isCredentialValid(UUID credentialId) {
        return credentialStatusRepository.isCredentialValid(credentialId);
    }

    /**
     * Check if credential is revoked (for verification)
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "credentialRevocation", key = "#credentialId")
    public boolean isCredentialRevoked(UUID credentialId) {
        return credentialStatusRepository.isCredentialRevoked(credentialId);
    }

    /**
     * Get all revoked credentials
     */
    @Transactional(readOnly = true)
    public List<CredentialStatusEntity> getRevokedCredentials() {
        return credentialStatusRepository.findByStatus(CredentialStatusEntity.Status.REVOKED);
    }

    /**
     * Get credentials revoked after a specific time
     */
    @Transactional(readOnly = true)
    public List<CredentialStatusEntity> getCredentialsRevokedAfter(Instant timestamp) {
        return credentialStatusRepository.findRevokedAfter(timestamp);
    }

    /**
     * Get credentials revoked by a specific admin
     */
    @Transactional(readOnly = true)
    public List<CredentialStatusEntity> getCredentialsRevokedBy(String revokedBy) {
        return credentialStatusRepository.findByRevokedBy(revokedBy);
    }

    /**
     * Initialize status for a newly issued credential
     */
    @Transactional
    public CredentialStatusEntity initializeCredentialStatus(CredentialEntity credential) {
        logger.debug("Initializing status for new credential {}", credential.getId());

        CredentialStatusEntity status = new CredentialStatusEntity(credential);
        return credentialStatusRepository.save(status);
    }

    /**
     * Validate admin authorization for revocation operations
     */
    public boolean validateAdminAuthorization(String adminToken) {
        // In a real implementation, this would validate JWT tokens or API keys
        // For MVP, we'll use a simple token validation
        return "admin-secret-token".equals(adminToken) || 
               "super-admin-token".equals(adminToken);
    }
}
