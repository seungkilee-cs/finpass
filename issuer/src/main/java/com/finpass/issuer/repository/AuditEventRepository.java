package com.finpass.issuer.repository;

import com.finpass.issuer.entity.AuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * AuditEventRepository for comprehensive audit logging and compliance
 */
@Repository
public interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID> {

    /**
     * Find audit events by event type
     */
    List<AuditEventEntity> findByEventType(String eventType);

    /**
     * Find audit events by user ID hash
     */
    List<AuditEventEntity> findByUserIdHash(String userIdHash);

    /**
     * Find audit events by session ID
     */
    List<AuditEventEntity> findBySessionId(String sessionId);

    /**
     * Find audit events by IP address
     */
    List<AuditEventEntity> findByIpAddress(String ipAddress);

    /**
     * Find audit events by severity
     */
    List<AuditEventEntity> findBySeverity(String severity);

    /**
     * Find audit events by resource ID
     */
    List<AuditEventEntity> findByResourceId(String resourceId);

    /**
     * Find audit events by resource type
     */
    List<AuditEventEntity> findByResourceType(String resourceType);

    /**
     * Find audit events by action
     */
    List<AuditEventEntity> findByAction(String action);

    /**
     * Find audit events by outcome
     */
    List<AuditEventEntity> findByOutcome(String outcome);

    /**
     * Find audit events by performed by
     */
    List<AuditEventEntity> findByPerformedBy(String performedBy);

    /**
     * Find audit events by correlation ID
     */
    List<AuditEventEntity> findByCorrelationId(String correlationId);

    /**
     * Find audit events created within a date range
     */
    @Query("SELECT a FROM AuditEventEntity a WHERE a.createdAt BETWEEN :startDate AND :endDate")
    List<AuditEventEntity> findEventsCreatedBetween(@Param("startDate") Instant startDate, 
                                                    @Param("endDate") Instant endDate);

    /**
     * Find audit events created after a specific time
     */
    @Query("SELECT a FROM AuditEventEntity a WHERE a.createdAt > :timestamp ORDER BY a.createdAt DESC")
    List<AuditEventEntity> findEventsCreatedAfter(@Param("timestamp") Instant timestamp);

    /**
     * Find audit events created before a specific time
     */
    @Query("SELECT a FROM AuditEventEntity a WHERE a.createdAt < :timestamp ORDER BY a.createdAt DESC")
    List<AuditEventEntity> findEventsCreatedBefore(@Param("timestamp") Instant timestamp);

    /**
     * Find audit events in the last N days
     */
    @Query("SELECT a FROM AuditEventEntity a WHERE a.createdAt > :since ORDER BY a.createdAt DESC")
    List<AuditEventEntity> findRecentEvents(@Param("since") Instant since);

    /**
     * Find audit events by user and event type
     */
    @Query("SELECT a FROM AuditEventEntity a WHERE a.userIdHash = :userIdHash AND a.eventType = :eventType ORDER BY a.createdAt DESC")
    List<AuditEventEntity> findEventsByUserAndType(@Param("userIdHash") String userIdHash, 
                                                   @Param("eventType") String eventType);

    /**
     * Find audit events by resource and action
     */
    @Query("SELECT a FROM AuditEventEntity a WHERE a.resourceId = :resourceId AND a.action = :action ORDER BY a.createdAt DESC")
    List<AuditEventEntity> findEventsByResourceAndAction(@Param("resourceId") String resourceId, 
                                                         @Param("action") String action);

    /**
     * Find error events
     */
    @Query("SELECT a FROM AuditEventEntity a WHERE a.severity IN ('ERROR', 'CRITICAL') ORDER BY a.createdAt DESC")
    List<AuditEventEntity> findErrorEvents();

    /**
     * Find security events
     */
    @Query("SELECT a FROM AuditEventEntity a WHERE a.eventType LIKE '%SECURITY%' OR a.eventType LIKE '%AUTH%' ORDER BY a.createdAt DESC")
    List<AuditEventEntity> findSecurityEvents();

    /**
     * Find failed authentication attempts
     */
    @Query("SELECT a FROM AuditEventEntity a WHERE a.eventType = 'AUTHENTICATION_FAILED' ORDER BY a.createdAt DESC")
    List<AuditEventEntity> findFailedAuthentications();

    /**
     * Count audit events by event type
     */
    @Query("SELECT a.eventType, COUNT(a) FROM AuditEventEntity a GROUP BY a.eventType")
    List<Object[]> countEventsByType();

    /**
     * Count audit events by severity
     */
    @Query("SELECT a.severity, COUNT(a) FROM AuditEventEntity a GROUP BY a.severity")
    List<Object[]> countEventsBySeverity();

    /**
     * Count audit events by outcome
     */
    @Query("SELECT a.outcome, COUNT(a) FROM AuditEventEntity a GROUP BY a.outcome")
    List<Object[]> countEventsByOutcome();

    /**
     * Get audit statistics
     */
    @Query("SELECT " +
           "COUNT(a) as totalEvents, " +
           "COUNT(CASE WHEN a.severity = 'ERROR' THEN 1 END) as errorEvents, " +
           "COUNT(CASE WHEN a.severity = 'WARNING' THEN 1 END) as warningEvents, " +
           "COUNT(CASE WHEN a.severity = 'CRITICAL' THEN 1 END) as criticalEvents, " +
           "COUNT(CASE WHEN a.outcome = 'FAILURE' THEN 1 END) as failedEvents " +
           "FROM AuditEventEntity a")
    Object[] getAuditStatistics();

    /**
     * Count events per day
     */
    @Query("SELECT DATE(a.createdAt), COUNT(a) FROM AuditEventEntity a " +
           "WHERE a.createdAt >= :since " +
           "GROUP BY DATE(a.createdAt) " +
           "ORDER BY DATE(a.createdAt)")
    List<Object[]> countEventsPerDay(@Param("since") Instant since);

    /**
     * Count events by hour for the last 24 hours
     */
    @Query("SELECT EXTRACT(HOUR FROM a.createdAt), COUNT(a) FROM AuditEventEntity a " +
           "WHERE a.createdAt >= :since " +
           "GROUP BY EXTRACT(HOUR FROM a.createdAt) " +
           "ORDER BY EXTRACT(HOUR FROM a.createdAt)")
    List<Object[]> countEventsByHour(@Param("since") Instant since);

    /**
     * Find events with specific error code
     */
    List<AuditEventEntity> findByErrorCode(String errorCode);

    /**
     * Find events by compliance flags
     */
    @Query("SELECT a FROM AuditEventEntity a WHERE a.complianceFlags LIKE %:flag%")
    List<AuditEventEntity> findByComplianceFlag(@Param("flag") String flag);

    /**
     * Find events by source system
     */
    List<AuditEventEntity> findBySourceSystem(String sourceSystem);

    /**
     * Find events for a specific tenant
     */
    List<AuditEventEntity> findByTenantId(String tenantId);

    /**
     * Find events with duration longer than threshold
     */
    @Query("SELECT a FROM AuditEventEntity a WHERE a.durationMs > :threshold ORDER BY a.durationMs DESC")
    List<AuditEventEntity> findSlowEvents(@Param("threshold") Long threshold);

    /**
     * Find events by device fingerprint
     */
    List<AuditEventEntity> findByDeviceFingerprint(String deviceFingerprint);

    /**
     * Find events for batch processing (paginated)
     */
    @Query("SELECT a FROM AuditEventEntity a WHERE a.id > :lastId ORDER BY a.id")
    List<AuditEventEntity> findEventsForBatchProcessing(@Param("lastId") UUID lastId);

    /**
     * Search events by description
     */
    @Query("SELECT a FROM AuditEventEntity a WHERE LOWER(a.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<AuditEventEntity> searchEventsByDescription(@Param("searchTerm") String searchTerm);

    /**
     * Search events by details content
     */
    @Query("SELECT a FROM AuditEventEntity a WHERE a.details LIKE %:searchTerm%")
    List<AuditEventEntity> searchEventsByDetails(@Param("searchTerm") String searchTerm);

    /**
     * Get user activity timeline
     */
    @Query("SELECT a FROM AuditEventEntity a WHERE a.userIdHash = :userIdHash ORDER BY a.createdAt DESC")
    List<AuditEventEntity> getUserActivityTimeline(@Param("userIdHash") String userIdHash);

    /**
     * Get resource activity timeline
     */
    @Query("SELECT a FROM AuditEventEntity a WHERE a.resourceId = :resourceId ORDER BY a.createdAt DESC")
    List<AuditEventEntity> getResourceActivityTimeline(@Param("resourceId") String resourceId);

    /**
     * Find suspicious activities (multiple failed attempts)
     */
    @Query("SELECT a.ipAddress, COUNT(a) as attemptCount FROM AuditEventEntity a " +
           "WHERE a.eventType = 'AUTHENTICATION_FAILED' AND a.createdAt > :since " +
           "GROUP BY a.ipAddress " +
           "HAVING COUNT(a) > :threshold")
    List<Object[]> findSuspiciousActivities(@Param("since") Instant since, @Param("threshold") int threshold);

    /**
     * Delete old audit events based on retention policy
     */
    @Modifying
    @Query("DELETE FROM AuditEventEntity a WHERE a.createdAt < :cutoff AND " +
           "(a.retentionDays IS NULL OR a.createdAt < :retentionCutoff)")
    int deleteOldEvents(@Param("cutoff") Instant cutoff, @Param("retentionCutoff") Instant retentionCutoff);

    /**
     * Archive old events (move to archive table - would need separate implementation)
     */
    @Query("SELECT a FROM AuditEventEntity a WHERE a.createdAt < :cutoff")
    List<AuditEventEntity> findEventsForArchiving(@Param("cutoff") Instant cutoff);

    /**
     * Get compliance report data
     */
    @Query("SELECT " +
           "a.eventType, " +
           "a.severity, " +
           "COUNT(a) as eventCount, " +
           "MAX(a.createdAt) as lastOccurrence " +
           "FROM AuditEventEntity a " +
           "WHERE a.createdAt BETWEEN :start AND :end " +
           "GROUP BY a.eventType, a.severity " +
           "ORDER BY eventCount DESC")
    List<Object[]> getComplianceReport(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * Find events with specific geolocation
     */
    List<AuditEventEntity> findByGeolocation(String geolocation);

    /**
     * Count events by IP address
     */
    @Query("SELECT a.ipAddress, COUNT(a) FROM AuditEventEntity a " +
           "WHERE a.createdAt >= :since " +
           "GROUP BY a.ipAddress " +
           "ORDER BY COUNT(a) DESC")
    List<Object[]> countEventsByIpAddress(@Param("since") Instant since);

    /**
     * Get top performers (users with most successful actions)
     */
    @Query("SELECT a.performedBy, COUNT(a) FROM AuditEventEntity a " +
           "WHERE a.outcome = 'SUCCESS' AND a.createdAt >= :since " +
           "GROUP BY a.performedBy " +
           "ORDER BY COUNT(a) DESC")
    List<Object[]> getTopPerformers(@Param("since") Instant since);

    /**
     * Count total audit events
     */
    @Query("SELECT COUNT(a) FROM AuditEventEntity a")
    long countTotalEvents();

    /**
     * Get database size estimation for audit events
     */
    @Query("SELECT COUNT(a) FROM AuditEventEntity a WHERE a.createdAt >= :since")
    long countEventsSince(@Param("since") Instant since);

    /**
     * Find events by client version
     */
    List<AuditEventEntity> findByClientVersion(String clientVersion);

    /**
     * Find events by API version
     */
    List<AuditEventEntity> findByApiVersion(String apiVersion);

    /**
     * Find events by user ID hash ordered by creation date descending
     */
    List<AuditEventEntity> findByUserIdHashOrderByCreatedAtDesc(String userIdHash);
}
