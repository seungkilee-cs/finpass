package com.finpass.issuer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Payment Entity for tracking payment transactions
 */
@Entity
@Table(name = "payments", 
       indexes = {
           @Index(name = "idx_payer_did", columnList = "payer_did"),
           @Index(name = "idx_payee_did", columnList = "payee_did"),
           @Index(name = "idx_status", columnList = "status"),
           @Index(name = "idx_created_at", columnList = "created_at"),
           @Index(name = "idx_completed_at", columnList = "completed_at")
       })
public class PaymentEntity {

    public enum PaymentStatus {
        PENDING,
        AUTHORIZED,
        CAPTURED,
        FAILED,
        REFUNDED,
        CANCELLED
    }

    public enum PaymentType {
        ONE_TIME,
        RECURRING,
        SUBSCRIPTION
    }

    @Id
    private UUID id;

    @Column(name = "payer_did", nullable = false, length = 255)
    private String payerDid;

    @Column(name = "payee_did", nullable = false, length = 255)
    private String payeeDid;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "USD";

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "payment_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PaymentType paymentType = PaymentType.ONE_TIME;

    @Column(name = "kyc_decision_token", length = 500)
    private String kycDecisionToken;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "transaction_id", length = 255, unique = true)
    private String transactionId;

    @Column(name = "reference_id", length = 255)
    private String referenceId;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "fee_amount", precision = 18, scale = 2)
    private BigDecimal feeAmount;

    @Column(name = "tax_amount", precision = 18, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "total_amount", precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "authorized_at")
    private Instant authorizedAt;

    @Column(name = "captured_at")
    private Instant capturedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "refund_reason", length = 500)
    private String refundReason;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    // Constructors
    public PaymentEntity() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public PaymentEntity(String payerDid, String payeeDid, BigDecimal amount, String currency) {
        this();
        this.id = UUID.randomUUID();
        this.payerDid = payerDid;
        this.payeeDid = payeeDid;
        this.amount = amount;
        this.currency = currency;
        this.totalAmount = amount; // Default total to amount
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPayerDid() {
        return payerDid;
    }

    public void setPayerDid(String payerDid) {
        this.payerDid = payerDid;
        this.updatedAt = Instant.now();
    }

    public String getPayeeDid() {
        return payeeDid;
    }

    public void setPayeeDid(String payeeDid) {
        this.payeeDid = payeeDid;
        this.updatedAt = Instant.now();
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
        this.updatedAt = Instant.now();
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
        this.updatedAt = Instant.now();
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
        
        // Set timestamps based on status
        Instant now = Instant.now();
        switch (status) {
            case AUTHORIZED:
                this.authorizedAt = now;
                break;
            case CAPTURED:
                this.capturedAt = now;
                this.completedAt = now;
                break;
            case FAILED:
                this.failedAt = now;
                this.completedAt = now;
                break;
            case REFUNDED:
                this.refundedAt = now;
                break;
            case PENDING:
            case CANCELLED:
                // No specific timestamp for these statuses
                break;
        }
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(PaymentType paymentType) {
        this.paymentType = paymentType;
        this.updatedAt = Instant.now();
    }

    public String getKycDecisionToken() {
        return kycDecisionToken;
    }

    public void setKycDecisionToken(String kycDecisionToken) {
        this.kycDecisionToken = kycDecisionToken;
        this.updatedAt = Instant.now();
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
        this.updatedAt = Instant.now();
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
        this.updatedAt = Instant.now();
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
        this.updatedAt = Instant.now();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = Instant.now();
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
        this.updatedAt = Instant.now();
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
        this.updatedAt = Instant.now();
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
        this.updatedAt = Instant.now();
    }

    public Instant getAuthorizedAt() {
        return authorizedAt;
    }

    public void setAuthorizedAt(Instant authorizedAt) {
        this.authorizedAt = authorizedAt;
        this.updatedAt = Instant.now();
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(Instant capturedAt) {
        this.capturedAt = capturedAt;
        this.updatedAt = Instant.now();
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(Instant failedAt) {
        this.failedAt = failedAt;
        this.updatedAt = Instant.now();
    }

    public Instant getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(Instant refundedAt) {
        this.refundedAt = refundedAt;
        this.updatedAt = Instant.now();
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
        this.updatedAt = Instant.now();
    }

    public String getRefundReason() {
        return refundReason;
    }

    public void setRefundReason(String refundReason) {
        this.refundReason = refundReason;
        this.updatedAt = Instant.now();
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
        this.updatedAt = Instant.now();
    }

    // Business methods
    public boolean isPending() {
        return PaymentStatus.PENDING.equals(this.status);
    }

    public boolean isAuthorized() {
        return PaymentStatus.AUTHORIZED.equals(this.status);
    }

    public boolean isCaptured() {
        return PaymentStatus.CAPTURED.equals(this.status);
    }

    public boolean isFailed() {
        return PaymentStatus.FAILED.equals(this.status);
    }

    public boolean isRefunded() {
        return PaymentStatus.REFUNDED.equals(this.status);
    }

    public boolean isCompleted() {
        return isCaptured() || isFailed() || isRefunded();
    }

    public void authorize() {
        this.setStatus(PaymentStatus.AUTHORIZED);
    }

    public void capture() {
        this.setStatus(PaymentStatus.CAPTURED);
    }

    public void fail(String reason) {
        this.setStatus(PaymentStatus.FAILED);
        this.setFailureReason(reason);
    }

    public void refund(String reason) {
        this.setStatus(PaymentStatus.REFUNDED);
        this.setRefundReason(reason);
    }

    public void cancel() {
        this.setStatus(PaymentStatus.CANCELLED);
    }

    public BigDecimal calculateTotalAmount() {
        BigDecimal total = this.amount;
        if (this.feeAmount != null) {
            total = total.add(this.feeAmount);
        }
        if (this.taxAmount != null) {
            total = total.add(this.taxAmount);
        }
        return total;
    }

    @Override
    public String toString() {
        return "PaymentEntity{" +
                "id=" + id +
                ", payerDid='" + payerDid + '\'' +
                ", payeeDid='" + payeeDid + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}
