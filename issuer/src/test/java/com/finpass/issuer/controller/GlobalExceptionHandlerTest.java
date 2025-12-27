package com.finpass.issuer.controller;

import com.finpass.issuer.dto.ErrorResponse;
import com.finpass.issuer.exception.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.request.WebRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for GlobalExceptionHandler
 */
@WebMvcTest(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;

    private HttpServletRequest mockHttpRequest;
    private WebRequest mockWebRequest;

    @BeforeEach
    void setUp() {
        mockHttpRequest = mock(HttpServletRequest.class);
        mockWebRequest = mock(WebRequest.class);
        
        when(mockHttpRequest.getRequestURI()).thenReturn("/test/path");
        when(mockHttpRequest.getMethod()).thenReturn("POST");
        when(mockHttpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(mockWebRequest.getDescription(false)).thenReturn("uri=/test/path");
    }

    // Validation Exception Tests
    @Test
    void testHandleValidationException() {
        ValidationException ex = new ValidationException("INVALID_DID", "DID format is invalid");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidationException(
            ex, mockWebRequest, mockHttpRequest);

        assertEquals(400, response.getStatusCodeValue());
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("INVALID_DID", errorResponse.getError());
        assertEquals("DID format is invalid", errorResponse.getErrorDescription());
        assertNotNull(errorResponse.getTimestamp());
        assertNotNull(errorResponse.getCorrelationId());
        assertEquals("/test/path", errorResponse.getPath());
    }

    // Authentication Exception Tests
    @Test
    void testHandleAuthenticationException() {
        AuthenticationException ex = new AuthenticationException("UNAUTHORIZED", "Authentication failed");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleAuthenticationException(
            ex, mockWebRequest, mockHttpRequest);

        assertEquals(401, response.getStatusCodeValue());
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("UNAUTHORIZED", errorResponse.getError());
        assertEquals("Authentication failed", errorResponse.getErrorDescription());
    }

    // Authorization Exception Tests
    @Test
    void testHandleAuthorizationException() {
        AuthorizationException ex = new AuthorizationException("FORBIDDEN", "Access denied");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleAuthorizationException(
            ex, mockWebRequest, mockHttpRequest);

        assertEquals(403, response.getStatusCodeValue());
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("FORBIDDEN", errorResponse.getError());
        assertEquals("Access denied", errorResponse.getErrorDescription());
    }

    // Resource Not Found Exception Tests
    @Test
    void testHandleResourceNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Credential", "cred-123");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleResourceNotFoundException(
            ex, mockWebRequest, mockHttpRequest);

        assertEquals(404, response.getStatusCodeValue());
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("RESOURCE_NOT_FOUND", errorResponse.getError());
        assertTrue(errorResponse.getErrorDescription().contains("Credential with ID 'cred-123' not found"));
        assertNotNull(errorResponse.getDetails());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> details = (Map<String, Object>) errorResponse.getDetails();
        assertEquals("Credential", details.get("resourceType"));
        assertEquals("cred-123", details.get("resourceId"));
    }

    // Resource Conflict Exception Tests
    @Test
    void testHandleResourceConflictException() {
        ResourceConflictException ex = new ResourceConflictException("Credential", "cred-123", "Duplicate credential");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleResourceConflictException(
            ex, mockWebRequest, mockHttpRequest);

        assertEquals(409, response.getStatusCodeValue());
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("RESOURCE_CONFLICT", errorResponse.getError());
        assertTrue(errorResponse.getErrorDescription().contains("Duplicate credential"));
        assertNotNull(errorResponse.getDetails());
    }

    // Business Rule Exception Tests
    @Test
    void testHandleBusinessRuleException() {
        BusinessRuleException ex = new BusinessRuleException("CREDENTIAL_REVOKED", "Credential has been revoked");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleBusinessRuleException(
            ex, mockWebRequest, mockHttpRequest);

        assertEquals(400, response.getStatusCodeValue());
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("CREDENTIAL_REVOKED", errorResponse.getError());
        assertEquals("Credential has been revoked", errorResponse.getErrorDescription());
    }

    // External Service Exception Tests
    @Test
    void testHandleExternalServiceException() {
        ExternalServiceException ex = new ExternalServiceException("Blockchain", "Service unavailable");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleExternalServiceException(
            ex, mockWebRequest, mockHttpRequest);

        assertEquals(502, response.getStatusCodeValue());
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("EXTERNAL_SERVICE_ERROR", errorResponse.getError());
        assertTrue(errorResponse.getErrorDescription().contains("Blockchain"));
        assertNotNull(errorResponse.getDetails());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> details = (Map<String, Object>) errorResponse.getDetails();
        assertEquals("Blockchain", details.get("serviceName"));
    }

    // Constraint Violation Exception Tests
    @Test
    void testHandleConstraintViolationException() {
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("Field must not be null");
        when(violation.getPropertyPath()).thenReturn(mock(javax.validation.Path.class));
        when(violation.getInvalidValue()).thenReturn(null);
        violations.add(violation);

        ConstraintViolationException ex = new ConstraintViolationException(violations);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleConstraintViolationException(
            ex, mockWebRequest, mockHttpRequest);

        assertEquals(400, response.getStatusCodeValue());
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("CONSTRAINT_VIOLATION", errorResponse.getError());
        assertTrue(errorResponse.getErrorDescription().contains("Field must not be null"));
        assertNotNull(errorResponse.getDetails());
    }

    // Illegal Argument Exception Tests
    @Test
    void testHandleIllegalArgumentException() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument provided");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleIllegalArgumentException(
            ex, mockWebRequest, mockHttpRequest);

        assertEquals(400, response.getStatusCodeValue());
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("INVALID_ARGUMENT", errorResponse.getError());
        assertEquals("Invalid argument provided", errorResponse.getErrorDescription());
    }

    // Illegal State Exception Tests
    @Test
    void testHandleIllegalStateException() {
        IllegalStateException ex = new IllegalStateException("Invalid state for operation");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleIllegalStateException(
            ex, mockWebRequest, mockHttpRequest);

        assertEquals(409, response.getStatusCodeValue());
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("INVALID_STATE", errorResponse.getError());
        assertEquals("Invalid state for operation", errorResponse.getErrorDescription());
    }

    // Generic Exception Tests
    @Test
    void testHandleGenericException() {
        RuntimeException ex = new RuntimeException("Unexpected error");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGenericException(
            ex, mockWebRequest, mockHttpRequest);

        assertEquals(500, response.getStatusCodeValue());
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("INTERNAL_SERVER_ERROR", errorResponse.getError());
        assertEquals("An unexpected error occurred. Please try again later.", errorResponse.getErrorDescription());
        
        // In production, details should not be included for security
        assertNull(errorResponse.getDetails());
    }

    // ErrorResponse Factory Method Tests
    @Test
    void testErrorResponseFactoryMethods() {
        ErrorResponse badRequest = ErrorResponse.badRequest("Bad request");
        assertEquals("BAD_REQUEST", badRequest.getError());
        assertEquals("Bad request", badRequest.getErrorDescription());
        assertNotNull(badRequest.getTimestamp());

        ErrorResponse unauthorized = ErrorResponse.unauthorized("Unauthorized");
        assertEquals("UNAUTHORIZED", unauthorized.getError());

        ErrorResponse forbidden = ErrorResponse.forbidden("Forbidden");
        assertEquals("FORBIDDEN", forbidden.getError());

        ErrorResponse notFound = ErrorResponse.notFound("Not found");
        assertEquals("NOT_FOUND", notFound.getError());

        ErrorResponse conflict = ErrorResponse.conflict("Conflict");
        assertEquals("CONFLICT", conflict.getError());

        ErrorResponse gone = ErrorResponse.gone("Gone");
        assertEquals("GONE", gone.getError());

        ErrorResponse internalServerError = ErrorResponse.internalServerError("Internal error");
        assertEquals("INTERNAL_SERVER_ERROR", internalServerError.getError());

        ErrorResponse serviceUnavailable = ErrorResponse.serviceUnavailable("Service unavailable");
        assertEquals("SERVICE_UNAVAILABLE", serviceUnavailable.getError());
    }

    // Correlation ID Tests
    @Test
    void testCorrelationIdGeneration() {
        ValidationException ex1 = new ValidationException("TEST_ERROR", "Test error");
        ValidationException ex2 = new ValidationException("TEST_ERROR", "Test error");

        ResponseEntity<ErrorResponse> response1 = globalExceptionHandler.handleValidationException(
            ex1, mockWebRequest, mockHttpRequest);
        ResponseEntity<ErrorResponse> response2 = globalExceptionHandler.handleValidationException(
            ex2, mockWebRequest, mockHttpRequest);

        ErrorResponse errorResponse1 = response1.getBody();
        ErrorResponse errorResponse2 = response2.getBody();

        assertNotNull(errorResponse1.getCorrelationId());
        assertNotNull(errorResponse2.getCorrelationId());
        assertNotEquals(errorResponse1.getCorrelationId(), errorResponse2.getCorrelationId());
        
        // Correlation ID should be 16 characters
        assertEquals(16, errorResponse1.getCorrelationId().length());
        assertEquals(16, errorResponse2.getCorrelationId().length());
    }

    // Timestamp Tests
    @Test
    void testTimestampGeneration() {
        ValidationException ex = new ValidationException("TEST_ERROR", "Test error");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidationException(
            ex, mockWebRequest, mockHttpRequest);

        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse.getTimestamp());
        
        // Timestamp should be recent (within 1 second)
        Instant now = Instant.now();
        Instant timestamp = errorResponse.getTimestamp();
        assertTrue(Math.abs(timestamp.toEpochMilli() - now.toEpochMilli()) < 1000);
    }

    // JSON Serialization Tests
    @Test
    void testErrorResponseJsonSerialization() throws Exception {
        ErrorResponse errorResponse = new ErrorResponse("TEST_ERROR", "Test message", "corr-123", "/test/path");
        
        String json = objectMapper.writeValueAsString(errorResponse);
        
        assertTrue(json.contains("\"error\":\"TEST_ERROR\""));
        assertTrue(json.contains("\"error_description\":\"Test message\""));
        assertTrue(json.contains("\"correlation_id\":\"corr-123\""));
        assertTrue(json.contains("\"path\":\"/test/path\""));
        assertTrue(json.contains("\"timestamp\""));
    }

    // Error Response toString Tests
    @Test
    void testErrorResponseToString() {
        ErrorResponse errorResponse = new ErrorResponse("TEST_ERROR", "Test message", "corr-123");
        String toString = errorResponse.toString();
        
        assertTrue(toString.contains("error='TEST_ERROR'"));
        assertTrue(toString.contains("errorDescription='Test message'"));
        assertTrue(toString.contains("correlationId='corr-123'"));
    }

    // Edge Cases
    @Test
    void testHandleExceptionWithNullMessage() {
        RuntimeException ex = new RuntimeException((String) null);

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGenericException(
            ex, mockWebRequest, mockHttpRequest);

        assertEquals(500, response.getStatusCodeValue());
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("INTERNAL_SERVER_ERROR", errorResponse.getError());
        assertEquals("An unexpected error occurred. Please try again later.", errorResponse.getErrorDescription());
    }

    @Test
    void testHandleExceptionWithEmptyMessage() {
        RuntimeException ex = new RuntimeException("");

        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGenericException(
            ex, mockWebRequest, mockHttpRequest);

        assertEquals(500, response.getStatusCodeValue());
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("INTERNAL_SERVER_ERROR", errorResponse.getError());
    }

    // MockMvc Integration Tests
    @Test
    void testMvcErrorHandling() throws Exception {
        // Test that the error handling works through the MVC layer
        mockMvc.perform(get("/non-existent-endpoint")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.error_description").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.correlation_id").exists());
    }

    @Test
    void testMvcBadRequest() throws Exception {
        // Test bad request handling
        mockMvc.perform(post("/test-endpoint")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_JSON"));
    }
}
