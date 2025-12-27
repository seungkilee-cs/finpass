package com.finpass.issuer.service;

import com.finpass.issuer.dto.CredentialStatusResponse;
import com.finpass.issuer.dto.RevocationRequest;
import com.finpass.issuer.entity.CredentialEntity;
import com.finpass.issuer.entity.CredentialStatusEntity;
import com.finpass.issuer.repository.CredentialRepository;
import com.finpass.issuer.repository.CredentialStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Revocation Service
 */
@ExtendWith(MockitoExtension.class)
class RevocationServiceTest {

    @Mock
    private CredentialStatusRepository credentialStatusRepository;

    @Mock
    private CredentialRepository credentialRepository;

    @InjectMocks
    private RevocationService revocationService;

    private CredentialEntity testCredential;
    private UUID testCredentialId;
    private RevocationRequest testRevocationRequest;

    @BeforeEach
    void setUp() {
        testCredentialId = UUID.randomUUID();
        testCredential = new CredentialEntity();
        testCredential.setId(testCredentialId);
        
        testRevocationRequest = new RevocationRequest(
            CredentialStatusEntity.RevocationReason.FRAUD,
            "admin-user",
            "Test revocation for fraud"
        );
    }

    @Test
    void testRevokeCredential_Success() {
        // Arrange
        when(credentialRepository.findById(testCredentialId)).thenReturn(Optional.of(testCredential));
        
        CredentialStatusEntity existingStatus = new CredentialStatusEntity(testCredential);
        when(credentialStatusRepository.findByCredentialId(testCredentialId)).thenReturn(Optional.of(existingStatus));
        when(credentialStatusRepository.save(any(CredentialStatusEntity.class))).thenReturn(existingStatus);

        // Act
        CredentialStatusResponse response = revocationService.revokeCredential(testCredentialId, testRevocationRequest);

        // Assert
        assertNotNull(response, "Response should not be null");
        assertEquals(testCredentialId, response.getCredentialId(), "Credential ID should match");
        assertEquals(CredentialStatusEntity.Status.REVOKED, response.getStatus(), "Status should be REVOKED");
        assertFalse(response.isValid(), "Credential should not be valid");
        assertEquals(CredentialStatusEntity.RevocationReason.FRAUD, response.getRevocationReason(), "Revocation reason should match");
        assertEquals("admin-user", response.getRevokedBy(), "Revoked by should match");
        assertEquals("Test revocation for fraud", response.getReasonDescription(), "Reason description should match");
        assertNotNull(response.getRevokedAt(), "Revoked at should not be null");

        verify(credentialStatusRepository).save(any(CredentialStatusEntity.class));
    }

    @Test
    void testRevokeCredential_CredentialNotFound() {
        // Arrange
        when(credentialRepository.findById(testCredentialId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            revocationService.revokeCredential(testCredentialId, testRevocationRequest);
        }, "Should throw exception for non-existent credential");
    }

