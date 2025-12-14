# Financial Passport (IDBlock + B·Pay) — PoC & MVP Implementation Plan

**Timeline:** 4–6 weeks (PoC: 2 weeks, MVP: 4 weeks)  
**Team size:** 3–4 engineers (1 TS/Frontend, 2 Java/Backend, 1 DevOps/Infra)  
**Deliverables:** Working wallet, issuer, verifier, payment demo  
**Blockchain:** Ethereum/Polygon testnet (no mainnet spend)

---

## **PHASE 0: PoC (Weeks 1–2)**

### **Goal**
End-to-end proof that SSI + payments can work together. User signs up → gets credential → pays with it → demo ends.

### **Scope (in scope ✅ / out of scope ❌)**

| Component | Included | Notes |
|-----------|----------|-------|
| **Wallet DID creation** | ✅ | In-memory or localStorage; simple Ed25519 keys |
| **Passport upload + VC issuance** | ✅ | Manual validation (no ML liveness); issuer stores only metadata |
| **VP presentation** | ✅ | Full disclosure (no selective disclosure yet) |
| **Verifier check** | ✅ | Validate signature + issue decision token |
| **Payment flow** | ✅ | Demo balance transfer (closed-loop, no PSP) |
| **Trust registry** | ❌ | Hardcoded issuer list in Java config |
| **Blockchain anchoring** | ❌ | No on-chain writes; database only |
| **Liveness detection** | ❌ | Manual passport validation |
| **Revocation** | ❌ | All credentials valid by default |
| **Real PSP** | ❌ | Demo in-app balance |
| **Audit logging** | ❌ | Console logs only |

### **Architecture (Simplified)**

```
┌─────────────────┐
│  React Web Wallet│ (TS)
│  - DID creation │
│  - VC holder    │
│  - Payment flow │
└────────┬────────┘
         │ HTTP
┌────────▼────────────────────┐
│  Spring Boot API (Java)     │
│  • Issuer (POST /issue)     │
│  • Verifier (POST /verify)  │
│  • Payment (POST /payment)  │
│  • Auth (POST /token)       │
└────────┬────────────────────┘
         │
┌────────▼────────┐
│  PostgreSQL     │
│  (users, creds, │
│   payments)     │
└─────────────────┘
```

### **Milestones**

#### **M0.1: Wallet Setup (Days 1–2)**

**Goal:** Basic DID creation and credential storage.

**Tasks:**

1. **Frontend scaffold**
   - Create React app with TypeScript
   - Install Veramo + ethers + jose
   - Create DID manager (create + load from localStorage)
   - Basic UI: "Create Wallet" button

2. **Key storage**
   - Store private keys in localStorage (PoC only; use sessionStorage if paranoid)
   - Export/import mnemonic (recovery seed)

3. **Testing**
   - Can create 5 DIDs independently
   - Can recover DID from seed

**Deliverable:** Wallet app that shows "Your DID: did:key:z6MkhaXgBZDvotDkL5257faWxcqV7SNqeKJzCeVEJcuAP..."

---

#### **M0.2: Issuer Scaffold (Days 2–3)**

**Goal:** Accept passport photo + issue dummy VC.

**Tasks:**

1. **Backend setup**
   - Initialize Spring Boot app
   - Create PostgreSQL schema: `users`, `credentials`, `issuances`
   - Add security (CORS, basic validation)

2. **Issuer endpoints**
   - `GET /.well-known/openid-credential-issuer` (hardcoded metadata)
   - `POST /issue` (accept passport upload → issue VC)
   - `POST /status/{credId}` (always return "valid")

3. **Issuer key management**
   - Generate Ed25519 issuer keypair (store in env or keystore)
   - Sign VCs with issuer key

4. **VC generation**
   - Build JWT-VC with passport claims
   - Return to wallet as JSON

5. **Testing**
   - Postman: send VC issuance request
   - Verify JWT decodes and has correct claims

**Deliverable:** POST request to `/issue` returns a signed JWT-VC.

---

#### **M0.3: Wallet ↔ Issuer Integration (Days 3–4)**

**Goal:** Wallet can request and store VCs from issuer.

**Tasks:**

1. **Wallet OpenID4VCI flow (simplified)**
   - Fetch issuer metadata
   - Authenticate (hardcoded token; skip OAuth)
   - POST credential request with proof (wallet signature)
   - Store returned VC in wallet

