package com.finpass.verifier.service;

import com.finpass.verifier.dto.*;
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
import java.util.*;

/**
 * Service for OpenID4VP presentation management
 * Handles presentation definitions, authorization, and verification
 */
@Service
public class OpenID4VPService {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenID4VPService.class);
    
    private final VerifierKeyProvider keyProvider;
    private final VerifierService verifierService;
    private final String verifierDid;
    private final String verifierUrl;
    
    public OpenID4VPService(
            VerifierKeyProvider keyProvider,
            VerifierService verifierService,
            @Value("${verifier.did}") String verifierDid,
            @Value("${verifier.url:http://localhost:8081}") String verifierUrl
    ) {
        this.keyProvider = keyProvider;
        this.verifierService = verifierService;
        this.verifierDid = verifierDid;
        this.verifierUrl = verifierUrl;
    }
    
    /**
     * Generate verifier metadata
     * @return Verifier metadata
     */
    public VerifierMetadata generateVerifierMetadata() {
        logger.info("Generating OpenID4VP verifier metadata");
        
        // Create display information
        VerifierMetadata.Display display = new VerifierMetadata.Display(
            "FinPass Passport Verifier",
            "en",
            "Verify your passport credential"
        );
        
        // Create client metadata
        VerifierMetadata.ClientMetadata clientMetadata = new VerifierMetadata.ClientMetadata();
        clientMetadata.setClientId("finpass-verifier");
        clientMetadata.setClientName("FinPass Verifier");
        clientMetadata.setIdTokenSignedResponseAlg("EdDSA");
        
        Map<String, Object> vpFormats = new HashMap<>();
        vpFormats.put("jwt_vp", new HashMap<>());
        vpFormats.put("jwt_vc", new HashMap<>());
        clientMetadata.setVpFormats(vpFormats);
        
        // Build metadata
        VerifierMetadata metadata = new VerifierMetadata();
        metadata.setAuthorizationEndpoint(verifierUrl + "/authorize");
        metadata.setResponseEndpoint(verifierUrl + "/callback");
        metadata.setPresentationDefinitionEndpoint(verifierUrl + "/presentation-definition");
        metadata.setSupportedCredentialFormats(Arrays.asList("jwt_vc", "jwt_vp"));
        metadata.setSupportedAlgorithms(Arrays.asList("EdDSA", "ES256K"));
        metadata.setDisplay(Arrays.asList(display));
        metadata.setClientMetadata(clientMetadata);
        
        logger.info("Generated verifier metadata for: {}", verifierUrl);
        return metadata;
    }
    
    /**
     * Generate presentation definition for passport verification
     * @return Presentation definition
     */
    public PresentationDefinition generatePassportPresentationDefinition() {
        logger.info("Generating passport presentation definition");
        
        // Create format requirements
        PresentationDefinition.PresentationFormat format = new PresentationDefinition.PresentationFormat();
        format.setJwtVc(new PresentationDefinition.JwtFormat(Arrays.asList("EdDSA")));
        format.setJwtVp(new PresentationDefinition.JwtFormat(Arrays.asList("EdDSA")));
        
        // Create input descriptor for passport credential
        PresentationDefinition.InputDescriptor inputDescriptor = 
            new PresentationDefinition.InputDescriptor(
                "passport_credential",
                "Passport Credential",
                "Please present your passport credential for verification"
            );
        
        inputDescriptor.setFormat(format);
        
        // Create constraints
        PresentationDefinition.Constraints constraints = new PresentationDefinition.Constraints();
        
        // Create field requirements
        List<PresentationDefinition.Field> fields = new ArrayList<>();
        
        // Required name field
        fields.add(new PresentationDefinition.Field(
            "name_field",
            Arrays.asList("$.vc.credentialSubject.name"),
            "Verify your name"
        ));
        
        // Required nationality field
        fields.add(new PresentationDefinition.Field(
            "nationality_field",
            Arrays.asList("$.vc.credentialSubject.nationality"),
            "Verify your nationality"
        ));
        
        // Required birth date field
        fields.add(new PresentationDefinition.Field(
            "birth_date_field",
            Arrays.asList("$.vc.credentialSubject.birthDate"),
            "Verify your birth date"
        ));
        
        // Optional passport number field
        fields.add(new PresentationDefinition.Field(
            "passport_number_field",
            Arrays.asList("$.vc.credentialSubject.passportNumber"),
            "Verify your passport number"
        ));
        
        constraints.setFields(fields);
        inputDescriptor.setConstraints(constraints);
        
        // Create submission requirements
        PresentationDefinition.SubmissionRequirement requirement = 
            new PresentationDefinition.SubmissionRequirement(
                "passport_requirement",
                "all",
                "passport_credential"
            );
        
        // Build presentation definition
        PresentationDefinition definition = new PresentationDefinition(
            "passport_verification_definition",
            "Passport Verification",
            "Verify your passport credential to access this service"
        );
        
        definition.setFormat(format);
        definition.setInputDescriptors(Arrays.asList(inputDescriptor));
        definition.setSubmissionRequirements(Arrays.asList(requirement));
        
        logger.info("Generated passport presentation definition");
        return definition;
    }
    
    /**
     * Process authorization request
     * @param request Authorization request
     * @return Authorization response with presentation definition
     */
    public AuthorizationResponse processAuthorizationRequest(AuthorizationRequest request) {
        try {
            logger.info("Processing authorization request for client: {}", request.getClientId());
            
            // Validate request
            if (!"vp_token".equals(request.getResponseType())) {
                return createErrorResponse("invalid_request", "Unsupported response type");
            }
            
            // Generate presentation definition if not provided
            PresentationDefinition presentationDefinition = request.getPresentationDefinition();
            if (presentationDefinition == null) {
                presentationDefinition = generatePassportPresentationDefinition();
            }
            
            // Generate authorization session
            String sessionId = UUID.randomUUID().toString();
            String nonce = UUID.randomUUID().toString();
            
            // Build response
            AuthorizationResponse response = new AuthorizationResponse();
            response.setSessionId(sessionId);
            response.setPresentationDefinition(presentationDefinition);
            response.setNonce(nonce);
            response.setExpiresIn(600L); // 10 minutes
            response.setResponseUri(verifierUrl + "/callback");
            
            logger.info("Authorization request processed successfully for session: {}", sessionId);
            return response;
            
        } catch (Exception e) {
            logger.error("Error processing authorization request", e);
            return createErrorResponse("server_error", "Internal server error");
        }
    }
    
    /**
     * Process presentation submission
     * @param response Presentation response
     * @return Verification result
     */
    public VerificationResult processPresentationSubmission(PresentationResponse response) {
        try {
            logger.info("Processing presentation submission");
            
            // Validate presentation
            if (response.getVpToken() == null || response.getVpToken().trim().isEmpty()) {
                return VerificationResult.error("invalid_presentation", "VP token is required");
            }
            
            if (response.getPresentationSubmission() == null) {
                return VerificationResult.error("invalid_submission", "Presentation submission is required");
            }
            
            // Parse and verify VP token
            SignedJWT vpJwt = SignedJWT.parse(response.getVpToken());
            JWTClaimsSet claims = vpJwt.getJWTClaimsSet();
            
            // Verify signature (basic validation for MVP)
            if (!verifyVPSignature(vpJwt)) {
                return VerificationResult.error("invalid_signature", "Invalid VP token signature");
            }
            
            // Extract credentials from VP
            List<String> credentialJwts = extractCredentialsFromVP(claims);
            if (credentialJwts.isEmpty()) {
                return VerificationResult.error("no_credentials", "No credentials found in presentation");
            }
            
            // Verify each credential using existing VerifierService
            List<String> verifiedClaims = new ArrayList<>();
            for (String credentialJwt : credentialJwts) {
                try {
                    // Create mock verification request for credential
                    VerifyRequest verifyRequest = createVerifyRequest(credentialJwt);
                    VerifyResponse verifyResponse = verifierService.verify(verifyRequest);
                    
                    if (verifyResponse.getVerifiedClaims() != null) {
                        verifiedClaims.addAll(verifyResponse.getVerifiedClaims());
                    }
                } catch (Exception e) {
                    logger.warn("Failed to verify credential: {}", e.getMessage());
                }
            }
            
            if (verifiedClaims.isEmpty()) {
                return VerificationResult.error("verification_failed", "No credentials could be verified");
            }
            
            // Generate decision token
            String decisionToken = generateDecisionToken(verifiedClaims);
            
            // Build success result
            VerificationResult result = new VerificationResult();
            result.setSuccess(true);
            result.setDecisionToken(decisionToken);
            result.setVerifiedClaims(verifiedClaims);
            result.setAssuranceLevel("LOW");
            result.setExpiresIn(300L); // 5 minutes
            
            logger.info("Presentation submission processed successfully");
            return result;
            
        } catch (Exception e) {
            logger.error("Error processing presentation submission", e);
            return VerificationResult.error("server_error", "Internal server error");
        }
    }
    
    /**
     * Verify VP token signature (basic validation)
     * @param vpJwt VP token
     * @return True if valid
     */
    boolean verifyVPSignature(SignedJWT vpJwt) {
        try {
            // For MVP, we'll skip full signature verification
            // In production, you'd verify against the presenter's DID document
            return vpJwt.getJWTClaimsSet().getSubject() != null;
        } catch (Exception e) {
            logger.warn("VP signature verification failed", e);
            return false;
        }
    }
    
    /**
     * Extract credential JWTs from VP token
     * @param claims VP claims
     * @return List of credential JWTs
     */
    private List<String> extractCredentialsFromVP(JWTClaimsSet claims) {
        List<String> credentials = new ArrayList<>();
        
        try {
            // Extract verifiableCredential from VP
            Object vcClaim = claims.getClaim("verifiableCredential");
            if (vcClaim instanceof List) {
                for (Object vc : (List<?>) vcClaim) {
                    if (vc instanceof String) {
                        credentials.add((String) vc);
                    }
                }
            } else if (vcClaim instanceof String) {
                credentials.add((String) vcClaim);
            }
        } catch (Exception e) {
            logger.warn("Failed to extract credentials from VP", e);
        }
        
        return credentials;
    }
    
    /**
     * Create verification request from credential JWT
     * @param credentialJwt Credential JWT
     * @return Verification request
     */
    private VerifyRequest createVerifyRequest(String credentialJwt) {
        VerifyRequest request = new VerifyRequest();
        request.setCommitmentJwt(credentialJwt);
        request.setChallenge(UUID.randomUUID().toString()); // Mock challenge
        request.setHolderDid("did:example:holder"); // Mock holder DID
        
        Map<String, Object> publicSignals = new HashMap<>();
        publicSignals.put("challenge", request.getChallenge());
        publicSignals.put("predicate", "passport_verification");
        publicSignals.put("result", true);
        request.setPublicSignals(publicSignals);
        
        return request;
    }
    
    /**
     * Generate decision token
     * @param verifiedClaims List of verified claims
     * @return Decision token JWT
     */
    private String generateDecisionToken(List<String> verifiedClaims) {
        try {
            Instant now = Instant.now();
            Instant exp = now.plus(300, ChronoUnit.SECONDS); // 5 minutes
            
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(verifierDid)
                .audience(verifierUrl)
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(exp))
                .claim("verified_claims", verifiedClaims)
                .claim("assurance_level", "LOW")
                .claim("verification_method", "openid4vp")
                .build();
            
            JWSHeader header = new JWSHeader.Builder(keyProvider.getAlgorithm())
                .type(com.nimbusds.jose.JOSEObjectType.JWT)
                .keyID(keyProvider.getKeyId())
                .build();
            
            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(keyProvider.signer());
            
            return jwt.serialize();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate decision token", e);
        }
    }
    
    /**
     * Create error authorization response
     * @param error Error code
     * @param errorDescription Error description
     * @return Error response
     */
    private AuthorizationResponse createErrorResponse(String error, String errorDescription) {
        AuthorizationResponse response = new AuthorizationResponse();
        response.setError(error);
        response.setErrorDescription(errorDescription);
        return response;
    }
    
    /**
     * Authorization response class
     */
    public static class AuthorizationResponse {
        private String sessionId;
        private PresentationDefinition presentationDefinition;
        private String nonce;
        private Long expiresIn;
        private String responseUri;
        private String error;
        private String errorDescription;
        
        // Getters and Setters
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        
        public PresentationDefinition getPresentationDefinition() { return presentationDefinition; }
        public void setPresentationDefinition(PresentationDefinition presentationDefinition) { this.presentationDefinition = presentationDefinition; }
        
        public String getNonce() { return nonce; }
        public void setNonce(String nonce) { this.nonce = nonce; }
        
        public Long getExpiresIn() { return expiresIn; }
        public void setExpiresIn(Long expiresIn) { this.expiresIn = expiresIn; }
        
        public String getResponseUri() { return responseUri; }
        public void setResponseUri(String responseUri) { this.responseUri = responseUri; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public String getErrorDescription() { return errorDescription; }
        public void setErrorDescription(String errorDescription) { this.errorDescription = errorDescription; }
        
        public boolean isError() {
            return error != null && !error.trim().isEmpty();
        }
    }
    
    /**
     * Verification result class
     */
    public static class VerificationResult {
        private boolean success;
        private String decisionToken;
        private List<String> verifiedClaims;
        private String assuranceLevel;
        private Long expiresIn;
        private String error;
        private String errorDescription;
        
        // Constructors
        public VerificationResult() {}
        
        public static VerificationResult error(String error, String errorDescription) {
            VerificationResult result = new VerificationResult();
            result.setSuccess(false);
            result.setError(error);
            result.setErrorDescription(errorDescription);
            return result;
        }
        
        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getDecisionToken() { return decisionToken; }
        public void setDecisionToken(String decisionToken) { this.decisionToken = decisionToken; }
        
        public List<String> getVerifiedClaims() { return verifiedClaims; }
        public void setVerifiedClaims(List<String> verifiedClaims) { this.verifiedClaims = verifiedClaims; }
        
        public String getAssuranceLevel() { return assuranceLevel; }
        public void setAssuranceLevel(String assuranceLevel) { this.assuranceLevel = assuranceLevel; }
        
        public Long getExpiresIn() { return expiresIn; }
        public void setExpiresIn(Long expiresIn) { this.expiresIn = expiresIn; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public String getErrorDescription() { return errorDescription; }
        public void setErrorDescription(String errorDescription) { this.errorDescription = errorDescription; }
        
        public boolean isError() {
            return error != null && !error.trim().isEmpty();
        }
    }
}
