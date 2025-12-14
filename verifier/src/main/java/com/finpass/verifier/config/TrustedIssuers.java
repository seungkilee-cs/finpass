package com.finpass.verifier.config;

import java.text.ParseException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.Ed25519Verifier;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;

@Component
public class TrustedIssuers {

	private final String trustedIssuerDid;
	private final OctetKeyPair trustedIssuerPublicJwk;
	private final JWSVerifier trustedIssuerVerifier;

	public TrustedIssuers(
			@Value("${trusted.issuerDid}") String trustedIssuerDid,
			@Value("${trusted.issuerPublicJwk:}") String trustedIssuerPublicJwk
	) {
		this.trustedIssuerDid = trustedIssuerDid;
		this.trustedIssuerPublicJwk = parsePublicJwk(trustedIssuerPublicJwk);
		try {
			this.trustedIssuerVerifier = new Ed25519Verifier(this.trustedIssuerPublicJwk);
		} catch (JOSEException e) {
			throw new RuntimeException("Failed to initialize trusted issuer verifier", e);
		}
	}

	public boolean isTrusted(String issuerDid) {
		return issuerDid != null && issuerDid.equals(trustedIssuerDid);
	}

	public JWSVerifier verifierFor(String issuerDid) {
		if (!isTrusted(issuerDid)) {
			throw new IllegalArgumentException("Untrusted issuer: " + issuerDid);
		}
		return trustedIssuerVerifier;
	}

	public OctetKeyPair trustedIssuerPublicJwk() {
		return trustedIssuerPublicJwk;
	}

	private static OctetKeyPair parsePublicJwk(String jwkJson) {
		if (jwkJson == null || jwkJson.isBlank()) {
			throw new IllegalStateException("trusted.issuerPublicJwk (TRUSTED_ISSUER_PUBLIC_JWK) must be set for verifier");
		}
		try {
			OctetKeyPair parsed = OctetKeyPair.parse(jwkJson);
			return parsed.toPublicJWK();
		} catch (ParseException ignored) {
			// continue
		}

		try {
			JWKSet jwks = JWKSet.parse(jwkJson);
			if (jwks.getKeys() == null || jwks.getKeys().isEmpty()) {
				throw new IllegalArgumentException("trusted.issuerPublicJwk JWKS has no keys");
			}
			JWK first = jwks.getKeys().get(0);
			OctetKeyPair okp = first.toOctetKeyPair();
			return okp.toPublicJWK();
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid trusted.issuerPublicJwk", e);
		}
	}
}
