package com.finpass.issuer.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class IssueWithProofRequest {

	@NotBlank
	private String holderDid;

	@NotNull
	private Map<String, Object> passportData;

	@NotBlank
	private String holderAddress;

	@NotBlank
	private String proofPayload;

	@NotBlank
	private String proofSignature;

	@NotNull
	private LivenessProof livenessProof;

	public String getHolderDid() {
		return holderDid;
	}

	public void setHolderDid(String holderDid) {
		this.holderDid = holderDid;
	}

	public Map<String, Object> getPassportData() {
		return passportData;
	}

	public void setPassportData(Map<String, Object> passportData) {
		this.passportData = passportData;
	}

	public String getHolderAddress() {
		return holderAddress;
	}

	public void setHolderAddress(String holderAddress) {
		this.holderAddress = holderAddress;
	}

	public String getProofPayload() {
		return proofPayload;
	}

	public void setProofPayload(String proofPayload) {
		this.proofPayload = proofPayload;
	}

	public String getProofSignature() {
		return proofSignature;
	}

	public void setProofSignature(String proofSignature) {
		this.proofSignature = proofSignature;
	}

	public LivenessProof getLivenessProof() {
		return livenessProof;
	}

	public void setLivenessProof(LivenessProof livenessProof) {
		this.livenessProof = livenessProof;
	}
}
