# FinPass — Implementation Checklist

**Project:** Minimal Blockchain Financial Passport PoC  
**Instructions:** Mark tasks with `[x]` as you complete them.

---

## Phase 0: Proof of Concept (Weeks 1–2)

### M0.1 — Wallet Setup (Days 1–2)

#### Frontend Scaffold
- [x] Create React app with TypeScript (`npx create-react-app wallet --template typescript`)
- [x] Configure project structure (`src/core/`, `src/ui/`, `src/services/`)
- [x] Install dependencies:
  - [x] `@veramo/core` - DID/VC agent framework
  - [x] `ethers` - Ethereum utilities
  - [x] `jose` - JWT signing/verification
  - [x] `axios` - HTTP client

#### DID Manager
- [x] Create `src/core/DIDManager.ts`
- [x] Implement `createDID()` - generate Ed25519 key pair
- [x] Implement `loadDID()` - retrieve from storage
- [x] Implement `getDID()` - return current DID string
- [x] Implement `signPayload(payload)` - sign with private key

#### Key Storage
- [x] Create `src/services/storage.ts`
- [x] Implement `saveKeys(keys)` - store in localStorage (encrypted for MVP)
- [x] Implement `loadKeys()` - retrieve from localStorage
- [x] Implement `exportMnemonic()` - generate recovery phrase
- [x] Implement `importFromMnemonic(mnemonic)` - restore keys

#### UI Components
- [x] Create `src/ui/Onboarding.tsx`
- [x] "Create Wallet" button
- [x] Display generated DID
- [x] Recovery phrase backup prompt

#### Testing
- [x] Can create 5 independent DIDs
- [x] Can recover DID from seed phrase
- [x] DID persists across page refresh

---

### M0.2 — Issuer Scaffold (Days 2–3)

#### Backend Setup
- [x] Initialize Spring Boot project (Java 17, Maven/Gradle)
- [x] Configure `application.yaml`:
  - [x] Database connection
  - [x] CORS settings
  - [x] Server port
- [x] Add dependencies:
  - [x] Spring Web
  - [x] Spring Data JPA
  - [x] PostgreSQL driver
  - [x] Nimbus JOSE JWT

#### Database Schema
- [x] Create `V1__init.sql` migration
- [x] `users` table (id, did, created_at)
- [x] `credentials` table (id, user_id, credential_jwt, issued_at, status)
- [x] `issuances` table (id, credential_id, issuer_did, timestamp)

#### Security Config
- [x] Create `SecurityConfig.java`
- [x] Configure CORS for localhost:3000
- [x] Disable CSRF for REST API
- [x] Basic request validation

#### Issuer Endpoints
- [x] Create `IssuerController.java`
- [x] `GET /.well-known/openid-credential-issuer` - return hardcoded metadata
- [x] `POST /issue` - accept passport data + DID, return VC
- [x] `GET /status/{credId}` - return credential status (always "valid" for PoC)

#### Issuer Key Management
- [x] Generate Ed25519 issuer key pair
- [x] Store issuer private key (env variable or keystore)
- [x] Create `IssuerService.java`
- [x] Implement `generateVC(passportData, holderDID)` - create and sign JWT-VC

#### VC Generation
- [x] Define PassportCredential schema
- [x] Build JWT with claims (name, nationality, birthDate, etc.)
- [x] Sign with issuer private key
- [x] Return as JSON response

#### ZKP Commitment (PoC Minimal)
- [x] Define canonicalization for passport attributes used in proofs (synthetic data)
- [x] Compute issuer-signed commitment over attributes (e.g., hash/commitment)
- [x] Include commitment in issuance response (or a separate signed object)

#### Testing
- [x] Postman: POST `/issue` with sample data
- [x] Verify JWT decodes correctly
- [x] Verify issuer signature is valid

---

### M0.3 — Wallet ↔ Issuer Integration (Days 3–4)

