package com.finpass.issuer.validation;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Comprehensive validation utility for FinPass inputs
 */
@Component
public class ValidationService {

    private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);

    // DID pattern: did:method:specific-id
    private static final Pattern DID_PATTERN = Pattern.compile("^did:[a-z0-9]+:[a-zA-Z0-9._-]+$");
    
    // JWT structure patterns
    private static final Pattern JWT_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]*$");
    
    // Reasonable amount limits (in smallest currency unit)
    private static final BigDecimal MIN_AMOUNT = new BigDecimal("0.01");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999999.99");

    /**
     * Validates DID format
     */
    public ValidationResult validateDid(String did) {
        if (did == null || did.trim().isEmpty()) {
            return ValidationResult.error("DID_REQUIRED", "DID is required");
        }

        if (!DID_PATTERN.matcher(did).matches()) {
            return ValidationResult.error("INVALID_DID_FORMAT", 
                "DID must follow format: did:method:specific-id");
        }

        if (did.length() > 2048) {
            return ValidationResult.error("DID_TOO_LONG", "DID exceeds maximum length of 2048 characters");
        }

        return ValidationResult.success();
    }

    /**
     * Validates JWT structure
     */
    public ValidationResult validateJwtStructure(String jwt) {
        if (jwt == null || jwt.trim().isEmpty()) {
            return ValidationResult.error("JWT_REQUIRED", "JWT is required");
        }

        if (!JWT_PATTERN.matcher(jwt).matches()) {
            return ValidationResult.error("INVALID_JWT_STRUCTURE", 
                "JWT must have valid structure with header, payload, and signature");
        }

        try {
            SignedJWT.parse(jwt);
        } catch (Exception e) {
            return ValidationResult.error("JWT_PARSE_ERROR", 
                "JWT cannot be parsed: " + e.getMessage());
        }

        return ValidationResult.success();
    }

    /**
     * Validates JWT signature using provided JWK Set
     */
    public ValidationResult validateJwtSignature(String jwt, JWKSet jwkSet) {
        ValidationResult structureValidation = validateJwtStructure(jwt);
        if (!structureValidation.isValid()) {
            return structureValidation;
        }

        try {
            SignedJWT signedJWT = SignedJWT.parse(jwt);
            JWSHeader header = signedJWT.getHeader();
            
            // Find the key that matches the JWT key ID
            String keyId = header.getKeyID();
            if (keyId == null) {
                return ValidationResult.error("MISSING_KEY_ID", "JWT header missing key ID");
            }

            // Get all keys and find matching key ID
            List<JWK> keys = jwkSet.getKeys();
            JWK matchingKey = null;
            for (JWK key : keys) {
                if (keyId.equals(key.getKeyID())) {
                    matchingKey = key;
                    break;
                }
            }
            
            if (matchingKey == null) {
                return ValidationResult.error("KEY_NOT_FOUND", 
                    "No key found for key ID: " + keyId);
            }

            ECDSAVerifier verifier = new ECDSAVerifier((ECPublicKey) matchingKey.toRSAKey().toPublicKey());
            
            if (!signedJWT.verify(verifier)) {
                return ValidationResult.error("INVALID_SIGNATURE", "JWT signature verification failed");
            }

        } catch (Exception e) {
            logger.error("JWT signature validation error", e);
            return ValidationResult.error("SIGNATURE_VALIDATION_ERROR", 
                "Error validating JWT signature: " + e.getMessage());
        }

        return ValidationResult.success();
    }

    /**
     * Validates JWT expiry and other claims
     */
    public ValidationResult validateJwtClaims(String jwt) {
        ValidationResult structureValidation = validateJwtStructure(jwt);
        if (!structureValidation.isValid()) {
            return structureValidation;
        }

        try {
            SignedJWT signedJWT = SignedJWT.parse(jwt);
            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

            // Check expiry
            Date expirationTime = claimsSet.getExpirationTime();
            if (expirationTime != null && expirationTime.before(new Date())) {
                return ValidationResult.error("JWT_EXPIRED", 
                    "JWT expired at: " + expirationTime);
            }

            // Check not before
            Date notBeforeTime = claimsSet.getNotBeforeTime();
            if (notBeforeTime != null && notBeforeTime.after(new Date())) {
                return ValidationResult.error("JWT_NOT_YET_VALID", 
                    "JWT is not valid until: " + notBeforeTime);
            }

            // Check issued at (should not be in future with significant skew)
            Date issuedAt = claimsSet.getIssueTime();
            if (issuedAt != null) {
                Instant now = Instant.now();
                Instant issuedInstant = issuedAt.toInstant();
                
                // Allow 5 minutes clock skew
                if (issuedInstant.isAfter(now.plusSeconds(300))) {
                    return ValidationResult.error("JWT_ISSUED_IN_FUTURE", 
                        "JWT issued time is too far in the future: " + issuedAt);
                }
            }

            // Validate required claims based on token type
            String issuer = claimsSet.getIssuer();
            if (issuer == null || issuer.trim().isEmpty()) {
                return ValidationResult.error("MISSING_ISSUER", "JWT missing issuer claim");
            }

            String subject = claimsSet.getSubject();
            if (subject == null || subject.trim().isEmpty()) {
                return ValidationResult.error("MISSING_SUBJECT", "JWT missing subject claim");
            }

        } catch (Exception e) {
            logger.error("JWT claims validation error", e);
            return ValidationResult.error("CLAIMS_VALIDATION_ERROR", 
                "Error validating JWT claims: " + e.getMessage());
        }

        return ValidationResult.success();
    }

    /**
     * Validates payment amount
     */
    public ValidationResult validateAmount(BigDecimal amount) {
        if (amount == null) {
            return ValidationResult.error("AMOUNT_REQUIRED", "Amount is required");
        }

        if (amount.compareTo(MIN_AMOUNT) < 0) {
            return ValidationResult.error("AMOUNT_TOO_SMALL", 
                "Amount must be at least " + MIN_AMOUNT);
        }

        if (amount.compareTo(MAX_AMOUNT) > 0) {
            return ValidationResult.error("AMOUNT_TOO_LARGE", 
                "Amount exceeds maximum of " + MAX_AMOUNT);
        }

        // Check for reasonable decimal places (max 2 for currency)
        if (amount.scale() > 2) {
            return ValidationResult.error("INVALID_DECIMAL_PLACES", 
                "Amount cannot have more than 2 decimal places");
        }

        return ValidationResult.success();
    }

    /**
     * Validates amount from string
     */
    public ValidationResult validateAmount(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            return ValidationResult.error("AMOUNT_REQUIRED", "Amount is required");
        }

        try {
            BigDecimal amount = new BigDecimal(amountStr);
            return validateAmount(amount);
        } catch (NumberFormatException e) {
            return ValidationResult.error("INVALID_AMOUNT_FORMAT", 
                "Amount must be a valid decimal number");
        }
    }

    /**
     * Validates timestamp is not in future (with reasonable tolerance)
     */
    public ValidationResult validateTimestamp(Instant timestamp) {
        if (timestamp == null) {
            return ValidationResult.error("TIMESTAMP_REQUIRED", "Timestamp is required");
        }

        Instant now = Instant.now();
        
        // Allow 5 minutes for clock skew
        if (timestamp.isAfter(now.plusSeconds(300))) {
            return ValidationResult.error("TIMESTAMP_IN_FUTURE", 
                "Timestamp cannot be more than 5 minutes in the future");
        }

        // Don't allow timestamps that are too old (more than 1 year)
        if (timestamp.isBefore(now.minusSeconds(31536000))) {
            return ValidationResult.error("TIMESTAMP_TOO_OLD", 
                "Timestamp is too old (more than 1 year)");
        }

        return ValidationResult.success();
    }

    /**
     * Validates timestamp from string
     */
    public ValidationResult validateTimestamp(String timestampStr) {
        if (timestampStr == null || timestampStr.trim().isEmpty()) {
            return ValidationResult.error("TIMESTAMP_REQUIRED", "Timestamp is required");
        }

        try {
            Instant timestamp = Instant.parse(timestampStr);
            return validateTimestamp(timestamp);
        } catch (Exception e) {
            return ValidationResult.error("INVALID_TIMESTAMP_FORMAT", 
                "Timestamp must be in ISO-8601 format");
        }
    }

    /**
     * Validates email format
     */
    public ValidationResult validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return ValidationResult.success(); // Email is optional
        }

        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        if (!email.matches(emailRegex)) {
            return ValidationResult.error("INVALID_EMAIL_FORMAT", 
                "Email must be in valid format");
        }

        if (email.length() > 254) {
            return ValidationResult.error("EMAIL_TOO_LONG", 
                "Email exceeds maximum length of 254 characters");
        }

        return ValidationResult.success();
    }

    /**
     * Validates phone number format
     */
    public ValidationResult validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return ValidationResult.success(); // Phone is optional
        }

        // Remove common formatting characters
        String cleanPhone = phone.replaceAll("[\\s\\-\\(\\)]", "");
        
        // Basic phone validation - digits only, reasonable length
        if (!cleanPhone.matches("^\\+?[1-9]\\d{6,14}$")) {
            return ValidationResult.error("INVALID_PHONE_FORMAT", 
                "Phone number must be in valid international format");
        }

        return ValidationResult.success();
    }

    /**
     * Validates currency code
     */
    public ValidationResult validateCurrency(String currency) {
        if (currency == null || currency.trim().isEmpty()) {
            return ValidationResult.error("CURRENCY_REQUIRED", "Currency code is required");
        }

        if (!currency.matches("^[A-Z]{3}$")) {
            return ValidationResult.error("INVALID_CURRENCY_CODE", 
                "Currency code must be 3 uppercase letters (ISO 4217)");
        }

        // List of common valid currencies (could be expanded)
        List<String> validCurrencies = List.of("USD", "EUR", "GBP", "JPY", "KRW", "CNY", "AUD", "CAD");
        if (!validCurrencies.contains(currency)) {
            return ValidationResult.error("UNSUPPORTED_CURRENCY", 
                "Currency code is not supported: " + currency);
        }

        return ValidationResult.success();
    }

    /**
     * Validates payment method
     */
    public ValidationResult validatePaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            return ValidationResult.success(); // Optional
        }

        List<String> validMethods = List.of("CREDIT_CARD", "DEBIT_CARD", "BANK_TRANSFER", 
                                           "DIGITAL_WALLET", "CRYPTOCURRENCY");
        
        if (!validMethods.contains(paymentMethod.toUpperCase())) {
            return ValidationResult.error("INVALID_PAYMENT_METHOD", 
                "Payment method is not supported: " + paymentMethod);
        }

        return ValidationResult.success();
    }

    /**
     * Validates KYC decision token format
     */
    public ValidationResult validateKycDecisionToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return ValidationResult.success(); // Optional
        }

        if (token.length() < 10 || token.length() > 1000) {
            return ValidationResult.error("INVALID_KYC_TOKEN_LENGTH", 
                "KYC decision token length must be between 10 and 1000 characters");
        }

        // Basic format validation - alphanumeric with some special chars
        if (!token.matches("^[A-Za-z0-9._-]+$")) {
            return ValidationResult.error("INVALID_KYC_TOKEN_FORMAT", 
                "KYC decision token contains invalid characters");
        }

        return ValidationResult.success();
    }

    /**
     * Comprehensive credential validation
     */
    public ValidationResult validateCredential(String did, String jwt, Instant timestamp) {
        ValidationResult didValidation = validateDid(did);
        if (!didValidation.isValid()) {
            return didValidation;
        }

        ValidationResult jwtValidation = validateJwtStructure(jwt);
        if (!jwtValidation.isValid()) {
            return jwtValidation;
        }

        ValidationResult timestampValidation = validateTimestamp(timestamp);
        if (!timestampValidation.isValid()) {
            return timestampValidation;
        }

        return ValidationResult.success();
    }

    /**
     * Comprehensive payment validation
     */
    public ValidationResult validatePayment(String payerDid, String payeeDid, 
                                          BigDecimal amount, String currency) {
        ValidationResult payerValidation = validateDid(payerDid);
        if (!payerValidation.isValid()) {
            return payerValidation;
        }

        ValidationResult payeeValidation = validateDid(payeeDid);
        if (!payeeValidation.isValid()) {
            return payeeValidation;
        }

        ValidationResult amountValidation = validateAmount(amount);
        if (!amountValidation.isValid()) {
            return amountValidation;
        }

        ValidationResult currencyValidation = validateCurrency(currency);
        if (!currencyValidation.isValid()) {
            return currencyValidation;
        }

        return ValidationResult.success();
    }

    /**
     * Result class for validation operations
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorCode;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorCode, String errorMessage) {
            this.valid = valid;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null, null);
        }

        public static ValidationResult error(String errorCode, String errorMessage) {
            return new ValidationResult(false, errorCode, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        @Override
        public String toString() {
            if (valid) {
                return "ValidationResult{valid=true}";
            } else {
                return "ValidationResult{valid=false, errorCode='" + errorCode + 
                       "', errorMessage='" + errorMessage + "'}";
            }
        }
    }
}
