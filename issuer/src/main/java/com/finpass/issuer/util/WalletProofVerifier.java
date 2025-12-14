package com.finpass.issuer.util;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.crypto.Sign.SignatureData;
import org.web3j.utils.Numeric;

public final class WalletProofVerifier {

	private WalletProofVerifier() {
	}

	public static boolean verifyPrefixedMessageSignature(String message, String signatureHex, String expectedAddress) {
		try {
			String recovered = recoverAddressFromPrefixedMessage(message, signatureHex);
			return normalizeAddress(recovered).equals(normalizeAddress(expectedAddress));
		} catch (Exception e) {
			return false;
		}
	}

	public static String recoverAddressFromPrefixedMessage(String message, String signatureHex) throws Exception {
		byte[] sig = Numeric.hexStringToByteArray(signatureHex);
		if (sig.length != 65) {
			throw new IllegalArgumentException("Invalid signature length");
		}

		byte[] r = Arrays.copyOfRange(sig, 0, 32);
		byte[] s = Arrays.copyOfRange(sig, 32, 64);
		byte v = sig[64];
		if (v < 27) {
			v = (byte) (v + 27);
		}

		SignatureData signatureData = new SignatureData(v, r, s);
		BigInteger publicKey = Sign.signedPrefixedMessageToKey(message.getBytes(StandardCharsets.UTF_8), signatureData);
		return "0x" + Keys.getAddress(publicKey);
	}

	private static String normalizeAddress(String address) {
		if (address == null) return "";
		String a = address.trim().toLowerCase();
		if (a.startsWith("0x")) {
			a = a.substring(2);
		}
		return a;
	}
}