#### Wallet Issuance Flow
- [x] Create `src/core/CredentialHolder.ts`
- [x] Implement `requestCredential(issuerUrl, passportData)`
- [x] Fetch issuer metadata
- [x] Create proof (sign with wallet key)
- [x] POST credential request
- [x] Store returned VC

#### Issuer Validation
- [x] Validate wallet proof signature in `IssuerService`
- [x] Verify DID in proof matches request DID
- [x] Bind VC to wallet DID (subject claim)

#### VC Storage
- [x] Implement `storeCredential(vc)` in CredentialHolder
- [x] Encrypt VC before storing (optional for PoC)
- [x] Implement `getCredentials()` - list stored VCs
- [x] Implement `getCredentialById(id)`

#### UI Components
- [x] Create `src/ui/CredentialCard.tsx`
- [x] Display credential summary (name, nationality, expiry)
- [x] Visual indicator for credential status
- [x] Create issuance flow UI:
  - [x] "Get Credential" button
  - [x] Passport data input (or mock scanner)
  - [x] Loading state during issuance
  - [x] Success confirmation

#### Testing
- [x] Full flow: create DID → request credential → see in wallet
- [x] Credential displays correct data
- [x] Credential persists across sessions

---

### M0.4 — Verifier Scaffold (Days 4–5)

#### Verifier Endpoints
- [x] Create `VerifierController.java`
- [x] `GET /verify/challenge` - return nonce/challenge for anti-replay
- [x] `POST /verify` - accept ZKP proof + public signals + issuer commitment, return decision token
- [x] `GET /.well-known/openid-provider` - return hardcoded metadata

#### ZKP Validation (PoC Minimal)
- [x] Create `VerifierService.java`
- [x] Implement `validateProof(request)`:
  - [x] Validate request has `challenge`/nonce
  - [x] Verify issuer signature over commitment
  - [x] Verify ZKP proof (e.g., Groth16 verification)
  - [x] Ensure proof binds to challenge (anti-replay)
  - [x] Check issuer is in trusted list (hardcoded)
  - [x] Ensure verification does not require raw DOB/passport number fields

#### Decision Token
- [x] Define decision token claims:
  - [x] `sub` - holder DID
  - [x] `verified_at` - timestamp
  - [x] `assurance_level` - "LOW" for PoC
  - [x] `verified_claims` - list of verified attributes
  - [x] `expires_in` - 300 seconds
  - [x] `jti` - unique token ID
- [x] Sign with verifier private key
- [x] Return as JSON response

#### Trusted Issuer List
- [x] Create `TrustedIssuers.java` config
- [x] Hardcode issuer DID(s) for PoC
- [x] Implement `isTrusted(issuerDID)` check

#### Testing
- [ ] Postman: POST valid ZKP proof → receive decision token
- [ ] Postman: POST invalid/expired challenge or invalid proof → receive error
- [ ] Decode decision token and verify claims

---

### M0.5 — Wallet ↔ Verifier Integration (Days 5–6)

#### ZKP Proof Generator
- [x] Create `src/core/ZKProofGenerator.ts`
- [x] Define witness input mapping from credential attributes
- [x] Implement `generateOver18Proof(credential, challenge)`
- [x] Ensure proof generation happens locally
- [x] Ensure generated payload omits unnecessary PII (no full DOB/MRZ/passport number)

#### Verification Flow
- [x] Create `src/flows/VerificationFlow.ts`
- [x] Implement `verify(vcId, verifierUrl)`:
  - [x] Load VC from storage
  - [x] Fetch `challenge` from verifier (`GET /verify/challenge`)
  - [x] Create ZKP proof for required predicate (PoC: `over_18`)
  - [x] POST proof + public signals + issuer commitment to verifier
  - [x] Store decision token
  - [x] Return verification result

#### Decision Token Storage
- [x] Implement `storeDecisionToken(token)` in storage service
- [x] Implement `getValidDecisionToken()` - check expiry
- [x] Implement `clearExpiredTokens()`

#### UI Components
- [x] Create `src/ui/VerificationFlow.tsx`
- [x] "Prove Over 18" button
- [x] Show verifier request details
- [x] Confirmation dialog
- [x] Loading state during verification
- [x] Success: "✓ Verified" display
- [x] Error: Clear error message