2. **Issuer validation**
   - Verify wallet proof signature (use wallet DID to check)
   - Issue VC bound to wallet DID

3. **UI**
   - "Get Credential" button → upload photo → receive VC
   - Show credential in wallet (name, nationality, expiry)

4. **Testing**
   - Full flow: create DID → upload photo → get VC → see in wallet

**Deliverable:** "Passport Credential" card visible in wallet UI.

---

#### **M0.4: Verifier Scaffold (Days 4–5)**

**Goal:** Accept VP and return decision token.

**Tasks:**

1. **Verifier endpoints**
   - `POST /verify` (accept VP → validate → return decision token)
   - `GET /.well-known/openid-provider` (hardcoded metadata)

2. **VP validation**
   - Decode JWT
   - Check wallet signature (use VP holder DID)
   - Verify issuer is in hardcoded trusted list
   - Extract claims

3. **Decision token minting**
   - Create JWT with claims:
     ```json
     {
       "sub": "holder-did",
       "verified_at": "2025-12-13T...",
       "assurance_level": "LOW",
       "verified_claims": ["name", "nationality"],
       "issuer_id": "issuer-1",
       "expires_in": 300,
       "jti": "token-uuid"
     }
     ```
   - Sign with verifier key

4. **Testing**
   - Postman: send VP → get decision token
   - Decode and verify claims

**Deliverable:** Decision token JWT returned from verifier.

---

#### **M0.5: Wallet ↔ Verifier Integration (Days 5–6)**

**Goal:** Wallet can create VP and present to verifier.

**Tasks:**

1. **Wallet OpenID4VP flow (simplified)**
   - Create presentation request (hardcoded)
   - Load VC from holder
   - Build VP (sign with holder key)
   - POST VP to verifier
   - Store decision token

2. **UI**
   - "Present Credential" button
   - Shows verifier request (e.g., "Verify you are over 18")
   - User confirms → creates VP → sends to verifier
   - Show result: "✓ Verified"

3. **Testing**
   - Full flow: present VC → get decision token → confirm UI

**Deliverable:** Wallet shows "Verified" status after presentation.

---

#### **M0.6: Payment Demo (Days 6–7)**

**Goal:** Verification gated payment with demo balance.

**Tasks:**

1. **Backend payment service**
   - `POST /payments/intents` (create payment intent)
   - `POST /payments/{id}/verify-kyc` (verify credential)
   - `POST /payments/{id}/confirm` (move balance around)

2. **Demo balance ledger**
   - Simple in-memory map: `holder_did → balance`
   - Issuer wallet starts with 1000 demo units
   - Merchant wallet starts with 0

3. **Payment flow**
   - Wallet initiates payment (amount, merchant)
   - Payment service requires KYC verification
   - Wallet presents VC → gets decision token
   - Payment service confirms → debits wallet, credits merchant
   - Show receipt

4. **UI**
   - "Send Payment" button
   - Input amount + merchant
   - Verification checkbox → payment flow
   - Show balance before/after

5. **Testing**
   - Create 2 wallets
   - Issuer: get credential
   - User A sends 100 to User B
   - Both balances updated

**Deliverable:** Working payment demo with verified identity requirement.

---

#### **M0.7: Integration Test & Demo (Day 7)**

**Goal:** Entire flow works end-to-end.

**Tasks:**

1. **End-to-end walkthrough**
   - User 1: create wallet
   - User 1: get passport VC
   - User 2: create wallet (merchant)
   - User 1: send payment to User 2 (with VP verification)
   - Both see updated balances

2. **Demo script**
   - Record 5-min walkthrough
   - Test all error cases (bad VP, expired token, etc.)

3. **Cleanup**
   - Remove console logs
   - Basic error handling
   - README with setup instructions

**Deliverable:** PoC fully working; can be demoed to stakeholders.

---

### **PoC Deliverables**

```
├── wallet/
│   ├── src/
│   │   ├── DIDManager.ts
│   │   ├── CredentialHolder.ts
│   │   ├── PresentationCreator.ts
│   │   ├── PaymentClient.ts
│   │   ├── App.tsx (main UI)
│   │   └── components/
│   │       ├── Onboarding.tsx
│   │       ├── CredentialCard.tsx
│   │       ├── PaymentFlow.tsx
│   │       └── VerificationFlow.tsx
│   └── package.json
├── backend/
│   ├── src/main/java/com/idblock/
│   │   ├── IssuerController.java
│   │   ├── VerifierController.java
│   │   ├── PaymentController.java
│   │   ├── IssuerService.java
│   │   ├── VerifierService.java
│   │   ├── PaymentService.java
│   │   └── config/
│   │       └── SecurityConfig.java
│   ├── pom.xml
│   └── application.yaml
└── README.md (setup + demo walkthrough)
```

