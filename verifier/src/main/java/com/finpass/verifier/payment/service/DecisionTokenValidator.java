package com.finpass.verifier.payment.service;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.finpass.verifier.service.VerifierKeyProvider;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.Ed25519Verifier;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@Component
public class DecisionTokenValidator {

	private final String verifierDid;
	private final JWSVerifier verifier;

	public DecisionTokenValidator(
			VerifierKeyProvider keyProvider,
			@Value("${verifier.did}") String verifierDid
	) {
		this.verifierDid = verifierDid;
		try {
			OctetKeyPair publicJwk = OctetKeyPair.parse(keyProvider.exportPublicJwkJson()).toPublicJWK();
			this.verifier = new Ed25519Verifier(publicJwk);
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize decision token verifier", e);
		}
	}

	public JWTClaimsSet verifyOrThrow(String decisionToken) {
		try {
			SignedJWT jwt = SignedJWT.parse(decisionToken);
			boolean ok = jwt.verify(verifier);
			if (!ok) {
				throw new IllegalArgumentException("Invalid decision token signature");
			}

			JWTClaimsSet claims = jwt.getJWTClaimsSet();
			if (claims.getIssuer() == null || !claims.getIssuer().equals(verifierDid)) {
				throw new IllegalArgumentException("Invalid decision token issuer");
			}

			if (claims.getSubject() == null || claims.getSubject().isBlank()) {
				throw new IllegalArgumentException("Decision token missing sub");
			}

			if (claims.getExpirationTime() == null) {
				throw new IllegalArgumentException("Decision token missing exp");
			}
			if (Instant.now().isAfter(claims.getExpirationTime().toInstant())) {
				throw new IllegalArgumentException("Decision token expired");
			}

			Object verifiedClaims = claims.getClaim("verified_claims");
			if (!(verifiedClaims instanceof List)) {
				throw new IllegalArgumentException("Decision token missing verified_claims");
			}

			return claims;
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid decision token", e);
		}
	}

	public static boolean hasClaim(JWTClaimsSet claims, String requiredClaim) {
		Object verifiedClaims = claims.getClaim("verified_claims");
		if (!(verifiedClaims instanceof List)) return false;
		@SuppressWarnings("unchecked")
		List<Object> list = (List<Object>) verifiedClaims;
		return list.stream().anyMatch(v -> requiredClaim.equals(String.valueOf(v)));
	}

	public static String subjectDid(JWTClaimsSet claims) {
		return claims.getSubject();
	}
}
