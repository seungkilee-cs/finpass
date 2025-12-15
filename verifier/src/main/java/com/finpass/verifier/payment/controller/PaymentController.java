package com.finpass.verifier.payment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finpass.verifier.payment.dto.AttachKycRequest;
import com.finpass.verifier.payment.dto.BalanceResponse;
import com.finpass.verifier.payment.dto.ConfirmPaymentResponse;
import com.finpass.verifier.payment.dto.CreateIntentRequest;
import com.finpass.verifier.payment.dto.CreateIntentResponse;
import com.finpass.verifier.payment.dto.PaymentIntentResponse;
import com.finpass.verifier.payment.model.PaymentIntent;
import com.finpass.verifier.payment.service.PaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/payments")
@Validated
public class PaymentController {

	private final PaymentService paymentService;

	public PaymentController(PaymentService paymentService) {
		this.paymentService = paymentService;
	}

	@PostMapping("/intents")
	public ResponseEntity<CreateIntentResponse> createIntent(@Valid @RequestBody CreateIntentRequest request) {
		PaymentIntent intent = paymentService.createIntent(request.getPayerDid(), request.getReceiverDid(), request.getAmount());
		CreateIntentResponse resp = new CreateIntentResponse();
		resp.setIntentId(intent.getId());
		resp.setStatus(intent.getStatus().name());
		resp.setAmount(intent.getAmount());
		resp.setPayerDid(intent.getPayerDid());
		resp.setReceiverDid(intent.getReceiverDid());
		resp.setKycVerified(intent.getStatus() == PaymentIntent.Status.KYC_VERIFIED || intent.getStatus() == PaymentIntent.Status.CONFIRMED);
		return ResponseEntity.ok(resp);
	}

	@GetMapping("/intents/{id}")
	public ResponseEntity<PaymentIntentResponse> getIntent(@PathVariable("id") String id) {
		PaymentIntent intent = paymentService.getIntentOrThrow(id);
		return ResponseEntity.ok(toIntentResponse(intent));
	}

	@PostMapping("/{id}/verify-kyc")
	public ResponseEntity<PaymentIntentResponse> attachKyc(@PathVariable("id") String id, @Valid @RequestBody AttachKycRequest request) {
		PaymentIntent intent = paymentService.attachKyc(id, request.getDecisionToken());
		return ResponseEntity.ok(toIntentResponse(intent));
	}

	@PostMapping("/{id}/confirm")
	public ResponseEntity<ConfirmPaymentResponse> confirm(@PathVariable("id") String id) {
		PaymentIntent intent = paymentService.confirm(id);
		ConfirmPaymentResponse resp = new ConfirmPaymentResponse();
		resp.setIntentId(intent.getId());
		resp.setStatus(intent.getStatus().name());
		resp.setAmount(intent.getAmount());
		resp.setPayerDid(intent.getPayerDid());
		resp.setReceiverDid(intent.getReceiverDid());
		resp.setPayerBalance(paymentService.getBalance(intent.getPayerDid()));
		resp.setReceiverBalance(paymentService.getBalance(intent.getReceiverDid()));
		return ResponseEntity.ok(resp);
	}

	@GetMapping("/balance/{did}")
	public ResponseEntity<BalanceResponse> balance(@PathVariable("did") String did) {
		long bal = paymentService.getBalance(did);
		BalanceResponse resp = new BalanceResponse();
		resp.setDid(did);
		resp.setBalance(bal);
		return ResponseEntity.ok(resp);
	}

	private static PaymentIntentResponse toIntentResponse(PaymentIntent intent) {
		PaymentIntentResponse resp = new PaymentIntentResponse();
		resp.setIntentId(intent.getId());
		resp.setStatus(intent.getStatus().name());
		resp.setAmount(intent.getAmount());
		resp.setPayerDid(intent.getPayerDid());
		resp.setReceiverDid(intent.getReceiverDid());
		resp.setKycVerified(intent.getStatus() == PaymentIntent.Status.KYC_VERIFIED || intent.getStatus() == PaymentIntent.Status.CONFIRMED);
		return resp;
	}
}
