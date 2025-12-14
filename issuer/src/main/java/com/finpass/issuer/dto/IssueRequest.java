package com.finpass.issuer.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class IssueRequest {

	@NotBlank
	private String holderDid;

	@NotNull
	private Map<String, Object> passportData;

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
}
