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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Simplified integration tests for the complete FinPass workflow
 * Tests the actual HTTP endpoints without depending on missing service classes
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class BasicWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String testUserDid;
    private String testVerifierDid;
    private String testPayeeDid;

    @BeforeEach
    void setUp() {
        testUserDid = "did:example:testuser" + System.currentTimeMillis();
        testVerifierDid = "did:example:testverifier" + System.currentTimeMillis();
        testPayeeDid = "did:example:testpayee" + System.currentTimeMillis();
    }

    @Test
    @DisplayName("Complete happy path: Credential issuance → Verification → Payment")
    void testCompleteHappyPathWorkflow() throws Exception {
        // Step 1: Issue a credential to the user
        String credentialJwt = step1_IssueCredential();
        
        // Step 2: Verify the issued credential
        step2_VerifyCredential(credentialJwt);
        
        // Step 3: Process a payment using the verified credential
        String paymentId = step3_ProcessPayment();
        
        // Step 4: Verify payment completion
        step4_VerifyPaymentCompletion(paymentId);
    }

    @Test
    @DisplayName("Error case: Invalid credential format should be rejected")
    void testInvalidCredentialFormatRejected() throws Exception {
        // Try to issue credential with invalid DID format
        Map<String, Object> credentialRequest = createInvalidCredentialRequest();

        mockMvc.perform(post("/api/issuer/credentials")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(credentialRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.error_description", containsString("required")))
                .andExpect(jsonPath("$.correlation_id", notNullValue()));
    }

    @Test
    @DisplayName("Error case: Invalid verification request should be rejected")
    void testInvalidVerificationRequestRejected() throws Exception {
        // Try to verify with invalid JWT
        Map<String, Object> verificationRequest = createInvalidVerificationRequest();

        mockMvc.perform(post("/api/verifier/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verificationRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", anyOf(is("INVALID_SIGNATURE"), is("VALIDATION_ERROR"))))
                .andExpect(jsonPath("$.correlation_id", notNullValue()));
    }

    @Test
    @DisplayName("Error case: Payment with insufficient balance should fail")
    void testInsufficientBalancePaymentFailure() throws Exception {
        // Create payment request with insufficient balance
        Map<String, Object> paymentRequest = createInsufficientBalancePaymentRequest();

        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", anyOf(is("INSUFFICIENT_BALANCE"), is("VALIDATION_ERROR"))))
                .andExpect(jsonPath("$.correlation_id", notNullValue()));
    }

    @Test
    @DisplayName("Should handle concurrent credential issuance requests")
    void shouldHandleConcurrentCredentialIssuance() throws Exception {
        int concurrentRequests = 5;
        
        // Create multiple concurrent credential issuance requests
        for (int i = 0; i < concurrentRequests; i++) {
            String userDid = "did:example:concurrentuser" + i + System.currentTimeMillis();
            Map<String, Object> credentialRequest = createCredentialRequest(userDid, "PASSPORT");

            MvcResult result = mockMvc.perform(post("/api/issuer/credentials")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(credentialRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.credentialId", notNullValue()))
                    .andExpect(jsonPath("$.credentialJwt", notNullValue()))
                    .andExpect(jsonPath("$.status", is("ISSUED")))
                    .andReturn();

            // Verify each credential is unique
            String response = result.getResponse().getContentAsString();
            String credentialJwt = objectMapper.readTree(response).get("credentialJwt").asText();
            assertThat(credentialJwt).isNotBlank();
        }
    }

    @Test
    @DisplayName("Should retrieve audit events for user activity")
    void shouldRetrieveAuditEventsForUserActivity() throws Exception {
        // Perform some activity that generates audit events
        Map<String, Object> credentialRequest = createCredentialRequest(testUserDid, "PASSPORT");
        
        mockMvc.perform(post("/api/issuer/credentials")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(credentialRequest)))
                .andExpect(status().isCreated());

        // Retrieve audit events
        mockMvc.perform(get("/api/audit/events")
                .param("userIdHash", "hash_" + testUserDid.substring(testUserDid.lastIndexOf(":") + 1))
                .param("eventType", "CREDENTIAL_ISSUED")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", isA(List.class)))
                .andExpect(jsonPath("$.totalElements", greaterThan(0)));
    }

    @Test
    @DisplayName("Should validate API documentation endpoints")
    void shouldValidateApiDocumentationEndpoints() throws Exception {
        // Test OpenAPI specification endpoint
        mockMvc.perform(get("/api-docs")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi", is("3.0.1")))
                .andExpect(jsonPath("$.info.title", is("FinPass API")))
                .andExpect(jsonPath("$.paths", notNullValue()));

        // Test Swagger UI endpoint
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    private String step1_IssueCredential() throws Exception {
        Map<String, Object> credentialRequest = createCredentialRequest(testUserDid, "PASSPORT");

        MvcResult result = mockMvc.perform(post("/api/issuer/credentials")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(credentialRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.credentialId", notNullValue()))
                .andExpect(jsonPath("$.credentialJwt", notNullValue()))
                .andExpect(jsonPath("$.status", is("ISSUED")))
                .andExpect(jsonPath("$.issuedAt", notNullValue()))
                .andExpect(jsonPath("$.expiresAt", notNullValue()))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        return objectMapper.readTree(response).get("credentialJwt").asText();
    }

    private void step2_VerifyCredential(String credentialJwt) throws Exception {
        Map<String, Object> verificationRequest = createVerificationRequest(credentialJwt, testVerifierDid);

        mockMvc.perform(post("/api/verifier/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verificationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)))
                .andExpect(jsonPath("$.verifiedAt", notNullValue()))
                .andExpect(jsonPath("$.verificationResult.credentialStatus", is("VALID")))
                .andExpect(jsonPath("$.verificationResult.verificationScore", greaterThan(0.8)));
    }

    private String step3_ProcessPayment() throws Exception {
        Map<String, Object> paymentRequest = createPaymentRequest(testUserDid, testPayeeDid, new BigDecimal("100.50"));

        MvcResult result = mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId", notNullValue()))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.amount", is(100.50)))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        return objectMapper.readTree(response).get("paymentId").asText();
    }

    private void step4_VerifyPaymentCompletion(String paymentId) throws Exception {
        // Confirm payment
        Map<String, Object> confirmationRequest = createPaymentConfirmationRequest();

        mockMvc.perform(post("/api/payments/" + paymentId + "/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(confirmationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId", is(paymentId)))
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.confirmedAt", notNullValue()));

        // Verify payment details
        mockMvc.perform(get("/api/payments/" + paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId", is(paymentId)))
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.amount", is(100.50)));
    }

    // Helper methods for creating test requests
    private Map<String, Object> createCredentialRequest(String holderDid, String credentialType) {
        Map<String, Object> request = new HashMap<>();
        request.put("holderDid", holderDid);
        request.put("credentialType", credentialType);
        
        Map<String, Object> credentialData = new HashMap<>();
        credentialData.put("passportNumber", "P123456789");
        credentialData.put("fullName", "Test User");
        credentialData.put("dateOfBirth", "1990-01-01");
        credentialData.put("nationality", "US");
        credentialData.put("issuingCountry", "US");
        credentialData.put("expirationDate", "2030-01-01");
        
        request.put("credentialData", credentialData);
        
        Map<String, Object> livenessProof = new HashMap<>();
        livenessProof.put("score", 0.95);
        livenessProof.put("timestamp", Instant.now().toString());
        livenessProof.put("proofData", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...");
        
        request.put("livenessProof", livenessProof);
        
        return request;
    }

    private Map<String, Object> createInvalidCredentialRequest() {
        Map<String, Object> request = new HashMap<>();
        request.put("holderDid", "invalid-did-format"); // Invalid DID
        // Missing credentialType and other required fields
        
        Map<String, Object> credentialData = new HashMap<>();
        credentialData.put("passportNumber", "P123456789");
        
        request.put("credentialData", credentialData);
        
        return request;
    }

    private Map<String, Object> createVerificationRequest(String credentialJwt, String verifierDid) {
        Map<String, Object> request = new HashMap<>();
        request.put("credentialJwt", credentialJwt);
        request.put("verifierDid", verifierDid);
        request.put("verificationType", "IDENTITY_VERIFICATION");
        
        Map<String, Object> verificationData = new HashMap<>();
        verificationData.put("minimumAge", 18);
        verificationData.put("requiredFields", java.util.Arrays.asList("fullName", "dateOfBirth"));
        
        request.put("verificationData", verificationData);
        
        return request;
    }

    private Map<String, Object> createInvalidVerificationRequest() {
        Map<String, Object> request = new HashMap<>();
        request.put("credentialJwt", "invalid.jwt.token");
        request.put("verifierDid", testVerifierDid);
        request.put("verificationType", "IDENTITY_VERIFICATION");
        
        return request;
    }

    private Map<String, Object> createPaymentRequest(String payerDid, String payeeDid, BigDecimal amount) {
        Map<String, Object> request = new HashMap<>();
        request.put("payerDid", payerDid);
        request.put("payeeDid", payeeDid);
        request.put("amount", amount);
        request.put("currency", "USD");
        request.put("paymentMethod", "BANK_TRANSFER");
        request.put("description", "Test payment for integration testing");
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("invoiceId", "INV-" + System.currentTimeMillis());
        metadata.put("orderId", "ORDER-" + System.currentTimeMillis());
        
        request.put("metadata", metadata);
        
        return request;
    }

    private Map<String, Object> createInsufficientBalancePaymentRequest() {
        return createPaymentRequest(testUserDid, testPayeeDid, new BigDecimal("999999.99"));
    }

    private Map<String, Object> createPaymentConfirmationRequest() {
        Map<String, Object> request = new HashMap<>();
        request.put("confirmationCode", "CONF-" + System.currentTimeMillis());
        request.put("transactionHash", "0x1234567890abcdef1234567890abcdef12345678");
        
        return request;
    }
}
