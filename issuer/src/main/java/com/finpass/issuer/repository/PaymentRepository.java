package com.finpass.issuer.repository;

import com.finpass.issuer.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PaymentRepository with comprehensive payment tracking capabilities
 */
@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {

    /**
     * Find payments by payer DID
     */
    List<PaymentEntity> findByPayerDid(String payerDid);

    /**
     * Find payments by payee DID
     */
    List<PaymentEntity> findByPayeeDid(String payeeDid);

    /**
     * Find payments by status
     */
    List<PaymentEntity> findByStatus(PaymentEntity.PaymentStatus status);

    /**
     * Find payments by payment type
     */
    List<PaymentEntity> findByPaymentType(PaymentEntity.PaymentType paymentType);

    /**
     * Find payment by transaction ID
     */
    Optional<PaymentEntity> findByTransactionId(String transactionId);

    /**
     * Find payments by reference ID
     */
    List<PaymentEntity> findByReferenceId(String referenceId);

    /**
     * Find payments by payment method
     */
    List<PaymentEntity> findByPaymentMethod(String paymentMethod);

    /**
     * Find payments created within a date range
     */
    @Query("SELECT p FROM PaymentEntity p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    List<PaymentEntity> findPaymentsCreatedBetween(@Param("startDate") Instant startDate, 
                                                   @Param("endDate") Instant endDate);

    /**
     * Find payments completed within a date range
     */
    @Query("SELECT p FROM PaymentEntity p WHERE p.completedAt BETWEEN :startDate AND :endDate")
    List<PaymentEntity> findPaymentsCompletedBetween(@Param("startDate") Instant startDate, 
                                                     @Param("endDate") Instant endDate);

    /**
     * Find payments between payer and payee
     */
    @Query("SELECT p FROM PaymentEntity p WHERE p.payerDid = :payerDid AND p.payeeDid = :payeeDid")
    List<PaymentEntity> findPaymentsBetweenParties(@Param("payerDid") String payerDid, 
                                                   @Param("payeeDid") String payeeDid);

    /**
     * Find payments by amount range
     */
    @Query("SELECT p FROM PaymentEntity p WHERE p.amount BETWEEN :minAmount AND :maxAmount")
    List<PaymentEntity> findPaymentsByAmountRange(@Param("minAmount") BigDecimal minAmount, 
                                                  @Param("maxAmount") BigDecimal maxAmount);

    /**
     * Find payments by currency
     */
    List<PaymentEntity> findByCurrency(String currency);

    /**
     * Find pending payments
     */
    @Query("SELECT p FROM PaymentEntity p WHERE p.status = 'PENDING' ORDER BY p.createdAt ASC")
    List<PaymentEntity> findPendingPayments();

    /**
     * Find failed payments
     */
    @Query("SELECT p FROM PaymentEntity p WHERE p.status = 'FAILED' ORDER BY p.failedAt DESC")
    List<PaymentEntity> findFailedPayments();

    /**
     * Find authorized payments not yet captured
     */
    @Query("SELECT p FROM PaymentEntity p WHERE p.status = 'AUTHORIZED' ORDER BY p.authorizedAt ASC")
    List<PaymentEntity> findAuthorizedPayments();

    /**
     * Find payments for a specific user (as payer or payee)
     */
    @Query("SELECT p FROM PaymentEntity p WHERE p.payerDid = :userDid OR p.payeeDid = :userDid ORDER BY p.createdAt DESC")
    List<PaymentEntity> findPaymentsForUser(@Param("userDid") String userDid);

    /**
     * Count payments by status
     */
    @Query("SELECT p.status, COUNT(p) FROM PaymentEntity p GROUP BY p.status")
    List<Object[]> countPaymentsByStatus();

    /**
     * Count payments by type
     */
    @Query("SELECT p.paymentType, COUNT(p) FROM PaymentEntity p GROUP BY p.paymentType")
    List<Object[]> countPaymentsByType();

    /**
     * Count payments by currency
     */
    @Query("SELECT p.currency, COUNT(p) FROM PaymentEntity p GROUP BY p.currency")
    List<Object[]> countPaymentsByCurrency();

    /**
     * Get payment statistics
     */
    @Query("SELECT " +
           "COUNT(p) as totalPayments, " +
           "SUM(p.amount) as totalAmount, " +
           "COUNT(CASE WHEN p.status = 'COMPLETED' THEN 1 END) as completedPayments, " +
           "COUNT(CASE WHEN p.status = 'FAILED' THEN 1 END) as failedPayments, " +
           "COUNT(CASE WHEN p.status = 'PENDING' THEN 1 END) as pendingPayments " +
           "FROM PaymentEntity p")
    Object[] getPaymentStatistics();

    /**
     * Get total amount by status
     */
    @Query("SELECT p.status, SUM(p.amount) FROM PaymentEntity p GROUP BY p.status")
    List<Object[]> getTotalAmountByStatus();

    /**
     * Find payments with KYC decision token
     */
    List<PaymentEntity> findByKycDecisionTokenIsNotNull();

    /**
     * Find payments by KYC decision token
     */
    Optional<PaymentEntity> findByKycDecisionToken(String kycDecisionToken);

    /**
     * Find payments created in the last N days
     */
    @Query("SELECT p FROM PaymentEntity p WHERE p.createdAt > :since ORDER BY p.createdAt DESC")
    List<PaymentEntity> findRecentlyCreatedPayments(@Param("since") Instant since);

    /**
     * Find completed payments in the last N days
     */
    @Query("SELECT p FROM PaymentEntity p WHERE p.completedAt > :since ORDER BY p.completedAt DESC")
    List<PaymentEntity> findRecentlyCompletedPayments(@Param("since") Instant since);

    /**
     * Update payment status
     */
    @Query("UPDATE PaymentEntity p SET p.status = :status, p.updatedAt = :updatedAt WHERE p.id = :paymentId")
    int updatePaymentStatus(@Param("paymentId") UUID paymentId, 
                           @Param("status") PaymentEntity.PaymentStatus status, 
                           @Param("updatedAt") Instant updatedAt);

    /**
     * Authorize payment
     */
    @Query("UPDATE PaymentEntity p SET " +
           "p.status = 'AUTHORIZED', " +
           "p.authorizedAt = :authorizedAt, " +
           "p.updatedAt = :updatedAt " +
           "WHERE p.id = :paymentId")
    int authorizePayment(@Param("paymentId") UUID paymentId, 
                        @Param("authorizedAt") Instant authorizedAt, 
                        @Param("updatedAt") Instant updatedAt);

    /**
     * Capture payment
     */
    @Query("UPDATE PaymentEntity p SET " +
           "p.status = 'CAPTURED', " +
           "p.capturedAt = :capturedAt, " +
           "p.completedAt = :completedAt, " +
           "p.updatedAt = :updatedAt " +
           "WHERE p.id = :paymentId")
    int capturePayment(@Param("paymentId") UUID paymentId, 
                      @Param("capturedAt") Instant capturedAt, 
                      @Param("completedAt") Instant completedAt, 
                      @Param("updatedAt") Instant updatedAt);

    /**
     * Fail payment
     */
    @Query("UPDATE PaymentEntity p SET " +
           "p.status = 'FAILED', " +
           "p.failedAt = :failedAt, " +
           "p.failureReason = :failureReason, " +
           "p.completedAt = :completedAt, " +
           "p.updatedAt = :updatedAt " +
           "WHERE p.id = :paymentId")
    int failPayment(@Param("paymentId") UUID paymentId, 
                   @Param("failedAt") Instant failedAt, 
                   @Param("failureReason") String failureReason, 
                   @Param("completedAt") Instant completedAt, 
                   @Param("updatedAt") Instant updatedAt);

    /**
     * Refund payment
     */
    @Query("UPDATE PaymentEntity p SET " +
           "p.status = 'REFUNDED', " +
           "p.refundedAt = :refundedAt, " +
           "p.refundReason = :refundReason, " +
           "p.updatedAt = :updatedAt " +
           "WHERE p.id = :paymentId")
    int refundPayment(@Param("paymentId") UUID paymentId, 
                     @Param("refundedAt") Instant refundedAt, 
                     @Param("refundReason") String refundReason, 
                     @Param("updatedAt") Instant updatedAt);

    /**
     * Count payments per day
     */
    @Query("SELECT DATE(p.createdAt), COUNT(p) FROM PaymentEntity p " +
           "WHERE p.createdAt >= :since " +
           "GROUP BY DATE(p.createdAt) " +
           "ORDER BY DATE(p.createdAt)")
    List<Object[]> countPaymentsPerDay(@Param("since") Instant since);

    /**
     * Sum payments per day
     */
    @Query("SELECT DATE(p.createdAt), SUM(p.amount) FROM PaymentEntity p " +
           "WHERE p.createdAt >= :since " +
           "GROUP BY DATE(p.createdAt) " +
           "ORDER BY DATE(p.createdAt)")
    List<Object[]> sumPaymentsPerDay(@Param("since") Instant since);

    /**
     * Find payments for batch processing (paginated)
     */
    @Query("SELECT p FROM PaymentEntity p WHERE p.id > :lastId ORDER BY p.id")
    List<PaymentEntity> findPaymentsForBatchProcessing(@Param("lastId") UUID lastId);

    /**
     * Find payments with metadata containing specific key-value
     */
    @Query("SELECT p FROM PaymentEntity p WHERE p.metadata LIKE %:keyValue%")
    List<PaymentEntity> findPaymentsByMetadata(@Param("keyValue") String keyValue);

    /**
     * Get payment volume by user
     */
    @Query("SELECT p.payerDid, COUNT(p), SUM(p.amount) FROM PaymentEntity p " +
           "WHERE p.status = 'CAPTURED' " +
           "GROUP BY p.payerDid")
    List<Object[]> getPaymentVolumeByUser();

    /**
     * Get payment analytics by date range
     */
    @Query("SELECT " +
           "DATE(p.createdAt) as date, " +
           "COUNT(p) as count, " +
           "SUM(p.amount) as total, " +
           "AVG(p.amount) as average " +
           "FROM PaymentEntity p " +
           "WHERE p.createdAt BETWEEN :start AND :end " +
           "GROUP BY DATE(p.createdAt) " +
           "ORDER BY DATE(p.createdAt)")
    List<Object[]> getPaymentAnalyticsByDateRange(@Param("start") Instant start, 
                                                   @Param("end") Instant end);

    /**
     * Find high-value payments
     */
    @Query("SELECT p FROM PaymentEntity p WHERE p.amount > :threshold ORDER BY p.amount DESC")
    List<PaymentEntity> findHighValuePayments(@Param("threshold") BigDecimal threshold);

    /**
     * Count total payments
     */
    @Query("SELECT COUNT(p) FROM PaymentEntity p")
    long countTotalPayments();

    /**
     * Sum total payment amount
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentEntity p WHERE p.status = 'CAPTURED'")
    BigDecimal sumTotalCapturedAmount();

    /**
     * Find payments that need attention (pending too long, failed, etc.)
     */
    @Query("SELECT p FROM PaymentEntity p WHERE " +
           "(p.status = 'PENDING' AND p.createdAt < :pendingCutoff) OR " +
           "(p.status = 'AUTHORIZED' AND p.authorizedAt < :authorizedCutoff) OR " +
           "p.status = 'FAILED'")
    List<PaymentEntity> findPaymentsNeedingAttention(@Param("pendingCutoff") Instant pendingCutoff, 
                                                     @Param("authorizedCutoff") Instant authorizedCutoff);
}
