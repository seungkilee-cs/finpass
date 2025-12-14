package com.finpass.verifier.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.finpass.verifier.config.TrustedIssuers;
import com.finpass.verifier.dto.VerifyRequest;
import com.finpass.verifier.dto.VerifyResponse;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@Service
public class VerifierService {

	private final TrustedIssuers trustedIssuers;
	private final VerifierKeyProvider keyProvider;
	private final ChallengeStore challengeStore;
	private final String verifierDid;
	private final long decisionTtlSeconds;

	public VerifierService(
			TrustedIssuers trustedIssuers,
			VerifierKeyProvider keyProvider,
			ChallengeStore challengeStore,
			@Value("${verifier.did}") String verifierDid,
			@Value("${decision.ttlSeconds:300}") long decisionTtlSeconds
	) {
		this.trustedIssuers = trustedIssuers;
		this.keyProvider = keyProvider;
		this.challengeStore = challengeStore;
		this.verifierDid = verifierDid;
		this.decisionTtlSeconds = decisionTtlSeconds;
	}

	public String mintChallenge() {
		return challengeStore.mint();
	}

	public long challengeTtlSeconds() {
		return challengeStore.ttlSeconds();
	}

	public VerifyResponse verify(VerifyRequest request) {
		challengeStore.consumeOrThrow(request.getChallenge());

		String issuerDid = verifyCommitmentJwtOrThrow(request);
		if (!trustedIssuers.isTrusted(issuerDid)) {
			throw new IllegalArgumentException("Untrusted issuer");
		}

		List<String> verifiedClaims = validateProofPoC(request);
		String decisionToken = signDecisionToken(request.getHolderDid(), verifiedClaims);

		VerifyResponse resp = new VerifyResponse();
		resp.setDecisionToken(decisionToken);
		resp.setAssuranceLevel("LOW");
		resp.setVerifiedClaims(verifiedClaims);
		resp.setExpiresIn(decisionTtlSeconds);
		return resp;
	}

	private String verifyCommitmentJwtOrThrow(VerifyRequest request) {
		try {
			SignedJWT jwt = SignedJWT.parse(request.getCommitmentJwt());
			JWTClaimsSet claims = jwt.getJWTClaimsSet();

			String issuer = claims.getIssuer();
			String subject = claims.getSubject();
			if (issuer == null || issuer.isBlank()) {
				throw new IllegalArgumentException("commitmentJwt missing iss");
			}
			if (subject == null || subject.isBlank()) {
				throw new IllegalArgumentException("commitmentJwt missing sub");
			}
			if (!subject.equals(request.getHolderDid())) {
				throw new IllegalArgumentException("commitmentJwt sub must match holderDid");
			}

			Object commitmentHash = claims.getClaim("commitment_hash");
			if (commitmentHash == null || commitmentHash.toString().isBlank()) {
				throw new IllegalArgumentException("commitmentJwt missing commitment_hash");
			}

			boolean ok = jwt.verify(trustedIssuers.verifierFor(issuer));
			if (!ok) {
				throw new IllegalArgumentException("Invalid issuer signature over commitmentJwt");
			}

			return issuer;
		} catch (Exception e) {
			if (e instanceof IllegalArgumentException) throw (IllegalArgumentException) e;
			throw new IllegalArgumentException("Invalid commitmentJwt", e);
		}
	}

	private static List<String> validateProofPoC(VerifyRequest request) {
		String proof = request.getProof();
		if (proof == null || proof.isBlank()) {
			throw new IllegalArgumentException("Missing proof");
		}

		Map<String, Object> signals = request.getPublicSignals();
		Object challenge = signals.get("challenge");
		if (challenge == null || !request.getChallenge().equals(challenge.toString())) {
			throw new IllegalArgumentException("Proof must bind to challenge");
		}
		Object predicate = signals.get("predicate");
		Object result = signals.get("result");

		if (predicate == null || !"over_18".equals(predicate.toString())) {
			throw new IllegalArgumentException("Unsupported predicate (expected over_18)");
		}
		if (result == null || !(result instanceof Boolean) || !((Boolean) result)) {
			throw new IllegalArgumentException("Proof result must be true");
		}

		return List.of("over_18");
	}

	private String signDecisionToken(String holderDid, List<String> verifiedClaims) {
		try {
			Instant now = Instant.now();
			Instant exp = now.plusSeconds(decisionTtlSeconds);

			JWTClaimsSet claims = new JWTClaimsSet.Builder()
					.issuer(verifierDid)
					.subject(holderDid)
					.issueTime(java.util.Date.from(now))
					.expirationTime(java.util.Date.from(exp))
					.jwtID(UUID.randomUUID().toString())
					.claim("verified_at", now.toString())
					.claim("assurance_level", "LOW")
					.claim("verified_claims", verifiedClaims)
					.claim("expires_in", decisionTtlSeconds)
					.build();

			JWSHeader header = new JWSHeader.Builder(keyProvider.getAlgorithm())
					.type(JOSEObjectType.JWT)
					.keyID(keyProvider.getKeyId())
					.build();

			SignedJWT jwt = new SignedJWT(header, claims);
			jwt.sign(keyProvider.signer());
			return jwt.serialize();
		} catch (Exception e) {
			throw new RuntimeException("Failed to sign decision token", e);
		}
	}
}
