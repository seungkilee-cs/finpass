package com.finpass.issuer.controller;

import com.finpass.issuer.entity.AuditEventEntity;
import com.finpass.issuer.service.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for AuditController
 */
@WebMvcTest(AuditController.class)
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditService auditService;

    @Autowired
    private ObjectMapper objectMapper;

    private String testUserHash;
    private AuditEventEntity testEvent;

    @BeforeEach
    void setUp() {
        testUserHash = "hashedUserId123";
        testEvent = createTestAuditEvent();
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testGetUserEvents_Success() throws Exception {
        // Given
        List<AuditEventEntity> events = Arrays.asList(testEvent);
        when(auditService.getUserEvents(testUserHash)).thenReturn(events);

        // When & Then
        mockMvc.perform(get("/audit/events/{userHash}", testUserHash)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(testEvent.getId().toString()))
                .andExpect(jsonPath("$[0].eventType").value(testEvent.getEventType()));

        verify(auditService).getUserEvents(testUserHash);
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testGetUserEvents_ServiceError() throws Exception {
        // Given
        when(auditService.getUserEvents(testUserHash))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(get("/audit/events/{userHash}", testUserHash)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(auditService).getUserEvents(testUserHash);
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void testGetUserEvents_AccessDenied() throws Exception {
        // When & Then
        mockMvc.perform(get("/audit/events/{userHash}", testUserHash)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        verify(auditService, never()).getUserEvents(anyString());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testGetFilteredEvents_WithTypeAndDateRange() throws Exception {
        // Given
        Instant from = Instant.now().minusSeconds(3600);
        Instant to = Instant.now();
        List<AuditEventEntity> events = Arrays.asList(testEvent);
        
        when(auditService.getEventsByTypeAndDateRange(any(AuditEventEntity.EventType.class), eq(from), eq(to)))
                .thenReturn(events);

        // When & Then
        mockMvc.perform(get("/audit/events")
                .param("type", "CREDENTIAL_ISSUED")
                .param("from", from.toString())
                .param("to", to.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].eventType").value(testEvent.getEventType()));

        verify(auditService).getEventsByTypeAndDateRange(any(AuditEventEntity.EventType.class), eq(from), eq(to));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testGetFilteredEvents_WithSeverityFilter() throws Exception {
        // Given
        Instant from = Instant.now().minusSeconds(3600);
        Instant to = Instant.now();
        List<AuditEventEntity> events = Arrays.asList(testEvent);
        
        when(auditService.getEventsByTypeAndDateRange(any(AuditEventEntity.EventType.class), eq(from), eq(to)))
                .thenReturn(events);

        // When & Then
        mockMvc.perform(get("/audit/events")
                .param("type", "CREDENTIAL_ISSUED")
                .param("from", from.toString())
                .param("to", to.toString())
                .param("severity", "INFO")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(auditService).getEventsByTypeAndDateRange(any(AuditEventEntity.EventType.class), eq(from), eq(to));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testGetFilteredEvents_WithPagination() throws Exception {
        // Given
        List<AuditEventEntity> events = Arrays.asList(testEvent);
        when(auditService.getEventsCreatedBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(events);

        // When & Then
        mockMvc.perform(get("/audit/events")
                .param("page", "1")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(auditService).getEventsCreatedBetween(any(Instant.class), any(Instant.class));
    }

    @Test
    @WithMockUser(roles = {"AUDITOR"})
    void testGetAuditMetrics_Success() throws Exception {
        // Given
        Object[] stats = {100L, 5L, 2L, 1L, 5L};
        when(auditService.getAuditStatistics()).thenReturn(stats);

        // When & Then
        mockMvc.perform(get("/audit/metrics")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalEvents").value(100))
                .andExpect(jsonPath("$.errorEvents").value(5))
                .andExpect(jsonPath("$.warningEvents").value(2))
                .andExpect(jsonPath("$.criticalEvents").value(1))
                .andExpect(jsonPath("$.failedEvents").value(5))
                .andExpect(jsonPath("$.eventTypes").exists())
                .andExpect(jsonPath("$.severityBreakdown").exists())
                .andExpect(jsonPath("$.recentActivity").exists())
                .andExpect(jsonPath("$.topUsers").exists());

        verify(auditService).getAuditStatistics();
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testGetEventsByType_Success() throws Exception {
        // Given
        List<AuditEventEntity> events = Arrays.asList(testEvent);
        when(auditService.getEventsByTypeAndDateRange(any(AuditEventEntity.EventType.class), any(Instant.class), any(Instant.class)))
                .thenReturn(events);

        // When & Then
        mockMvc.perform(get("/audit/events/type/{eventType}", "CREDENTIAL_ISSUED")
                .param("page", "0")
                .param("size", "100")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].eventType").value(testEvent.getEventType()));

        verify(auditService).getEventsByTypeAndDateRange(any(AuditEventEntity.EventType.class), any(Instant.class), any(Instant.class));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testGetEventsByType_InvalidEventType() throws Exception {
        // When & Then
        mockMvc.perform(get("/audit/events/type/{eventType}", "INVALID_TYPE")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(roles = {"SECURITY"})
    void testGetSecurityEvents_Success() throws Exception {
        // Given
        List<AuditEventEntity> events = Arrays.asList(testEvent);
        when(auditService.findSecurityEvents()).thenReturn(events);

        // When & Then
        mockMvc.perform(get("/audit/events/security")
                .param("page", "0")
                .param("size", "100")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());

        verify(auditService).findSecurityEvents();
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testGetErrorEvents_Success() throws Exception {
        // Given
        List<AuditEventEntity> events = Arrays.asList(testEvent);
        when(auditService.findErrorEvents()).thenReturn(events);

        // When & Then
        mockMvc.perform(get("/audit/events/errors")
                .param("page", "0")
                .param("size", "100")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());

        verify(auditService).findErrorEvents();
    }

    @Test
    @WithMockUser(roles = {"SECURITY"})
    void testGetFailedAuthentications_Success() throws Exception {
        // Given
        List<AuditEventEntity> events = Arrays.asList(testEvent);
        when(auditService.findFailedAuthentications()).thenReturn(events);

        // When & Then
        mockMvc.perform(get("/audit/events/auth-failures")
                .param("page", "0")
                .param("size", "100")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());

        verify(auditService).findFailedAuthentications();
    }

    @Test
    @WithMockUser(roles = {"COMPLIANCE"})
    void testGetComplianceReport_Success() throws Exception {
        // Given
        Instant from = Instant.now().minusSeconds(86400);
        Instant to = Instant.now();
        List<Object[]> reportData = Arrays.asList(
                new Object[]{"CREDENTIAL_ISSUED", "INFO", 50L},
                new Object[]{"CREDENTIAL_REVOKED", "WARNING", 5L}
        );
        when(auditService.getComplianceReport(from, to)).thenReturn(reportData);

        // When & Then
        mockMvc.perform(get("/audit/compliance/report")
                .param("from", from.toString())
                .param("to", to.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.period.from").value(from.toString()))
                .andExpect(jsonPath("$.period.to").value(to.toString()))
                .andExpect(jsonPath("$.eventsByTypeAndSeverity").isArray())
                .andExpect(jsonPath("$.totalEvents").value(55))
                .andExpect(jsonPath("$.errorRate").exists())
                .andExpect(jsonPath("$.criticalEvents").value(0));

        verify(auditService).getComplianceReport(from, to);
    }

    @Test
    @WithMockUser(roles = {"SECURITY"})
    void testGetSuspiciousActivities_Success() throws Exception {
        // Given
        List<Object[]> activities = Arrays.asList(
                new Object[]{"192.168.1.1", 15L},
                new Object[]{"192.168.1.2", 8L}
        );
        when(auditService.findSuspiciousActivities(any(Instant.class), eq(10))).thenReturn(activities);

        // When & Then
        mockMvc.perform(get("/audit/security/suspicious")
                .param("threshold", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").isArray())
                .andExpect(jsonPath("$[0][0]").value("192.168.1.1"))
                .andExpect(jsonPath("$[0][1]").value(15));

        verify(auditService).findSuspiciousActivities(any(Instant.class), eq(10));
    }

    @Test
    @WithMockUser(roles = {"AUDITOR"})
    void testGetUserTimeline_Success() throws Exception {
        // Given
        List<AuditEventEntity> timeline = Arrays.asList(testEvent);
        when(auditService.getUserActivityTimeline(testUserHash)).thenReturn(timeline);

        // When & Then
        mockMvc.perform(get("/audit/users/{userHash}/timeline", testUserHash)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(testEvent.getId().toString()));

        verify(auditService).getUserActivityTimeline(testUserHash);
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testGetFilteredEvents_EmptyResult() throws Exception {
        // Given
        when(auditService.getEventsCreatedBetween(any(Instant.class), any(Instant.class)))
                .thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/audit/events")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(auditService).getEventsCreatedBetween(any(Instant.class), any(Instant.class));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testGetFilteredEvents_ServiceException() throws Exception {
        // Given
        when(auditService.getEventsCreatedBetween(any(Instant.class), any(Instant.class)))
                .thenThrow(new RuntimeException("Service unavailable"));

        // When & Then
        mockMvc.perform(get("/audit/events")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(auditService).getEventsCreatedBetween(any(Instant.class), any(Instant.class));
    }

    @Test
    void testEndpoints_WithoutAuthentication() throws Exception {
        // Test that endpoints require authentication
        mockMvc.perform(get("/audit/events/{userHash}", testUserHash)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/audit/metrics")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void testEndpoints_WithInsufficientRole() throws Exception {
        // Test that endpoints require proper roles
        mockMvc.perform(get("/audit/events/{userHash}", testUserHash)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/audit/metrics")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testGetFilteredEvents_InvalidDateFormat() throws Exception {
        // When & Then
        mockMvc.perform(get("/audit/events")
                .param("from", "invalid-date")
                .param("to", "invalid-date")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testGetFilteredEvents_NegativePage() throws Exception {
        // When & Then
        mockMvc.perform(get("/audit/events")
                .param("page", "-1")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()); // Should handle negative page gracefully
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testGetFilteredEvents_ZeroSize() throws Exception {
        // When & Then
        mockMvc.perform(get("/audit/events")
                .param("page", "0")
                .param("size", "0")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()); // Should handle zero size gracefully
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void testGetFilteredEvents_LargeSize() throws Exception {
        // When & Then
        mockMvc.perform(get("/audit/events")
                .param("page", "0")
                .param("size", "1000")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()); // Should handle large size requests
    }

    private AuditEventEntity createTestAuditEvent() {
        AuditEventEntity event = new AuditEventEntity(AuditEventEntity.EventType.CREDENTIAL_ISSUED);
        event.setId(UUID.randomUUID());
        event.setEventType(AuditEventEntity.EventType.CREDENTIAL_ISSUED.name());
        event.setUserIdHash(testUserHash);
        event.setResourceType("CREDENTIAL");
        event.setAction("ISSUE");
        event.setOutcome("SUCCESS");
        event.setSeverity("INFO");
        event.setDescription("Credential issued successfully");
        event.setCreatedAt(Instant.now());
        event.setDetails("{\"credentialType\":\"PassportCredential\",\"issuerId\":\"finpass-issuer\"}");
        return event;
    }
}
