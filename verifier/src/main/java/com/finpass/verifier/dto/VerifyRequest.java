package com.finpass.verifier.dto;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class VerifyRequest {

	@NotBlank
	private String holderDid;

	@NotBlank
	private String challenge;

	@NotBlank
	private String commitmentJwt;

	@NotBlank
	private String proof;

	@NotNull
	private Map<String, Object> publicSignals;

	private List<String> requestedClaims;

	public String getHolderDid() {
		return holderDid;
	}

	public void setHolderDid(String holderDid) {
		this.holderDid = holderDid;
	}

	public String getChallenge() {
		return challenge;
	}

	public void setChallenge(String challenge) {
		this.challenge = challenge;
	}

	public String getCommitmentJwt() {
		return commitmentJwt;
	}

	public void setCommitmentJwt(String commitmentJwt) {
		this.commitmentJwt = commitmentJwt;
	}

	public String getProof() {
		return proof;
	}

	public void setProof(String proof) {
		this.proof = proof;
	}

	public Map<String, Object> getPublicSignals() {
		return publicSignals;
	}

	public void setPublicSignals(Map<String, Object> publicSignals) {
		this.publicSignals = publicSignals;
	}

	public List<String> getRequestedClaims() {
		return requestedClaims;
	}

	public void setRequestedClaims(List<String> requestedClaims) {
		this.requestedClaims = requestedClaims;
	}
}
