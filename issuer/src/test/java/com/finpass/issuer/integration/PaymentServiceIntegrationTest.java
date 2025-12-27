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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PaymentService endpoints
 * Tests payment processing, confirmation, and error handling
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class PaymentServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String testUserDid;
    private String testPayeeDid;

    @BeforeEach
    void setUp() {
        testUserDid = "did:example:testuser" + System.currentTimeMillis();
        testPayeeDid = "did:example:testpayee" + System.currentTimeMillis();
    }

    @Test
    @DisplayName("Should create payment successfully")
    void shouldCreatePaymentSuccessfully() throws Exception {
        Map<String, Object> paymentRequest = createPaymentRequest(testUserDid, testPayeeDid, new BigDecimal("100.50"));

        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentId", notNullValue()))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.payerDid", is(testUserDid)))
                .andExpect(jsonPath("$.payeeDid", is(testPayeeDid)))
                .andExpect(jsonPath("$.amount", is(100.50)))
                .andExpect(jsonPath("$.currency", is("USD")))
                .andExpect(jsonPath("$.paymentMethod", is("BANK_TRANSFER")))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }

    @Test
    @DisplayName("Should retrieve payment details successfully")
    void shouldRetrievePaymentDetailsSuccessfully() throws Exception {
        // Create a payment first
        String paymentId = createTestPayment();

        // Retrieve payment details
        mockMvc.perform(get("/api/payments/" + paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId", is(paymentId)))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.amount", is(100.50)))
                .andExpect(jsonPath("$.currency", is("USD")));
    }

    @Test
    @DisplayName("Should confirm payment successfully")
    void shouldConfirmPaymentSuccessfully() throws Exception {
        // Create a payment first
        String paymentId = createTestPayment();

        // Confirm the payment
        Map<String, Object> confirmationRequest = createPaymentConfirmationRequest();

        mockMvc.perform(post("/api/payments/" + paymentId + "/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(confirmationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId", is(paymentId)))
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.confirmedAt", notNullValue()))
                .andExpect(jsonPath("$.transactionId", notNullValue()));
    }

    @Test
    @DisplayName("Should list payments for user successfully")
    void shouldListPaymentsForUserSuccessfully() throws Exception {
        // Create multiple payments for the same user
        createTestPayment();
        createTestPayment();
        createTestPayment();

        // List payments for the user
        mockMvc.perform(get("/api/payments")
                .param("payerDid", testUserDid)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.content[*].payerDid", everyItem(is(testUserDid))))
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.totalPages", is(1)));
    }

    @Test
    @DisplayName("Should cancel payment successfully")
    void shouldCancelPaymentSuccessfully() throws Exception {
        // Create a payment first
        String paymentId = createTestPayment();

        // Cancel the payment
        mockMvc.perform(delete("/api/payments/" + paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId", is(paymentId)))
                .andExpect(jsonPath("$.status", is("CANCELLED")))
                .andExpect(jsonPath("$.cancelledAt", notNullValue()));
    }

    @Test
    @DisplayName("Should reject payment with invalid amount")
    void shouldRejectPaymentWithInvalidAmount() throws Exception {
        Map<String, Object> paymentRequest = createPaymentRequest(testUserDid, testPayeeDid, BigDecimal.ZERO);

        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.error_description", containsString("amount")))
                .andExpect(jsonPath("$.correlation_id", notNullValue()));
    }

    @Test
    @DisplayName("Should reject payment with invalid DID format")
    void shouldRejectPaymentWithInvalidDidFormat() throws Exception {
        Map<String, Object> paymentRequest = createPaymentRequest("invalid-did", testPayeeDid, new BigDecimal("100.50"));

        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.correlation_id", notNullValue()));
    }

    @Test
    @DisplayName("Should reject confirmation for non-existent payment")
    void shouldRejectConfirmationForNonExistentPayment() throws Exception {
        String nonExistentPaymentId = "non-existent-payment-id";
        Map<String, Object> confirmationRequest = createPaymentConfirmationRequest();

        mockMvc.perform(post("/api/payments/" + nonExistentPaymentId + "/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(confirmationRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("PAYMENT_NOT_FOUND")))
                .andExpect(jsonPath("$.correlation_id", notNullValue()));
    }

    @Test
    @DisplayName("Should handle multiple payment methods")
    void shouldHandleMultiplePaymentMethods() throws Exception {
        String[] paymentMethods = {"BANK_TRANSFER", "DIGITAL_WALLET", "CREDIT_CARD"};

        for (String paymentMethod : paymentMethods) {
            Map<String, Object> paymentRequest = createPaymentRequest(testUserDid, testPayeeDid, new BigDecimal("50.00"));
            paymentRequest.put("paymentMethod", paymentMethod);

            mockMvc.perform(post("/api/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(paymentRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.paymentMethod", is(paymentMethod)));
        }
    }

    @Test
    @DisplayName("Should handle different currencies")
    void shouldHandleDifferentCurrencies() throws Exception {
        String[] currencies = {"USD", "EUR", "GBP"};
        BigDecimal[] amounts = {new BigDecimal("100.00"), new BigDecimal("85.00"), new BigDecimal("75.00")};

        for (int i = 0; i < currencies.length; i++) {
            Map<String, Object> paymentRequest = createPaymentRequest(testUserDid, testPayeeDid, amounts[i]);
            paymentRequest.put("currency", currencies[i]);

            mockMvc.perform(post("/api/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(paymentRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.currency", is(currencies[i])))
                    .andExpect(jsonPath("$.amount", is(amounts[i].doubleValue())));
        }
    }

    @Test
    @DisplayName("Should validate payment metadata")
    void shouldValidatePaymentMetadata() throws Exception {
        Map<String, Object> paymentRequest = createPaymentRequest(testUserDid, testPayeeDid, new BigDecimal("100.50"));
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("invoiceId", "INV-12345");
        metadata.put("orderId", "ORDER-67890");
        metadata.put("description", "Test payment with metadata");
        metadata.put("customField", "customValue");
        
        paymentRequest.put("metadata", metadata);

        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.metadata.invoiceId", is("INV-12345")))
                .andExpect(jsonPath("$.metadata.orderId", is("ORDER-67890")))
                .andExpect(jsonPath("$.metadata.description", is("Test payment with metadata")))
                .andExpect(jsonPath("$.metadata.customField", is("customValue")));
    }

    private String createTestPayment() throws Exception {
        Map<String, Object> paymentRequest = createPaymentRequest(testUserDid, testPayeeDid, new BigDecimal("100.50"));

        var result = mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        return objectMapper.readTree(response).get("paymentId").asText();
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

    private Map<String, Object> createPaymentConfirmationRequest() {
        Map<String, Object> request = new HashMap<>();
        request.put("confirmationCode", "CONF-" + System.currentTimeMillis());
        request.put("transactionHash", "0x1234567890abcdef1234567890abcdef12345678");
        
        return request;
    }
}
