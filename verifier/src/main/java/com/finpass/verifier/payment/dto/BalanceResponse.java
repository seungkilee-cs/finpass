package com.finpass.verifier.payment.dto;

public class BalanceResponse {

	private String did;
	private long balance;

	public String getDid() {
		return did;
	}

	public void setDid(String did) {
		this.did = did;
	}

	public long getBalance() {
		return balance;
	}

	public void setBalance(long balance) {
		this.balance = balance;
	}
}
