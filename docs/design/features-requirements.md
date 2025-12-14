# FinPass — Feature Requirements

**Project:** Minimal Blockchain Financial Passport PoC  
**Version:** 1.0  
**Last Updated:** December 2024

---

## Overview

FinPass is a "digital passport for finance" — a privacy-first identity wallet that enables users to prove who they are once, then reuse that proof to unlock payment flows without repeatedly sharing personal data. It combines SSI-style identity credentials with a KYC-gated payment ledger.

---

## Problem Statement

Cross-border users (tourists, expats, remote workers) face:
- **Repeated KYC steps** across different services
- **Unfamiliar local payment systems** in foreign countries
- **Merchants unable to trust foreign IDs** easily

Merchants face:
- **Liability of storing sensitive passport data**
- **Need for reliable "is this person verified?" signal**

---

## Solution

A decentralized identity system where:
1. User receives a **Verifiable Credential (VC)** in their wallet
2. User proves eligibility via a **Zero-Knowledge Proof (ZKP)** (e.g., “I am over 18”) without revealing raw passport PII
3. Verifier returns a short-lived **Decision Token** (e.g., "KYC passed, valid 5 minutes")
4. Payment service accepts only the Decision Token — **never the passport credential itself**

---

## Core Features

### Phase 1: Identity Core (The "Digital Passport")

#### F1.1 — DID Creation & Management
- **Description:** App generates a Decentralized Identifier (DID) and cryptographic key pair for the user
- **Requirements:**
  - Generate Ed25519 or secp256k1 key pair on first launch
  - Store private key securely (Secure Enclave on device, localStorage for PoC)
  - Support `did:key` format (PoC) or `did:ethr` (MVP)
  - Export/import recovery seed (mnemonic)
- **Acceptance Criteria:**
  - User can create multiple independent DIDs
  - User can recover DID from seed phrase

#### F1.2 — Passport Credential Issuance
- **Description:** Convert passport data into a signed Verifiable Credential
- **Requirements:**
  - Capture passport data (MRZ scanning for realism, synthetic data for PoC)
  - Send passport data + user DID to Issuer Service
  - Issuer validates format and signs VC with issuer private key
  - Return JWT-VC to wallet for encrypted local storage
- **VC Schema (PassportCredential):**
  ```json
  {
    "fullName": "string",
    "birthDate": "date",
    "nationality": "string",
    "expiryDate": "date",
    "passportVerified": true
  }
  ```
- **Acceptance Criteria:**
  - Wallet displays "Passport Credential" card after issuance
  - VC is signed and verifiable

#### F1.3 — Privacy-Preserving Verification (ZKP)
- **Description:** Prove eligibility (e.g., over-18, valid passport credential) without revealing unnecessary PII
- **Minimal PoC Approach:**
  - Issuer issues a VC, and also includes (or separately issues) an **issuer-signed commitment** to the passport attributes.
  - Holder generates a **zkSNARK proof** locally that they know passport attributes whose hash equals the commitment and that a predicate holds (e.g., age ≥ 18).
  - Verifier checks:
    1. Issuer signature over the commitment (trust)
    2. zkSNARK proof validity (privacy-preserving predicate proof)
    3. Holder binding (proof is bound to holder DID via a challenge/nonce)
- **Requirements:**
  - **Commitment:** Use a deterministic hash (e.g., Poseidon/SHA-256) over canonicalized passport attributes (synthetic data for PoC)
  - **Predicate proofs (PoC):**
    - Prove `age >= 18` without revealing `birthDate`
    - Optionally prove `nationality in {…}` without revealing full PII
  - **Anti-replay:** Verifier provides a nonce/challenge; proof must bind to it
  - **No raw passport fields sent to verifier** in the ZKP flow
- **Acceptance Criteria:**
  - Verifier can return “Verified: Over 18” without seeing DOB
  - Network payload does not include raw passport number / MRZ / full DOB
  - Replay attempt with old proof fails due to nonce

#### F1.4 — Decision Token (Post-Verification)
- **Description:** Verifier issues a short-lived authorization token after successful verification
- **Requirements:**
  - Return Decision Token only after issuer trust + ZKP verification pass
  - Decision token should contain only **minimal claims** needed for gating (avoid PII)
- **Decision Token Claims:**
  ```json
  {
    "sub": "holder-did",
    "verified_at": "timestamp",
    "assurance_level": "LOW|MEDIUM|HIGH",
    "verified_claims": ["over_18"],
    "expires_in": 300
  }
  ```
