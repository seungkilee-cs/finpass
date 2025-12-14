package com.finpass.issuer.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpass.issuer.dto.IssueWithProofRequest;
import com.finpass.issuer.dto.IssueResponse;
import com.finpass.issuer.entity.CredentialEntity;
import com.finpass.issuer.entity.IssuanceEntity;
import com.finpass.issuer.entity.UserEntity;
import com.finpass.issuer.repository.CredentialRepository;
import com.finpass.issuer.repository.IssuanceRepository;
import com.finpass.issuer.repository.UserRepository;
import com.finpass.issuer.util.CanonicalJson;
import com.finpass.issuer.util.Hashing;
import com.finpass.issuer.util.WalletProofVerifier;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@Service
public class IssuerService {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final UserRepository userRepository;
	private final CredentialRepository credentialRepository;
	private final IssuanceRepository issuanceRepository;
	private final IssuerKeyProvider keyProvider;
	private final String issuerDid;

	public IssuerService(
			UserRepository userRepository,
			CredentialRepository credentialRepository,
			IssuanceRepository issuanceRepository,
			IssuerKeyProvider keyProvider,
			@Value("${issuer.did}") String issuerDid
	) {
		this.userRepository = userRepository;
		this.credentialRepository = credentialRepository;
		this.issuanceRepository = issuanceRepository;
		this.keyProvider = keyProvider;
		this.issuerDid = issuerDid;
	}

	@Transactional
	public IssueResponse issuePassportCredential(String holderDid, Map<String, Object> passportData) {
		Instant now = Instant.now();

		UserEntity user = userRepository.findByDid(holderDid).orElseGet(() -> {
			UserEntity u = new UserEntity();
			u.setId(UUID.randomUUID());
			u.setDid(holderDid);
			u.setCreatedAt(now);
			return userRepository.save(u);
		});

		String canonical = CanonicalJson.stringify(passportData);
		String commitmentHash = Hashing.sha256Hex(canonical);

		String vcJwt = signVcJwt(holderDid, passportData, now);
		String commitmentJwt = signCommitmentJwt(holderDid, commitmentHash, now);

		CredentialEntity cred = new CredentialEntity();
		cred.setId(UUID.randomUUID());
		cred.setUser(user);
		cred.setCredentialJwt(vcJwt);
		cred.setIssuedAt(now);
		cred.setStatus("valid");
		credentialRepository.save(cred);

		IssuanceEntity issuance = new IssuanceEntity();
		issuance.setId(UUID.randomUUID());
		issuance.setCredential(cred);
		issuance.setIssuerDid(issuerDid);
		issuance.setTimestamp(now);
		issuanceRepository.save(issuance);

		IssueResponse resp = new IssueResponse();
		resp.setCredId(cred.getId());
		resp.setIssuerDid(issuerDid);
		resp.setStatus(cred.getStatus());
		resp.setCredentialJwt(vcJwt);
		resp.setCommitmentHash(commitmentHash);
		resp.setCommitmentJwt(commitmentJwt);
		return resp;
	}

	@Transactional
	public IssueResponse issuePassportCredentialWithProof(IssueWithProofRequest request) {
		validateWalletProof(request);
		return issuePassportCredential(request.getHolderDid(), request.getPassportData());
	}

	private static void validateWalletProof(IssueWithProofRequest request) {
		Map<String, Object> payload;
		try {
			payload = OBJECT_MAPPER.readValue(request.getProofPayload(), new TypeReference<Map<String, Object>>() {
			});
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid proofPayload JSON", e);
		}

		Object payloadHolderDid = payload.get("holderDid");
		if (payloadHolderDid == null || !request.getHolderDid().equals(payloadHolderDid.toString())) {
			throw new IllegalArgumentException("holderDid mismatch between request and proofPayload");
		}

		Object payloadHolderAddress = payload.get("holderAddress");
		if (payloadHolderAddress == null || !request.getHolderAddress().equalsIgnoreCase(payloadHolderAddress.toString())) {
			throw new IllegalArgumentException("holderAddress mismatch between request and proofPayload");
		}

		Object payloadPassportData = payload.get("passportData");
		String reqPassport = CanonicalJson.stringify(request.getPassportData());
		String payloadPassport = CanonicalJson.stringify(payloadPassportData);
		if (!reqPassport.equals(payloadPassport)) {
			throw new IllegalArgumentException("passportData mismatch between request and proofPayload");
		}

		String canonicalPayload = CanonicalJson.stringify(payload);
		if (!canonicalPayload.equals(request.getProofPayload())) {
			throw new IllegalArgumentException("proofPayload must be canonical JSON");
		}

		boolean ok = WalletProofVerifier.verifyPrefixedMessageSignature(
				request.getProofPayload(),
				request.getProofSignature(),
				request.getHolderAddress()
		);
		if (!ok) {
			throw new IllegalArgumentException("Invalid wallet proof signature");
		}
	}

	private String signVcJwt(String holderDid, Map<String, Object> passportData, Instant now) {
		try {
			JWTClaimsSet claims = new JWTClaimsSet.Builder()
					.issuer(issuerDid)
					.subject(holderDid)
					.jwtID(UUID.randomUUID().toString())
					.issueTime(java.util.Date.from(now))
					.claim("vc", Map.of(
							"@context", java.util.List.of("https://www.w3.org/2018/credentials/v1"),
							"type", java.util.List.of("VerifiableCredential", "PassportCredential"),
							"credentialSubject", passportData
					))
					.build();

			JWSHeader header = new JWSHeader.Builder(keyProvider.getAlgorithm())
					.type(JOSEObjectType.JWT)
					.keyID(keyProvider.getKeyId())
					.build();

			SignedJWT jwt = new SignedJWT(header, claims);
			jwt.sign(keyProvider.signer());
			return jwt.serialize();
		} catch (Exception e) {
			throw new RuntimeException("Failed to sign VC JWT", e);
		}
	}

	private String signCommitmentJwt(String holderDid, String commitmentHash, Instant now) {
		try {
			JWTClaimsSet claims = new JWTClaimsSet.Builder()
					.issuer(issuerDid)
					.subject(holderDid)
					.jwtID(UUID.randomUUID().toString())
					.issueTime(java.util.Date.from(now))
					.claim("commitment_hash", commitmentHash)
					.build();

			JWSHeader header = new JWSHeader.Builder(keyProvider.getAlgorithm())
					.type(JOSEObjectType.JWT)
					.keyID(keyProvider.getKeyId())
					.build();

			SignedJWT jwt = new SignedJWT(header, claims);
			jwt.sign(keyProvider.signer());
			return jwt.serialize();
		} catch (Exception e) {
			throw new RuntimeException("Failed to sign commitment JWT", e);
		}
	}
}
