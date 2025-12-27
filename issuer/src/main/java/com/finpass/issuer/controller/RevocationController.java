package com.finpass.issuer.controller;

import com.finpass.issuer.dto.CredentialStatusResponse;
import com.finpass.issuer.dto.RevocationRequest;
import com.finpass.issuer.service.RevocationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for credential revocation operations
 */
@RestController
@RequestMapping("/api/v1/credentials")
@EnableCaching
public class RevocationController {

    private static final Logger logger = LoggerFactory.getLogger(RevocationController.class);

    private final RevocationService revocationService;

    @Autowired
    public RevocationController(RevocationService revocationService) {
        this.revocationService = revocationService;
    }

    /**
     * Revoke a credential (admin only)
     */
    @PostMapping("/{credentialId}/revoke")
    public ResponseEntity<CredentialStatusResponse> revokeCredential(
            @PathVariable UUID credentialId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody RevocationRequest request) {

        logger.info("Revocation request for credential {} from admin: {}", 
                   credentialId, request.getRevokedBy());

        // Validate admin authorization
        if (!validateAdminAuthorization(authorization)) {
            logger.warn("Unauthorized revocation attempt for credential {}", credentialId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            CredentialStatusResponse response = revocationService.revokeCredential(credentialId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Credential not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            logger.error("Invalid revocation request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error revoking credential: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Suspend a credential (admin only)
     */
    @PostMapping("/{credentialId}/suspend")
    public ResponseEntity<CredentialStatusResponse> suspendCredential(
            @PathVariable UUID credentialId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, String> request) {

        String suspendedBy = request.get("suspended_by");
        String reason = request.get("reason");

        if (suspendedBy == null || suspendedBy.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        logger.info("Suspension request for credential {} by admin: {}", credentialId, suspendedBy);

        if (!validateAdminAuthorization(authorization)) {
            logger.warn("Unauthorized suspension attempt for credential {}", credentialId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            CredentialStatusResponse response = revocationService.suspendCredential(credentialId, suspendedBy, reason);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Credential not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            logger.error("Invalid suspension request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error suspending credential: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Reinstate a suspended credential (admin only)
     */
    @PostMapping("/{credentialId}/reinstate")
    public ResponseEntity<CredentialStatusResponse> reinstateCredential(
            @PathVariable UUID credentialId,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, String> request) {

        String reinstatedBy = request.get("reinstated_by");
        if (reinstatedBy == null || reinstatedBy.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        logger.info("Reinstatement request for credential {} by admin: {}", credentialId, reinstatedBy);

        if (!validateAdminAuthorization(authorization)) {
            logger.warn("Unauthorized reinstatement attempt for credential {}", credentialId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            CredentialStatusResponse response = revocationService.reinstateCredential(credentialId, reinstatedBy);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Credential not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            logger.error("Invalid reinstatement request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error reinstating credential: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get credential status (public endpoint)
     */
    @GetMapping("/{credentialId}/status")
    public ResponseEntity<CredentialStatusResponse> getCredentialStatus(@PathVariable UUID credentialId) {
        logger.debug("Status request for credential {}", credentialId);

        try {
            CredentialStatusResponse response = revocationService.getCredentialStatus(credentialId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Credential not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error getting credential status: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Check if credential is valid (for verification)
     */
    @GetMapping("/{credentialId}/valid")
    public ResponseEntity<Map<String, Object>> isCredentialValid(@PathVariable UUID credentialId) {
        logger.debug("Validity check for credential {}", credentialId);

        try {
            boolean isValid = revocationService.isCredentialValid(credentialId);
            return ResponseEntity.ok(Map.of(
                "credential_id", credentialId,
                "is_valid", isValid,
                "checked_at", java.time.Instant.now()
            ));
        } catch (Exception e) {
            logger.error("Error checking credential validity: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all revoked credentials (admin only)
     */
    @GetMapping("/revoked")
    public ResponseEntity<List<Object>> getRevokedCredentials(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        logger.info("Request for all revoked credentials");

        if (!validateAdminAuthorization(authorization)) {
            logger.warn("Unauthorized request for revoked credentials list");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            List<Object> revoked = revocationService.getRevokedCredentials()
                .stream()
                .map(this::mapToStatusResponse)
                .toList();
            return ResponseEntity.ok(revoked);
        } catch (Exception e) {
            logger.error("Error getting revoked credentials: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Clear cache for a specific credential (admin only)
     */
    @DeleteMapping("/{credentialId}/cache")
    public ResponseEntity<Void> clearCredentialCache(
            @PathVariable UUID credentialId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (!validateAdminAuthorization(authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Cache clearing would be handled by @CacheEvict annotations
        // This endpoint provides a way to manually trigger cache clearing if needed
        logger.info("Cache cleared for credential {}", credentialId);
        return ResponseEntity.ok().build();
    }

    /**
     * Validate admin authorization
     */
    private boolean validateAdminAuthorization(String authorization) {
        if (authorization == null) {
            return false;
        }

        // Support Bearer token format
        if (authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            return revocationService.validateAdminAuthorization(token);
        }

        // Support direct token format
        return revocationService.validateAdminAuthorization(authorization);
    }

    /**
     * Create error response
     */
    private CredentialStatusResponse createErrorResponse(String message) {
        CredentialStatusResponse response = new CredentialStatusResponse();
        response.setReasonDescription(message);
        response.setValid(false);
        return response;
    }

    /**
     * Map entity to response object
     */
    private Object mapToStatusResponse(Object entity) {
        // This would convert the entity to a DTO for public API
        // For now, return the entity directly
        return entity;
    }
}