    @Test
    void testRevokeCredential_AlreadyRevoked() {
        // Arrange
        when(credentialRepository.findById(testCredentialId)).thenReturn(Optional.of(testCredential));
        
        CredentialStatusEntity revokedStatus = new CredentialStatusEntity(testCredential);
        revokedStatus.setStatus(CredentialStatusEntity.Status.REVOKED);
        when(credentialStatusRepository.findByCredentialId(testCredentialId)).thenReturn(Optional.of(revokedStatus));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            revocationService.revokeCredential(testCredentialId, testRevocationRequest);
        }, "Should throw exception for already revoked credential");
    }

    @Test
    void testRevokeCredential_NewStatusRecord() {
        // Arrange
        when(credentialRepository.findById(testCredentialId)).thenReturn(Optional.of(testCredential));
        when(credentialStatusRepository.findByCredentialId(testCredentialId)).thenReturn(Optional.empty());
        
        CredentialStatusEntity newStatus = new CredentialStatusEntity(testCredential);
        when(credentialStatusRepository.save(any(CredentialStatusEntity.class))).thenReturn(newStatus);

        // Act
        CredentialStatusResponse response = revocationService.revokeCredential(testCredentialId, testRevocationRequest);

        // Assert
        assertNotNull(response, "Response should not be null");
        verify(credentialStatusRepository).save(any(CredentialStatusEntity.class));
    }

    @Test
    void testSuspendCredential_Success() {
        // Arrange
        when(credentialRepository.findById(testCredentialId)).thenReturn(Optional.of(testCredential));
        
        CredentialStatusEntity existingStatus = new CredentialStatusEntity(testCredential);
        when(credentialStatusRepository.findByCredentialId(testCredentialId)).thenReturn(Optional.of(existingStatus));
        when(credentialStatusRepository.save(any(CredentialStatusEntity.class))).thenReturn(existingStatus);

        // Act
        CredentialStatusResponse response = revocationService.suspendCredential(testCredentialId, "admin-user", "Test suspension");

        // Assert
        assertNotNull(response, "Response should not be null");
        assertEquals(testCredentialId, response.getCredentialId(), "Credential ID should match");
        assertEquals(CredentialStatusEntity.Status.SUSPENDED, response.getStatus(), "Status should be SUSPENDED");
        assertFalse(response.isValid(), "Credential should not be valid");
        assertEquals("admin-user", response.getRevokedBy(), "Suspended by should match");
        assertEquals("Test suspension", response.getReasonDescription(), "Reason should match");

        verify(credentialStatusRepository).save(any(CredentialStatusEntity.class));
    }

    @Test
    void testSuspendCredential_AlreadyRevoked() {
        // Arrange
        when(credentialRepository.findById(testCredentialId)).thenReturn(Optional.of(testCredential));
        
        CredentialStatusEntity revokedStatus = new CredentialStatusEntity(testCredential);
        revokedStatus.setStatus(CredentialStatusEntity.Status.REVOKED);
        when(credentialStatusRepository.findByCredentialId(testCredentialId)).thenReturn(Optional.of(revokedStatus));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            revocationService.suspendCredential(testCredentialId, "admin-user", "Test suspension");
        }, "Should throw exception for revoked credential");
    }

    @Test
    void testReinstateCredential_Success() {
        // Arrange
        when(credentialRepository.findById(testCredentialId)).thenReturn(Optional.of(testCredential));
        
        CredentialStatusEntity suspendedStatus = new CredentialStatusEntity(testCredential);
        suspendedStatus.setStatus(CredentialStatusEntity.Status.SUSPENDED);
        when(credentialStatusRepository.findByCredentialId(testCredentialId)).thenReturn(Optional.of(suspendedStatus));
        when(credentialStatusRepository.save(any(CredentialStatusEntity.class))).thenReturn(suspendedStatus);

        // Act
        CredentialStatusResponse response = revocationService.reinstateCredential(testCredentialId, "admin-user");

        // Assert
        assertNotNull(response, "Response should not be null");
        assertEquals(testCredentialId, response.getCredentialId(), "Credential ID should match");
        assertEquals(CredentialStatusEntity.Status.VALID, response.getStatus(), "Status should be VALID");
        assertTrue(response.isValid(), "Credential should be valid");

        verify(credentialStatusRepository).save(any(CredentialStatusEntity.class));
    }

    @Test
    void testReinstateCredential_RevokedCredential() {
        // Arrange
        when(credentialRepository.findById(testCredentialId)).thenReturn(Optional.of(testCredential));
        
        CredentialStatusEntity revokedStatus = new CredentialStatusEntity(testCredential);
        revokedStatus.setStatus(CredentialStatusEntity.Status.REVOKED);
        when(credentialStatusRepository.findByCredentialId(testCredentialId)).thenReturn(Optional.of(revokedStatus));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            revocationService.reinstateCredential(testCredentialId, "admin-user");
        }, "Should throw exception for revoked credential");
    }

    @Test
    void testGetCredentialStatus_Valid() {
        // Arrange
        when(credentialRepository.existsById(testCredentialId)).thenReturn(true);
        when(credentialStatusRepository.findByCredentialId(testCredentialId)).thenReturn(Optional.empty());

        // Act
        CredentialStatusResponse response = revocationService.getCredentialStatus(testCredentialId);

        // Assert
        assertNotNull(response, "Response should not be null");
        assertEquals(testCredentialId, response.getCredentialId(), "Credential ID should match");
        assertEquals(CredentialStatusEntity.Status.VALID, response.getStatus(), "Status should be VALID");
        assertTrue(response.isValid(), "Credential should be valid");
    }

    @Test
    void testGetCredentialStatus_Revoked() {
        // Arrange
        when(credentialRepository.existsById(testCredentialId)).thenReturn(true);
        
        CredentialStatusEntity revokedStatus = new CredentialStatusEntity(testCredential);
        revokedStatus.setStatus(CredentialStatusEntity.Status.REVOKED);
        revokedStatus.setRevokedAt(Instant.now());
        revokedStatus.setRevocationReason(CredentialStatusEntity.RevocationReason.FRAUD);
        revokedStatus.setRevokedBy("admin-user");
        revokedStatus.setReasonDescription("Test revocation");
        when(credentialStatusRepository.findByCredentialId(testCredentialId)).thenReturn(Optional.of(revokedStatus));

        // Act
        CredentialStatusResponse response = revocationService.getCredentialStatus(testCredentialId);

        // Assert
        assertNotNull(response, "Response should not be null");
        assertEquals(CredentialStatusEntity.Status.REVOKED, response.getStatus(), "Status should be REVOKED");
        assertFalse(response.isValid(), "Credential should not be valid");
        assertEquals(CredentialStatusEntity.RevocationReason.FRAUD, response.getRevocationReason(), "Revocation reason should match");
    }

    @Test
    void testGetCredentialStatus_CredentialNotFound() {
        // Arrange
        when(credentialRepository.existsById(testCredentialId)).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            revocationService.getCredentialStatus(testCredentialId);
        }, "Should throw exception for non-existent credential");
    }

    @Test
    void testIsCredentialValid() {
        // Arrange
        when(credentialStatusRepository.isCredentialValid(testCredentialId)).thenReturn(true);

        // Act
        boolean isValid = revocationService.isCredentialValid(testCredentialId);

        // Assert
        assertTrue(isValid, "Credential should be valid");
        verify(credentialStatusRepository).isCredentialValid(testCredentialId);
    }

    @Test
    void testIsCredentialRevoked() {
        // Arrange
        when(credentialStatusRepository.isCredentialRevoked(testCredentialId)).thenReturn(true);

        // Act
        boolean isRevoked = revocationService.isCredentialRevoked(testCredentialId);

        // Assert
        assertTrue(isRevoked, "Credential should be revoked");
        verify(credentialStatusRepository).isCredentialRevoked(testCredentialId);
    }

    @Test
    void testGetRevokedCredentials() {
        // Arrange
        List<CredentialStatusEntity> revokedList = List.of(new CredentialStatusEntity());
        when(credentialStatusRepository.findByStatus(CredentialStatusEntity.Status.REVOKED)).thenReturn(revokedList);

        // Act
        List<CredentialStatusEntity> result = revocationService.getRevokedCredentials();

        // Assert
        assertEquals(revokedList, result, "Should return revoked credentials list");
        verify(credentialStatusRepository).findByStatus(CredentialStatusEntity.Status.REVOKED);
    }

    @Test
    void testInitializeCredentialStatus() {
        // Arrange
        CredentialStatusEntity newStatus = new CredentialStatusEntity(testCredential);
        when(credentialStatusRepository.save(any(CredentialStatusEntity.class))).thenReturn(newStatus);

        // Act
        CredentialStatusEntity result = revocationService.initializeCredentialStatus(testCredential);

        // Assert
        assertNotNull(result, "Status should not be null");
        assertEquals(CredentialStatusEntity.Status.VALID, result.getStatus(), "Initial status should be VALID");
        verify(credentialStatusRepository).save(any(CredentialStatusEntity.class));
    }

    @Test
    void testValidateAdminAuthorization_ValidToken() {
        // Act
        boolean isValid = revocationService.validateAdminAuthorization("admin-secret-token");

        // Assert
        assertTrue(isValid, "Valid admin token should be accepted");
    }

    @Test
    void testValidateAdminAuthorization_InvalidToken() {
        // Act
        boolean isValid = revocationService.validateAdminAuthorization("invalid-token");

        // Assert
        assertFalse(isValid, "Invalid admin token should be rejected");
    }

    @Test
    void testValidateAdminAuthorization_SuperAdminToken() {
        // Act
        boolean isValid = revocationService.validateAdminAuthorization("super-admin-token");

        // Assert
        assertTrue(isValid, "Super admin token should be accepted");
    }
}
