package com.finpass.issuer.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "issuances")
public class IssuanceEntity {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "credential_id", nullable = false)
	private CredentialEntity credential;

	@Column(name = "issuer_did", nullable = false)
	private String issuerDid;

	@Column(nullable = false)
	private Instant timestamp;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public CredentialEntity getCredential() {
		return credential;
	}

	public void setCredential(CredentialEntity credential) {
		this.credential = credential;
	}

	public String getIssuerDid() {
		return issuerDid;
	}

	public void setIssuerDid(String issuerDid) {
		this.issuerDid = issuerDid;
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}
}
