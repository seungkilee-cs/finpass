package com.finpass.issuer.repository;

import com.finpass.issuer.entity.ProductionCredentialEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Production CredentialRepository with enhanced query capabilities
 */
@Repository
public interface ProductionCredentialRepository extends JpaRepository<ProductionCredentialEntity, UUID> {

    /**
     * Find credentials by user ID
     */
    List<ProductionCredentialEntity> findByUserId(UUID userId);

    /**
     * Find credentials by issuer ID
     */
    List<ProductionCredentialEntity> findByIssuerId(String issuerId);

    /**
     * Find credentials by credential type
     */
    List<ProductionCredentialEntity> findByCredentialType(String credentialType);

    /**
     * Find credentials by status
     */
    List<ProductionCredentialEntity> findByStatus(String status);

    /**
     * Find credentials by subject DID
     */
    List<ProductionCredentialEntity> findBySubjectDid(String subjectDid);

    /**
     * Find credential by credential hash
     */
    Optional<ProductionCredentialEntity> findByCredentialHash(String credentialHash);

    /**
     * Find credential by revocation handle
     */
    Optional<ProductionCredentialEntity> findByRevocationHandle(String revocationHandle);

    /**
     * Find credentials issued within a date range
     */
    @Query("SELECT c FROM ProductionCredentialEntity c WHERE c.issuedAt BETWEEN :startDate AND :endDate")
    List<ProductionCredentialEntity> findCredentialsIssuedBetween(@Param("startDate") Instant startDate, 
                                                                  @Param("endDate") Instant endDate);

    /**
     * Find credentials expiring within a date range
     */
    @Query("SELECT c FROM ProductionCredentialEntity c WHERE c.expiresAt BETWEEN :startDate AND :endDate")
    List<ProductionCredentialEntity> findCredentialsExpiringBetween(@Param("startDate") Instant startDate, 
                                                                    @Param("endDate") Instant endDate);

    /**
     * Find expired credentials
     */
    @Query("SELECT c FROM ProductionCredentialEntity c WHERE c.expiresAt < :now AND c.status = 'VALID'")
    List<ProductionCredentialEntity> findExpiredCredentials(@Param("now") Instant now);

    /**
     * Find credentials expiring soon (within N days)
     */
    @Query("SELECT c FROM ProductionCredentialEntity c WHERE c.expiresAt BETWEEN :now AND :future AND c.status = 'VALID'")
    List<ProductionCredentialEntity> findCredentialsExpiringSoon(@Param("now") Instant now, 
                                                                @Param("future") Instant future);

    /**
     * Find revoked credentials
     */
    @Query("SELECT c FROM ProductionCredentialEntity c WHERE c.status = 'REVOKED' ORDER BY c.revokedAt DESC")
    List<ProductionCredentialEntity> findRevokedCredentials();

    /**
     * Find credentials revoked after a specific time
     */
    @Query("SELECT c FROM ProductionCredentialEntity c WHERE c.revokedAt > :timestamp")
    List<ProductionCredentialEntity> findCredentialsRevokedAfter(@Param("timestamp") Instant timestamp);

    /**
     * Find credentials revoked by a specific person
     */
    List<ProductionCredentialEntity> findByRevokedBy(String revokedBy);

    /**
     * Find credentials by user and type
     */
    @Query("SELECT c FROM ProductionCredentialEntity c WHERE c.user.id = :userId AND c.credentialType = :credentialType")
    List<ProductionCredentialEntity> findByUserAndType(@Param("userId") UUID userId, 
                                                       @Param("credentialType") String credentialType);

    /**
     * Find valid credentials for a user
     */
    @Query("SELECT c FROM ProductionCredentialEntity c WHERE c.user.id = :userId AND c.status = 'VALID' AND (c.expiresAt IS NULL OR c.expiresAt > :now)")
    List<ProductionCredentialEntity> findValidCredentialsForUser(@Param("userId") UUID userId, 
                                                                 @Param("now") Instant now);

    /**
     * Count credentials by status
     */
    @Query("SELECT c.status, COUNT(c) FROM ProductionCredentialEntity c GROUP BY c.status")
    List<Object[]> countCredentialsByStatus();

    /**
     * Count credentials by type
     */
    @Query("SELECT c.credentialType, COUNT(c) FROM ProductionCredentialEntity c GROUP BY c.credentialType")
    List<Object[]> countCredentialsByType();

    /**
     * Count credentials by issuer
     */
    @Query("SELECT c.issuerId, COUNT(c) FROM ProductionCredentialEntity c GROUP BY c.issuerId")
    List<Object[]> countCredentialsByIssuer();

    /**
     * Get credential statistics
     */
    @Query("SELECT " +
           "COUNT(c) as totalCredentials, " +
           "COUNT(CASE WHEN c.status = 'VALID' THEN 1 END) as validCredentials, " +
           "COUNT(CASE WHEN c.status = 'REVOKED' THEN 1 END) as revokedCredentials, " +
           "COUNT(CASE WHEN c.status = 'SUSPENDED' THEN 1 END) as suspendedCredentials, " +
           "COUNT(CASE WHEN c.expiresAt <= :now THEN 1 END) as expiredCredentials " +
           "FROM ProductionCredentialEntity c")
    Object[] getCredentialStatistics(@Param("now") Instant now);

