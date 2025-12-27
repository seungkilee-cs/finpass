package com.finpass.issuer.service;

import com.finpass.issuer.dto.*;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

/**
 * Service for OpenID4VCI token and credential management
 * Handles pre-authorized code flow and JWT proof validation
 */
@Service
public class OpenID4VCIService {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenID4VCIService.class);
    
    private final IssuerKeyProvider keyProvider;
    private final IssuerService issuerService;
    private final String issuerDid;
    private final String issuerUrl;
    
    // Token configuration
    private static final long ACCESS_TOKEN_TTL_SECONDS = 3600; // 1 hour
    private static final long C_NONCE_TTL_SECONDS = 300; // 5 minutes
    
    public OpenID4VCIService(
            IssuerKeyProvider keyProvider,
            IssuerService issuerService,
            @Value("${issuer.did}") String issuerDid,
            @Value("${issuer.url:http://localhost:8080}") String issuerUrl
    ) {
        this.keyProvider = keyProvider;
        this.issuerService = issuerService;
        this.issuerDid = issuerDid;
        this.issuerUrl = issuerUrl;
    }
    
    /**
     * Generate credential issuer metadata
     * @return Credential issuer metadata
     */
    public CredentialIssuerMetadata generateIssuerMetadata() {
        logger.info("Generating OpenID4VCI issuer metadata");
        
        // Create display information
        CredentialIssuerMetadata.Display display = new CredentialIssuerMetadata.Display(
            "FinPass Passport Issuer", 
            "en"
        );
        
        // Create supported credential types
        CredentialIssuerMetadata.CredentialSupported passportCredential = 
            new CredentialIssuerMetadata.CredentialSupported(
                "jwt_vc", 
                java.util.List.of("VerifiableCredential", "PassportCredential")
            );
        passportCredential.setCryptographicBindingMethodsSupported(
            java.util.List.of("did:jwk")
        );
        passportCredential.setCryptographicSuitesSupported(
            java.util.List.of("Ed25519VerificationKey2018")
        );
        
        CredentialIssuerMetadata.Display credentialDisplay = 
            new CredentialIssuerMetadata.Display(
                "Passport Credential",
                "en"
            );
        passportCredential.setDisplay(java.util.List.of(credentialDisplay));
        
        // Build metadata
        CredentialIssuerMetadata metadata = new CredentialIssuerMetadata();
        metadata.setCredentialIssuer(issuerUrl);
        metadata.setCredentialEndpoint(issuerUrl + "/credential");
        metadata.setTokenEndpoint(issuerUrl + "/token");
        metadata.setDisplay(java.util.List.of(display));
        metadata.setCredentialsSupported(java.util.List.of(passportCredential));
        
        logger.info("Generated issuer metadata for: {}", issuerUrl);
        return metadata;
    }
    
    /**
     * Process token request for pre-authorized code flow
     * @param tokenRequest Token request
     * @return Token response
     */
    public TokenResponse processTokenRequest(TokenRequest tokenRequest) {
        try {
            logger.info("Processing token request with grant type: {}", tokenRequest.getGrantType());
            
            // Validate grant type
            if (!"urn:ietf:params:oauth:grant-type:pre-authorized_code".equals(tokenRequest.getGrantType())) {
                return createErrorResponse("unsupported_grant_type", "Unsupported grant type");
            }
            
            // Validate pre-authorized code (for MVP, accept any non-empty code)
            String preAuthCode = tokenRequest.getPreAuthorizedCode();
            if (preAuthCode == null || preAuthCode.trim().isEmpty()) {
                return createErrorResponse("invalid_grant", "Invalid pre-authorized code");
            }
            
            // Generate access token
            String accessToken = generateAccessToken(preAuthCode);
            
            // Generate c_nonce
            String cNonce = generateCNonce();
            
            // Build response
            TokenResponse response = new TokenResponse();
            response.setAccessToken(accessToken);
            response.setTokenType("Bearer");
            response.setExpiresIn(ACCESS_TOKEN_TTL_SECONDS);
            response.setCNonce(cNonce);
            response.setCNonceExpiresIn(C_NONCE_TTL_SECONDS);
            
            logger.info("Token request processed successfully");
            return response;
            
        } catch (Exception e) {
            logger.error("Error processing token request", e);
            return createErrorResponse("server_error", "Internal server error");
        }
    }
    
    /**
     * Process credential request
     * @param credentialRequest Credential request
     * @param accessToken Access token for authorization
     * @return Credential response
     */
    public CredentialResponse processCredentialRequest(CredentialRequest credentialRequest, String accessToken) {
        try {
            logger.info("Processing credential request for format: {}", credentialRequest.getFormat());
            
            // Validate access token (basic validation for MVP)
            if (!validateAccessToken(accessToken)) {
                return CredentialResponse.error("invalid_token", "Invalid or expired access token");
            }
            
            // Validate proof
            if (!validateProof(credentialRequest.getProof())) {
                return CredentialResponse.error("invalid_proof", "Invalid proof");
            }
            
            // Extract subject from proof
            String subjectDid = extractSubjectFromProof(credentialRequest.getProof());
            if (subjectDid == null) {
                return CredentialResponse.error("invalid_subject", "Cannot extract subject from proof");
            }
            
            // Generate credential based on request
            String credentialJwt = generateCredential(subjectDid, credentialRequest);
            
            // Generate new c_nonce for next request
            String cNonce = generateCNonce();
            
            // Build response
            CredentialResponse response = new CredentialResponse();
            response.setFormat(credentialRequest.getFormat());
            response.setCredential(credentialJwt);
            response.setCNonce(cNonce);
            response.setCNonceExpiresIn(C_NONCE_TTL_SECONDS);
            
            logger.info("Credential request processed successfully for subject: {}", subjectDid);
            return response;
            
        } catch (Exception e) {
            logger.error("Error processing credential request", e);
            return CredentialResponse.error("server_error", "Internal server error");
        }
    }
    
    /**
     * Generate JWT access token
     * @param preAuthCode Pre-authorized code
     * @return JWT access token
     */
    String generateAccessToken(String preAuthCode) {
        try {
            Instant now = Instant.now();
            Instant exp = now.plus(ACCESS_TOKEN_TTL_SECONDS, ChronoUnit.SECONDS);
            
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuerDid)
                .audience(issuerUrl)
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(exp))
                .claim("pre_authorized_code", preAuthCode)
                .claim("scope", "credential_request")
                .build();
            
            JWSHeader header = new JWSHeader.Builder(keyProvider.getAlgorithm())
                .type(com.nimbusds.jose.JOSEObjectType.JWT)
                .keyID(keyProvider.getKeyId())
                .build();
            
            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(keyProvider.signer());
            
            return jwt.serialize();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate access token", e);
        }
    }
    
    /**
     * Generate c_nonce for proof validation
     * @return Random nonce
     */
    String generateCNonce() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * Validate access token (basic validation)
     * @param accessToken Access token
     * @return True if valid
     */
    boolean validateAccessToken(String accessToken) {
        try {
            if (accessToken == null || accessToken.trim().isEmpty()) {
                return false;
            }
            
            // Parse and verify JWT
            SignedJWT jwt = SignedJWT.parse(accessToken);
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            
            // Check expiration
            if (claims.getExpirationTime().before(new Date())) {
                return false;
            }
            
            // Verify signature (for MVP, we'll skip full verification)
            // In production, you'd verify against the issuer's public key
            return true;
            
        } catch (Exception e) {
            logger.warn("Access token validation failed", e);
            return false;
        }
    }
    
    /**
     * Validate proof in credential request
     * @param proof Proof object
     * @return True if valid
     */
    boolean validateProof(CredentialRequest.Proof proof) {
        try {
            if (proof == null || proof.getJwt() == null) {
                return false;
            }
            
            // Parse proof JWT
            SignedJWT proofJwt = SignedJWT.parse(proof.getJwt());
            JWTClaimsSet claims = proofJwt.getJWTClaimsSet();
            
            // Check required claims
            if (claims.getSubject() == null || claims.getIssuer() == null) {
                return false;
            }
            
            // Verify signature (for MVP, we'll skip full verification)
            // In production, you'd verify against the subject's DID document
            // For now, just check basic structure
            return true;
            
        } catch (Exception e) {
            logger.warn("Proof validation failed", e);
            return false;
        }
    }
    
    /**
     * Extract subject DID from proof
     * @param proof Proof object
     * @return Subject DID
     */
    String extractSubjectFromProof(CredentialRequest.Proof proof) {
        try {
            SignedJWT proofJwt = SignedJWT.parse(proof.getJwt());
            return proofJwt.getJWTClaimsSet().getSubject();
        } catch (Exception e) {
            logger.warn("Failed to extract subject from proof", e);
            return null;
        }
    }
    
    /**
     * Generate credential based on request
     * @param subjectDid Subject DID
     * @param request Credential request
     * @return Credential JWT
     */
    private String generateCredential(String subjectDid, CredentialRequest request) {
        try {
            // For MVP, generate a mock passport credential
            // In production, you'd use actual passport data
            java.util.Map<String, Object> passportData = java.util.Map.of(
                "name", "John Doe",
                "nationality", "US",
                "birthDate", "1990-01-01",
                "passportNumber", "123456789"
            );
            
            return issuerService.issuePassportCredential(subjectDid, passportData).getCredentialJwt();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate credential", e);
        }
    }
    
    /**
     * Create error token response
     * @param error Error code
     * @param errorDescription Error description
     * @return Error token response
     */
    private TokenResponse createErrorResponse(String error, String errorDescription) {
        TokenResponse response = new TokenResponse();
        response.setError(error);
        response.setErrorDescription(errorDescription);
        return response;
    }
}
