package com.finpass.issuer.service;

import java.text.ParseException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator;
import com.nimbusds.jose.crypto.Ed25519Signer;
import com.nimbusds.jose.JWSSigner;

@Component
public class IssuerKeyProvider {

	private final OctetKeyPair signingKey;
	private final JWSSigner signer;

	public IssuerKeyProvider(@Value("${issuer.privateJwk:}") String privateJwk) {
		this.signingKey = loadOrGenerate(privateJwk);
		try {
			this.signer = new Ed25519Signer(signingKey);
		} catch (JOSEException e) {
			throw new RuntimeException("Failed to initialize issuer signer", e);
		}
	}

	public String getKeyId() {
		return signingKey.getKeyID();
	}

	public JWSAlgorithm getAlgorithm() {
		return JWSAlgorithm.EdDSA;
	}

	public JWSSigner signer() {
		return signer;
	}

	public String exportPrivateJwkJson() {
		return signingKey.toJSONString();
	}

	private static OctetKeyPair loadOrGenerate(String privateJwk) {
		if (privateJwk != null && !privateJwk.isBlank()) {
			try {
				return OctetKeyPair.parse(privateJwk);
			} catch (ParseException e) {
				throw new IllegalArgumentException("Invalid issuer.privateJwk", e);
			}
		}

		try {
			return new OctetKeyPairGenerator(Curve.Ed25519)
					.keyUse(KeyUse.SIGNATURE)
					.keyID(UUID.randomUUID().toString())
					.generate();
		} catch (Exception e) {
			throw new RuntimeException("Failed to generate issuer signing key", e);
		}
	}
}