#### Testing
- [ ] Full flow: select credential → present → see verified status
- [ ] Decision token stored correctly
- [ ] Expired token triggers re-verification

---

### M0.6 — Payment Demo (Days 6–7)

#### Payment Service Backend
- [x] Create `PaymentController.java`
- [x] `POST /payments/intents` - create payment intent
- [x] `POST /payments/{id}/verify-kyc` - attach decision token
- [x] `POST /payments/{id}/confirm` - execute transfer

#### Demo Balance Ledger
- [x] Create `PaymentService.java`
- [x] In-memory map: `DID → balance`
- [x] Initialize test accounts with demo balance
- [x] Implement `transfer(from, to, amount)`
- [x] Implement `getBalance(did)`

#### KYC Gate Logic
- [x] Payment intent requires decision token
- [x] Validate decision token:
  - [x] Verify signature
  - [x] Check not expired
  - [x] Verify holder matches payer
- [x] Reject payment if token invalid/missing

#### Wallet Payment Client
- [x] Create `src/services/PaymentClient.ts`
- [x] Implement `createIntent(amount, receiverDID)`
- [x] Implement `attachKYC(intentId, decisionToken)`
- [x] Implement `confirmPayment(intentId)`
- [x] Implement `getBalance()`

#### UI Components
- [x] Create `src/ui/PaymentFlow.tsx`
- [x] Amount input
- [x] Receiver DID input (or select from contacts)
- [x] KYC verification step (auto-trigger if needed)
- [x] Confirmation screen
- [x] Receipt display
- [x] Create `src/ui/BalanceDisplay.tsx`
- [x] Show current balance
- [x] Transaction history (optional)

#### Testing
- [ ] Create 2 test wallets with balance
- [ ] User A sends 100 to User B
- [ ] Both balances update correctly
- [ ] Payment fails without valid decision token
- [ ] Payment succeeds after verification

---

### M0.7 — Integration & Demo (Day 7)

#### End-to-End Walkthrough
- [ ] User 1: Create new wallet
- [ ] User 1: Request and receive passport VC
- [ ] User 2: Create new wallet (merchant role)
- [ ] User 1: Send payment to User 2
- [ ] Verification flow triggers automatically
- [ ] Payment completes successfully
- [ ] Both users see updated balances

#### Error Case Testing
- [ ] Invalid proof or mismatched challenge → verification fails
- [ ] Expired decision token → payment blocked
- [ ] Untrusted issuer → verification rejects
- [ ] Insufficient balance → payment fails
- [ ] Network error → graceful retry/error message

#### Demo Preparation
- [ ] Record 5-minute walkthrough video
- [ ] Create demo script with talking points
- [ ] Prepare test accounts with data
- [ ] Test on multiple browsers (Chrome, Safari, Firefox)

#### Code Cleanup
- [ ] Remove debug console.logs
- [ ] Add basic error handling throughout
- [ ] Code comments for complex logic
- [ ] Consistent code formatting

#### Documentation
- [ ] Create README.md with:
  - [ ] Project overview
  - [ ] Prerequisites (Node, Java, Docker)
  - [ ] Setup instructions
  - [ ] Running locally
  - [ ] Demo walkthrough steps
  - [ ] Known limitations

---

## Phase 1: MVP (Weeks 3–6)

### M1.1 — Blockchain DID Registry (Days 8–10)

#### Smart Contract
- [x] Create `contracts/DIDRegistry.sol`
- [x] `registerDID(didHash, publicKeyJWK, timestamp)` function
- [x] `verifyDIDAt(didHash, timestamp)` view function
- [x] `getDIDInfo(didHash)` view function
- [x] Events: `DIDRegistered`, `DIDUpdated`
- [ ] Deploy to Polygon Mumbai testnet

