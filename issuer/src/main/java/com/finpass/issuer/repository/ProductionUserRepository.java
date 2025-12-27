package com.finpass.issuer.repository;

import com.finpass.issuer.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Production UserRepository with enhanced query capabilities
 */
@Repository
public interface ProductionUserRepository extends JpaRepository<UserEntity, UUID> {

    /**
     * Find user by DID
     */
    Optional<UserEntity> findByDid(String did);

    /**
     * Check if user exists by DID
     */
    boolean existsByDid(String did);

    /**
     * Find users by status
     */
    List<UserEntity> findByStatus(String status);

    /**
     * Find users by KYC verification status
     */
    List<UserEntity> findByKycVerified(Boolean kycVerified);

    /**
     * Find users with KYC verified after a specific time
     */
    @Query("SELECT u FROM UserEntity u WHERE u.kycVerified = true AND u.kycVerifiedAt > :timestamp")
    List<UserEntity> findKycVerifiedAfter(@Param("timestamp") Instant timestamp);

    /**
     * Find users who haven't been seen since a specific time
     */
    @Query("SELECT u FROM UserEntity u WHERE u.lastSeen < :timestamp OR u.lastSeen IS NULL")
    List<UserEntity> findInactiveUsersSince(@Param("timestamp") Instant timestamp);

    /**
     * Find users created within a date range
     */
    @Query("SELECT u FROM UserEntity u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    List<UserEntity> findUsersCreatedBetween(@Param("startDate") Instant startDate, 
                                                      @Param("endDate") Instant endDate);

    /**
     * Find users by email
     */
    Optional<UserEntity> findByEmail(String email);

    /**
     * Find users by phone
     */
    Optional<UserEntity> findByPhone(String phone);

    /**
     * Search users by DID, email, or phone
     */
    @Query("SELECT u FROM UserEntity u WHERE " +
           "LOWER(u.did) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<UserEntity> searchUsers(@Param("searchTerm") String searchTerm);

    /**
     * Count users by status
     */
    @Query("SELECT u.status, COUNT(u) FROM UserEntity u GROUP BY u.status")
    List<Object[]> countUsersByStatus();

    /**
     * Count KYC verified vs non-verified users
     */
    @Query("SELECT u.kycVerified, COUNT(u) FROM UserEntity u GROUP BY u.kycVerified")
    List<Object[]> countUsersByKycStatus();

    /**
     * Get user statistics
     */
    @Query("SELECT " +
           "COUNT(u) as totalUsers, " +
           "COUNT(CASE WHEN u.kycVerified = true THEN 1 END) as kycVerifiedUsers, " +
           "COUNT(CASE WHEN u.status = 'ACTIVE' THEN 1 END) as activeUsers, " +
           "COUNT(CASE WHEN u.lastSeen > :since THEN 1 END) as recentlyActiveUsers " +
           "FROM UserEntity u")
    Object[] getUserStatistics(@Param("since") Instant since);

    /**
     * Find users with recent activity
     */
    @Query("SELECT u FROM UserEntity u WHERE u.lastSeen > :since ORDER BY u.lastSeen DESC")
    List<UserEntity> findRecentlyActiveUsers(@Param("since") Instant since);

    /**
     * Find users created in the last N days
     */
    @Query("SELECT u FROM UserEntity u WHERE u.createdAt > :since ORDER BY u.createdAt DESC")
    List<UserEntity> findRecentlyCreatedUsers(@Param("since") Instant since);

    /**
     * Update user last seen timestamp
     */
    @Query("UPDATE UserEntity u SET u.lastSeen = :timestamp WHERE u.id = :userId")
    int updateLastSeen(@Param("userId") UUID userId, @Param("timestamp") Instant timestamp);

    /**
     * Update user KYC verification status
     */
    @Query("UPDATE UserEntity u SET u.kycVerified = :verified, u.kycVerifiedAt = :verifiedAt WHERE u.id = :userId")
    int updateKycStatus(@Param("userId") UUID userId, 
                       @Param("verified") Boolean verified, 
                       @Param("verifiedAt") Instant verifiedAt);

    /**
     * Update user status
     */
    @Query("UPDATE UserEntity u SET u.status = :status WHERE u.id = :userId")
    int updateUserStatus(@Param("userId") UUID userId, @Param("status") String status);

    /**
     * Find users with specific metadata key-value pair
     */
    @Query("SELECT u FROM UserEntity u WHERE u.metadata LIKE %:keyValue%")
    List<UserEntity> findUsersByMetadata(@Param("keyValue") String keyValue);

    /**
     * Get users with expiring credentials (requires join with credentials)
     */
    @Query("SELECT DISTINCT u FROM UserEntity u " +
           "JOIN u.credentials c " +
           "WHERE c.expiresAt BETWEEN :start AND :end")
    List<UserEntity> findUsersWithExpiringCredentials(@Param("start") Instant start, 
                                                               @Param("end") Instant end);

    /**
     * Count users created per day
     */
    @Query("SELECT DATE(u.createdAt), COUNT(u) FROM UserEntity u " +
           "WHERE u.createdAt >= :since " +
           "GROUP BY DATE(u.createdAt) " +
           "ORDER BY DATE(u.createdAt)")
    List<Object[]> countUsersCreatedPerDay(@Param("since") Instant since);

    /**
     * Find duplicate DIDs (data integrity check)
     */
    @Query("SELECT u.did, COUNT(u) FROM UserEntity u GROUP BY u.did HAVING COUNT(u) > 1")
    List<Object[]> findDuplicateDids();

    /**
     * Bulk update user status for inactive users
     */
    @Query("UPDATE UserEntity u SET u.status = 'SUSPENDED' " +
           "WHERE u.status = 'ACTIVE' AND u.lastSeen < :cutoff")
    int suspendInactiveUsers(@Param("cutoff") Instant cutoff);

    /**
     * Get users for batch processing (paginated)
     */
    @Query("SELECT u FROM UserEntity u WHERE u.id > :lastId ORDER BY u.id")
    List<UserEntity> findUsersForBatchProcessing(@Param("lastId") UUID lastId);

    /**
     * Count total users
     */
    @Query("SELECT COUNT(u) FROM UserEntity u")
    long countTotalUsers();

    /**
     * Count users by status and KYC verification
     */
    @Query("SELECT u.status, u.kycVerified, COUNT(u) FROM UserEntity u " +
           "GROUP BY u.status, u.kycVerified")
    List<Object[]> countUsersByStatusAndKyc();
}
