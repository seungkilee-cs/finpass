package com.finpass.issuer.dto;

import java.util.UUID;

public class StatusResponse {

	private UUID credId;
	private String status;

	public UUID getCredId() {
		return credId;
	}

	public void setCredId(UUID credId) {
		this.credId = credId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