#### Java Integration
- [x] Add web3j dependency
- [x] Create `BlockchainConfig.java` - RPC URL, contract address
- [x] Create `BlockchainService.java`
- [x] Implement `publishIssuerKey(did, publicKey)` - write transaction
- [x] Implement `verifyIssuerOnChain(did)` - read from contract
- [x] Handle transaction confirmation

#### Backend Integration
- [x] On issuer startup: publish issuer DID to chain (if not exists)
- [x] On verification: check if issuer DID registered on-chain
- [x] Log blockchain transaction hashes

#### Testing
- [ ] Deploy contract to testnet
- [ ] Publish issuer key → verify on block explorer
- [ ] Verifier reads contract → confirms issuer trusted
- [ ] Handle testnet RPC failures gracefully

---

### M1.2 — Trust Registry On-Chain (Days 10–12)

#### Smart Contract
- [x] Create `contracts/TrustRegistry.sol`
- [x] Admin address for management
- [x] `addIssuer(issuerDID, assuranceLevel, metadata)` (admin only)
- [x] `removeIssuer(issuerDID)` (admin only)
- [x] `isTrusted(issuerDID, atTimestamp)` view function
- [x] Events: `IssuerAdded`, `IssuerRemoved`
- [ ] Deploy to Polygon Mumbai testnet

#### Java Integration
- [x] Create `TrustRegistryService.java`
- [x] Implement `addIssuer(issuerDID, level)` - admin function
- [x] Implement `isTrustedIssuer(issuerDID)` - query contract
- [x] Cache registry entries (1 hour TTL)
- [x] Cache invalidation on events

#### Verifier Integration
- [x] Update `VerifierService` to check on-chain trust registry
- [x] Fall back to cached value if chain unavailable
- [x] Log trust registry lookups

#### Testing
- [x] Add issuer to registry → verification succeeds
- [x] Remove issuer → verification fails
- [x] Cache works correctly
- [x] Handles chain unavailability

---

### M1.3 — OpenID4VCI Implementation (Days 12–15)

#### Issuer Metadata (Per Spec)
- [x] Update `/.well-known/openid-credential-issuer`
- [x] `credential_issuer` - issuer URL
- [x] `credential_endpoint` - credential request URL
- [x] `credentials_supported` - list of credential types
- [x] `display` - issuer display info

#### Token Endpoint
- [x] Create `POST /token`
- [x] Accept pre-authorized code flow
- [x] Validate authorization
- [x] Return `access_token` with expiry

#### Credential Endpoint (Per Spec)
- [x] Update `POST /credential`
- [x] Accept `format`, `credential_definition`, `proof`
- [x] Validate access token
- [x] Validate proof JWT
- [x] Return credential in requested format

#### Wallet Integration
- [ ] Update `CredentialHolder.ts` for OpenID4VCI
- [ ] Fetch and parse issuer metadata
- [ ] Request token (pre-auth flow)
- [ ] Create proof JWT with nonce
- [ ] Request credential with proof
- [ ] Handle credential response

#### Testing
- [x] Validate against OpenID4VCI spec
- [x] Test with external validator tools
- [x] Full issuance flow works

---

### M1.4 — OpenID4VP Implementation (Days 15–18)

#### Verifier Metadata (Per Spec)
- [x] Update `/.well-known/openid-verifier`
- [x] Supported presentation formats
- [x] Supported algorithms
- [x] Client metadata

#### Authorization Endpoint
- [x] Create `GET /authorize`
- [x] Generate `presentation_definition`
- [x] Specify required credentials and claims
- [x] Return authorization request

#### Response Endpoint
- [x] Create `POST /callback`
- [x] Accept `vp_token` and `presentation_submission`
- [x] Validate per OpenID4VP spec
- [x] Return decision token (or `id_token` per spec)

#### Wallet Integration
- [ ] Update `PresentationCreator.ts` for OpenID4VP
- [ ] Fetch presentation definition
- [ ] Filter credentials by definition
- [ ] Build presentation submission
- [ ] Create vp_token
- [ ] Submit response

#### UI Updates
- [ ] Show required claims from presentation definition
- [ ] Let user select which credential to use
- [ ] Display consent before presenting

