package com.finpass.verifier.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpass.verifier.dto.ChallengeResponse;
import com.finpass.verifier.dto.VerifyRequest;
import com.finpass.verifier.dto.VerifyResponse;
import com.finpass.verifier.service.VerifierKeyProvider;
import com.finpass.verifier.service.VerifierService;

import jakarta.validation.Valid;

@RestController
@RequestMapping
@Validated
public class VerifierController {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final VerifierService verifierService;
	private final VerifierKeyProvider keyProvider;
	private final String verifierDid;

	public VerifierController(
			VerifierService verifierService,
			VerifierKeyProvider keyProvider,
			@Value("${verifier.did}") String verifierDid
	) {
		this.verifierService = verifierService;
		this.keyProvider = keyProvider;
		this.verifierDid = verifierDid;
	}

	@GetMapping("/verify/challenge")
	public ResponseEntity<ChallengeResponse> challenge() {
		String c = verifierService.mintChallenge();
		ChallengeResponse resp = new ChallengeResponse();
		resp.setChallenge(c);
		resp.setExpiresIn(verifierService.challengeTtlSeconds());
		return ResponseEntity.ok(resp);
	}

	@PostMapping("/verify")
	public ResponseEntity<VerifyResponse> verify(@Valid @RequestBody VerifyRequest request) {
		VerifyResponse resp = verifierService.verify(request);
		return ResponseEntity.ok(resp);
	}

	@GetMapping("/.well-known/openid-provider")
	public Map<String, Object> wellKnown() {
		Map<String, Object> resp = new LinkedHashMap<>();
		resp.put("issuer", verifierDid);
		resp.put("jwks_uri", "http://localhost:8090/jwks.json");
		resp.put("verification_endpoint", "http://localhost:8090/verify");
		resp.put("challenge_endpoint", "http://localhost:8090/verify/challenge");
		return resp;
	}

	@GetMapping("/jwks.json")
	public Map<String, Object> jwks() {
		try {
			Map<String, Object> jwk = OBJECT_MAPPER.readValue(keyProvider.exportPublicJwkJson(), new TypeReference<Map<String, Object>>() {
			});
			return Map.of("keys", java.util.List.of(jwk));
		} catch (Exception e) {
			throw new IllegalStateException("Failed to build JWKS", e);
		}
	}
}
