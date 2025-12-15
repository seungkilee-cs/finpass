package com.finpass.verifier.payment.dto;

import jakarta.validation.constraints.NotBlank;

public class AttachKycRequest {

	@NotBlank
	private String decisionToken;

	public String getDecisionToken() {
		return decisionToken;
	}

	public void setDecisionToken(String decisionToken) {
		this.decisionToken = decisionToken;
	}
}
