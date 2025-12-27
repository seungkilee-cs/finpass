package com.finpass.issuer.service;

import com.finpass.issuer.dto.LivenessProof;
import com.finpass.issuer.entity.CredentialEntity;
import com.finpass.issuer.entity.UserEntity;
import com.finpass.issuer.repository.CredentialRepository;
import com.finpass.issuer.repository.UserRepository;
import com.finpass.issuer.validation.ValidationService;
import com.finpass.issuer.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Service for issuing verifiable credentials with liveness validation
 */
@Service
public class IssuerService {

    private static final Logger logger = LoggerFactory.getLogger(IssuerService.class);

    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;
    private final LivenessValidationService livenessValidationService;
    private final RevocationService revocationService;
    private final AuditService auditService;
    private final ValidationService validationService;

    public IssuerService(
            UserRepository userRepository,
            CredentialRepository credentialRepository,
            LivenessValidationService livenessValidationService,
            RevocationService revocationService,
            AuditService auditService,
            ValidationService validationService
    ) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.livenessValidationService = livenessValidationService;
        this.revocationService = revocationService;
        this.auditService = auditService;
        this.validationService = validationService;
    }

    /**
     * Issue passport credential (backward compatible)
     */
    @Transactional
    public com.finpass.issuer.dto.IssueResponse issuePassportCredential(String holderDid, Map<String, Object> passportData) {
        return issuePassportCredential(holderDid, passportData, null);
    }

    /**
     * Issue passport credential with liveness validation
     */
    @Transactional
    public com.finpass.issuer.dto.IssueResponse issuePassportCredential(String holderDid, Map<String, Object> passportData, LivenessProof livenessProof) {
        Instant now = Instant.now();

        // Validate inputs
        ValidationService.ValidationResult didValidation = validationService.validateDid(holderDid);
        if (!didValidation.isValid()) {
            throw new ValidationException(didValidation.getErrorCode(), didValidation.getErrorMessage());
        }

        ValidationService.ValidationResult timestampValidation = validationService.validateTimestamp(now);
        if (!timestampValidation.isValid()) {
            throw new ValidationException(timestampValidation.getErrorCode(), timestampValidation.getErrorMessage());
        }

        // Validate passport data
        if (passportData == null || passportData.isEmpty()) {
            throw new ValidationException("PASSPORT_DATA_REQUIRED", "Passport data is required");
        }

        // Validate liveness proof if provided
        if (livenessProof != null) {
            LivenessValidationService.ValidationResult validation = livenessValidationService.validateLivenessProof(livenessProof);
            if (!validation.isValid()) {
                throw new ValidationException("LIVENESS_VALIDATION_FAILED", 
                    "Liveness validation failed: " + validation.getMessage());
            }
            logger.info("Liveness proof validated successfully with score: {}", livenessProof.getScore());
        } else {
            logger.warn("Issuing credential without liveness proof - consider enabling liveness checks");
        }

        UserEntity user = userRepository.findByDid(holderDid).orElseGet(() -> {
            UserEntity u = new UserEntity();
            u.setId(UUID.randomUUID());
            u.setDid(holderDid);
            u.setCreatedAt(now);
            return userRepository.save(u);
        });

        // Create credential (simplified for MVP)
        CredentialEntity cred = new CredentialEntity();
        cred.setId(UUID.randomUUID());
        cred.setUser(user);
        cred.setCredentialJwt("mock_jwt_" + UUID.randomUUID());
        cred.setStatus("ISSUED");
        cred.setIssuedAt(now);

        credentialRepository.save(cred);

        // Initialize credential status
        revocationService.initializeCredentialStatus(cred);

        // Log credential issuance
        auditService.logCredentialIssued(user.getId().toString(), cred.getId().toString(), 
                                       "PassportCredential", "finpass-issuer");

        com.finpass.issuer.dto.IssueResponse response = new com.finpass.issuer.dto.IssueResponse();
        response.setCredentialJwt(cred.getCredentialJwt());
        response.setCredId(cred.getId());

        return response;
    }
}