    /**
     * Find credentials issued in the last N days
     */
    @Query("SELECT c FROM ProductionCredentialEntity c WHERE c.issuedAt > :since ORDER BY c.issuedAt DESC")
    List<ProductionCredentialEntity> findRecentlyIssuedCredentials(@Param("since") Instant since);

    /**
     * Find credentials with specific proof type
     */
    List<ProductionCredentialEntity> findByProofType(String proofType);

    /**
     * Find credentials with specific verification method
     */
    List<ProductionCredentialEntity> findByVerificationMethod(String verificationMethod);

    /**
     * Search credentials by JWT content
     */
    @Query("SELECT c FROM ProductionCredentialEntity c WHERE c.credentialJwt LIKE %:searchTerm%")
    List<ProductionCredentialEntity> searchCredentialsByJwt(@Param("searchTerm") String searchTerm);

    /**
     * Update credential status
     */
    @Query("UPDATE ProductionCredentialEntity c SET c.status = :status, c.updatedAt = :updatedAt WHERE c.id = :credentialId")
    int updateCredentialStatus(@Param("credentialId") UUID credentialId, 
                              @Param("status") String status, 
                              @Param("updatedAt") Instant updatedAt);

    /**
     * Revoke credential
     */
    @Query("UPDATE ProductionCredentialEntity c SET " +
           "c.status = 'REVOKED', " +
           "c.revokedAt = :revokedAt, " +
           "c.revocationReason = :reason, " +
           "c.revokedBy = :revokedBy, " +
           "c.updatedAt = :updatedAt " +
           "WHERE c.id = :credentialId")
    int revokeCredential(@Param("credentialId") UUID credentialId, 
                        @Param("revokedAt") Instant revokedAt, 
                        @Param("reason") String reason, 
                        @Param("revokedBy") String revokedBy, 
                        @Param("updatedAt") Instant updatedAt);

    /**
     * Count credentials issued per day
     */
    @Query("SELECT DATE(c.issuedAt), COUNT(c) FROM ProductionCredentialEntity c " +
           "WHERE c.issuedAt >= :since " +
           "GROUP BY DATE(c.issuedAt) " +
           "ORDER BY DATE(c.issuedAt)")
    List<Object[]> countCredentialsIssuedPerDay(@Param("since") Instant since);

    /**
     * Find credentials for batch processing (paginated)
     */
    @Query("SELECT c FROM ProductionCredentialEntity c WHERE c.id > :lastId ORDER BY c.id")
    List<ProductionCredentialEntity> findCredentialsForBatchProcessing(@Param("lastId") UUID lastId);

    /**
     * Find credentials with metadata containing specific key-value
     */
    @Query("SELECT c FROM ProductionCredentialEntity c WHERE c.metadata LIKE %:keyValue%")
    List<ProductionCredentialEntity> findCredentialsByMetadata(@Param("keyValue") String keyValue);

    /**
     * Count credentials by user
     */
    @Query("SELECT c.user.id, COUNT(c) FROM ProductionCredentialEntity c GROUP BY c.user.id")
    List<Object[]> countCredentialsByUser();

    /**
     * Get credentials usage analytics
     */
    @Query("SELECT " +
           "c.credentialType, " +
           "c.issuerId, " +
           "COUNT(c) as total, " +
           "COUNT(CASE WHEN c.status = 'VALID' THEN 1 END) as valid, " +
           "COUNT(CASE WHEN c.status = 'REVOKED' THEN 1 END) as revoked " +
           "FROM ProductionCredentialEntity c " +
           "GROUP BY c.credentialType, c.issuerId")
    List<Object[]> getCredentialUsageAnalytics();

    /**
     * Find credentials that need renewal (expiring soon)
     */
    @Query("SELECT c FROM ProductionCredentialEntity c " +
           "WHERE c.expiresAt BETWEEN :now AND :renewalThreshold " +
           "AND c.status = 'VALID' " +
           "ORDER BY c.expiresAt ASC")
    List<ProductionCredentialEntity> findCredentialsNeedingRenewal(@Param("now") Instant now, 
                                                                   @Param("renewalThreshold") Instant renewalThreshold);

    /**
     * Bulk update expired credentials status
     */
    @Query("UPDATE ProductionCredentialEntity c SET c.status = 'EXPIRED', c.updatedAt = :now " +
           "WHERE c.expiresAt < :now AND c.status = 'VALID'")
    int updateExpiredCredentialsStatus(@Param("now") Instant now);

    /**
     * Count total credentials
     */
    @Query("SELECT COUNT(c) FROM ProductionCredentialEntity c")
    long countTotalCredentials();

    /**
     * Find credentials by nonce
     */
    Optional<ProductionCredentialEntity> findByNonce(String nonce);
}
