package com.finpass.issuer.validation;

import com.finpass.issuer.validation.ValidationService.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for ValidationService
 */
class ValidationServiceTest {

    private ValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new ValidationService();
    }

    // DID Validation Tests
    @Test
    void testValidateDid_ValidDids() {
        // Valid DIDs
        assertTrue(validationService.validateDid("did:example:123456789abcdefghi").isValid());
        assertTrue(validationService.validateDid("did:ethr:0x1234567890123456789012345678901234567890").isValid());
        assertTrue(validationService.validateDid("did:web:example.com:users:123").isValid());
        assertTrue(validationService.validateDid("did:key:z6MkhaXgBZDvotDkL5257faiztiGiC2QtKLGpbnnEGta2do7").isValid());
    }

    @Test
    void testValidateDid_InvalidDids() {
        // Null or empty
        ValidationResult result1 = validationService.validateDid(null);
        assertFalse(result1.isValid());
        assertEquals("DID_REQUIRED", result1.getErrorCode());

        ValidationResult result2 = validationService.validateDid("");
        assertFalse(result2.isValid());
        assertEquals("DID_REQUIRED", result2.getErrorCode());

        // Invalid format
        ValidationResult result3 = validationService.validateDid("invalid-did");
        assertFalse(result3.isValid());
        assertEquals("INVALID_DID_FORMAT", result3.getErrorCode());

        ValidationResult result4 = validationService.validateDid("did:");
        assertFalse(result4.isValid());
        assertEquals("INVALID_DID_FORMAT", result4.getErrorCode());

        // Too long
        String longDid = "did:example:" + "a".repeat(2050);
        ValidationResult result5 = validationService.validateDid(longDid);
        assertFalse(result5.isValid());
        assertEquals("DID_TOO_LONG", result5.getErrorCode());
    }

    // JWT Structure Validation Tests
    @Test
    void testValidateJwtStructure_ValidJwts() {
        // Valid JWT structure (header.payload.signature)
        assertTrue(validationService.validateJwtStructure("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c").isValid());
        assertTrue(validationService.validateJwtStructure("header.payload.signature").isValid());
        assertTrue(validationService.validateJwtStructure("a.b.c").isValid());
    }

    @Test
    void testValidateJwtStructure_InvalidJwts() {
        // Null or empty
        ValidationResult result1 = validationService.validateJwtStructure(null);
        assertFalse(result1.isValid());
        assertEquals("JWT_REQUIRED", result1.getErrorCode());

        ValidationResult result2 = validationService.validateJwtStructure("");
        assertFalse(result2.isValid());
        assertEquals("JWT_REQUIRED", result2.getErrorCode());

        // Invalid structure
        ValidationResult result3 = validationService.validateJwtStructure("invalid.jwt");
        assertFalse(result3.isValid());
        assertEquals("INVALID_JWT_STRUCTURE", result3.getErrorCode());

        ValidationResult result4 = validationService.validateJwtStructure("onlyheader");
        assertFalse(result4.isValid());
        assertEquals("INVALID_JWT_STRUCTURE", result3.getErrorCode());

        // Malformed JWT
        ValidationResult result5 = validationService.validateJwtStructure("invalid..signature");
        assertFalse(result5.isValid());
        assertEquals("JWT_PARSE_ERROR", result5.getErrorCode());
    }

    // Amount Validation Tests
    @Test
    void testValidateAmount_ValidAmounts() {
        assertTrue(validationService.validateAmount(new BigDecimal("1.00")).isValid());
        assertTrue(validationService.validateAmount(new BigDecimal("0.01")).isValid());
        assertTrue(validationService.validateAmount(new BigDecimal("999999999.99")).isValid());
        assertTrue(validationService.validateAmount(new BigDecimal("100.50")).isValid());
    }

    @Test
    void testValidateAmount_InvalidAmounts() {
        // Null
        ValidationResult result1 = validationService.validateAmount(null);
        assertFalse(result1.isValid());
        assertEquals("AMOUNT_REQUIRED", result1.getErrorCode());

        // Too small
        ValidationResult result2 = validationService.validateAmount(new BigDecimal("0.001"));
        assertFalse(result2.isValid());
        assertEquals("AMOUNT_TOO_SMALL", result2.getErrorCode());

        ValidationResult result3 = validationService.validateAmount(new BigDecimal("0"));
        assertFalse(result3.isValid());
        assertEquals("AMOUNT_TOO_SMALL", result3.getErrorCode());

        ValidationResult result4 = validationService.validateAmount(new BigDecimal("-1"));
        assertFalse(result4.isValid());
        assertEquals("AMOUNT_TOO_SMALL", result4.getErrorCode());

        // Too large
        ValidationResult result5 = validationService.validateAmount(new BigDecimal("1000000000"));
        assertFalse(result5.isValid());
        assertEquals("AMOUNT_TOO_LARGE", result5.getErrorCode());

        // Too many decimal places
        ValidationResult result6 = validationService.validateAmount(new BigDecimal("1.001"));
        assertFalse(result6.isValid());
        assertEquals("INVALID_DECIMAL_PLACES", result6.getErrorCode());
    }

    @Test
    void testValidateAmount_StringInput() {
        // Valid string amounts
        assertTrue(validationService.validateAmount((String) "100.50").isValid());
        assertTrue(validationService.validateAmount((String) "0.01").isValid());

        // Invalid string amounts
        ValidationResult result1 = validationService.validateAmount((String) "invalid");
        assertFalse(result1.isValid());
        assertEquals("INVALID_AMOUNT_FORMAT", result1.getErrorCode());

        ValidationResult result2 = validationService.validateAmount((String) "");
        assertFalse(result2.isValid());
        assertEquals("AMOUNT_REQUIRED", result2.getErrorCode());
    }

    // Timestamp Validation Tests
    @Test
    void testValidateTimestamp_ValidTimestamps() {
        Instant now = Instant.now();
        assertTrue(validationService.validateTimestamp(now).isValid());
        assertTrue(validationService.validateTimestamp(now.minus(60, ChronoUnit.SECONDS)).isValid());
        assertTrue(validationService.validateTimestamp(now.minus(1, ChronoUnit.DAYS)).isValid());
        assertTrue(validationService.validateTimestamp(now.minus(6, ChronoUnit.MONTHS)).isValid());
    }

    @Test
    void testValidateTimestamp_InvalidTimestamps() {
        Instant now = Instant.now();

        // Null
        ValidationResult result1 = validationService.validateTimestamp(null);
        assertFalse(result1.isValid());
        assertEquals("TIMESTAMP_REQUIRED", result1.getErrorCode());

        // Too far in future
        ValidationResult result2 = validationService.validateTimestamp(now.plusSeconds(400));
        assertFalse(result2.isValid());
        assertEquals("TIMESTAMP_IN_FUTURE", result2.getErrorCode());

        // Too old
        ValidationResult result3 = validationService.validateTimestamp(now.minus(31600000, ChronoUnit.SECONDS));
        assertFalse(result3.isValid());
        assertEquals("TIMESTAMP_TOO_OLD", result3.getErrorCode());
    }

    @Test
    void testValidateTimestamp_StringInput() {
        // Valid string timestamps
        assertTrue(validationService.validateTimestamp((String) "2023-12-01T10:00:00Z").isValid());
        assertTrue(validationService.validateTimestamp((String) Instant.now().toString()).isValid());

        // Invalid string timestamps
        ValidationResult result1 = validationService.validateTimestamp((String) "invalid-date");
        assertFalse(result1.isValid());
        assertEquals("INVALID_TIMESTAMP_FORMAT", result1.getErrorCode());

        ValidationResult result2 = validationService.validateTimestamp((String) "");
        assertFalse(result2.isValid());
        assertEquals("TIMESTAMP_REQUIRED", result2.getErrorCode());
    }

    // Email Validation Tests
    @Test
    void testValidateEmail_ValidEmails() {
        assertTrue(validationService.validateEmail("user@example.com").isValid());
        assertTrue(validationService.validateEmail("test.email+tag@example.co.uk").isValid());
        assertTrue(validationService.validateEmail("user123@test-domain.com").isValid());
        assertTrue(validationService.validateEmail(null).isValid()); // Optional field
        assertTrue(validationService.validateEmail("").isValid()); // Optional field
    }

    @Test
    void testValidateEmail_InvalidEmails() {
        ValidationResult result1 = validationService.validateEmail("invalid-email");
        assertFalse(result1.isValid());
        assertEquals("INVALID_EMAIL_FORMAT", result1.getErrorCode());

        ValidationResult result2 = validationService.validateEmail("@example.com");
        assertFalse(result2.isValid());
        assertEquals("INVALID_EMAIL_FORMAT", result2.getErrorCode());

        ValidationResult result3 = validationService.validateEmail("user@");
        assertFalse(result3.isValid());
        assertEquals("INVALID_EMAIL_FORMAT", result3.getErrorCode());

        // Too long
        String longEmail = "user@" + "a".repeat(250) + ".com";
        ValidationResult result4 = validationService.validateEmail(longEmail);
        assertFalse(result4.isValid());
        assertEquals("EMAIL_TOO_LONG", result4.getErrorCode());
    }

    // Phone Validation Tests
    @Test
    void testValidatePhone_ValidPhones() {
        assertTrue(validationService.validatePhone("+1234567890").isValid());
        assertTrue(validationService.validatePhone("+44 20 7946 0958").isValid());
        assertTrue(validationService.validatePhone("+1 (555) 123-4567").isValid());
        assertTrue(validationService.validatePhone(null).isValid()); // Optional field
        assertTrue(validationService.validatePhone("").isValid()); // Optional field
    }

    @Test
    void testValidatePhone_InvalidPhones() {
        ValidationResult result1 = validationService.validatePhone("1234567890");
        assertFalse(result1.isValid());
        assertEquals("INVALID_PHONE_FORMAT", result1.getErrorCode());

        ValidationResult result2 = validationService.validatePhone("abc123");
        assertFalse(result2.isValid());
        assertEquals("INVALID_PHONE_FORMAT", result2.getErrorCode());

        ValidationResult result3 = validationService.validatePhone("123");
        assertFalse(result3.isValid());
        assertEquals("INVALID_PHONE_FORMAT", result3.getErrorCode());
    }

    // Currency Validation Tests
    @Test
    void testValidateCurrency_ValidCurrencies() {
        assertTrue(validationService.validateCurrency("USD").isValid());
        assertTrue(validationService.validateCurrency("EUR").isValid());
        assertTrue(validationService.validateCurrency("GBP").isValid());
        assertTrue(validationService.validateCurrency("JPY").isValid());
        assertTrue(validationService.validateCurrency("KRW").isValid());
    }

    @Test
    void testValidateCurrency_InvalidCurrencies() {
        ValidationResult result1 = validationService.validateCurrency(null);
        assertFalse(result1.isValid());
        assertEquals("CURRENCY_REQUIRED", result1.getErrorCode());

        ValidationResult result2 = validationService.validateCurrency("");
        assertFalse(result2.isValid());
        assertEquals("CURRENCY_REQUIRED", result2.getErrorCode());

        ValidationResult result3 = validationService.validateCurrency("usd"); // lowercase
        assertFalse(result3.isValid());
        assertEquals("INVALID_CURRENCY_CODE", result3.getErrorCode());

        ValidationResult result4 = validationService.validateCurrency("US"); // too short
        assertFalse(result4.isValid());
        assertEquals("INVALID_CURRENCY_CODE", result4.getErrorCode());

        ValidationResult result5 = validationService.validateCurrency("USDD"); // too long
        assertFalse(result5.isValid());
        assertEquals("INVALID_CURRENCY_CODE", result5.getErrorCode());

        ValidationResult result6 = validationService.validateCurrency("XYZ"); // unsupported
        assertFalse(result6.isValid());
        assertEquals("UNSUPPORTED_CURRENCY", result6.getErrorCode());
    }

    // Payment Method Validation Tests
    @Test
    void testValidatePaymentMethod_ValidMethods() {
        assertTrue(validationService.validatePaymentMethod("CREDIT_CARD").isValid());
        assertTrue(validationService.validatePaymentMethod("DEBIT_CARD").isValid());
        assertTrue(validationService.validatePaymentMethod("BANK_TRANSFER").isValid());
        assertTrue(validationService.validatePaymentMethod(null).isValid()); // Optional
        assertTrue(validationService.validatePaymentMethod("").isValid()); // Optional
    }

    @Test
    void testValidatePaymentMethod_InvalidMethods() {
        ValidationResult result1 = validationService.validatePaymentMethod("INVALID_METHOD");
        assertFalse(result1.isValid());
        assertEquals("INVALID_PAYMENT_METHOD", result1.getErrorCode());

        ValidationResult result2 = validationService.validatePaymentMethod("cash");
        assertFalse(result2.isValid());
        assertEquals("INVALID_PAYMENT_METHOD", result2.getErrorCode());
    }

    // KYC Token Validation Tests
    @Test
    void testValidateKycDecisionToken_ValidTokens() {
        assertTrue(validationService.validateKycDecisionToken("valid_token_123").isValid());
        assertTrue(validationService.validateKycDecisionToken("TOKEN-ABC-123").isValid());
        assertTrue(validationService.validateKycDecisionToken(null).isValid()); // Optional
        assertTrue(validationService.validateKycDecisionToken("").isValid()); // Optional
    }

    @Test
    void testValidateKycDecisionToken_InvalidTokens() {
        ValidationResult result1 = validationService.validateKycDecisionToken("short");
        assertFalse(result1.isValid());
        assertEquals("INVALID_KYC_TOKEN_LENGTH", result1.getErrorCode());

        String longToken = "a".repeat(1001);
        ValidationResult result2 = validationService.validateKycDecisionToken(longToken);
        assertFalse(result2.isValid());
        assertEquals("INVALID_KYC_TOKEN_LENGTH", result2.getErrorCode());

        ValidationResult result3 = validationService.validateKycDecisionToken("invalid@token");
        assertFalse(result3.isValid());
        assertEquals("INVALID_KYC_TOKEN_FORMAT", result3.getErrorCode());
    }

    // Comprehensive Validation Tests
    @Test
    void testValidateCredential_ValidInputs() {
        assertTrue(validationService.validateCredential(
            "did:example:123456789abcdefghi",
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
            Instant.now()
        ).isValid());
    }

    @Test
    void testValidateCredential_InvalidInputs() {
        // Invalid DID
        ValidationResult result1 = validationService.validateCredential(
            "invalid-did",
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
            Instant.now()
        );
        assertFalse(result1.isValid());
        assertEquals("INVALID_DID_FORMAT", result1.getErrorCode());

        // Invalid JWT
        ValidationResult result2 = validationService.validateCredential(
            "did:example:123456789abcdefghi",
            "invalid-jwt",
            Instant.now()
        );
        assertFalse(result2.isValid());
        assertEquals("INVALID_JWT_STRUCTURE", result2.getErrorCode());
    }

    @Test
    void testValidatePayment_ValidInputs() {
        assertTrue(validationService.validatePayment(
            "did:example:payer123",
            "did:example:payee456",
            new BigDecimal("100.50"),
            "USD"
        ).isValid());
    }

    @Test
    void testValidatePayment_InvalidInputs() {
        // Invalid amount
        ValidationResult result1 = validationService.validatePayment(
            "did:example:payer123",
            "did:example:payee456",
            new BigDecimal("-100"),
            "USD"
        );
        assertFalse(result1.isValid());
        assertEquals("AMOUNT_TOO_SMALL", result1.getErrorCode());

        // Invalid currency
        ValidationResult result2 = validationService.validatePayment(
            "did:example:payer123",
            "did:example:payee456",
            new BigDecimal("100"),
            "INVALID"
        );
        assertFalse(result2.isValid());
        assertEquals("UNSUPPORTED_CURRENCY", result2.getErrorCode());
    }

    // Parameterized Tests
    @ParameterizedTest
    @ValueSource(strings = {
        "did:example:123456789abcdefghi",
        "did:ethr:0x1234567890123456789012345678901234567890",
        "did:web:example.com:users:123",
        "did:key:z6MkhaXgBZDvotDkL5257faiztiGiC2QtKLGpbnnEGta2do7"
    })
    void testValidDids(String did) {
        assertTrue(validationService.validateDid(did).isValid(), 
            "DID should be valid: " + did);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "invalid-did",
        "did:",
        "did:invalid method:123",
        "123456789",
        ""
    })
    void testInvalidDids(String did) {
        assertFalse(validationService.validateDid(did).isValid(), 
            "DID should be invalid: " + did);
    }

    // Edge Cases
    @Test
    void testValidationResult_ToString() {
        ValidationResult success = ValidationResult.success();
        assertEquals("ValidationResult{valid=true}", success.toString());

        ValidationResult error = ValidationResult.error("TEST_ERROR", "Test message");
        assertEquals("ValidationResult{valid=false, errorCode='TEST_ERROR', errorMessage='Test message'}", 
                     error.toString());
    }

    @Test
    void testValidationResult_StaticMethods() {
        ValidationResult success = ValidationResult.success();
        assertTrue(success.isValid());
        assertNull(success.getErrorCode());
        assertNull(success.getErrorMessage());

        ValidationResult error = ValidationResult.error("CODE", "Message");
        assertFalse(error.isValid());
        assertEquals("CODE", error.getErrorCode());
        assertEquals("Message", error.getErrorMessage());
    }
}