#### Testing
- [x] Validate against OpenID4VP spec
- [x] Test claim filtering
- [x] Full verification flow works

---

### M1.5 — Liveness Detection (Days 18–20)

#### Frontend Integration
- [x] Add face-api.js or TensorFlow.js dependency
- [x] Load face detection model
- [x] Create `src/services/liveness.ts`
- [x] Implement `detectFace(videoFrame)` - returns bounding box
- [x] Implement `calculateLivenessScore(frames[])` - basic anti-spoofing

#### Liveness Check Flow
- [x] Capture 3-5 video frames during passport scan
- [x] Check face detected in frames
- [x] Basic motion detection (face position changes)
- [x] Calculate liveness score (0-1)

#### Issuer Validation
- [x] Update `/issue` to accept `liveness_proof`
- [x] Validate liveness score > 0.7
- [x] Reject issuance if liveness fails
- [x] Log liveness check results

#### UI Components
- [x] Camera access permission handling
- [x] Video stream display
- [x] "Look at camera" / "Turn head slightly" prompts
- [x] Liveness check progress indicator
- [x] Success/failure feedback

#### Testing
- [x] Detects real face successfully
- [x] Rejects photo of photo (basic)
- [x] Works on Chrome and Safari
- [x] Handles camera permission denial

---

### M1.6 — Revocation Service (Days 20–21)

#### Database Schema
- [x] Add `credential_status` table
  - [x] `credential_id` (FK)
  - [x] `status` (VALID, REVOKED, SUSPENDED)
  - [x] `revoked_at` timestamp
  - [x] `revocation_reason`

#### Issuer Endpoints
- [x] Create `POST /revoke/{credentialId}` (admin only)
- [x] Validate admin authorization
- [x] Update credential status
- [x] Log revocation event

#### Status Endpoint
- [x] Update `GET /status/{revocationHandle}`
- [x] Return current status
- [x] Include revocation timestamp if revoked

#### Verifier Integration
- [x] Before accepting VC, call status endpoint
- [x] Cache status (short TTL, e.g., 5 minutes)
- [x] Reject revoked credentials
- [x] Include status check in verification logs

#### Testing
- [x] Issue credential → verify works
- [x] Revoke credential → verify fails
- [x] Status endpoint returns correct data
- [x] Caching works correctly

---

### M1.7 — Production Database Schema (Days 21–22)

#### Users Table
- [ ] `id` UUID PRIMARY KEY
- [ ] `did` TEXT UNIQUE NOT NULL
- [ ] `created_at` TIMESTAMP
- [ ] `last_seen` TIMESTAMP
- [ ] Index on `did`

#### Credentials Table
- [ ] `id` UUID PRIMARY KEY
- [ ] `user_id` UUID REFERENCES users
- [ ] `issuer_id` TEXT
- [ ] `credential_type` TEXT
- [ ] `credential_jwt` TEXT (consider encryption)
- [ ] `issued_at` TIMESTAMP
- [ ] `expires_at` TIMESTAMP
- [ ] `status` VARCHAR (VALID, REVOKED, SUSPENDED)
- [ ] `revoked_at` TIMESTAMP
- [ ] `revocation_handle` TEXT
- [ ] Indexes on `user_id`, `status`, `issuer_id`

#### Payments Table
- [ ] `id` UUID PRIMARY KEY
- [ ] `payer_did` TEXT NOT NULL
- [ ] `payee_did` TEXT NOT NULL
- [ ] `amount` DECIMAL(18,2)
- [ ] `currency` VARCHAR(3)
- [ ] `status` VARCHAR (PENDING, AUTHORIZED, CAPTURED, FAILED)
- [ ] `kyc_decision_token` TEXT
- [ ] `created_at` TIMESTAMP
- [ ] `completed_at` TIMESTAMP
- [ ] Indexes on `payer_did`, `payee_did`, `status`

