package com.finpass.issuer.documentation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for API documentation and OpenAPI specification
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class ApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testOpenApiSpecAvailable() throws Exception {
        mockMvc.perform(get("/api-docs")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.openapi", is("3.0.3")))
                .andExpect(jsonPath("$.info.title", is("FinPass API")))
                .andExpect(jsonPath("$.info.version", is("1.0.0")))
                .andExpect(jsonPath("$.servers", hasSize(3)))
                .andExpect(jsonPath("$.components.securitySchemes", hasKey("BearerAuth")));
    }

    @Test
    void testSwaggerUiAvailable() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    void testSwaggerUiResources() throws Exception {
        mockMvc.perform(get("/swagger-ui/swagger-ui.css"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/css"));

        mockMvc.perform(get("/swagger-ui/swagger-ui-bundle.js"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/javascript"));
    }

    @Test
    void testOpenApiSpecContainsIssuerEndpoints() throws Exception {
        MvcResult result = mockMvc.perform(get("/api-docs")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        
        // Verify issuer endpoints are documented
        assertThat(content).contains("/api/issuer/credentials");
        assertThat(content).contains("issueCredential");
        assertThat(content).contains("getCredential");
        assertThat(content).contains("revokeCredential");
        
        // Verify request/response schemas
        assertThat(content).contains("CredentialRequest");
        assertThat(content).contains("CredentialResponse");
        assertThat(content).contains("LivenessProof");
    }

    @Test
    void testOpenApiSpecContainsVerifierEndpoints() throws Exception {
        MvcResult result = mockMvc.perform(get("/api-docs")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        
        // Verify verifier endpoints are documented
        assertThat(content).contains("/api/verifier/verify");
        assertThat(content).contains("verifyCredential");
        assertThat(content).contains("VerificationRequest");
        assertThat(content).contains("VerificationResponse");
    }

    @Test
    void testOpenApiSpecContainsPaymentEndpoints() throws Exception {
        MvcResult result = mockMvc.perform(get("/api-docs")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        
        // Verify payment endpoints are documented
        assertThat(content).contains("/api/payments");
        assertThat(content).contains("initiatePayment");
        assertThat(content).contains("getPayment");
        assertThat(content).contains("PaymentRequest");
        assertThat(content).contains("PaymentResponse");
    }

    @Test
    void testOpenApiSpecContainsAuditEndpoints() throws Exception {
        MvcResult result = mockMvc.perform(get("/api-docs")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        
        // Verify audit endpoints are documented
        assertThat(content).contains("/api/audit/events");
        assertThat(content).contains("/api/audit/metrics");
        assertThat(content).contains("getAuditEvents");
        assertThat(content).contains("getAuditMetrics");
        assertThat(content).contains("AuditEvent");
        assertThat(content).contains("PaginatedResponse");
    }

    @Test
    void testOpenApiSpecContainsErrorResponses() throws Exception {
        MvcResult result = mockMvc.perform(get("/api-docs")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        
        // Verify error response schemas are documented
        assertThat(content).contains("ErrorResponse");
        assertThat(content).contains("BadRequest");
        assertThat(content).contains("Unauthorized");
        assertThat(content).contains("Forbidden");
        assertThat(content).contains("NotFound");
        assertThat(content).contains("TooManyRequests");
        assertThat(content).contains("InternalServerError");
    }

    @Test
    void testOpenApiSpecContainsSecuritySchemes() throws Exception {
        mockMvc.perform(get("/api-docs")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.BearerAuth.type", is("http")))
                .andExpect(jsonPath("$.components.securitySchemes.BearerAuth.scheme", is("bearer")))
                .andExpect(jsonPath("$.components.securitySchemes.BearerAuth.bearerFormat", is("JWT")))
                .andExpect(jsonPath("$.security", hasSize(1)))
                .andExpect(jsonPath("$.security[0].BearerAuth", hasSize(0)));
    }

    @Test
    void testOpenApiSpecContainsTags() throws Exception {
        mockMvc.perform(get("/api-docs")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags", hasSize(4)))
                .andExpect(jsonPath("$.tags[*].name", hasItems("Issuer", "Verifier", "Payment", "Audit")));
    }

    @Test
    void testOpenApiSpecContainsExamples() throws Exception {
        MvcResult result = mockMvc.perform(get("/api-docs")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        
        // Verify examples are included
        assertThat(content).contains("example");
        assertThat(content).contains("did:example:");
        assertThat(content).contains("PASSPORT");
        assertThat(content).contains("IDENTITY_VERIFICATION");
    }

    @Test
    void testOpenApiSpecValidation() throws Exception {
        mockMvc.perform(get("/api-docs")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi", matchesPattern("^3\\.0\\.[0-9]+$")))
                .andExpect(jsonPath("$.info.title", not(emptyString())))
                .andExpect(jsonPath("$.info.version", not(emptyString())))
                .andExpect(jsonPath("$.servers", not(empty())))
                .andExpect(jsonPath("$.paths", not(empty())))
                .andExpect(jsonPath("$.components", not(empty())));
    }

    @Test
    void testOpenApiSpecGroupConfiguration() throws Exception {
        mockMvc.perform(get("/api-docs")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/issuer/credentials']", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/verifier/verify']", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/payments']", notNullValue()))
                .andExpect(jsonPath("$.paths['/api/audit/events']", notNullValue()));
    }

    @Test
    void testOpenApiSpecResponseFormats() throws Exception {
        mockMvc.perform(get("/api-docs")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths[*][*].responses[*].content['application/json']", not(empty())));
    }

    @Test
    void testOpenApiSpecParameterValidation() throws Exception {
        MvcResult result = mockMvc.perform(get("/api-docs")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        
        // Verify path parameters are properly documented
        assertThat(content).contains("credentialId");
        assertThat(content).contains("paymentId");
        assertThat(content).contains("policyId");
        
        // Verify parameter types and formats
        assertThat(content).contains("uuid");
        assertThat(content).contains("date-time");
    }

    @Test
    void testOpenApiSpecSchemaValidation() throws Exception {
        mockMvc.perform(get("/api-docs")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas", hasSize(greaterThan(10))))
                .andExpect(jsonPath("$.components.schemas.ErrorResponse", notNullValue()))
                .andExpect(jsonPath("$.components.schemas.CredentialRequest", notNullValue()))
                .andExpect(jsonPath("$.components.schemas.VerificationRequest", notNullValue()))
                .andExpect(jsonPath("$.components.schemas.PaymentRequest", notNullValue()))
                .andExpect(jsonPath("$.components.schemas.AuditEvent", notNullValue()));
    }

    @Test
    void testOpenApiSpecServerConfiguration() throws Exception {
        mockMvc.perform(get("/api-docs")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servers", hasSize(3)))
                .andExpect(jsonPath("$.servers[0].url", is("https://api.finpass.io")))
                .andExpect(jsonPath("$.servers[0].description", is("Production server")))
                .andExpect(jsonPath("$.servers[1].url", is("https://staging-api.finpass.io")))
                .andExpect(jsonPath("$.servers[1].description", is("Staging server")))
                .andExpect(jsonPath("$.servers[2].url", is("http://localhost:8080")))
                .andExpect(jsonPath("$.servers[2].description", is("Development server")));
    }

    @Test
    void testOpenApiSpecContactAndLicense() throws Exception {
        mockMvc.perform(get("/api-docs")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.contact.name", is("FinPass API Team")))
                .andExpect(jsonPath("$.info.contact.email", is("api-support@finpass.io")))
                .andExpect(jsonPath("$.info.contact.url", is("https://finpass.io/support")))
                .andExpect(jsonPath("$.info.license.name", is("MIT")))
                .andExpect(jsonPath("$.info.license.url", is("https://opensource.org/licenses/MIT")));
    }

    @Test
    void testOpenApiSpecDescriptionContent() throws Exception {
        MvcResult result = mockMvc.perform(get("/api-docs")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        
        // Verify comprehensive description
        assertThat(content).contains("FinPass");
        assertThat(content).contains("decentralized identity");
        assertThat(content).contains("blockchain technology");
        assertThat(content).contains("verifiable credentials");
        assertThat(content).contains("payment processing");
        assertThat(content).contains("audit logging");
        
        // Verify authentication documentation
        assertThat(content).contains("JWT-based authentication");
        assertThat(content).contains("Authorization: Bearer");
        
        // Verify feature documentation
        assertThat(content).contains("DID-based");
        assertThat(content).contains("W3C-compliant");
        assertThat(content).contains("blockchain-based");
    }

    @Test
    void testSwaggerUiCustomization() throws Exception {
        // Test that Swagger UI is properly configured
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(xpath("//title").string("Swagger UI"));
    }

    @Test
    void testApiDocsContentType() throws Exception {
        mockMvc.perform(get("/api-docs.yaml")
                .accept("application/vnd.oai.openapi"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.oai.openapi"));

        mockMvc.perform(get("/api-docs")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void testOpenApiSpecCompleteness() throws Exception {
        MvcResult result = mockMvc.perform(get("/api-docs")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        
        // Verify all major components are present
        assertThat(content).contains("openapi");
        assertThat(content).contains("info");
        assertThat(content).contains("servers");
        assertThat(content).contains("paths");
        assertThat(content).contains("components");
        assertThat(content).contains("security");
        assertThat(content).contains("tags");
        
        // Verify all service groups are documented
        assertThat(content).contains("Issuer");
        assertThat(content).contains("Verifier");
        assertThat(content).contains("Payment");
        assertThat(content).contains("Audit");
        
        // Verify error handling is documented
        assertThat(content).contains("ErrorResponse");
        assertThat(content).contains("error");
        assertThat(content).contains("error_description");
        assertThat(content).contains("correlation_id");
    }

    @Test
    void testOpenApiSpecResponseExamples() throws Exception {
        MvcResult result = mockMvc.perform(get("/api-docs")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        
        // Verify response examples are included
        assertThat(content).contains("example");
        assertThat(content).contains("credentialJwt");
        assertThat(content).contains("verificationScore");
        assertThat(content).contains("transactionId");
        assertThat(content).contains("eventId");
        
        // Verify error response examples
        assertThat(content).contains("BAD_REQUEST");
        assertThat(content).contains("UNAUTHORIZED");
        assertThat(content).contains("INTERNAL_SERVER_ERROR");
    }

    @Test
    void testOpenApiSpecDataTypes() throws Exception {
        mockMvc.perform(get("/api-docs")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.Did.type", is("string")))
                .andExpect(jsonPath("$.components.schemas.Did.pattern", is("^did:[a-z0-9]+:[a-zA-Z0-9._-]+$")))
                .andExpect(jsonPath("$.components.schemas.CredentialResponse.credentialId.format", is("uuid")))
                .andExpect(jsonPath("$.components.schemas.PaymentRequest.amount.type", is("number")))
                .andExpect(jsonPath("$.components.schemas.PaymentRequest.amount.format", is("decimal")))
                .andExpect(jsonPath("$.components.schemas.AuditEvent.timestamp.format", is("date-time")));
    }

    @Test
    void testOpenApiSpecRequiredFields() throws Exception {
        mockMvc.perform(get("/api-docs")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.CredentialRequest.required", hasItems("holderDid", "credentialType", "credentialData")))
                .andExpect(jsonPath("$.components.schemas.VerificationRequest.required", hasItems("credentialJwt", "verifierDid", "verificationType")))
                .andExpect(jsonPath("$.components.schemas.PaymentRequest.required", hasItems("payerDid", "payeeDid", "amount", "currency")))
                .andExpect(jsonPath("$.components.schemas.ErrorResponse.required", hasItems("error", "error_description", "timestamp")));
    }

    @Test
    void testOpenApiSpecEnumValues() throws Exception {
        mockMvc.perform(get("/api-docs")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.CredentialRequest.properties.credentialType.enum", 
                    hasItems("PASSPORT", "DRIVERS_LICENSE", "NATIONAL_ID", "RESIDENCE_PERMIT")))
                .andExpect(jsonPath("$.components.schemas.VerificationRequest.properties.verificationType.enum", 
                    hasItems("AGE_VERIFICATION", "IDENTITY_VERIFICATION", "ADDRESS_VERIFICATION", "CUSTOM")))
                .andExpect(jsonPath("$.components.schemas.PaymentRequest.properties.paymentMethod.enum", 
                    hasItems("CREDIT_CARD", "DEBIT_CARD", "BANK_TRANSFER", "DIGITAL_WALLET", "CRYPTOCURRENCY")));
    }

    @Test
    void testOpenApiSpecPaginationSchema() throws Exception {
        mockMvc.perform(get("/api-docs")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.PaginatedResponse.required", 
                    hasItems("content", "page", "size", "totalElements", "totalPages")))
                .andExpect(jsonPath("$.components.schemas.PaginatedResponse.properties.page.type", is("integer")))
                .andExpect(jsonPath("$.components.schemas.PaginatedResponse.properties.page.minimum", is(0)))
                .andExpect(jsonPath("$.components.schemas.PaginatedResponse.properties.size.type", is("integer")))
                .andExpect(jsonPath("$.components.schemas.PaginatedResponse.properties.size.minimum", is(1)))
                .andExpect(jsonPath("$.components.schemas.PaginatedResponse.properties.size.maximum", is(100)));
    }
}
