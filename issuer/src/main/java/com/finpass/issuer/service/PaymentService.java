package com.finpass.issuer.service;

import com.finpass.issuer.entity.PaymentEntity;
import com.finpass.issuer.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for payment processing with comprehensive audit logging
 */
@Service
@Transactional
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final AuditService auditService;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository, AuditService auditService) {
        this.paymentRepository = paymentRepository;
        this.auditService = auditService;
    }

    /**
     * Initiate a new payment
     */
    public PaymentEntity initiatePayment(String payerDid, String payeeDid, BigDecimal amount, 
                                       String currency, Map<String, Object> paymentDetails,
                                       HttpServletRequest request) {
        Instant now = Instant.now();
        
        // Create payment entity
        PaymentEntity payment = new PaymentEntity();
        payment.setId(UUID.randomUUID());
        payment.setPayerDid(payerDid);
        payment.setPayeeDid(payeeDid);
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment.setStatus(PaymentEntity.PaymentStatus.PENDING);
        payment.setPaymentType(PaymentEntity.PaymentType.ONE_TIME);
        payment.setTransactionId(generateTransactionId());
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);

        // Set additional payment details
        if (paymentDetails != null) {
            if (paymentDetails.containsKey("description")) {
                payment.setDescription((String) paymentDetails.get("description"));
            }
            if (paymentDetails.containsKey("paymentMethod")) {
                payment.setPaymentMethod((String) paymentDetails.get("paymentMethod"));
            }
            if (paymentDetails.containsKey("kycDecisionToken")) {
                payment.setKycDecisionToken((String) paymentDetails.get("kycDecisionToken"));
            }
            if (paymentDetails.containsKey("feeAmount")) {
                payment.setFeeAmount((BigDecimal) paymentDetails.get("feeAmount"));
            }
            if (paymentDetails.containsKey("taxAmount")) {
                payment.setTaxAmount((BigDecimal) paymentDetails.get("taxAmount"));
            }
        }

        // Save payment
        payment = paymentRepository.save(payment);

        // Log payment initiation
        auditService.logPaymentInitiated(payerDid, payeeDid, payment.getId().toString(), 
                                       amount, currency);

        logger.info("Payment initiated: {} for amount: {} {}", 
                   payment.getId(), amount, currency);

        return payment;
    }

    /**
     * Authorize a payment
     */
    public PaymentEntity authorizePayment(UUID paymentId, String authorizedBy, 
                                        HttpServletRequest request) {
        PaymentEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentEntity.PaymentStatus.PENDING) {
            throw new IllegalStateException("Payment cannot be authorized in current state: " + payment.getStatus());
        }

        Instant now = Instant.now();
        payment.setStatus(PaymentEntity.PaymentStatus.AUTHORIZED);
        payment.setAuthorizedAt(now);
        payment.setUpdatedAt(now);

        payment = paymentRepository.save(payment);

        // Log payment authorization
        Map<String, Object> details = Map.of(
            "action", "AUTHORIZE",
            "resourceId", payment.getId().toString(),
            "resourceType", "PAYMENT",
            "authorizedBy", authorizedBy,
            "amount", payment.getAmount().toString(),
            "currency", payment.getCurrency(),
            "description", "Payment authorized: " + payment.getAmount() + " " + payment.getCurrency()
        );

        auditService.logEvent(com.finpass.issuer.entity.AuditEventEntity.EventType.PAYMENT_COMPLETED, 
                            payment.getPayerDid(), details, request);

        logger.info("Payment authorized: {} by {}", paymentId, authorizedBy);
        return payment;
    }

    /**
     * Capture a payment
     */
    public PaymentEntity capturePayment(UUID paymentId, String capturedBy, 
                                     HttpServletRequest request) {
        PaymentEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentEntity.PaymentStatus.AUTHORIZED) {
            throw new IllegalStateException("Payment cannot be captured in current state: " + payment.getStatus());
        }

        Instant now = Instant.now();
        payment.setStatus(PaymentEntity.PaymentStatus.CAPTURED);
        payment.setCapturedAt(now);
        payment.setCompletedAt(now);
        payment.setUpdatedAt(now);

        // Calculate total amount
        BigDecimal totalAmount = payment.getAmount();
        if (payment.getFeeAmount() != null) {
            totalAmount = totalAmount.add(payment.getFeeAmount());
        }
        if (payment.getTaxAmount() != null) {
            totalAmount = totalAmount.add(payment.getTaxAmount());
        }
        payment.setTotalAmount(totalAmount);

        payment = paymentRepository.save(payment);

        // Log payment capture
        auditService.logPaymentCompleted(payment.getPayerDid(), payment.getId().toString(), 
                                       "CAPTURED", totalAmount);

        logger.info("Payment captured: {} for amount: {} {}", 
                   paymentId, totalAmount, payment.getCurrency());
        return payment;
    }

    /**
     * Fail a payment
     */
    public PaymentEntity failPayment(UUID paymentId, String reason, String errorCode, 
                                   HttpServletRequest request) {
        PaymentEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        Instant now = Instant.now();
        payment.setStatus(PaymentEntity.PaymentStatus.FAILED);
        payment.setFailedAt(now);
        payment.setCompletedAt(now);
        payment.setFailureReason(reason);
        payment.setUpdatedAt(now);

        payment = paymentRepository.save(payment);

        // Log payment failure
        auditService.logPaymentFailed(payment.getPayerDid(), payment.getId().toString(), 
                                    reason, errorCode);

        logger.warn("Payment failed: {} - {}", paymentId, reason);
        return payment;
    }

    /**
     * Refund a payment
     */
    public PaymentEntity refundPayment(UUID paymentId, String reason, String refundedBy, 
                                     HttpServletRequest request) {
        PaymentEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentEntity.PaymentStatus.CAPTURED) {
            throw new IllegalStateException("Payment cannot be refunded in current state: " + payment.getStatus());
        }

        Instant now = Instant.now();
        payment.setStatus(PaymentEntity.PaymentStatus.REFUNDED);
        payment.setRefundedAt(now);
        payment.setRefundReason(reason);
        payment.setUpdatedAt(now);

        payment = paymentRepository.save(payment);

        // Log payment refund
        Map<String, Object> details = Map.of(
            "action", "REFUND",
            "resourceId", payment.getId().toString(),
            "resourceType", "PAYMENT",
            "refundReason", reason,
            "refundedBy", refundedBy,
            "amount", payment.getAmount().toString(),
            "currency", payment.getCurrency(),
            "description", "Payment refunded: " + reason
        );

        auditService.logEvent(com.finpass.issuer.entity.AuditEventEntity.EventType.PAYMENT_FAILED, 
                            payment.getPayerDid(), details, request);

        logger.info("Payment refunded: {} - {}", paymentId, reason);
        return payment;
    }

    /**
     * Cancel a payment
     */
    public PaymentEntity cancelPayment(UUID paymentId, String reason, String cancelledBy, 
                                     HttpServletRequest request) {
        PaymentEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentEntity.PaymentStatus.PENDING && 
            payment.getStatus() != PaymentEntity.PaymentStatus.AUTHORIZED) {
            throw new IllegalStateException("Payment cannot be cancelled in current state: " + payment.getStatus());
        }

        Instant now = Instant.now();
        payment.setStatus(PaymentEntity.PaymentStatus.CANCELLED);
        payment.setUpdatedAt(now);

        // Set failure reason for tracking
        payment.setFailureReason(reason);

        payment = paymentRepository.save(payment);

        // Log payment cancellation
        Map<String, Object> details = Map.of(
            "action", "CANCEL",
            "resourceId", payment.getId().toString(),
            "resourceType", "PAYMENT",
            "cancellationReason", reason,
            "cancelledBy", cancelledBy,
            "amount", payment.getAmount().toString(),
            "currency", payment.getCurrency(),
            "description", "Payment cancelled: " + reason
        );

        auditService.logEvent(com.finpass.issuer.entity.AuditEventEntity.EventType.PAYMENT_FAILED, 
                            payment.getPayerDid(), details, request);

        logger.info("Payment cancelled: {} - {}", paymentId, reason);
        return payment;
    }

    /**
     * Get payment by ID
     */
    public PaymentEntity getPayment(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
    }

    /**
     * Get payments for a user
     */
    public List<PaymentEntity> getUserPayments(String userDid) {
        return paymentRepository.findPaymentsForUser(userDid);
    }

    /**
     * Get payments by status
     */
    public List<PaymentEntity> getPaymentsByStatus(PaymentEntity.PaymentStatus status) {
        return paymentRepository.findByStatus(status);
    }

    /**
     * Get payments that need attention
     */
    public List<PaymentEntity> getPaymentsNeedingAttention() {
        Instant pendingCutoff = Instant.now().minusSeconds(3600); // 1 hour
        Instant authorizedCutoff = Instant.now().minusSeconds(1800); // 30 minutes
        
        return paymentRepository.findPaymentsNeedingAttention(pendingCutoff, authorizedCutoff);
    }

    /**
     * Get payment statistics
     */
    public Object[] getPaymentStatistics() {
        return paymentRepository.getPaymentStatistics();
    }

    /**
     * Process pending payments
     */
    public void processPendingPayments() {
        List<PaymentEntity> pendingPayments = paymentRepository.findPendingPayments();
        
        for (PaymentEntity payment : pendingPayments) {
            try {
                // Check if payment is too old and should be failed
                if (payment.getCreatedAt().isBefore(Instant.now().minusSeconds(7200))) { // 2 hours
                    failPayment(payment.getId(), "Payment timed out", "TIMEOUT", null);
                }
            } catch (Exception e) {
                logger.error("Error processing pending payment: {}", payment.getId(), e);
            }
        }
    }

    /**
     * Generate unique transaction ID
     */
    private String generateTransactionId() {
        return "txn_" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Validate payment amount
     */
    private void validatePaymentAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        
        // Maximum amount limit (e.g., 1,000,000)
        BigDecimal maxAmount = new BigDecimal("1000000");
        if (amount.compareTo(maxAmount) > 0) {
            throw new IllegalArgumentException("Payment amount exceeds maximum limit");
        }
    }

    /**
     * Validate currency code
     */
    private void validateCurrency(String currency) {
        if (currency == null || currency.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency is required");
        }
        
        if (!currency.matches("^[A-Z]{3}$")) {
            throw new IllegalArgumentException("Invalid currency code format");
        }
    }

    /**
     * Validate DID format
     */
    private void validateDid(String did) {
        if (did == null || !did.startsWith("did:")) {
            throw new IllegalArgumentException("Invalid DID format");
        }
    }
}
