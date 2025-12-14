package com.finpass.verifier.dto;

import java.util.List;

public class VerifyResponse {

	private String decisionToken;
	private String assuranceLevel;
	private List<String> verifiedClaims;
	private long expiresIn;

	public String getDecisionToken() {
		return decisionToken;
	}

	public void setDecisionToken(String decisionToken) {
		this.decisionToken = decisionToken;
	}

	public String getAssuranceLevel() {
		return assuranceLevel;
	}

	public void setAssuranceLevel(String assuranceLevel) {
		this.assuranceLevel = assuranceLevel;
	}

	public List<String> getVerifiedClaims() {
		return verifiedClaims;
	}

	public void setVerifiedClaims(List<String> verifiedClaims) {
		this.verifiedClaims = verifiedClaims;
	}

	public long getExpiresIn() {
		return expiresIn;
	}

	public void setExpiresIn(long expiresIn) {
		this.expiresIn = expiresIn;
	}
}
