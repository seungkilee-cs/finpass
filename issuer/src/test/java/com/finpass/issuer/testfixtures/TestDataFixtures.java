package com.finpass.issuer.testfixtures;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Test data fixtures for integration tests
 */
@Component
public class TestDataFixtures {

    private final Random random = new Random();

    /**
     * Creates a test user with valid DID
     */
    public TestUser createTestUser() {
        String userId = "user" + System.currentTimeMillis() + random.nextInt(1000);
        return TestUser.builder()
                .did("did:example:" + userId)
                .email(userId + "@test.com")
                .name("Test User " + userId)
                .phoneNumber("+1-555-" + String.format("%07d", random.nextInt(10000000)))
                .dateOfBirth("1990-01-01")
                .nationality("US")
                .address("123 Test St, Test City, TC 12345")
                .build();
    }

    /**
     * Creates a test issuer (trusted entity)
     */
    public TestIssuer createTestIssuer() {
        String issuerId = "issuer" + System.currentTimeMillis() + random.nextInt(1000);
        return TestIssuer.builder()
                .did("did:example:" + issuerId)
                .name("Test Issuer " + issuerId)
                .email(issuerId + "@issuer.test")
                .trusted(true)
                .establishedDate("2020-01-01")
                .jurisdiction("US")
                .build();
    }

    /**
     * Creates a test verifier
     */
    public TestVerifier createTestVerifier() {
        String verifierId = "verifier" + System.currentTimeMillis() + random.nextInt(1000);
        return TestVerifier.builder()
                .did("did:example:" + verifierId)
                .name("Test Verifier " + verifierId)
                .email(verifierId + "@verifier.test")
                .verificationTypes(Arrays.asList("IDENTITY_VERIFICATION", "AGE_VERIFICATION", "ADDRESS_VERIFICATION"))
                .build();
    }

    /**
     * Creates a test passport credential request
     */
    public Map<String, Object> createPassportCredentialRequest(TestUser user) {
        Map<String, Object> request = new HashMap<>();
        request.put("holderDid", user.getDid());
        request.put("credentialType", "PASSPORT");
        
        Map<String, Object> credentialData = new HashMap<>();
        credentialData.put("passportNumber", "P" + String.format("%09d", random.nextInt(1000000000)));
        credentialData.put("fullName", user.getName());
        credentialData.put("dateOfBirth", user.getDateOfBirth());
        credentialData.put("nationality", user.getNationality());
        credentialData.put("issuingCountry", "US");
        credentialData.put("expirationDate", "2030-12-31");
        credentialData.put("placeOfBirth", "Test City, US");
        credentialData.put("sex", "M");
        
        request.put("credentialData", credentialData);
        request.put("livenessProof", createLivenessProof());
        
        return request;
    }

    /**
     * Creates a test driver's license credential request
     */
    public Map<String, Object> createDriversLicenseCredentialRequest(TestUser user) {
        Map<String, Object> request = new HashMap<>();
        request.put("holderDid", user.getDid());
        request.put("credentialType", "DRIVERS_LICENSE");
        
        Map<String, Object> credentialData = new HashMap<>();
        credentialData.put("licenseNumber", "DL" + String.format("%09d", random.nextInt(1000000000)));
        credentialData.put("fullName", user.getName());
        credentialData.put("dateOfBirth", user.getDateOfBirth());
        credentialData.put("nationality", user.getNationality());
        credentialData.put("issuingState", "CA");
        credentialData.put("expirationDate", "2028-12-31");
        credentialData.put("address", user.getAddress());
        credentialData.put("vehicleClass", "C");
        credentialData.put("sex", "M");
        
        request.put("credentialData", credentialData);
        request.put("livenessProof", createLivenessProof());
        
        return request;
    }

    /**
     * Creates a test national ID credential request
     */
    public Map<String, Object> createNationalIdCredentialRequest(TestUser user) {
        Map<String, Object> request = new HashMap<>();
        request.put("holderDid", user.getDid());
        request.put("credentialType", "NATIONAL_ID");
        
        Map<String, Object> credentialData = new HashMap<>();
        credentialData.put("nationalIdNumber", "NID" + String.format("%09d", random.nextInt(1000000000)));
        credentialData.put("fullName", user.getName());
        credentialData.put("dateOfBirth", user.getDateOfBirth());
        credentialData.put("nationality", user.getNationality());
        credentialData.put("issuingAuthority", "Social Security Administration");
        credentialData.put("expirationDate", "2035-12-31");
        credentialData.put("address", user.getAddress());
        credentialData.put("sex", "M");
        
        request.put("credentialData", credentialData);
        request.put("livenessProof", createLivenessProof());
        
        return request;
    }

