package com.finpass.issuer.service;

import com.finpass.issuer.entity.AuditEventEntity;
import com.finpass.issuer.repository.AuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for AuditService
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AuditService auditService;

    private String testUserId;
    private String testCredentialId;
    private String testPaymentId;

    @BeforeEach
    void setUp() {
        testUserId = "did:example:123456789abcdefghi";
        testCredentialId = UUID.randomUUID().toString();
        testPaymentId = UUID.randomUUID().toString();
    }

    @Test
    void testLogEvent_Basic() {
        // Given
        AuditEventEntity.EventType eventType = AuditEventEntity.EventType.CREDENTIAL_ISSUED;
        Map<String, Object> details = Map.of("test", "value");

        // When
        auditService.logEvent(eventType, testUserId, details);

        // Then
        ArgumentCaptor<AuditEventEntity> eventCaptor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(auditEventRepository).save(eventCaptor.capture());

        AuditEventEntity savedEvent = eventCaptor.getValue();
        assertEquals(eventType.name(), savedEvent.getEventType());
        assertNotNull(savedEvent.getUserIdHash());
        assertEquals("value", savedEvent.getDetails());
        assertEquals("SUCCESS", savedEvent.getOutcome());
        assertEquals("INFO", savedEvent.getSeverity());
    }

    @Test
    void testLogEvent_WithRequest() {
        // Given
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(request.getHeader("User-Agent")).thenReturn("Test-Agent");
        when(request.getSession()).thenReturn(mock(jakarta.servlet.http.HttpSession.class));
        when(request.getSession().getId()).thenReturn("session123");
        when(request.getHeader("X-Request-ID")).thenReturn("req-123");

        AuditEventEntity.EventType eventType = AuditEventEntity.EventType.USER_LOGIN;
        Map<String, Object> details = Map.of("action", "LOGIN");

        // When
        auditService.logEvent(eventType, testUserId, details, request);

        // Then
        ArgumentCaptor<AuditEventEntity> eventCaptor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(auditEventRepository).save(eventCaptor.capture());

        AuditEventEntity savedEvent = eventCaptor.getValue();
        assertEquals("192.168.1.1", savedEvent.getIpAddress());
        assertEquals("Test-Agent", savedEvent.getUserAgent());
        assertEquals("session123", savedEvent.getSessionId());
        assertEquals("req-123", savedEvent.getRequestId());
    }

    @Test
    void testLogEvent_WithDuration() {
        // Given
        AuditEventEntity.EventType eventType = AuditEventEntity.EventType.PAYMENT_COMPLETED;
        Map<String, Object> details = Map.of("amount", "100.00");
        Long duration = 1500L;

        // When
        auditService.logEvent(eventType, testUserId, details, duration);

        // Then
        ArgumentCaptor<AuditEventEntity> eventCaptor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(auditEventRepository).save(eventCaptor.capture());

        AuditEventEntity savedEvent = eventCaptor.getValue();
        assertEquals(duration, savedEvent.getDurationMs());
    }

    @Test
    void testLogEvent_WithErrorDetails() {
        // Given
        AuditEventEntity.EventType eventType = AuditEventEntity.EventType.PAYMENT_FAILED;
        Map<String, Object> details = Map.of(
            "errorCode", "PAYMENT_ERROR",
            "errorMessage", "Payment processing failed",
            "stackTrace", "java.lang.Exception: Test error"
        );

        // When
        auditService.logEvent(eventType, testUserId, details);

        // Then
        ArgumentCaptor<AuditEventEntity> eventCaptor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(auditEventRepository).save(eventCaptor.capture());

        AuditEventEntity savedEvent = eventCaptor.getValue();
        assertEquals("PAYMENT_ERROR", savedEvent.getErrorCode());
        assertEquals("Payment processing failed", savedEvent.getErrorMessage());
        assertEquals("java.lang.Exception: Test error", savedEvent.getStackTrace());
    }

    @Test
    void testLogCredentialIssued() {
        // When
        auditService.logCredentialIssued(testUserId, testCredentialId, "PassportCredential", "finpass-issuer");

        // Then
        ArgumentCaptor<AuditEventEntity> eventCaptor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(auditEventRepository).save(eventCaptor.capture());

        AuditEventEntity savedEvent = eventCaptor.getValue();
        assertEquals(AuditEventEntity.EventType.CREDENTIAL_ISSUED.name(), savedEvent.getEventType());
        assertEquals(testCredentialId, savedEvent.getResourceId());
        assertEquals("CREDENTIAL", savedEvent.getResourceType());
        assertEquals("ISSUE", savedEvent.getAction());
        assertEquals("SUCCESS", savedEvent.getOutcome());
        assertTrue(savedEvent.getDetails().contains("PassportCredential"));
    }

    @Test
    void testLogCredentialRevoked() {
        // When
        auditService.logCredentialRevoked(testUserId, testCredentialId, "User request", "admin");

        // Then
        ArgumentCaptor<AuditEventEntity> eventCaptor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(auditEventRepository).save(eventCaptor.capture());

        AuditEventEntity savedEvent = eventCaptor.getValue();
        assertEquals(AuditEventEntity.EventType.CREDENTIAL_REVOKED.name(), savedEvent.getEventType());
        assertEquals(testCredentialId, savedEvent.getResourceId());
        assertEquals("REVOKE", savedEvent.getAction());
        assertEquals("admin", savedEvent.getPerformedBy());
        assertEquals("WARNING", savedEvent.getSeverity());
        assertTrue(savedEvent.getDetails().contains("User request"));
    }

    @Test
    void testLogCredentialSuspended() {
        // When
        auditService.logCredentialSuspended(testUserId, testCredentialId, "Security review", "security-admin");

        // Then
        ArgumentCaptor<AuditEventEntity> eventCaptor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(auditEventRepository).save(eventCaptor.capture());

        AuditEventEntity savedEvent = eventCaptor.getValue();
        assertEquals(AuditEventEntity.EventType.CREDENTIAL_SUSPENDED.name(), savedEvent.getEventType());
        assertEquals("SUSPEND", savedEvent.getAction());
        assertEquals("security-admin", savedEvent.getPerformedBy());
        assertEquals("WARNING", savedEvent.getSeverity());
    }

    @Test
    void testLogCredentialReinstated() {
        // When
        auditService.logCredentialReinstated(testUserId, testCredentialId, "admin");

        // Then
        ArgumentCaptor<AuditEventEntity> eventCaptor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(auditEventRepository).save(eventCaptor.capture());

        AuditEventEntity savedEvent = eventCaptor.getValue();
        assertEquals(AuditEventEntity.EventType.CREDENTIAL_REINSTATED.name(), savedEvent.getEventType());
        assertEquals("REINSTATE", savedEvent.getAction());
        assertEquals("admin", savedEvent.getPerformedBy());
        assertEquals("SUCCESS", savedEvent.getOutcome());
    }

    @Test
    void testLogPresentationVerified_Success() {
        // When
        auditService.logPresentationVerified(testUserId, "verifier123", testCredentialId, true, null);

        // Then
        ArgumentCaptor<AuditEventEntity> eventCaptor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(auditEventRepository).save(eventCaptor.capture());

        AuditEventEntity savedEvent = eventCaptor.getValue();
        assertEquals(AuditEventEntity.EventType.PRESENTATION_VERIFIED.name(), savedEvent.getEventType());
        assertEquals(testCredentialId, savedEvent.getResourceId());
        assertEquals("PRESENTATION", savedEvent.getResourceType());
        assertEquals("VERIFY", savedEvent.getAction());
        assertEquals("SUCCESS", savedEvent.getOutcome());
        assertTrue(savedEvent.getDetails().contains("verifier123"));
    }

    @Test
    void testLogPresentationVerified_Failure() {
        // When
        auditService.logPresentationVerified(testUserId, "verifier123", testCredentialId, false, "Expired credential");

        // Then
        ArgumentCaptor<AuditEventEntity> eventCaptor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(auditEventRepository).save(eventCaptor.capture());

        AuditEventEntity savedEvent = eventCaptor.getValue();
        assertEquals("FAILURE", savedEvent.getOutcome());
        assertTrue(savedEvent.getDetails().contains("Expired credential"));
    }

    @Test
    void testLogPaymentInitiated() {
        // Given
        BigDecimal amount = new BigDecimal("100.50");
        String currency = "USD";

        // When
        auditService.logPaymentInitiated("payer123", "payee456", testPaymentId, amount, currency);

        // Then
        ArgumentCaptor<AuditEventEntity> eventCaptor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(auditEventRepository).save(eventCaptor.capture());

        AuditEventEntity savedEvent = eventCaptor.getValue();
        assertEquals(AuditEventEntity.EventType.PAYMENT_INITIATED.name(), savedEvent.getEventType());
        assertEquals(testPaymentId, savedEvent.getResourceId());
        assertEquals("PAYMENT", savedEvent.getResourceType());
        assertEquals("INITIATE", savedEvent.getAction());
        assertTrue(savedEvent.getDetails().contains("100.50"));
        assertTrue(savedEvent.getDetails().contains("USD"));
    }

    @Test
    void testLogPaymentCompleted() {
        // Given
        BigDecimal amount = new BigDecimal("100.50");

        // When
        auditService.logPaymentCompleted("payer123", testPaymentId, "CAPTURED", amount);

        // Then
        ArgumentCaptor<AuditEventEntity> eventCaptor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(auditEventRepository).save(eventCaptor.capture());

        AuditEventEntity savedEvent = eventCaptor.getValue();
        assertEquals(AuditEventEntity.EventType.PAYMENT_COMPLETED.name(), savedEvent.getEventType());
        assertEquals("COMPLETE", savedEvent.getAction());
        assertEquals("SUCCESS", savedEvent.getOutcome());
    }

    @Test
    void testLogPaymentFailed() {
        // When
        auditService.logPaymentFailed("payer123", testPaymentId, "Insufficient funds", "INSUFFICIENT_FUNDS");

        // Then
        ArgumentCaptor<AuditEventEntity> eventCaptor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(auditEventRepository).save(eventCaptor.capture());

        AuditEventEntity savedEvent = eventCaptor.getValue();
        assertEquals(AuditEventEntity.EventType.PAYMENT_FAILED.name(), savedEvent.getEventType());
        assertEquals("FAIL", savedEvent.getAction());
        assertEquals("FAILURE", savedEvent.getOutcome());
        assertEquals("WARNING", savedEvent.getSeverity());
        assertEquals("INSUFFICIENT_FUNDS", savedEvent.getErrorCode());
    }

    @Test
    void testLogUserLogin_Success() {
        // When
        auditService.logUserLogin(testUserId, "192.168.1.1", true, null);

        // Then
        ArgumentCaptor<AuditEventEntity> eventCaptor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(auditEventRepository).save(eventCaptor.capture());

        AuditEventEntity savedEvent = eventCaptor.getValue();
        assertEquals(AuditEventEntity.EventType.USER_LOGIN.name(), savedEvent.getEventType());
        assertEquals("SUCCESS", savedEvent.getOutcome());
        assertEquals("INFO", savedEvent.getSeverity());
    }

    @Test
    void testLogUserLogin_Failure() {
        // When
        auditService.logUserLogin(testUserId, "192.168.1.1", false, "Invalid password");

        // Then
        ArgumentCaptor<AuditEventEntity> eventCaptor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(auditEventRepository).save(eventCaptor.capture());

        AuditEventEntity savedEvent = eventCaptor.getValue();
        assertEquals(AuditEventEntity.EventType.AUTHENTICATION_FAILED.name(), savedEvent.getEventType());
        assertEquals("FAILURE", savedEvent.getOutcome());
        assertEquals("WARNING", savedEvent.getSeverity());
        assertTrue(savedEvent.getDetails().contains("Invalid password"));
    }

    @Test
    void testLogUserRegistered() {
        // When
        auditService.logUserRegistered(testUserId, testUserId);

        // Then
        ArgumentCaptor<AuditEventEntity> eventCaptor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(auditEventRepository).save(eventCaptor.capture());

        AuditEventEntity savedEvent = eventCaptor.getValue();
        assertEquals(AuditEventEntity.EventType.USER_REGISTERED.name(), savedEvent.getEventType());
        assertEquals("USER", savedEvent.getResourceType());
        assertEquals("REGISTER", savedEvent.getAction());
    }

    @Test
    void testLogSecurityEvent() {
        // When
        auditService.logSecurityEvent(testUserId, "BRUTE_FORCE", "Multiple failed login attempts", "CRITICAL");

        // Then
        ArgumentCaptor<AuditEventEntity> eventCaptor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(auditEventRepository).save(eventCaptor.capture());

        AuditEventEntity savedEvent = eventCaptor.getValue();
        assertEquals(AuditEventEntity.EventType.SECURITY_BREACH_ATTEMPT.name(), savedEvent.getEventType());
        assertEquals("SECURITY", savedEvent.getAction());
        assertEquals("CRITICAL", savedEvent.getSeverity());
        assertEquals("SECURITY", savedEvent.getComplianceFlags());
    }

    @Test
    void testLogSystemEvent() {
        // When
        auditService.logSystemEvent("SYSTEM_STARTUP", "System started successfully", Map.of("version", "1.0.0"));

        // Then
        ArgumentCaptor<AuditEventEntity> eventCaptor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(auditEventRepository).save(eventCaptor.capture());

        AuditEventEntity savedEvent = eventCaptor.getValue();
        assertEquals(AuditEventEntity.EventType.SYSTEM_ERROR.name(), savedEvent.getEventType());
        assertEquals("SYSTEM", savedEvent.getAction());
        assertEquals("system", savedEvent.getPerformedBy());
        assertEquals("finpass-issuer", savedEvent.getSourceSystem());
    }

    @Test
    void testHashUserId() {
        // Given
        String userId = "did:example:123456789abcdefghi";

        // When
        String hashedUserId = (String) ReflectionTestUtils.invokeMethod(auditService, "hashUserId", userId);

        // Then
        assertNotNull(hashedUserId);
        assertEquals(64, hashedUserId.length()); // SHA-256 produces 64 character hex string
        assertNotEquals(userId, hashedUserId);
        
        // Test consistency - same input should produce same hash
        String hashedUserId2 = (String) ReflectionTestUtils.invokeMethod(auditService, "hashUserId", userId);
        assertEquals(hashedUserId, hashedUserId2);
    }

    @Test
    void testHashUserId_NullOrEmpty() {
        // Test null input
        String result1 = (String) ReflectionTestUtils.invokeMethod(auditService, "hashUserId", (String) null);
        assertNull(result1);

        // Test empty input
        String result2 = (String) ReflectionTestUtils.invokeMethod(auditService, "hashUserId", "");
        assertNull(result2);

        // Test whitespace input
        String result3 = (String) ReflectionTestUtils.invokeMethod(auditService, "hashUserId", "   ");
        assertNull(result3);
    }

    @Test
    void testGetClientIpAddress() {
        // Given
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 192.168.1.1");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // When
        String ipAddress = (String) ReflectionTestUtils.invokeMethod(auditService, "getClientIpAddress", request);

        // Then
        assertEquals("203.0.113.1", ipAddress); // Should get first IP from X-Forwarded-For
    }

    @Test
    void testGetClientIpAddress_XRealIP() {
        // Given
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("203.0.113.2");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // When
        String ipAddress = (String) ReflectionTestUtils.invokeMethod(auditService, "getClientIpAddress", request);

        // Then
        assertEquals("203.0.113.2", ipAddress);
    }

    @Test
    void testGetClientIpAddress_RemoteAddr() {
        // Given
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // When
        String ipAddress = (String) ReflectionTestUtils.invokeMethod(auditService, "getClientIpAddress", request);

        // Then
        assertEquals("127.0.0.1", ipAddress);
    }

    @Test
    void testConvertDetailsToJson() {
        // Given
        Map<String, Object> details = Map.of(
            "stringField", "test value",
            "numberField", 123,
            "booleanField", true
        );

        // When
        String json = (String) ReflectionTestUtils.invokeMethod(auditService, "convertDetailsToJson", details);

        // Then
        assertNotNull(json);
        assertTrue(json.contains("\"stringField\":\"test value\""));
        assertTrue(json.contains("\"numberField\":123"));
        assertTrue(json.contains("\"booleanField\":true"));
        assertTrue(json.startsWith("{"));
        assertTrue(json.endsWith("}"));
    }

    @Test
    void testConvertDetailsToJson_NullOrEmpty() {
        // Test null input
        String result1 = (String) ReflectionTestUtils.invokeMethod(auditService, "convertDetailsToJson", null);
        assertNull(result1);

        // Test empty input
        String result2 = (String) ReflectionTestUtils.invokeMethod(auditService, "convertDetailsToJson", Map.of());
        assertNull(result2);
    }

    @Test
    void testLogEvent_ExceptionHandling() {
        // Given
        AuditEventEntity.EventType eventType = AuditEventEntity.EventType.CREDENTIAL_ISSUED;
        Map<String, Object> details = Map.of("test", "value");
        
        // Mock repository to throw exception
        doThrow(new RuntimeException("Database error")).when(auditEventRepository).save(any(AuditEventEntity.class));

        // When & Then - Should not throw exception
        assertDoesNotThrow(() -> {
            auditService.logEvent(eventType, testUserId, details);
        });

        // Verify that save was attempted
        verify(auditEventRepository).save(any(AuditEventEntity.class));
    }

    @Test
    void testGetAuditStatistics() {
        // Given
        Object[] expectedStats = {100L, 5L, 2L, 1L, 5L};
        when(auditEventRepository.getAuditStatistics()).thenReturn(expectedStats);

        // When
        Object[] result = auditService.getAuditStatistics();

        // Then
        assertArrayEquals(expectedStats, result);
        verify(auditEventRepository).getAuditStatistics();
    }

    @Test
    void testGetUserEvents() {
        // Given
        String userHash = "hashedUserId123";
        List<AuditEventEntity> expectedEvents = Arrays.asList(
            createMockAuditEvent(),
            createMockAuditEvent()
        );
        when(auditEventRepository.findByUserIdHash(userHash)).thenReturn(expectedEvents);

        // When
        List<AuditEventEntity> result = auditService.getUserEvents(userHash);

        // Then
        assertEquals(expectedEvents, result);
        verify(auditEventRepository).findByUserIdHash(userHash);
    }

    @Test
    void testGetEventsByTypeAndDateRange() {
        // Given
        AuditEventEntity.EventType eventType = AuditEventEntity.EventType.CREDENTIAL_ISSUED;
        Instant start = Instant.now().minusSeconds(3600);
        Instant end = Instant.now();
        
        List<AuditEventEntity> allEvents = Arrays.asList(
            createMockAuditEvent(),
            createMockAuditEvent()
        );
        when(auditEventRepository.findEventsCreatedBetween(start, end)).thenReturn(allEvents);

        // When
        List<AuditEventEntity> result = auditService.getEventsByTypeAndDateRange(eventType, start, end);

        // Then
        assertEquals(2, result.size());
        verify(auditEventRepository).findEventsCreatedBetween(start, end);
    }

    private AuditEventEntity createMockAuditEvent() {
        AuditEventEntity event = new AuditEventEntity(AuditEventEntity.EventType.CREDENTIAL_ISSUED);
        event.setId(UUID.randomUUID());
        event.setEventType(AuditEventEntity.EventType.CREDENTIAL_ISSUED.name());
        event.setUserIdHash("hashedUserId123");
        event.setCreatedAt(Instant.now());
        return event;
    }
}
