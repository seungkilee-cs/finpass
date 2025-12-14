package com.finpass.issuer.dto;

import java.util.UUID;

public class IssueResponse {

	private UUID credId;
	private String issuerDid;
	private String status;
	private String credentialJwt;
	private String commitmentHash;
	private String commitmentJwt;

	public UUID getCredId() {
		return credId;
	}

	public void setCredId(UUID credId) {
		this.credId = credId;
	}

	public String getIssuerDid() {
		return issuerDid;
	}

	public void setIssuerDid(String issuerDid) {
		this.issuerDid = issuerDid;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getCredentialJwt() {
		return credentialJwt;
	}

	public void setCredentialJwt(String credentialJwt) {
		this.credentialJwt = credentialJwt;
	}

	public String getCommitmentHash() {
		return commitmentHash;
	}

	public void setCommitmentHash(String commitmentHash) {
		this.commitmentHash = commitmentHash;
	}

	public String getCommitmentJwt() {
		return commitmentJwt;
	}

	public void setCommitmentJwt(String commitmentJwt) {
		this.commitmentJwt = commitmentJwt;
	}
}