    /**
     * Creates a test residence permit credential request
     */
    public Map<String, Object> createResidencePermitCredentialRequest(TestUser user) {
        Map<String, Object> request = new HashMap<>();
        request.put("holderDid", user.getDid());
        request.put("credentialType", "RESIDENCE_PERMIT");
        
        Map<String, Object> credentialData = new HashMap<>();
        credentialData.put("permitNumber", "RP" + String.format("%09d", random.nextInt(1000000000)));
        credentialData.put("fullName", user.getName());
        credentialData.put("dateOfBirth", user.getDateOfBirth());
        credentialData.put("nationality", user.getNationality());
        credentialData.put("issuingCountry", "US");
        credentialData.put("expirationDate", "2025-12-31");
        credentialData.put("address", user.getAddress());
        credentialData.put("permitType", "TEMPORARY");
        
        request.put("credentialData", credentialData);
        request.put("livenessProof", createLivenessProof());
        
        return request;
    }

    /**
     * Creates a verification request
     */
    public Map<String, Object> createVerificationRequest(String credentialJwt, TestVerifier verifier) {
        Map<String, Object> request = new HashMap<>();
        request.put("credentialJwt", credentialJwt);
        request.put("verifierDid", verifier.getDid());
        request.put("verificationType", "IDENTITY_VERIFICATION");
        
        Map<String, Object> verificationData = new HashMap<>();
        verificationData.put("minimumAge", 18);
        verificationData.put("requiredFields", Arrays.asList("fullName", "dateOfBirth"));
        
        request.put("verificationData", verificationData);
        
        return request;
    }

    /**
     * Creates a payment request
     */
    public Map<String, Object> createPaymentRequest(TestUser payer, TestUser payee, BigDecimal amount) {
        Map<String, Object> request = new HashMap<>();
        request.put("payerDid", payer.getDid());
        request.put("payeeDid", payee.getDid());
        request.put("amount", amount);
        request.put("currency", "USD");
        request.put("paymentMethod", "BANK_TRANSFER");
        request.put("description", "Test payment for " + amount + " USD");
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("invoiceId", "INV-" + System.currentTimeMillis());
        metadata.put("orderId", "ORDER-" + System.currentTimeMillis());
        
        request.put("metadata", metadata);
        
        return request;
    }

    /**
     * Creates a liveness proof for biometric verification
     */
    private Map<String, Object> createLivenessProof() {
        Map<String, Object> livenessProof = new HashMap<>();
        livenessProof.put("score", 0.90 + random.nextDouble() * 0.09); // 0.90 to 0.99
        livenessProof.put("timestamp", Instant.now().toString());
        livenessProof.put("proofData", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c");
        livenessProof.put("biometricType", "FACE");
        livenessProof.put("deviceFingerprint", "fp_" + UUID.randomUUID().toString());
        
        return livenessProof;
    }

    /**
     * Creates a verification challenge request
     */
    public Map<String, Object> createVerificationChallengeRequest(TestVerifier verifier, List<String> requestedCredentials) {
        Map<String, Object> request = new HashMap<>();
        request.put("verifierDid", verifier.getDid());
        request.put("challengeType", "PRESENTATION");
        request.put("requestedCredentials", requestedCredentials);
        request.put("expirationTime", Instant.now().plusSeconds(3600).toString()); // 1 hour from now
        
        return request;
    }

    /**
     * Creates an expired credential request (for error testing)
     */
    public Map<String, Object> createExpiredCredentialRequest(TestUser user) {
        Map<String, Object> request = createPassportCredentialRequest(user);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> credentialData = (Map<String, Object>) request.get("credentialData");
        credentialData.put("expirationDate", "2020-01-01"); // Expired date
        
        return request;
    }

    /**
     * Creates a credential request with invalid DID format (for error testing)
     */
    public Map<String, Object> createInvalidDidCredentialRequest() {
        Map<String, Object> request = new HashMap<>();
        request.put("holderDid", "invalid-did-format"); // Invalid DID format
        request.put("credentialType", "PASSPORT");
        
        Map<String, Object> credentialData = new HashMap<>();
        credentialData.put("passportNumber", "P123456789");
        credentialData.put("fullName", "John Doe");
        
        request.put("credentialData", credentialData);
        request.put("livenessProof", createLivenessProof());
        
        return request;
    }

    /**
     * Creates a payment request with insufficient balance (for error testing)
     */
    public Map<String, Object> createInsufficientBalancePaymentRequest(TestUser payer, TestUser payee) {
        Map<String, Object> request = createPaymentRequest(payer, payee, new BigDecimal("999999.99"));
        request.put("description", "Large payment that should fail due to insufficient balance");
        
        return request;
    }

    /**
     * Creates a batch of test users
     */
    public List<TestUser> createTestUsers(int count) {
        List<TestUser> users = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            users.add(createTestUser());
        }
        return users;
    }

