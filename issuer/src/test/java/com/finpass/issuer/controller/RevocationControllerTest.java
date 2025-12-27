package com.finpass.issuer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpass.issuer.dto.CredentialStatusResponse;
import com.finpass.issuer.dto.RevocationRequest;
import com.finpass.issuer.entity.CredentialStatusEntity;
import com.finpass.issuer.service.RevocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for Revocation Controller
 */
@WebMvcTest(RevocationController.class)
class RevocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RevocationService revocationService;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID testCredentialId;
    private RevocationRequest testRevocationRequest;
    private CredentialStatusResponse testStatusResponse;

    @BeforeEach
    void setUp() {
        testCredentialId = UUID.randomUUID();
        testRevocationRequest = new RevocationRequest(
            CredentialStatusEntity.RevocationReason.FRAUD,
            "admin-user",
            "Test revocation"
        );

        testStatusResponse = new CredentialStatusResponse();
        testStatusResponse.setCredentialId(testCredentialId);
        testStatusResponse.setStatus(CredentialStatusEntity.Status.REVOKED);
        testStatusResponse.setValid(false);
        testStatusResponse.setRevokedBy("admin-user");
        testStatusResponse.setReasonDescription("Test revocation");
    }

    @Test
    void testRevokeCredential_Success() throws Exception {
        // Arrange
        when(revocationService.validateAdminAuthorization(any())).thenReturn(true);
        when(revocationService.revokeCredential(eq(testCredentialId), any(RevocationRequest.class)))
            .thenReturn(testStatusResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/credentials/{credentialId}/revoke", testCredentialId)
                .header("Authorization", "Bearer admin-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRevocationRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credential_id").value(testCredentialId.toString()))
                .andExpect(jsonPath("$.status").value("REVOKED"))
                .andExpect(jsonPath("$.is_valid").value(false))
                .andExpect(jsonPath("$.revoked_by").value("admin-user"));
    }

    @Test
    void testRevokeCredential_Unauthorized() throws Exception {
        // Arrange
        when(revocationService.validateAdminAuthorization(any())).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/api/v1/credentials/{credentialId}/revoke", testCredentialId)
                .header("Authorization", "invalid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRevocationRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testRevokeCredential_NoAuthorization() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/credentials/{credentialId}/revoke", testCredentialId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRevocationRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testRevokeCredential_CredentialNotFound() throws Exception {
        // Arrange
        when(revocationService.validateAdminAuthorization(any())).thenReturn(true);
        when(revocationService.revokeCredential(eq(testCredentialId), any(RevocationRequest.class)))
            .thenThrow(new IllegalArgumentException("Credential not found"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/credentials/{credentialId}/revoke", testCredentialId)
                .header("Authorization", "Bearer admin-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRevocationRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testRevokeCredential_AlreadyRevoked() throws Exception {
        // Arrange
        when(revocationService.validateAdminAuthorization(any())).thenReturn(true);
        when(revocationService.revokeCredential(eq(testCredentialId), any(RevocationRequest.class)))
            .thenThrow(new IllegalStateException("Credential already revoked"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/credentials/{credentialId}/revoke", testCredentialId)
                .header("Authorization", "Bearer admin-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRevocationRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSuspendCredential_Success() throws Exception {
        // Arrange
        CredentialStatusResponse suspendedResponse = new CredentialStatusResponse();
        suspendedResponse.setCredentialId(testCredentialId);
        suspendedResponse.setStatus(CredentialStatusEntity.Status.SUSPENDED);
        suspendedResponse.setValid(false);
        suspendedResponse.setRevokedBy("admin-user");
        suspendedResponse.setReasonDescription("Test suspension");

        when(revocationService.validateAdminAuthorization(any())).thenReturn(true);
        when(revocationService.suspendCredential(eq(testCredentialId), eq("admin-user"), eq("Test suspension")))
            .thenReturn(suspendedResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/credentials/{credentialId}/suspend", testCredentialId)
                .header("Authorization", "Bearer admin-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"suspended_by\":\"admin-user\",\"reason\":\"Test suspension\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credential_id").value(testCredentialId.toString()))
                .andExpect(jsonPath("$.status").value("SUSPENDED"))
                .andExpect(jsonPath("$.is_valid").value(false))
                .andExpect(jsonPath("$.revoked_by").value("admin-user"));
    }

    @Test
    void testReinstateCredential_Success() throws Exception {
        // Arrange
        CredentialStatusResponse validResponse = CredentialStatusResponse.valid(testCredentialId);

        when(revocationService.validateAdminAuthorization(any())).thenReturn(true);
        when(revocationService.reinstateCredential(eq(testCredentialId), eq("admin-user")))
            .thenReturn(validResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/credentials/{credentialId}/reinstate", testCredentialId)
                .header("Authorization", "Bearer admin-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reinstated_by\":\"admin-user\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credential_id").value(testCredentialId.toString()))
                .andExpect(jsonPath("$.status").value("VALID"))
                .andExpect(jsonPath("$.is_valid").value(true));
    }

    @Test
    void testGetCredentialStatus_Success() throws Exception {
        // Arrange
        when(revocationService.getCredentialStatus(testCredentialId)).thenReturn(testStatusResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/credentials/{credentialId}/status", testCredentialId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credential_id").value(testCredentialId.toString()))
                .andExpect(jsonPath("$.status").value("REVOKED"))
                .andExpect(jsonPath("$.is_valid").value(false));
    }

    @Test
    void testGetCredentialStatus_NotFound() throws Exception {
        // Arrange
        when(revocationService.getCredentialStatus(testCredentialId))
            .thenThrow(new IllegalArgumentException("Credential not found"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/credentials/{credentialId}/status", testCredentialId))
                .andExpect(status().isNotFound());
    }

    @Test
    void testIsCredentialValid_Valid() throws Exception {
        // Arrange
        when(revocationService.isCredentialValid(testCredentialId)).thenReturn(true);

        // Act & Assert
        mockMvc.perform(get("/api/v1/credentials/{credentialId}/valid", testCredentialId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credential_id").value(testCredentialId.toString()))
                .andExpect(jsonPath("$.is_valid").value(true))
                .andExpect(jsonPath("$.checked_at").exists());
    }

    @Test
    void testIsCredentialValid_Revoked() throws Exception {
        // Arrange
        when(revocationService.isCredentialValid(testCredentialId)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/v1/credentials/{credentialId}/valid", testCredentialId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credential_id").value(testCredentialId.toString()))
                .andExpect(jsonPath("$.is_valid").value(false))
                .andExpect(jsonPath("$.checked_at").exists());
    }

    @Test
    void testGetRevokedCredentials_Success() throws Exception {
        // Arrange
        when(revocationService.validateAdminAuthorization(any())).thenReturn(true);
        when(revocationService.getRevokedCredentials()).thenReturn(java.util.List.of());

        // Act & Assert
        mockMvc.perform(get("/api/v1/credentials/revoked")
                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testGetRevokedCredentials_Unauthorized() throws Exception {
        // Arrange
        when(revocationService.validateAdminAuthorization(any())).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/v1/credentials/revoked")
                .header("Authorization", "invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testClearCredentialCache_Success() throws Exception {
        // Arrange
        when(revocationService.validateAdminAuthorization(any())).thenReturn(true);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/credentials/{credentialId}/cache", testCredentialId)
                .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    @Test
    void testClearCredentialCache_Unauthorized() throws Exception {
        // Arrange
        when(revocationService.validateAdminAuthorization(any())).thenReturn(false);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/credentials/{credentialId}/cache", testCredentialId)
                .header("Authorization", "invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testRevokeCredential_BearerTokenFormat() throws Exception {
        // Arrange
        when(revocationService.validateAdminAuthorization("admin-secret-token")).thenReturn(true);
        when(revocationService.revokeCredential(eq(testCredentialId), any(RevocationRequest.class)))
            .thenReturn(testStatusResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/credentials/{credentialId}/revoke", testCredentialId)
                .header("Authorization", "Bearer admin-secret-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRevocationRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void testRevokeCredential_DirectTokenFormat() throws Exception {
        // Arrange
        when(revocationService.validateAdminAuthorization("admin-secret-token")).thenReturn(true);
        when(revocationService.revokeCredential(eq(testCredentialId), any(RevocationRequest.class)))
            .thenReturn(testStatusResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/credentials/{credentialId}/revoke", testCredentialId)
                .header("Authorization", "admin-secret-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRevocationRequest)))
                .andExpect(status().isOk());
    }
}