---

## **PHASE 1: MVP (Weeks 3–6)**

### **Goal**
Production-ready PoC that adds real infrastructure: proper database, blockchain anchoring, liveness detection, standards compliance.

### **New Features (vs PoC)**

| Feature | Purpose | Effort |
|---------|---------|--------|
| **OpenID4VCI compliance** | Standards-based wallet ↔ issuer | Medium |
| **OpenID4VP compliance** | Standards-based wallet ↔ verifier | Medium |
| **Liveness detection** | Basic camera-based face detection | Low |
| **Blockchain DID registry** | Anchoring issuer key on testnet | Medium |
| **Trust registry (on-chain)** | Immutable issuer whitelist | Medium |
| **Status/revocation** | Credential lifecycle tracking | Low |
| **Real database schema** | Users, credentials, payments, audit | Low |
| **Multi-device awareness** | Graceful handling (not recovery yet) | Low |
| **Error handling & validation** | Proper HTTP codes, input validation | Low |
| **API documentation** | OpenAPI/Swagger for integration | Low |
| **Audit logging** | Event trail for compliance | Low |

### **Out of Scope (for MVP)**

- ❌ Selective disclosure / ZKP
- ❌ NFC passport reading
- ❌ Real PSP integration
- ❌ Multi-region deployment
- ❌ Mobile app (React Native)
- ❌ Device binding / attestation
- ❌ Merchant portal
- ❌ Risk scoring engine

---

### **Milestones**

#### **M1.1: Blockchain DID Registry (Days 8–10)**

**Goal:** Issuer key anchored on Ethereum testnet.

**Tasks:**

1. **Smart contract**
   - Deploy simple DIDRegistry.sol (Remix or Hardhat)
   - `registerDID(didHash, publicKeyJWK, timestamp)`
   - `verifyDIDAt(didHash, timestamp) → isValid`

2. **Java integration**
   - Add web3j dependency
   - Create BlockchainService
   - `publishIssuerKey(did, pubKey)` → transaction
   - `verifyIssuerOnChain(did)` → read from contract

3. **Backend integration**
   - On issuance: publish issuer key hash to testnet (async)
   - On verification: check if issuer DID registered on-chain

4. **Testing**
   - Publish issuer key → verify on Etherscan
   - Verifier reads contract → confirms issuer trusted

**Deliverable:** Issuer DID stored on-chain; verifier validates against it.

---

#### **M1.2: Trust Registry (On-Chain) (Days 10–12)**

**Goal:** Immutable list of trusted issuers.

**Tasks:**

1. **Smart contract**
   - TrustRegistry.sol
   - Admin-managed issuer whitelist
   - Events: `IssuerAdded`, `IssuerRemoved`

2. **Java integration**
   - `addIssuer(issuerDID, assuranceLevel, createdAt)`
   - `isTrustedIssuer(issuerDID, atTimestamp)`

3. **Backend integration**
   - On verification: verifier checks if issuer in trust registry
   - Cache registry entries (check on-chain every 1 hour)

4. **Testing**
   - Add issuer to registry
   - Verification trusts issuer
   - Remove issuer → verification rejects

**Deliverable:** Immutable on-chain trust list; verifier uses it.

---

#### **M1.3: OpenID4VCI Implementation (Days 12–15)**

**Goal:** Wallet ↔ issuer uses standard protocol.

**Tasks:**

1. **Issuer metadata endpoint**
   - `GET /.well-known/openid-credential-issuer`
   - Return credential types, token endpoint, proof formats

2. **OAuth token endpoint (simplified)**
   - `POST /token` (pre-auth flow; no full auth code needed yet)
   - Accept device_code or auth_code
   - Return access_token

3. **Credential request format**
   - Wallet sends proof (JWT signed by holder key)
   - Issuer validates proof
   - Issue VC with claims

4. **Wallet integration**
   - Use jose + axios to fetch metadata
   - Request token → credential
   - Store VC in encrypted holder

5. **Testing**
   - Wallet requests credential per OpenID4VCI spec
   - Issuer validates per spec
   - Verify with online validation tools

