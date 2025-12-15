package com.finpass.verifier.payment.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.finpass.verifier.payment.model.PaymentIntent;
import com.finpass.verifier.payment.model.PaymentIntent.Status;
import com.nimbusds.jwt.JWTClaimsSet;

@Service
public class PaymentService {

	private final DecisionTokenValidator decisionTokenValidator;
	private final Map<String, PaymentIntent> intents = new ConcurrentHashMap<>();
	private final Map<String, Long> balances = new ConcurrentHashMap<>();

	public PaymentService(DecisionTokenValidator decisionTokenValidator) {
		this.decisionTokenValidator = decisionTokenValidator;
	}

	public PaymentIntent createIntent(String payerDid, String receiverDid, long amount) {
		if (payerDid == null || payerDid.isBlank()) throw new IllegalArgumentException("payerDid is required");
		if (receiverDid == null || receiverDid.isBlank()) throw new IllegalArgumentException("receiverDid is required");
		if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
		if (payerDid.equals(receiverDid)) throw new IllegalArgumentException("payerDid must differ from receiverDid");

		// Demo defaults
		balances.putIfAbsent(payerDid, 1_000L);
		balances.putIfAbsent(receiverDid, 0L);

		PaymentIntent intent = new PaymentIntent();
		intent.setId(UUID.randomUUID().toString());
		intent.setPayerDid(payerDid);
		intent.setReceiverDid(receiverDid);
		intent.setAmount(amount);
		intent.setStatus(Status.CREATED);
		intent.setCreatedAt(Instant.now());

		intents.put(intent.getId(), intent);
		return intent;
	}

	public PaymentIntent getIntentOrThrow(String intentId) {
		PaymentIntent intent = intents.get(intentId);
		if (intent == null) throw new IllegalArgumentException("Payment intent not found");
		return intent;
	}

	public PaymentIntent attachKyc(String intentId, String decisionToken) {
		PaymentIntent intent = getIntentOrThrow(intentId);
		if (intent.getStatus() == Status.CONFIRMED) {
			throw new IllegalArgumentException("Payment intent already confirmed");
		}

		JWTClaimsSet claims = decisionTokenValidator.verifyOrThrow(decisionToken);

		String sub = DecisionTokenValidator.subjectDid(claims);
		if (!intent.getPayerDid().equals(sub)) {
			throw new IllegalArgumentException("Decision token subject must match payerDid");
		}
		if (!DecisionTokenValidator.hasClaim(claims, "over_18")) {
			throw new IllegalArgumentException("Decision token missing required claim: over_18");
		}

		intent.setDecisionToken(decisionToken);
		intent.setStatus(Status.KYC_VERIFIED);
		return intent;
	}

	public PaymentIntent confirm(String intentId) {
		PaymentIntent intent = getIntentOrThrow(intentId);
		if (intent.getStatus() != Status.KYC_VERIFIED) {
			throw new IllegalArgumentException("KYC verification required before confirm");
		}

		long payerBalance = balances.getOrDefault(intent.getPayerDid(), 0L);
		if (payerBalance < intent.getAmount()) {
			throw new IllegalArgumentException("Insufficient balance");
		}
		long receiverBalance = balances.getOrDefault(intent.getReceiverDid(), 0L);

		balances.put(intent.getPayerDid(), payerBalance - intent.getAmount());
		balances.put(intent.getReceiverDid(), receiverBalance + intent.getAmount());

		intent.setStatus(Status.CONFIRMED);
		intent.setConfirmedAt(Instant.now());
		return intent;
	}

	public long getBalance(String did) {
		if (did == null || did.isBlank()) throw new IllegalArgumentException("did is required");
		balances.putIfAbsent(did, 0L);
		return balances.get(did);
	}
}
