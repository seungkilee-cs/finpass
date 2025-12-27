package com.finpass.verifier.controller;

import com.finpass.verifier.dto.*;
import com.finpass.verifier.service.OpenID4VPService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * OpenID4VP Controller
 * Implements OpenID for Verifiable Credentials Presentation endpoints
 */
@RestController
@RequestMapping
public class OpenID4VPController {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenID4VPController.class);
    
    @Autowired
    private OpenID4VPService openID4VPService;
    
    /**
     * Well-known verifier metadata endpoint
     * GET /.well-known/openid-verifier
     */
    @GetMapping("/.well-known/openid-verifier")
    public ResponseEntity<VerifierMetadata> getVerifierMetadata(
            HttpServletRequest request) {
        try {
            logger.info("Returning verifier metadata");
            
            VerifierMetadata metadata = openID4VPService.generateVerifierMetadata();
            
            return ResponseEntity.ok(metadata);
            
        } catch (Exception e) {
            logger.error("Error generating verifier metadata", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Authorization endpoint for presentation requests
     * GET /authorize
     */
    @GetMapping("/authorize")
    public ResponseEntity<OpenID4VPService.AuthorizationResponse> authorize(
            @RequestParam(required = false) String responseType,
            @RequestParam(required = false) String clientId,
            @RequestParam(required = false) String redirectUri,
            @RequestParam(required = false) String scope,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String nonce,
            @RequestParam(required = false) String responseMode,
            @RequestParam(required = false) String responseUri) {
        try {
            logger.info("Processing authorization request for client: {}", clientId);
            
            // Build authorization request
            AuthorizationRequest authRequest = new AuthorizationRequest();
            authRequest.setResponseType(responseType);
            authRequest.setClientId(clientId);
            authRequest.setRedirectUri(redirectUri);
            authRequest.setScope(scope);
            authRequest.setState(state);
            authRequest.setNonce(nonce);
            authRequest.setResponseMode(responseMode);
            authRequest.setResponseUri(responseUri);
            
            OpenID4VPService.AuthorizationResponse response = openID4VPService.processAuthorizationRequest(authRequest);
            
            if (response.isError()) {
                logger.warn("Authorization request failed: {} - {}", response.getError(), response.getErrorDescription());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error processing authorization request", e);
            
            OpenID4VPService.AuthorizationResponse errorResponse = new OpenID4VPService.AuthorizationResponse();
            errorResponse.setError("server_error");
            errorResponse.setErrorDescription("Internal server error");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Presentation definition endpoint
     * GET /presentation-definition
     */
    @GetMapping("/presentation-definition")
    public ResponseEntity<PresentationDefinition> getPresentationDefinition() {
        try {
            logger.info("Returning presentation definition");
            
            PresentationDefinition definition = openID4VPService.generatePassportPresentationDefinition();
            
            return ResponseEntity.ok(definition);
            
        } catch (Exception e) {
            logger.error("Error generating presentation definition", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Response endpoint for presentation submission
     * POST /callback
     */
    @PostMapping("/callback")
    public ResponseEntity<PresentationResponse> callback(@RequestBody PresentationResponse presentationResponse) {
        try {
            logger.info("Processing presentation submission");
            
            OpenID4VPService.VerificationResult result = openID4VPService.processPresentationSubmission(presentationResponse);
            
            if (result.isError()) {
                logger.warn("Presentation verification failed: {} - {}", result.getError(), result.getErrorDescription());
                
                PresentationResponse errorResponse = PresentationResponse.error(
                    result.getError(), 
                    result.getErrorDescription()
                );
                
                // Determine appropriate HTTP status based on error
                HttpStatus status = HttpStatus.BAD_REQUEST;
                if ("invalid_presentation".equals(result.getError()) || "invalid_signature".equals(result.getError())) {
                    status = HttpStatus.UNAUTHORIZED;
                } else if ("verification_failed".equals(result.getError())) {
                    status = HttpStatus.FORBIDDEN;
                }
                
                return ResponseEntity.status(status).body(errorResponse);
            }
            
            // Build success response
            PresentationResponse response = new PresentationResponse();
            response.setIdToken(result.getDecisionToken());
            response.setState(presentationResponse.getState());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error processing presentation submission", e);
            
            PresentationResponse errorResponse = PresentationResponse.error(
                "server_error", 
                "Internal server error"
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Health check endpoint for OpenID4VP service
     * GET /openid4vp/health
     */
    @GetMapping("/openid4vp/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("OpenID4VP service is healthy");
    }
}