**Deliverable:** OpenID4VCI-compliant issuance flow.

---

#### **M1.4: OpenID4VP Implementation (Days 15–18)**

**Goal:** Wallet ↔ verifier uses standard protocol.

**Tasks:**

1. **Verifier metadata endpoint**
   - `GET /.well-known/openid-provider`
   - Return presentation definition, scopes, response modes

2. **Presentation request endpoint**
   - `GET /auth?client_id=...&response_type=...`
   - Return presentation_definition (which claims needed)

3. **Response endpoint**
   - `POST /callback` (wallet posts VP here)
   - Validate VP per spec
   - Return decision token (or ID token per spec)

4. **Wallet integration**
   - Fetch presentation_definition
   - UI shows required claims
   - Build VP + submit response
   - Parse decision token

5. **Testing**
   - Full OpenID4VP flow per spec
   - Verify with validator tools
   - Test claim filtering

**Deliverable:** OpenID4VP-compliant verification flow.

---

#### **M1.5: Liveness Detection (Days 18–20)**

**Goal:** Basic camera-based liveness check.

**Tasks:**

1. **Frontend**
   - Add face-api.js or TensorFlow.js + face detection
   - Capture 3–5 video frames during passport scan
   - Check face detected + not a static image (basic)

2. **Issuer backend**
   - Accept liveness_score in credential request
   - Only issue if score > 0.7

3. **UI**
   - Camera stream + "Smile for camera" prompt
   - Show liveness score
   - "Liveness check passed" before issuing VC

4. **Testing**
   - Can detect real face
   - Rejects photo of photo
   - Works on Chrome/Safari

**Deliverable:** Passport issuance requires liveness check.

---

#### **M1.6: Revocation/Status Service (Days 20–21)**

**Goal:** Credentials can be revoked.

**Tasks:**

1. **Database schema**
   - `credential_status` table: credentialId, status, revokedAt

2. **Issuer endpoint**
   - `POST /revoke/{credentialId}` (admin only)
   - Mark credential as revoked

3. **Revocation check endpoint**
   - `GET /status/{revocationHandle}` → status

4. **Verifier integration**
   - Before accepting VC, check status endpoint
   - Reject if revoked

5. **Testing**
   - Issue VC
   - Revoke it
   - Verification fails

**Deliverable:** Revocation endpoint + verifier checks it.

---

#### **M1.7: Real Database Schema (Days 21–22)**

**Goal:** Production-ready data model.

**Tasks:**

1. **Schema design**
   ```sql
   -- Users (minimal PII)
   CREATE TABLE users (
     id UUID PRIMARY KEY,
     did TEXT UNIQUE,
     created_at TIMESTAMP,
     last_seen TIMESTAMP
   );

   -- Credentials
   CREATE TABLE credentials (
     id UUID PRIMARY KEY,
     user_id UUID REFERENCES users,
     issuer_id TEXT,
     credential_type TEXT,
     credential_jwt TEXT (encrypted),
     issued_at TIMESTAMP,
     expires_at TIMESTAMP,
     status VARCHAR (VALID, REVOKED, SUSPENDED),
     revoked_at TIMESTAMP,
     revocation_handle TEXT
   );

   -- Payments
   CREATE TABLE payments (
     id UUID PRIMARY KEY,
     payer_did TEXT,
     payee_did TEXT,
     amount DECIMAL,
     currency VARCHAR,
     status VARCHAR (PENDING, AUTHORIZED, CAPTURED, FAILED),
     kyc_decision_token TEXT,
     created_at TIMESTAMP,
     completed_at TIMESTAMP
   );

   -- Audit events
   CREATE TABLE audit_events (
     id UUID PRIMARY KEY,
     event_type VARCHAR,
     user_id_hash TEXT (SHA256),
     details JSONB,
     created_at TIMESTAMP
   );
   ```

2. **Migrate PoC in-memory storage to DB**
   - Update issuer to persist credentials
   - Update verifier to log decisions
   - Update payment to persist transactions

3. **Indexing**
   - Index on did, created_at, status

4. **Testing**
   - Data persists across restarts
   - Audit trail is complete

**Deliverable:** PostgreSQL schema in place; all services use it.

---

#### **M1.8: Audit Logging (Days 22–23)**

**Goal:** Compliance-ready event trail.

**Tasks:**

1. **AuditService**
   - Log events: CREDENTIAL_ISSUED, CREDENTIAL_VERIFIED, PAYMENT_INITIATED, etc.
   - Store: timestamp, user_id_hash, event_type, details (non-PII)

