package com.finpass.verifier.payment.dto;

public class CreateIntentResponse {

	private String intentId;
	private String status;
	private long amount;
	private String payerDid;
	private String receiverDid;
	private boolean kycVerified;

	public String getIntentId() {
		return intentId;
	}

	public void setIntentId(String intentId) {
		this.intentId = intentId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public long getAmount() {
		return amount;
	}

	public void setAmount(long amount) {
		this.amount = amount;
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

	public boolean isKycVerified() {
		return kycVerified;
	}

	public void setKycVerified(boolean kycVerified) {
		this.kycVerified = kycVerified;
	}
}
