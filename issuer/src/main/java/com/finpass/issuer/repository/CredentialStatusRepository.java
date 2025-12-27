package com.finpass.issuer.repository;

import com.finpass.issuer.entity.CredentialStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for credential status operations
 */
@Repository
public interface CredentialStatusRepository extends JpaRepository<CredentialStatusEntity, UUID> {

    /**
     * Find credential status by credential ID
     */
    Optional<CredentialStatusEntity> findByCredentialId(UUID credentialId);

    /**
     * Find credential status by credential ID and status
     */
    Optional<CredentialStatusEntity> findByCredentialIdAndStatus(UUID credentialId, CredentialStatusEntity.Status status);

    /**
     * Find all revoked credentials
     */
    List<CredentialStatusEntity> findByStatus(CredentialStatusEntity.Status status);

    /**
     * Find all credentials revoked after a certain time
     */
    @Query("SELECT cs FROM CredentialStatusEntity cs WHERE cs.revokedAt > :timestamp")
    List<CredentialStatusEntity> findRevokedAfter(@Param("timestamp") java.time.Instant timestamp);

    /**
     * Find all credentials revoked by a specific admin
     */
    List<CredentialStatusEntity> findByRevokedBy(String revokedBy);

    /**
     * Check if a credential is revoked
     */
    @Query("SELECT CASE WHEN COUNT(cs) > 0 THEN true ELSE false END FROM CredentialStatusEntity cs WHERE cs.credential.id = :credentialId AND cs.status = 'REVOKED'")
    boolean isCredentialRevoked(@Param("credentialId") UUID credentialId);

    /**
     * Check if a credential is valid
     */
    @Query("SELECT CASE WHEN COUNT(cs) > 0 THEN true ELSE false END FROM CredentialStatusEntity cs WHERE cs.credential.id = :credentialId AND cs.status = 'VALID'")
    boolean isCredentialValid(@Param("credentialId") UUID credentialId);

    /**
     * Get credential status for verification
     */
    @Query("SELECT cs FROM CredentialStatusEntity cs WHERE cs.credential.id = :credentialId")
    Optional<CredentialStatusEntity> getCredentialStatusForVerification(@Param("credentialId") UUID credentialId);
}