2. **Events to log**
   - Issuance: who, what time, assurance level
   - Verification: who, when, what verifier, decision
   - Payment: payer, payee, amount, KYC decision

3. **Query endpoints** (internal only)
   - `GET /audit/events/{user_hash}` → event history
   - `GET /audit/metrics` → dashboard (issuances/day, etc.)

4. **Testing**
   - Full flow generates audit trail
   - User can request their event history

**Deliverable:** Audit trail logged for all operations.

---

#### **M1.9: Error Handling & Validation (Days 23–24)**

**Goal:** Production-grade error responses.

**Tasks:**

1. **Input validation**
   - Did: check format (did:*)
   - JWT: validate signature + expiry
   - Amount: positive decimal
   - Timestamp: not in future

2. **HTTP error codes**
   - 400: bad request (invalid input)
   - 401: unauthorized (invalid signature)
   - 403: forbidden (policy violation)
   - 404: not found
   - 409: conflict (duplicate)
   - 410: gone (credential revoked)
   - 500: internal error

3. **Error response format**
   ```json
   {
     "error": "credential_revoked",
     "error_description": "Credential was revoked on 2025-12-13",
     "timestamp": "2025-12-13T10:00:00Z"
   }
   ```

4. **Testing**
   - Each endpoint tested with invalid inputs
   - Proper error codes returned

**Deliverable:** Consistent error handling across all endpoints.

---

#### **M1.10: API Documentation (Days 24–25)**

**Goal:** Clear integration docs for merchant/wallet developers.

**Tasks:**

1. **OpenAPI 3.0 spec**
   - Document all endpoints (issuer, verifier, payment)
   - Request/response schemas
   - Authentication (Bearer token)
   - Error codes

2. **Generate Swagger UI**
   - `GET /swagger-ui.html` → interactive docs
   - Try-it-out for each endpoint

3. **README**
   - Quick start (how to run locally)
   - Architecture overview
   - Data flow diagrams
   - Example requests/responses

4. **Testing**
   - Swagger spec validates against actual API
   - README walkthrough works end-to-end

**Deliverable:** OpenAPI spec + Swagger UI + docs.

---

#### **M1.11: Integration Tests (Days 25–26)**

**Goal:** Entire MVP flow is tested and works.

**Tasks:**

1. **Test scenarios**
   - **Happy path:** create wallet → get VC → verify → pay
   - **Revoked credential:** verify fails
   - **Untrusted issuer:** verification rejects
   - **Expired token:** payment fails
   - **Invalid signature:** presentation fails

2. **Test tools**
   - Postman collection (all endpoints)
   - Selenium/Playwright (UI e2e tests)
   - JUnit (backend unit tests)

3. **Test data**
   - Test user accounts pre-populated
   - Test merchant accounts
   - Sample credentials

4. **Coverage target**
   - 70%+ code coverage for critical paths
   - All endpoints tested

**Deliverable:** Full test suite passing.

---

#### **M1.12: Deployment & Documentation (Days 27–28)**

**Goal:** MVP can be deployed and run locally/in cloud.

**Tasks:**

1. **Docker setup**
   ```dockerfile
   # Backend
   FROM openjdk:17
   COPY target/idblock-backend.jar app.jar
   ENTRYPOINT ["java", "-jar", "app.jar"]

   # Database
   postgres:15
   ```

2. **docker-compose.yaml**
   ```yaml
   version: '3'
   services:
     api:
       build: ./backend
       ports:
         - "8080:8080"
       environment:
         - DB_URL=jdbc:postgresql://postgres:5432/idblock
     postgres:
       image: postgres:15
       environment:
         - POSTGRES_DB=idblock
   ```

3. **Deployment docs**
   - Local: `docker-compose up`
   - Cloud: guide for deploying to AWS/GCP/Azure
   - Environment variables needed

4. **Monitoring**
   - Health check: `GET /health`
   - Logs: structured JSON logs to stdout

5. **Testing**
   - Fully working deployment from scratch

**Deliverable:** Working deployment via docker-compose.

---

### **MVP Deliverables**

