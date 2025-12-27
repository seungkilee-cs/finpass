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
 * Integration tests for the complete FinPass workflow:
 * Create wallet → Issue credential → Verify credential → Process payment
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class FullWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String testUserDid;
    private String testVerifierDid;
    private String testPayeeDid;
    private String issuedCredentialJwt;
    private String paymentId;

    @BeforeEach
    void setUp() {
        testUserDid = "did:example:testuser" + System.currentTimeMillis();
        testVerifierDid = "did:example:testverifier" + System.currentTimeMillis();
        testPayeeDid = "did:example:testpayee" + System.currentTimeMillis();
    }

    @Test
    @DisplayName("Complete happy path: Wallet → Credential → Verification → Payment")
    void testCompleteHappyPathWorkflow() throws Exception {
        // Step 1: Issue a credential to the user
        step1_IssueCredential();
        
        // Step 2: Verify the issued credential
        step2_VerifyCredential();
        
        // Step 3: Process a payment using the verified credential
        step3_ProcessPayment();
        
        // Step 4: Verify payment completion
        step4_VerifyPaymentCompletion();
    }

    private void step1_IssueCredential() throws Exception {
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
                .andExpect(jsonPath("$.credentialId", notNullValue()))
                .andExpect(jsonPath("$.credentialJwt", notNullValue()))
                .andExpect(jsonPath("$.status", is("ISSUED")))
                .andExpect(jsonPath("$.issuedAt", notNullValue()))
                .andExpect(jsonPath("$.expiresAt", notNullValue()))
                .andExpect(jsonPath("$.revocationId", notNullValue()))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        issuedCredentialJwt = objectMapper.readTree(response).get("credentialJwt").asText();
    }

    private void step2_VerifyCredential() throws Exception {
        Map<String, Object> verificationRequest = new HashMap<>();
        verificationRequest.put("credentialJwt", issuedCredentialJwt);
        verificationRequest.put("verifierDid", testVerifierDid);
        verificationRequest.put("verificationType", "IDENTITY_VERIFICATION");
        
        Map<String, Object> verificationData = new HashMap<>();
        verificationData.put("minimumAge", 18);
        verificationData.put("requiredFields", java.util.Arrays.asList("fullName", "dateOfBirth"));
        
        verificationRequest.put("verificationData", verificationData);

        mockMvc.perform(post("/api/verifier/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verificationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)))
                .andExpect(jsonPath("$.verifiedAt", notNullValue()))
                .andExpect(jsonPath("$.verificationResult.credentialStatus", is("VALID")))
                .andExpect(jsonPath("$.verificationResult.issuerVerified", is(true)))
                .andExpect(jsonPath("$.verificationResult.verificationScore", greaterThan(0.9)))
                .andExpect(jsonPath("$.verificationResult.claimsVerified.fullName", is(true)))
                .andExpect(jsonPath("$.verificationResult.claimsVerified.dateOfBirth", is(true)))
                .andExpect(jsonPath("$.verificationResult.claimsVerified.nationality", is(true)));
    }

    private void step3_ProcessPayment() throws Exception {
        Map<String, Object> paymentRequest = new HashMap<>();
        paymentRequest.put("payerDid", testUserDid);
        paymentRequest.put("payeeDid", testPayeeDid);
        paymentRequest.put("amount", new BigDecimal("100.50"));
        paymentRequest.put("currency", "USD");
        paymentRequest.put("paymentMethod", "BANK_TRANSFER");
        paymentRequest.put("description", "Payment for identity verification service");
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("invoiceId", "INV-" + System.currentTimeMillis());
        metadata.put("orderId", "ORDER-" + System.currentTimeMillis());
        metadata.put("credentialId", issuedCredentialJwt);
        
        paymentRequest.put("metadata", metadata);

        MvcResult result = mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId", notNullValue()))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.amount", is(100.50)))
                .andExpect(jsonPath("$.currency", is("USD")))
                .andExpect(jsonPath("$.fee", greaterThan(0)))
                .andReturn();

        String response = result.getResponse().getContentAsString();
        paymentId = objectMapper.readTree(response).get("paymentId").asText();
    }

    private void step4_VerifyPaymentCompletion() throws Exception {
        // Simulate payment confirmation
        Map<String, Object> confirmationRequest = new HashMap<>();
        confirmationRequest.put("confirmationCode", "CONF-" + System.currentTimeMillis());
        confirmationRequest.put("transactionHash", "0x1234567890abcdef1234567890abcdef12345678");

        mockMvc.perform(post("/api/payments/" + paymentId + "/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(confirmationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId", is(paymentId)))
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.confirmedAt", notNullValue()))
                .andExpect(jsonPath("$.transactionId", notNullValue()));

        // Verify payment details
        mockMvc.perform(get("/api/payments/" + paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId", is(paymentId)))
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.amount", is(100.50)))
                .andExpect(jsonPath("$.currency", is("USD")))
                .andExpect(jsonPath("$.blockchainConfirmation", greaterThan(0)));
    }

    @Test
    @DisplayName("Multiple credentials for single user")
    void testMultipleCredentialsForUser() throws Exception {
        // Issue first credential (Passport)
        String passportCredential = issueCredentialOfType("PASSPORT");
        
        // Issue second credential (Driver's License)
        String licenseCredential = issueCredentialOfType("DRIVERS_LICENSE");
        
        // Issue third credential (National ID)
        String nationalIdCredential = issueCredentialOfType("NATIONAL_ID");
        
        // Verify all credentials are valid
        verifyCredential(passportCredential);
        verifyCredential(licenseCredential);
        verifyCredential(nationalIdCredential);
        
        // Check user's credential list
        mockMvc.perform(get("/api/issuer/credentials")
                .param("holderDid", testUserDid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[*].credentialType", 
                    hasItems("PASSPORT", "DRIVERS_LICENSE", "NATIONAL_ID")));
    }

    @Test
    @DisplayName("Multiple verifications for same credential")
    void testMultipleVerifications() throws Exception {
        // Issue a credential
        String credential = issueCredentialOfType("PASSPORT");
        
        // Perform multiple verifications with different verifiers
        for (int i = 0; i < 3; i++) {
            String verifierDid = "did:example:verifier" + i;
            verifyCredentialWithVerifier(credential, verifierDid);
        }
        
        // Check audit trail for multiple verifications
        mockMvc.perform(get("/api/audit/events")
                .param("userIdHash", testUserDid)
                .param("eventType", "PRESENTATION_VERIFIED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[*].outcome", everyItem(is("SUCCESS"))));
    }

    @Test
    @DisplayName("Multiple payments for same user")
    void testMultiplePayments() throws Exception {
        // Issue credential
        String credential = issueCredentialOfType("PASSPORT");
        verifyCredential(credential);
        
        // Process multiple payments
        String[] paymentIds = new String[3];
        for (int i = 0; i < 3; i++) {
            paymentIds[i] = processPayment(new BigDecimal("50.00"), "Payment " + (i + 1));
            confirmPayment(paymentIds[i]);
        }
        
        // Verify all payments are completed
        for (String paymentId : paymentIds) {
            mockMvc.perform(get("/api/payments/" + paymentId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("COMPLETED")));
        }
        
        // Check user's payment history
        mockMvc.perform(get("/api/payments")
                .param("payerDid", testUserDid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[*].status", everyItem(is("COMPLETED"))));
    }

    private String issueCredentialOfType(String credentialType) throws Exception {
        Map<String, Object> credentialRequest = new HashMap<>();
        credentialRequest.put("holderDid", testUserDid);
        credentialRequest.put("credentialType", credentialType);
        
        Map<String, Object> credentialData = new HashMap<>();
        switch (credentialType) {
            case "PASSPORT":
                credentialData.put("passportNumber", "P123456789");
                credentialData.put("fullName", "John Doe");
                break;
            case "DRIVERS_LICENSE":
                credentialData.put("licenseNumber", "DL123456789");
                credentialData.put("fullName", "John Doe");
                break;
            case "NATIONAL_ID":
                credentialData.put("nationalIdNumber", "NID123456789");
                credentialData.put("fullName", "John Doe");
                break;
        }
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
        return objectMapper.readTree(response).get("credentialJwt").asText();
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

    private void verifyCredentialWithVerifier(String credentialJwt, String verifierDid) throws Exception {
        Map<String, Object> verificationRequest = new HashMap<>();
        verificationRequest.put("credentialJwt", credentialJwt);
        verificationRequest.put("verifierDid", verifierDid);
        verificationRequest.put("verificationType", "IDENTITY_VERIFICATION");

        mockMvc.perform(post("/api/verifier/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verificationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)));
    }

    private String processPayment(BigDecimal amount, String description) throws Exception {
        Map<String, Object> paymentRequest = new HashMap<>();
        paymentRequest.put("payerDid", testUserDid);
        paymentRequest.put("payeeDid", testPayeeDid);
        paymentRequest.put("amount", amount);
        paymentRequest.put("currency", "USD");
        paymentRequest.put("paymentMethod", "BANK_TRANSFER");
        paymentRequest.put("description", description);

        MvcResult result = mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        return objectMapper.readTree(response).get("paymentId").asText();
    }

    private void confirmPayment(String paymentId) throws Exception {
        Map<String, Object> confirmationRequest = new HashMap<>();
        confirmationRequest.put("confirmationCode", "CONF-" + System.currentTimeMillis());
        confirmationRequest.put("transactionHash", "0x1234567890abcdef1234567890abcdef12345678");

        mockMvc.perform(post("/api/payments/" + paymentId + "/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(confirmationRequest)))
                .andExpect(status().isOk());
    }
}
