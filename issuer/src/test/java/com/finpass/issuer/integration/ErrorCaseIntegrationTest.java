package com.finpass.issuer.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for error cases and edge conditions in the FinPass workflow
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class ErrorCaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String testUserDid;
    private String testVerifierDid;
    private String testPayeeDid;
    private String untrustedIssuerDid;
    private String issuedCredentialJwt;
    private String revokedCredentialId;

    @BeforeEach
    void setUp() {
        testUserDid = "did:example:testuser" + System.currentTimeMillis();
        testVerifierDid = "did:example:testverifier" + System.currentTimeMillis();
        testPayeeDid = "did:example:testpayee" + System.currentTimeMillis();
        untrustedIssuerDid = "did:example:untrusted" + System.currentTimeMillis();
    }

    @Test
    @DisplayName("Revoked credential verification should fail")
    void testRevokedCredentialVerificationFails() throws Exception {
        // Step 1: Issue a credential
        issuedCredentialJwt = issueValidCredential();
        
        // Step 2: Revoke the credential
        revokeCredential();
        
        // Step 3: Try to verify the revoked credential - should fail
        Map<String, Object> verificationRequest = new HashMap<>();
        verificationRequest.put("credentialJwt", issuedCredentialJwt);
        verificationRequest.put("verifierDid", testVerifierDid);
        verificationRequest.put("verificationType", "IDENTITY_VERIFICATION");

        mockMvc.perform(post("/api/verifier/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verificationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(false)))
                .andExpect(jsonPath("$.verificationResult.credentialStatus", is("REVOKED")))
                .andExpect(jsonPath("$.verificationResult.verificationScore", lessThan(0.5)));
    }

    @Test
    @DisplayName("Untrusted issuer verification should be rejected")
    void testUntrustedIssuerVerificationRejected() throws Exception {
        // Create a credential with an untrusted issuer (simulate this)
        Map<String, Object> credentialRequest = new HashMap<>();
        credentialRequest.put("holderDid", testUserDid);
        credentialRequest.put("credentialType", "PASSPORT");
        credentialRequest.put("issuerDid", untrustedIssuerDid); // Untrusted issuer
        
        Map<String, Object> credentialData = new HashMap<>();
        credentialData.put("passportNumber", "P123456789");
        credentialData.put("fullName", "John Doe");
        credentialData.put("dateOfBirth", "1990-01-01");
        credentialData.put("nationality", "US");
        
        credentialRequest.put("credentialData", credentialData);
        
        Map<String, Object> livenessProof = new HashMap<>();
        livenessProof.put("score", 0.95);
        livenessProof.put("timestamp", Instant.now().toString());
        livenessProof.put("proofData", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...");
        
        credentialRequest.put("livenessProof", livenessProof);

        MvcResult result = mockMvc.perform(post("/api/issuer/credentials")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(credentialRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        String untrustedCredentialJwt = objectMapper.readTree(response).get("credentialJwt").asText();
        
        // Try to verify credential from untrusted issuer
        Map<String, Object> verificationRequest = new HashMap<>();
        verificationRequest.put("credentialJwt", untrustedCredentialJwt);
        verificationRequest.put("verifierDid", testVerifierDid);
        verificationRequest.put("verificationType", "IDENTITY_VERIFICATION");

        mockMvc.perform(post("/api/verifier/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verificationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(false)))
                .andExpect(jsonPath("$.verificationResult.issuerVerified", is(false)))
                .andExpect(jsonPath("$.verificationResult.verificationScore", lessThan(0.3)));
    }

    @Test
    @DisplayName("Expired decision token should block payment")
    void testExpiredDecisionTokenBlocksPayment() throws Exception {
        // Issue and verify credential
        issuedCredentialJwt = issueValidCredential();
        verifyCredential(issuedCredentialJwt);
        
        // Create payment with expired decision token (simulate)
        Map<String, Object> paymentRequest = new HashMap<>();
        paymentRequest.put("payerDid", testUserDid);
        paymentRequest.put("payeeDid", testPayeeDid);
        paymentRequest.put("amount", new BigDecimal("100.50"));
        paymentRequest.put("currency", "USD");
        paymentRequest.put("paymentMethod", "BANK_TRANSFER");
        paymentRequest.put("decisionToken", "expired_token_123");
        paymentRequest.put("decisionTokenExpiry", Instant.now().minusSeconds(3600).toString()); // Expired 1 hour ago
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("invoiceId", "INV-EXPIRED");
        metadata.put("credentialId", issuedCredentialJwt);
        
        paymentRequest.put("metadata", metadata);

        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("DECISION_TOKEN_EXPIRED")))
                .andExpect(jsonPath("$.error_description", containsString("expired")));
    }

    @Test
    @DisplayName("Invalid signature should cause presentation rejection")
    void testInvalidSignaturePresentationRejection() throws Exception {
        // Create credential with invalid signature (simulate)
        String invalidCredentialJwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.invalid_signature";

        Map<String, Object> verificationRequest = new HashMap<>();
        verificationRequest.put("credentialJwt", invalidCredentialJwt);
        verificationRequest.put("verifierDid", testVerifierDid);
        verificationRequest.put("verificationType", "IDENTITY_VERIFICATION");

        mockMvc.perform(post("/api/verifier/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verificationRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_SIGNATURE")))
                .andExpect(jsonPath("$.error_description", containsString("signature")))
                .andExpect(jsonPath("$.correlation_id", notNullValue()));
    }

    @Test
    @DisplayName("Insufficient balance should cause payment failure")
    void testInsufficientBalancePaymentFailure() throws Exception {
        // Issue and verify credential
        issuedCredentialJwt = issueValidCredential();
        verifyCredential(issuedCredentialJwt);
        
        // Create payment with amount exceeding available balance
        Map<String, Object> paymentRequest = new HashMap<>();
        paymentRequest.put("payerDid", testUserDid);
        paymentRequest.put("payeeDid", testPayeeDid);
        paymentRequest.put("amount", new BigDecimal("999999.99")); // Very large amount
        paymentRequest.put("currency", "USD");
        paymentRequest.put("paymentMethod", "BANK_TRANSFER");
        paymentRequest.put("description", "Large payment that should fail");
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("invoiceId", "INV-INSUFFICIENT");
        metadata.put("credentialId", issuedCredentialJwt);
        
        paymentRequest.put("metadata", metadata);

        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INSUFFICIENT_BALANCE")))
                .andExpect(jsonPath("$.error_description", containsString("balance")))
                .andExpect(jsonPath("$.correlation_id", notNullValue()));
    }

    @Test
    @DisplayName("Expired credential verification should fail")
    void testExpiredCredentialVerificationFails() throws Exception {
        // Create an expired credential (simulate)
        Map<String, Object> credentialRequest = new HashMap<>();
        credentialRequest.put("holderDid", testUserDid);
        credentialRequest.put("credentialType", "PASSPORT");
        
        Map<String, Object> credentialData = new HashMap<>();
        credentialData.put("passportNumber", "P123456789");
        credentialData.put("fullName", "John Doe");
        credentialData.put("dateOfBirth", "1990-01-01");
        credentialData.put("nationality", "US");
        credentialData.put("expirationDate", "2020-01-01"); // Expired date
        
        credentialRequest.put("credentialData", credentialData);
        
        Map<String, Object> livenessProof = new HashMap<>();
        livenessProof.put("score", 0.95);
        livenessProof.put("timestamp", Instant.now().toString());
        livenessProof.put("proofData", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...");
        
        credentialRequest.put("livenessProof", livenessProof);

        MvcResult result = mockMvc.perform(post("/api/issuer/credentials")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(credentialRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        String expiredCredentialJwt = objectMapper.readTree(response).get("credentialJwt").asText();
        
        // Try to verify expired credential
        Map<String, Object> verificationRequest = new HashMap<>();
        verificationRequest.put("credentialJwt", expiredCredentialJwt);
        verificationRequest.put("verifierDid", testVerifierDid);
        verificationRequest.put("verificationType", "IDENTITY_VERIFICATION");

        mockMvc.perform(post("/api/verifier/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verificationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(false)))
                .andExpect(jsonPath("$.verificationResult.credentialStatus", is("EXPIRED")))
                .andExpect(jsonPath("$.verificationResult.verificationScore", lessThan(0.5)));
    }

    @Test
    @DisplayName("Invalid DID format should be rejected")
    void testInvalidDidFormatRejected() throws Exception {
        // Try to issue credential with invalid DID format
        Map<String, Object> credentialRequest = new HashMap<>();
        credentialRequest.put("holderDid", "invalid-did-format"); // Invalid DID
        credentialRequest.put("credentialType", "PASSPORT");
        
        Map<String, Object> credentialData = new HashMap<>();
        credentialData.put("passportNumber", "P123456789");
        credentialData.put("fullName", "John Doe");
        
        credentialRequest.put("credentialData", credentialData);
        
        Map<String, Object> livenessProof = new HashMap<>();
        livenessProof.put("score", 0.95);
        livenessProof.put("timestamp", Instant.now().toString());
        livenessProof.put("proofData", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...");
        
        credentialRequest.put("livenessProof", livenessProof);

        mockMvc.perform(post("/api/issuer/credentials")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(credentialRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_DID_FORMAT")))
                .andExpect(jsonPath("$.error_description", containsString("DID format")))
                .andExpect(jsonPath("$.correlation_id", notNullValue()));
    }

    @Test
    @DisplayName("Missing required fields should cause validation errors")
    void testMissingRequiredFieldsValidationErrors() throws Exception {
        // Try to issue credential with missing required fields
        Map<String, Object> credentialRequest = new HashMap<>();
        credentialRequest.put("holderDid", testUserDid);
        // Missing credentialType
        
        Map<String, Object> credentialData = new HashMap<>();
        credentialData.put("passportNumber", "P123456789");
        
        credentialRequest.put("credentialData", credentialData);

        mockMvc.perform(post("/api/issuer/credentials")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(credentialRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.error_description", containsString("required")))
                .andExpect(jsonPath("$.correlation_id", notNullValue()));
    }

    @Test
    @DisplayName("Concurrent payment attempts should be handled gracefully")
    void testConcurrentPaymentAttempts() throws Exception {
        // Issue and verify credential
        issuedCredentialJwt = issueValidCredential();
        verifyCredential(issuedCredentialJwt);
        
        // Create multiple payment requests with same invoice ID (simulate concurrent attempts)
        Map<String, Object> paymentRequest = new HashMap<>();
        paymentRequest.put("payerDid", testUserDid);
        paymentRequest.put("payeeDid", testPayeeDid);
        paymentRequest.put("amount", new BigDecimal("100.50"));
        paymentRequest.put("currency", "USD");
        paymentRequest.put("paymentMethod", "BANK_TRANSFER");
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("invoiceId", "INV-DUPLICATE"); // Same invoice ID
        metadata.put("credentialId", issuedCredentialJwt);
        
        paymentRequest.put("metadata", metadata);

        // First payment should succeed
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isCreated());

        // Second payment with same invoice should fail
        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("DUPLICATE_PAYMENT")))
                .andExpect(jsonPath("$.error_description", containsString("duplicate")));
    }

    private String issueValidCredential() throws Exception {
        Map<String, Object> credentialRequest = new HashMap<>();
        credentialRequest.put("holderDid", testUserDid);
        credentialRequest.put("credentialType", "PASSPORT");
        
        Map<String, Object> credentialData = new HashMap<>();
        credentialData.put("passportNumber", "P123456789");
        credentialData.put("fullName", "John Doe");
        credentialData.put("dateOfBirth", "1990-01-01");
        credentialData.put("nationality", "US");
        credentialData.put("issuingCountry", "US");
        credentialData.put("expirationDate", "2030-01-01");
        
        credentialRequest.put("credentialData", credentialData);
        
        Map<String, Object> livenessProof = new HashMap<>();
        livenessProof.put("score", 0.95);
        livenessProof.put("timestamp", Instant.now().toString());
        livenessProof.put("proofData", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...");
        
        credentialRequest.put("livenessProof", livenessProof);

        MvcResult result = mockMvc.perform(post("/api/issuer/credentials")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(credentialRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        return objectMapper.readTree(response).get("credentialJwt").asText();
    }

    private void revokeCredential() throws Exception {
        // Extract credential ID from the JWT (simplified for test)
        // In real implementation, you'd decode the JWT to get the credential ID
        revokedCredentialId = "cred_" + System.currentTimeMillis();

        mockMvc.perform(delete("/api/issuer/credentials/" + revokedCredentialId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("revoked")))
                .andExpect(jsonPath("$.revokedAt", notNullValue()));
    }

    private void verifyCredential(String credentialJwt) throws Exception {
        Map<String, Object> verificationRequest = new HashMap<>();
        verificationRequest.put("credentialJwt", credentialJwt);
        verificationRequest.put("verifierDid", testVerifierDid);
        verificationRequest.put("verificationType", "IDENTITY_VERIFICATION");

        mockMvc.perform(post("/api/verifier/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verificationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)));
    }
}
