package com.finpass.issuer.controller;

import com.finpass.issuer.dto.*;
import com.finpass.issuer.service.OpenID4VCIService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * OpenID4VCI Controller
 * Implements OpenID for Verifiable Credentials Issuance endpoints
 */
@RestController
@RequestMapping
public class OpenID4VCIController {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenID4VCIController.class);
    
    @Autowired
    private OpenID4VCIService openID4VCIService;
    
    /**
     * Well-known credential issuer metadata endpoint
     * GET /.well-known/openid-credential-issuer
     */
    @GetMapping("/.well-known/openid-credential-issuer")
    public ResponseEntity<CredentialIssuerMetadata> getCredentialIssuerMetadata(
            HttpServletRequest request) {
        try {
            logger.info("Returning credential issuer metadata");
            
            CredentialIssuerMetadata metadata = openID4VCIService.generateIssuerMetadata();
            
            return ResponseEntity.ok(metadata);
            
        } catch (Exception e) {
            logger.error("Error generating credential issuer metadata", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Token endpoint for pre-authorized code flow
     * POST /token
     */
    @PostMapping("/token")
    public ResponseEntity<TokenResponse> getToken(@RequestBody TokenRequest tokenRequest) {
        try {
            logger.info("Processing token request with grant type: {}", tokenRequest.getGrantType());
            
            TokenResponse response = openID4VCIService.processTokenRequest(tokenRequest);
            
            if (response.isError()) {
                logger.warn("Token request failed: {} - {}", response.getError(), response.getErrorDescription());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error processing token request", e);
            
            TokenResponse errorResponse = new TokenResponse();
            errorResponse.setError("server_error");
            errorResponse.setErrorDescription("Internal server error");
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Credential endpoint
     * POST /credential
     */
    @PostMapping("/credential")
    public ResponseEntity<CredentialResponse> getCredential(
            @RequestBody CredentialRequest credentialRequest,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            logger.info("Processing credential request for format: {}", credentialRequest.getFormat());
            
            // Extract access token from Authorization header
            String accessToken = null;
            if (authorization != null && authorization.startsWith("Bearer ")) {
                accessToken = authorization.substring(7);
            }
            
            if (accessToken == null) {
                CredentialResponse errorResponse = CredentialResponse.error(
                    "invalid_token", 
                    "Missing or invalid Authorization header"
                );
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }
            
            CredentialResponse response = openID4VCIService.processCredentialRequest(
                credentialRequest, 
                accessToken
            );
            
            if (response.isError()) {
                logger.warn("Credential request failed: {} - {}", response.getError(), response.getErrorDescription());
                
                // Determine appropriate HTTP status based on error
                HttpStatus status = HttpStatus.BAD_REQUEST;
                if ("invalid_token".equals(response.getError())) {
                    status = HttpStatus.UNAUTHORIZED;
                } else if ("invalid_proof".equals(response.getError())) {
                    status = HttpStatus.FORBIDDEN;
                }
                
                return ResponseEntity.status(status).body(response);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error processing credential request", e);
            
            CredentialResponse errorResponse = CredentialResponse.error(
                "server_error", 
                "Internal server error"
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Health check endpoint for OpenID4VCI service
     * GET /openid4vci/health
     */
    @GetMapping("/openid4vci/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("OpenID4VCI service is healthy");
    }
}