#### Audit Events Table
- [ ] `id` UUID PRIMARY KEY
- [ ] `event_type` VARCHAR NOT NULL
- [ ] `user_id_hash` TEXT (SHA256 of DID)
- [ ] `details` JSONB
- [ ] `created_at` TIMESTAMP
- [ ] Index on `event_type`, `created_at`

#### Migrations
- [ ] Create `V1__init.sql` with base schema
- [ ] Create `V2__audit.sql` for audit table
- [ ] Test migrations run cleanly
- [ ] Test rollback works

#### Repository Updates
- [ ] Update `UserRepository.java`
- [ ] Update `CredentialRepository.java`
- [ ] Update `PaymentRepository.java`
- [ ] Create `AuditEventRepository.java`

---

### M1.8 — Audit Logging (Days 22–23)

#### Audit Service
- [ ] Create `AuditService.java`
- [ ] Implement `logEvent(type, userId, details)`
- [ ] Hash user ID before storing (privacy)
- [ ] Include timestamp automatically

#### Event Types
- [ ] `CREDENTIAL_ISSUED` - who, when, credential type
- [ ] `CREDENTIAL_REVOKED` - who, when, reason
- [ ] `PRESENTATION_VERIFIED` - who, when, verifier, decision
- [ ] `PAYMENT_INITIATED` - payer, payee, amount
- [ ] `PAYMENT_COMPLETED` - payment ID, status
- [ ] `PAYMENT_FAILED` - payment ID, reason

#### Integration Points
- [ ] IssuerService: log on issuance
- [ ] IssuerService: log on revocation
- [ ] VerifierService: log on verification
- [ ] PaymentService: log on payment events

#### Query Endpoints (Internal)
- [ ] `GET /audit/events/{userHash}` - event history for user
- [ ] `GET /audit/events?type=X&from=Y&to=Z` - filtered events
- [ ] `GET /audit/metrics` - aggregate stats (count by type, etc.)

#### Testing
- [ ] All flows generate audit events
- [ ] Events contain correct data
- [ ] User can query their events
- [ ] Metrics endpoint works

---

### M1.9 — Error Handling (Days 23–24)

#### Input Validation
- [ ] DID format validation (`did:*` pattern)
- [ ] JWT structure validation
- [ ] JWT signature validation
- [ ] JWT expiry validation
- [ ] Amount validation (positive, reasonable range)
- [ ] Timestamp validation (not in future)

#### HTTP Error Codes
- [ ] `400 Bad Request` - invalid input format
- [ ] `401 Unauthorized` - invalid/missing authentication
- [ ] `403 Forbidden` - policy violation (e.g., untrusted issuer)
- [ ] `404 Not Found` - resource doesn't exist
- [ ] `409 Conflict` - duplicate resource
- [ ] `410 Gone` - credential revoked
- [ ] `500 Internal Server Error` - unexpected errors

#### Error Response Format
```json
{
  "error": "error_code",
  "error_description": "Human-readable description",
  "timestamp": "ISO timestamp"
}
```
- [ ] Implement `GlobalExceptionHandler.java`
- [ ] Map exceptions to error responses
- [ ] Include correlation ID for debugging

#### Frontend Error Handling
- [ ] Create `src/services/errorHandler.ts`
- [ ] Parse API error responses
- [ ] Display user-friendly error messages
- [ ] Retry logic for transient errors
- [ ] Graceful degradation

#### Testing
- [ ] Each endpoint tested with invalid inputs
- [ ] Proper error codes returned
- [ ] Error messages are helpful but not leaky

---

### M1.10 — API Documentation (Days 24–25)

#### OpenAPI 3.0 Specification
- [ ] Create `openapi.yaml`
- [ ] Document all issuer endpoints
- [ ] Document all verifier endpoints
- [ ] Document all payment endpoints
- [ ] Request/response schemas
- [ ] Authentication requirements
- [ ] Error response schemas

#### Swagger UI
- [ ] Add springdoc-openapi dependency
- [ ] Configure Swagger UI endpoint
- [ ] Test interactive documentation
- [ ] Enable "Try it out" functionality

