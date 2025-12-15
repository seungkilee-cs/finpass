package com.finpass.verifier.payment.model;

import java.time.Instant;

public class PaymentIntent {

	public enum Status {
		CREATED,
		KYC_VERIFIED,
		CONFIRMED
	}

	private String id;
	private String payerDid;
	private String receiverDid;
	private long amount;
	private Status status;
	private String decisionToken;
	private Instant createdAt;
	private Instant confirmedAt;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getPayerDid() {
		return payerDid;
	}

	public void setPayerDid(String payerDid) {
		this.payerDid = payerDid;
	}

	public String getReceiverDid() {
		return receiverDid;
	}

	public void setReceiverDid(String receiverDid) {
		this.receiverDid = receiverDid;
	}

	public long getAmount() {
		return amount;
	}

	public void setAmount(long amount) {
		this.amount = amount;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public String getDecisionToken() {
		return decisionToken;
	}

	public void setDecisionToken(String decisionToken) {
		this.decisionToken = decisionToken;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getConfirmedAt() {
		return confirmedAt;
	}

	public void setConfirmedAt(Instant confirmedAt) {
		this.confirmedAt = confirmedAt;
	}
}
