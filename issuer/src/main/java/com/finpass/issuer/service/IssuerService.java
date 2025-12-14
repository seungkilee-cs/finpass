package com.finpass.issuer.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finpass.issuer.dto.IssueResponse;
import com.finpass.issuer.entity.CredentialEntity;
import com.finpass.issuer.entity.IssuanceEntity;
import com.finpass.issuer.entity.UserEntity;
import com.finpass.issuer.repository.CredentialRepository;
import com.finpass.issuer.repository.IssuanceRepository;
import com.finpass.issuer.repository.UserRepository;
import com.finpass.issuer.util.CanonicalJson;
import com.finpass.issuer.util.Hashing;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@Service
public class IssuerService {

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
