package com.finpass.verifier.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class CreateIntentRequest {

	@NotBlank
	private String payerDid;

	@NotBlank
	private String receiverDid;

	@Min(1)
	private long amount;

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
}