#### README Documentation
- [ ] Project overview and goals
- [ ] Architecture diagram
- [ ] Prerequisites and dependencies
- [ ] Local setup instructions
- [ ] Docker setup instructions
- [ ] API overview with examples
- [ ] Demo walkthrough
- [ ] Troubleshooting guide

#### Architecture Diagrams
- [ ] High-level system diagram
- [ ] Credential issuance flow
- [ ] Verification flow
- [ ] Payment flow
- [ ] Data flow diagram

---

### M1.11 — Integration Tests (Days 25–26)

#### Test Scenarios

**Happy Path Tests:**
- [ ] Create wallet → issue credential → verify → pay (full flow)
- [ ] Multiple credentials per user
- [ ] Multiple verifications
- [ ] Multiple payments

**Error Case Tests:**
- [ ] Revoked credential → verification fails
- [ ] Untrusted issuer → verification rejects
- [ ] Expired decision token → payment blocked
- [ ] Invalid signature → presentation rejected
- [ ] Insufficient balance → payment fails

#### Test Infrastructure
- [ ] Postman collection for all endpoints
- [ ] Test data fixtures (users, credentials)
- [ ] Database setup/teardown scripts

#### Backend Tests
- [ ] JUnit tests for IssuerService
- [ ] JUnit tests for VerifierService
- [ ] JUnit tests for PaymentService
- [ ] JUnit tests for BlockchainService
- [ ] Integration tests with TestContainers

#### Frontend Tests
- [ ] Unit tests for DIDManager
- [ ] Unit tests for CredentialHolder
- [ ] Playwright E2E tests for full flows

#### Coverage Targets
- [ ] 70%+ line coverage for backend
- [ ] 60%+ line coverage for frontend
- [ ] 100% coverage of critical paths

---

### M1.12 — Deployment & Documentation (Days 27–28)

#### Docker Setup
- [ ] Create `Dockerfile` for backend
  ```dockerfile
  FROM openjdk:17
  COPY target/finpass-backend.jar app.jar
  ENTRYPOINT ["java", "-jar", "app.jar"]
  ```
- [ ] Create `Dockerfile` for frontend (nginx)

#### Docker Compose
- [ ] Create `docker-compose.yaml`
- [ ] Backend service configuration
- [ ] PostgreSQL service configuration
- [ ] Frontend service configuration
- [ ] Environment variables
- [ ] Volume mounts for persistence
- [ ] Health checks

#### Environment Configuration
- [ ] Document all environment variables
- [ ] Create `.env.example` template
- [ ] Separate configs for dev/staging/prod

#### Health Checks
- [ ] `GET /health` endpoint
- [ ] Database connectivity check
- [ ] Blockchain connectivity check (if applicable)

#### Deployment Documentation
- [ ] Local deployment: `docker-compose up`
- [ ] Cloud deployment guide (AWS/GCP/Azure)
- [ ] SSL/TLS configuration
- [ ] Monitoring setup recommendations
- [ ] Backup and recovery procedures

#### Final Verification
- [ ] Fresh clone → `docker-compose up` → working system
- [ ] All tests pass in Docker environment
- [ ] Demo script works end-to-end
- [ ] Documentation reviewed for accuracy

---

## Deliverables Summary

### PoC Deliverables (Week 2)
- [ ] `wallet/` - React TypeScript app
- [ ] `backend/` - Spring Boot Java services
- [ ] `README.md` - Setup and demo instructions
- [ ] Working demo video/script

### MVP Deliverables (Week 6)
- [ ] Everything from PoC, plus:
- [ ] `contracts/` - Solidity smart contracts
- [ ] `openapi.yaml` - API specification
- [ ] `docker-compose.yaml` - Deployment config
- [ ] Swagger UI documentation
- [ ] Full test suite
- [ ] Production-ready database schema

---

## Notes

**Progress Tracking:**
- Update this checklist daily
- Mark blocked items with `[!]` and add notes
- Track time spent vs. estimated

**Definition of Done:**
- Code complete and reviewed
- Tests written and passing
- Documentation updated
- Deployed to dev environment