```
├── wallet/
│   ├── src/
│   │   ├── core/
│   │   │   ├── DIDManager.ts
│   │   │   ├── CredentialHolder.ts
│   │   │   ├── PresentationCreator.ts
│   │   │   └── KeyStore.ts
│   │   ├── flows/
│   │   │   ├── IssuanceFlow.ts (OpenID4VCI)
│   │   │   ├── VerificationFlow.ts (OpenID4VP)
│   │   │   └── PaymentFlow.ts
│   │   ├── services/
│   │   │   ├── api.ts
│   │   │   ├── storage.ts
│   │   │   └── blockchain.ts
│   │   ├── ui/
│   │   │   ├── Onboarding.tsx
│   │   │   ├── Dashboard.tsx
│   │   │   ├── CredentialCard.tsx
│   │   │   ├── PaymentFlow.tsx
│   │   │   └── VerificationFlow.tsx
│   │   └── App.tsx
│   ├── public/
│   └── package.json
├── backend/
│   ├── src/main/java/com/idblock/
│   │   ├── config/
│   │   │   ├── SecurityConfig.java
│   │   │   ├── CryptoConfig.java
│   │   │   └── BlockchainConfig.java
│   │   ├── controller/
│   │   │   ├── IssuerController.java
│   │   │   ├── VerifierController.java
│   │   │   ├── PaymentController.java
│   │   │   └── HealthController.java
│   │   ├── service/
│   │   │   ├── IssuerService.java
│   │   │   ├── VerifierService.java
│   │   │   ├── PaymentService.java
│   │   │   ├── BlockchainService.java
│   │   │   ├── TrustRegistryService.java
│   │   │   ├── RevocationService.java
│   │   │   └── AuditService.java
│   │   ├── model/
│   │   │   ├── User.java
│   │   │   ├── Credential.java
│   │   │   ├── Payment.java
│   │   │   └── AuditEvent.java
│   │   ├── repo/
│   │   │   ├── UserRepository.java
│   │   │   ├── CredentialRepository.java
│   │   │   └── PaymentRepository.java
│   │   └── IdblockApplication.java
│   ├── resources/
│   │   ├── application.yaml
│   │   └── db/migration/
│   │       ├── V1__init.sql
│   │       └── V2__audit.sql
│   ├── pom.xml
│   └── test/
│       └── java/com/idblock/
│           ├── IssuerTest.java
│           ├── VerifierTest.java
│           ├── PaymentTest.java
│           └── EndToEndTest.java
├── contracts/
│   ├── DIDRegistry.sol
│   └── TrustRegistry.sol
├── docker-compose.yaml
├── Dockerfile.backend
├── openapi.yaml
└── README.md
```

---

## **Task Breakdown by Role**

### **Frontend (TypeScript) — 1 Engineer**

| Week | Task | Effort |
|------|------|--------|
| 1 | Wallet scaffold, DID manager, CredentialHolder | 20h |
| 2 | Issuer integration, UI for VC storage | 20h |
| 2 | Verifier integration, VP creation, UI | 20h |
| 2 | Payment UI, balance display | 15h |
| 3 | OpenID4VCI client, liveness UI | 20h |
| 3 | OpenID4VP client, presentation UI | 20h |
| 4 | Bug fixes, UI polish, testing | 20h |
| **Total** | | **135h (~34 days)** |

---

### **Backend (Java) — 2 Engineers**

**Engineer A (Core Services)**

| Week | Task | Effort |
|------|------|--------|
| 1 | Spring Boot scaffold, issuer endpoints, VC signing | 25h |
| 1 | Verifier endpoints, signature validation, decision token | 25h |
| 2 | Payment service, demo balance ledger | 20h |
| 2 | OpenID4VCI metadata + token endpoint | 15h |
| 3 | Blockchain DID registry integration | 20h |
| 3 | Trust registry (on-chain), revocation service | 20h |
| 4 | Error handling, validation, testing | 20h |
| **Total** | | **145h (~36 days)** |

**Engineer B (Data & Infrastructure)**

| Week | Task | Effort |
|------|------|--------|
| 1 | PostgreSQL schema design, JPA setup | 15h |
| 1 | Database migrations, repository layer | 15h |
| 2 | OpenID4VP metadata + presentation endpoint | 15h |
| 2 | Liveness validation, revocation database | 15h |
| 3 | Audit logging service, event queries | 20h |
| 3 | Docker setup, docker-compose, env config | 15h |
| 4 | API documentation (OpenAPI), testing | 25h |
| **Total** | | **120h (~30 days)** |

---

### **DevOps / QA (shared, part-time)**