- **Acceptance Criteria:**
  - User can click "Present Credential" and see "✓ Verified"
  - Decision token is short-lived (5 minutes default)

---

### Phase 2: Payment Layer (The "B·Pay" Layer)

#### F2.1 — Demo Payment Ledger
- **Description:** Basic closed-loop payment system for PoC demonstration
- **Requirements:**
  - In-memory or database balance ledger (per DID)
  - Create payment intent (amount, receiver DID)
  - Transfer balance between DIDs
- **Acceptance Criteria:**
  - User A can send demo units to User B
  - Both balances update correctly

#### F2.2 — KYC-Gated Payments
- **Description:** Payments require valid identity verification
- **Requirements:**
  - Payment service requires valid Decision Token
  - If token missing/expired, trigger verification flow
  - Only process payment after KYC confirmation
- **Payment Flow:**
  1. User initiates payment
  2. Payment service checks for valid Decision Token
  3. If missing: wallet auto-triggers ZKP verification (e.g., prove `over_18`)
  4. Verifier returns Decision Token
  5. Payment proceeds with token attached
- **Acceptance Criteria:**
  - Payment fails without valid KYC token
  - Payment succeeds with valid token
  - Expired tokens trigger re-verification

---

## MVP Features (Post-PoC)

### F3.1 — OpenID4VCI Compliance
- Standards-based wallet ↔ issuer protocol
- Issuer metadata endpoint (`/.well-known/openid-credential-issuer`)
- OAuth token endpoint for credential requests

### F3.2 — OpenID4VP Compliance
- Standards-based wallet ↔ verifier protocol
- Presentation definition support
- Standard response modes

### F3.3 — Blockchain DID Registry
- Anchor issuer public keys on Ethereum/Polygon testnet
- On-chain DID verification
- No PII on-chain

### F3.4 — Trust Registry (On-Chain)
- Immutable list of trusted issuers
- Admin-managed whitelist
- Verifier checks registry before accepting credentials

### F3.5 — Liveness Detection
- Basic camera-based face detection
- Prevent photo-of-photo attacks
- Liveness score threshold for credential issuance

### F3.6 — Credential Revocation
- Status endpoint for credential lifecycle
- Revocation check during verification
- Revoked credentials rejected

### F3.7 — Audit Logging
- Event trail for compliance
- Log: issuances, verifications, payments
- Query endpoints for event history

---

## Out of Scope (Future Phases)

| Feature | Reason |
|---------|--------|
| Advanced ZKP suites (multiple predicates, full selective disclosure UX, complex circuits) | Keep PoC minimal (start with 1–2 predicates like over-18) |
| NFC Passport Reading | Hardware dependency; not needed for PoC |
| Real PSP Integration | Demo balance sufficient for validation |
| Mobile App (React Native) | Web-first approach for faster iteration |
| Device Binding / Attestation | Advanced security feature |
| Merchant Portal | Focus on user-side first |
| Risk Scoring Engine | ML complexity outside PoC scope |
| Multi-Region Deployment | Single region sufficient for demo |

---

## Non-Functional Requirements

### Security
- Private keys never leave device (except encrypted backup)
- All API communication over HTTPS
- Decision tokens are short-lived and signed
- No PII stored on blockchain
- Credentials stored encrypted at rest

### Performance
- VC issuance: < 5 seconds
- ZKP verification: < 2 seconds
- End-to-end payment: < 20 seconds

### Usability
- Single "Create Wallet" action for onboarding
- Clear credential card display
- Intuitive payment flow with KYC step visible

### Standards Compliance
- W3C Verifiable Credentials Data Model
- DID Core specification
- OpenID4VCI / OpenID4VP (MVP)

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| **Frontend** | React + TypeScript |
| **Identity/Crypto** | Veramo.js, ethers.js, jose |
| **Backend** | Java 17 + Spring Boot |
| **Database** | PostgreSQL |
| **Blockchain** | Ethereum/Polygon testnet |
| **DevOps** | Docker, docker-compose |

---

## Data Flow Summary

```
┌──────────────┐     VC      ┌──────────────┐
│   Wallet     │◄────────────│   Issuer     │
│   (TS)       │             │   (Java)     │
└──────┬───────┘             └──────────────┘
       │
       │ ZKP
       ▼
┌──────────────┐  Decision   ┌──────────────┐
│   Verifier   │────Token───►│   Payment    │
│   (Java)     │             │   (Java)     │
└──────────────┘             └──────────────┘
```

**Key Principle:** Payments never touch passport data — they only accept the verifier's short-lived Decision Token, keeping identity and money cleanly separated.
