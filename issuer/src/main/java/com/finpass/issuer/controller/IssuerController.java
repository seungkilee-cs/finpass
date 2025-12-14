package com.finpass.issuer.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finpass.issuer.dto.IssueRequest;
import com.finpass.issuer.dto.IssueWithProofRequest;
import com.finpass.issuer.dto.IssueResponse;
import com.finpass.issuer.dto.StatusResponse;
import com.finpass.issuer.repository.CredentialRepository;
import com.finpass.issuer.service.IssuerService;

import jakarta.validation.Valid;

@RestController
@RequestMapping
@Validated
public class IssuerController {

	private final IssuerService issuerService;
	private final CredentialRepository credentialRepository;
	private final String issuerDid;

	public IssuerController(
			IssuerService issuerService,
			CredentialRepository credentialRepository,
			@Value("${issuer.did}") String issuerDid
	) {
		this.issuerService = issuerService;
		this.credentialRepository = credentialRepository;
		this.issuerDid = issuerDid;
	}

	@GetMapping("/.well-known/openid-credential-issuer")
	public Map<String, Object> wellKnown() {
		Map<String, Object> resp = new LinkedHashMap<>();
		resp.put("issuer_did", issuerDid);
		resp.put("credential_endpoint", "http://localhost:8080/issue");
		resp.put("credential_endpoint_with_proof", "http://localhost:8080/issue-with-proof");
		resp.put("credential_configurations_supported", Map.of(
				"PassportCredential", Map.of(
						"format", "jwt_vc",
						"scope", "passport_credential",
						"cryptographic_binding_methods_supported", java.util.List.of("did"),
						"credential_signing_alg_values_supported", java.util.List.of("EdDSA")
				)
		));
		return resp;
	}

	@PostMapping("/issue")
	public ResponseEntity<IssueResponse> issue(@Valid @RequestBody IssueRequest request) {
		IssueResponse resp = issuerService.issuePassportCredential(request.getHolderDid(), request.getPassportData());
		return ResponseEntity.ok(resp);
	}

	@PostMapping("/issue-with-proof")
	public ResponseEntity<IssueResponse> issueWithProof(@Valid @RequestBody IssueWithProofRequest request) {
		IssueResponse resp = issuerService.issuePassportCredentialWithProof(request);
		return ResponseEntity.ok(resp);
	}

	@GetMapping("/status/{credId}")
	public ResponseEntity<StatusResponse> status(@PathVariable("credId") UUID credId) {
		return credentialRepository.findById(credId)
				.map(c -> {
					StatusResponse resp = new StatusResponse();
					resp.setCredId(c.getId());
					resp.setStatus(c.getStatus());
					return ResponseEntity.ok(resp);
				})
				.orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
	}
}