| Week | Task | Effort |
|------|------|--------|
| 1 | Environment setup, GitHub repos, CI/CD skeleton | 10h |
| 2 | Test data seeding, Postman collection | 10h |
| 3 | End-to-end testing, Blockchain testnet setup | 15h |
| 4 | Deployment docs, health checks, final testing | 15h |
| **Total** | | **50h (~12 days)** |

---

## **Dependency Graph**

```
Week 1:
  [Frontend: DID + Wallet] ─┐
  [Backend: Spring + DB] ───┤─→ M0.1, M0.2
  [Backend: Issuer endpoints] ┘

Week 2:
  [Wallet ↔ Issuer integration] ─┐
  [Verifier endpoints] ──────────┼─→ M0.3, M0.4
  [OpenID4VCI (basic)] ──────────┘

Week 2 (cont):
  [Wallet ↔ Verifier integration] ──┐
  [Payment service (demo balance)] ──┼─→ M0.5, M0.6
  [OpenID4VP (basic)] ───────────────┘

Week 3:
  [Blockchain: DID + Trust registry] ──┐
  [OpenID4VCI (standards-compliant)] ──┼─→ M1.1, M1.2, M1.3, M1.4
  [OpenID4VP (standards-compliant)] ───┘
  [Liveness detection] ────────────────→ M1.5

Week 4:
  [Revocation service] ──────┐
  [Database schema (final)] ──┼─→ M1.6, M1.7, M1.8, M1.9, M1.10
  [Audit logging] ───────────┤
  [Error handling] ──────────┤
  [API docs + deployment] ───┘
```

---

## **Risk Mitigation**

| Risk | Likelihood | Mitigation |
|------|------------|-----------|
| **Blockchain testnet issues** | Medium | Use Polygon testnet (faster); pre-test contract deploys |
| **OpenID4VCI/VP spec confusion** | Medium | Review spec early; use reference implementations |
| **Database migration problems** | Low | Flyway migrations tested locally first |
| **Wallet key management bugs** | High | Use well-tested Veramo library; careful testing |
| **Integration between services** | High | Daily integration testing; mock external deps |
| **Performance (slow verification)** | Low | Cache trust registry; optimize DB queries |

---

## **Definition of Done**

### **PoC (End of Week 2)**

- ✅ Wallet app runs locally
- ✅ Issuer issues VCs
- ✅ Verifier validates VPs + returns decision token
- ✅ Payment flow works (demo balance)
- ✅ End-to-end demo script works
- ✅ Basic README

### **MVP (End of Week 6)**

- ✅ All PoC requirements + additional features
- ✅ OpenID4VCI & OpenID4VP compliant
- ✅ Blockchain (DID + trust registry) functional
- ✅ Liveness detection working
- ✅ Revocation + status service
- ✅ Production database schema
- ✅ Audit logging + event trail
- ✅ Error handling consistent
- ✅ OpenAPI spec + Swagger UI
- ✅ Full test coverage (70%+)
- ✅ Docker deployment working
- ✅ All services documented

---

## **Success Metrics**

| Metric | PoC Target | MVP Target |
|--------|-----------|-----------|
| **Time to issue VC** | <10s | <5s |
| **Time to verify VP** | <5s | <2s |
| **End-to-end payment** | <30s | <20s |
| **Uptime** | 95% | 99.5% |
| **Test coverage** | 40% | 70% |
| **Documentation** | README only | Full API docs |
| **Team velocity** | 100 story points | 200 story points |

---

## **Communication & Sync**

- **Daily standup:** 10 min (async Slack or 15 min sync)
- **Weekly sync:** Design/architecture review (1 hour)
- **Blockers:** Escalate immediately to unblock
- **Git:** Trunk-based development; PR reviews before merge

---

## **Assumptions**

- Team familiar with TypeScript, Java, Spring Boot, PostgreSQL
- Ethereum/Polygon testnet access available (free)
- Can use Veramo.js for wallet (proven library)
- Stripe/Adyen sandbox not needed for MVP (demo balance sufficient)
- Single-region deployment sufficient

---

This plan is **achievable with 3–4 engineers in 6 weeks**. The PoC (2 weeks) gets to "working demo," and the MVP (next 4 weeks) adds production quality.

Would you like me to provide:
1. **Detailed API contract examples** (request/response for each endpoint)?
2. **Database schema with all fields**?
3. **Sample code snippets** for key components?
4. **Git workflow & branch strategy**?
5. **Detailed test plan** for integration tests?