    /**
     * Creates a batch of credential requests for a user
     */
    public List<Map<String, Object>> createAllCredentialTypesForUser(TestUser user) {
        return Arrays.asList(
            createPassportCredentialRequest(user),
            createDriversLicenseCredentialRequest(user),
            createNationalIdCredentialRequest(user),
            createResidencePermitCredentialRequest(user)
        );
    }

    // Inner classes for test data structures
    public static class TestUser {
        private String did;
        private String email;
        private String name;
        private String phoneNumber;
        private String dateOfBirth;
        private String nationality;
        private String address;

        public static Builder builder() {
            return new Builder();
        }

        // Getters and setters
        public String getDid() { return did; }
        public void setDid(String did) { this.did = did; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
        public String getDateOfBirth() { return dateOfBirth; }
        public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
        public String getNationality() { return nationality; }
        public void setNationality(String nationality) { this.nationality = nationality; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }

        public static class Builder {
            private TestUser user = new TestUser();
            
            public Builder did(String did) { user.did = did; return this; }
            public Builder email(String email) { user.email = email; return this; }
            public Builder name(String name) { user.name = name; return this; }
            public Builder phoneNumber(String phoneNumber) { user.phoneNumber = phoneNumber; return this; }
            public Builder dateOfBirth(String dateOfBirth) { user.dateOfBirth = dateOfBirth; return this; }
            public Builder nationality(String nationality) { user.nationality = nationality; return this; }
            public Builder address(String address) { user.address = address; return this; }
            
            public TestUser build() { return user; }
        }
    }

    public static class TestIssuer {
        private String did;
        private String name;
        private String email;
        private boolean trusted;
        private String establishedDate;
        private String jurisdiction;

        public static Builder builder() {
            return new Builder();
        }

        // Getters and setters
        public String getDid() { return did; }
        public void setDid(String did) { this.did = did; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public boolean isTrusted() { return trusted; }
        public void setTrusted(boolean trusted) { this.trusted = trusted; }
        public String getEstablishedDate() { return establishedDate; }
        public void setEstablishedDate(String establishedDate) { this.establishedDate = establishedDate; }
        public String getJurisdiction() { return jurisdiction; }
        public void setJurisdiction(String jurisdiction) { this.jurisdiction = jurisdiction; }

        public static class Builder {
            private TestIssuer issuer = new TestIssuer();
            
            public Builder did(String did) { issuer.did = did; return this; }
            public Builder name(String name) { issuer.name = name; return this; }
            public Builder email(String email) { issuer.email = email; return this; }
            public Builder trusted(boolean trusted) { issuer.trusted = trusted; return this; }
            public Builder establishedDate(String establishedDate) { issuer.establishedDate = establishedDate; return this; }
            public Builder jurisdiction(String jurisdiction) { issuer.jurisdiction = jurisdiction; return this; }
            
            public TestIssuer build() { return issuer; }
        }
    }

    public static class TestVerifier {
        private String did;
        private String name;
        private String email;
        private List<String> verificationTypes;

        public static Builder builder() {
            return new Builder();
        }

        // Getters and setters
        public String getDid() { return did; }
        public void setDid(String did) { this.did = did; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public List<String> getVerificationTypes() { return verificationTypes; }
        public void setVerificationTypes(List<String> verificationTypes) { this.verificationTypes = verificationTypes; }

        public static class Builder {
            private TestVerifier verifier = new TestVerifier();
            
            public Builder did(String did) { verifier.did = did; return this; }
            public Builder name(String name) { verifier.name = name; return this; }
            public Builder email(String email) { verifier.email = email; return this; }
            public Builder verificationTypes(List<String> verificationTypes) { verifier.verificationTypes = verificationTypes; return this; }
            
            public TestVerifier build() { return verifier; }
        }
    }
}
