package com.finpass.issuer.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class Hashing {

	private Hashing() {
	}

	public static String sha256Hex(String input) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
			return toHex(hash);
		} catch (Exception e) {
			throw new RuntimeException("Failed to compute SHA-256", e);
		}
	}

	private static String toHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}
}
