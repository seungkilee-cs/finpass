package com.finpass.issuer.controller;

import com.finpass.issuer.dto.IssueRequest;
import com.finpass.issuer.dto.IssueResponse;
import com.finpass.issuer.dto.IssueWithProofRequest;
import com.finpass.issuer.repository.CredentialRepository;
import com.finpass.issuer.service.IssuerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for credential issuance with liveness validation
 */
@RestController
@RequestMapping
public class IssuerController {

    private final IssuerService issuerService;
    private final CredentialRepository credentialRepository;
    private final String issuerDid;

    public IssuerController(
            IssuerService issuerService,
            CredentialRepository credentialRepository,
            @Value("${issuer.did}") String issuerDid
    ) {
        this.issuerService = issuerService;
        this.credentialRepository = credentialRepository;
        this.issuerDid = issuerDid;
    }

    @GetMapping("/.well-known/openid-credential-issuer")
    public Map<String, Object> wellKnown() {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("issuer_did", issuerDid);
        resp.put("credential_endpoint", "http://localhost:8080/issue");
        resp.put("credential_endpoint_with_proof", "http://localhost:8080/issue-with-proof");
        resp.put("jwks_uri", "http://localhost:8080/jwks.json");
        resp.put("credential_configurations_supported", Map.of(
                "PassportCredential", Map.of(
                        "format", "jwt_vc",
                        "scope", "passport_credential",
                        "cryptographic_binding_methods_supported", java.util.List.of("did"),
                        "credential_signing_alg_values_supported", java.util.List.of("EdDSA")
                )
        ));
        return resp;
    }

    @GetMapping("/jwks.json")
    public Map<String, Object> jwks() {
        try {
            // Simplified JWKS response for MVP
            Map<String, Object> jwk = Map.of(
                "kty", "OKP",
                "crv", "Ed25519",
                "x", "mock_public_key",
                "kid", "test-key-id"
            );
            return Map.of("keys", java.util.List.of(jwk));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build JWKS", e);
        }
    }

    @PostMapping("/issue")
    public ResponseEntity<IssueResponse> issue(@Valid @RequestBody IssueRequest request) {
        IssueResponse resp = issuerService.issuePassportCredential(request.getHolderDid(), request.getPassportData());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/issue-with-proof")
    public ResponseEntity<IssueResponse> issueWithProof(@Valid @RequestBody IssueWithProofRequest request) {
        IssueResponse resp = issuerService.issuePassportCredential(
            request.getHolderDid(), 
            request.getPassportData(), 
            request.getLivenessProof()
        );
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/status/{credId}")
    public ResponseEntity<StatusResponse> status(@PathVariable("credId") UUID credId) {
        return credentialRepository.findById(credId)
                .map(c -> {
                    StatusResponse resp = new StatusResponse();
                    resp.setCredId(c.getId());
                    resp.setStatus(c.getStatus());
                    resp.setIssuedAt(c.getIssuedAt());
                    return ResponseEntity.ok(resp);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Status response DTO
     */
    public static class StatusResponse {
        private UUID credId;
        private String status;
        private java.time.Instant issuedAt;

        public UUID getCredId() { return credId; }
        public void setCredId(UUID credId) { this.credId = credId; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public java.time.Instant getIssuedAt() { return issuedAt; }
        public void setIssuedAt(java.time.Instant issuedAt) { this.issuedAt = issuedAt; }
    }
}
