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
- [ ] Create `VerifierController.java`
- [ ] `GET /verify/challenge` - return nonce/challenge for anti-replay
- [ ] `POST /verify` - accept ZKP proof + public signals + issuer commitment, return decision token
- [ ] `GET /.well-known/openid-provider` - return hardcoded metadata

#### ZKP Validation (PoC Minimal)
- [ ] Create `VerifierService.java`
- [ ] Implement `validateProof(request)`:
  - [ ] Validate request has `challenge`/nonce
  - [ ] Verify issuer signature over commitment
  - [ ] Verify ZKP proof (e.g., Groth16 verification)
  - [ ] Ensure proof binds to challenge (anti-replay)
  - [ ] Check issuer is in trusted list (hardcoded)
  - [ ] Ensure verification does not require raw DOB/passport number fields

#### Decision Token
- [ ] Define decision token claims:
  - [ ] `sub` - holder DID
  - [ ] `verified_at` - timestamp
  - [ ] `assurance_level` - "LOW" for PoC
  - [ ] `verified_claims` - list of verified attributes
  - [ ] `expires_in` - 300 seconds
  - [ ] `jti` - unique token ID
- [ ] Sign with verifier private key
- [ ] Return as JSON response

#### Trusted Issuer List
- [ ] Create `TrustedIssuers.java` config
- [ ] Hardcode issuer DID(s) for PoC
- [ ] Implement `isTrusted(issuerDID)` check

#### Testing
- [ ] Postman: POST valid ZKP proof → receive decision token
- [ ] Postman: POST invalid/expired challenge or invalid proof → receive error
- [ ] Decode decision token and verify claims

---

### M0.5 — Wallet ↔ Verifier Integration (Days 5–6)

#### ZKP Proof Generator
- [ ] Create `src/core/ZKProofGenerator.ts`
- [ ] Define witness input mapping from credential attributes
- [ ] Implement `generateOver18Proof(credential, challenge)`
- [ ] Ensure proof generation happens locally
- [ ] Ensure generated payload omits unnecessary PII (no full DOB/MRZ/passport number)

#### Verification Flow
- [ ] Create `src/flows/VerificationFlow.ts`
- [ ] Implement `verify(vcId, verifierUrl)`:
  - [ ] Load VC from storage
  - [ ] Fetch `challenge` from verifier (`GET /verify/challenge`)
  - [ ] Create ZKP proof for required predicate (PoC: `over_18`)
  - [ ] POST proof + public signals + issuer commitment to verifier
  - [ ] Store decision token
  - [ ] Return verification result

#### Decision Token Storage
- [ ] Implement `storeDecisionToken(token)` in storage service
- [ ] Implement `getValidDecisionToken()` - check expiry
- [ ] Implement `clearExpiredTokens()`

#### UI Components
- [ ] Create `src/ui/VerificationFlow.tsx`
- [ ] "Prove Over 18" button
- [ ] Show verifier request details
- [ ] Confirmation dialog
- [ ] Loading state during verification
- [ ] Success: "✓ Verified" display
- [ ] Error: Clear error message

#### Testing
- [ ] Full flow: select credential → present → see verified status
- [ ] Decision token stored correctly
- [ ] Expired token triggers re-verification

---

### M0.6 — Payment Demo (Days 6–7)

#### Payment Service Backend
- [ ] Create `PaymentController.java`
- [ ] `POST /payments/intents` - create payment intent
- [ ] `POST /payments/{id}/verify-kyc` - attach decision token
- [ ] `POST /payments/{id}/confirm` - execute transfer

#### Demo Balance Ledger
- [ ] Create `PaymentService.java`
- [ ] In-memory map: `DID → balance`
- [ ] Initialize test accounts with demo balance
- [ ] Implement `transfer(from, to, amount)`
- [ ] Implement `getBalance(did)`

#### KYC Gate Logic
- [ ] Payment intent requires decision token
- [ ] Validate decision token:
  - [ ] Verify signature
  - [ ] Check not expired
  - [ ] Verify holder matches payer
- [ ] Reject payment if token invalid/missing

#### Wallet Payment Client
- [ ] Create `src/services/PaymentClient.ts`
- [ ] Implement `createIntent(amount, receiverDID)`
- [ ] Implement `attachKYC(intentId, decisionToken)`
- [ ] Implement `confirmPayment(intentId)`
- [ ] Implement `getBalance()`

#### UI Components
- [ ] Create `src/ui/PaymentFlow.tsx`
- [ ] Amount input
- [ ] Receiver DID input (or select from contacts)
- [ ] KYC verification step (auto-trigger if needed)
- [ ] Confirmation screen
- [ ] Receipt display
- [ ] Create `src/ui/BalanceDisplay.tsx`
- [ ] Show current balance
- [ ] Transaction history (optional)

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
- [ ] Create `contracts/DIDRegistry.sol`
- [ ] `registerDID(didHash, publicKeyJWK, timestamp)` function
- [ ] `verifyDIDAt(didHash, timestamp)` view function
- [ ] `getDIDInfo(didHash)` view function
- [ ] Events: `DIDRegistered`, `DIDUpdated`
- [ ] Deploy to Polygon Mumbai testnet

#### Java Integration
- [ ] Add web3j dependency
- [ ] Create `BlockchainConfig.java` - RPC URL, contract address
- [ ] Create `BlockchainService.java`
- [ ] Implement `publishIssuerKey(did, publicKey)` - write transaction
- [ ] Implement `verifyIssuerOnChain(did)` - read from contract
- [ ] Handle transaction confirmation

#### Backend Integration
- [ ] On issuer startup: publish issuer DID to chain (if not exists)
- [ ] On verification: check if issuer DID registered on-chain
- [ ] Log blockchain transaction hashes

#### Testing
- [ ] Deploy contract to testnet
- [ ] Publish issuer key → verify on block explorer
- [ ] Verifier reads contract → confirms issuer trusted
- [ ] Handle testnet RPC failures gracefully

---

### M1.2 — Trust Registry On-Chain (Days 10–12)

#### Smart Contract
- [ ] Create `contracts/TrustRegistry.sol`
- [ ] Admin address for management
- [ ] `addIssuer(issuerDID, assuranceLevel, metadata)` (admin only)
- [ ] `removeIssuer(issuerDID)` (admin only)
- [ ] `isTrusted(issuerDID, atTimestamp)` view function
- [ ] Events: `IssuerAdded`, `IssuerRemoved`
- [ ] Deploy to Polygon Mumbai testnet

#### Java Integration
- [ ] Create `TrustRegistryService.java`
- [ ] Implement `addIssuer(issuerDID, level)` - admin function
- [ ] Implement `isTrustedIssuer(issuerDID)` - query contract
- [ ] Cache registry entries (1 hour TTL)
- [ ] Cache invalidation on events

#### Verifier Integration
- [ ] Update `VerifierService` to check on-chain trust registry
- [ ] Fall back to cached value if chain unavailable
- [ ] Log trust registry lookups

#### Testing
- [ ] Add issuer to registry → verification succeeds
- [ ] Remove issuer → verification fails
- [ ] Cache works correctly
- [ ] Handles chain unavailability

---

### M1.3 — OpenID4VCI Implementation (Days 12–15)

#### Issuer Metadata (Per Spec)
- [ ] Update `/.well-known/openid-credential-issuer`
- [ ] `credential_issuer` - issuer URL
- [ ] `credential_endpoint` - credential request URL
- [ ] `credentials_supported` - list of credential types
- [ ] `display` - issuer display info

#### Token Endpoint
- [ ] Create `POST /token`
- [ ] Accept pre-authorized code flow
- [ ] Validate authorization
- [ ] Return `access_token` with expiry

#### Credential Endpoint (Per Spec)
- [ ] Update `POST /credential`
- [ ] Accept `format`, `credential_definition`, `proof`
- [ ] Validate access token
- [ ] Validate proof JWT
- [ ] Return credential in requested format

#### Wallet Integration
- [ ] Update `CredentialHolder.ts` for OpenID4VCI
- [ ] Fetch and parse issuer metadata
- [ ] Request token (pre-auth flow)
- [ ] Create proof JWT with nonce
- [ ] Request credential with proof
- [ ] Handle credential response

#### Testing
- [ ] Validate against OpenID4VCI spec
- [ ] Test with external validator tools
- [ ] Full issuance flow works

---

### M1.4 — OpenID4VP Implementation (Days 15–18)

#### Verifier Metadata (Per Spec)
- [ ] Update `/.well-known/openid-verifier`
- [ ] Supported presentation formats
- [ ] Supported algorithms
- [ ] Client metadata

#### Authorization Endpoint
- [ ] Create `GET /authorize`
- [ ] Generate `presentation_definition`
- [ ] Specify required credentials and claims
- [ ] Return authorization request

#### Response Endpoint
- [ ] Create `POST /callback`
- [ ] Accept `vp_token` and `presentation_submission`
- [ ] Validate per OpenID4VP spec
- [ ] Return decision token (or `id_token` per spec)

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
- [ ] Validate against OpenID4VP spec
- [ ] Test claim filtering
- [ ] Full verification flow works

---

### M1.5 — Liveness Detection (Days 18–20)

#### Frontend Integration
- [ ] Add face-api.js or TensorFlow.js dependency
- [ ] Load face detection model
- [ ] Create `src/services/liveness.ts`
- [ ] Implement `detectFace(videoFrame)` - returns bounding box
- [ ] Implement `calculateLivenessScore(frames[])` - basic anti-spoofing

#### Liveness Check Flow
- [ ] Capture 3-5 video frames during passport scan
- [ ] Check face detected in frames
- [ ] Basic motion detection (face position changes)
- [ ] Calculate liveness score (0-1)

#### Issuer Validation
- [ ] Update `/issue` to accept `liveness_proof`
- [ ] Validate liveness score > 0.7
- [ ] Reject issuance if liveness fails
- [ ] Log liveness check results

#### UI Components
- [ ] Camera access permission handling
- [ ] Video stream display
- [ ] "Look at camera" / "Turn head slightly" prompts
- [ ] Liveness check progress indicator
- [ ] Success/failure feedback

#### Testing
- [ ] Detects real face successfully
- [ ] Rejects photo of photo (basic)
- [ ] Works on Chrome and Safari
- [ ] Handles camera permission denial

---

### M1.6 — Revocation Service (Days 20–21)

#### Database Schema
- [ ] Add `credential_status` table
  - [ ] `credential_id` (FK)
  - [ ] `status` (VALID, REVOKED, SUSPENDED)
  - [ ] `revoked_at` timestamp
  - [ ] `revocation_reason`

#### Issuer Endpoints
- [ ] Create `POST /revoke/{credentialId}` (admin only)
- [ ] Validate admin authorization
- [ ] Update credential status
- [ ] Log revocation event

#### Status Endpoint
- [ ] Update `GET /status/{revocationHandle}`
- [ ] Return current status
- [ ] Include revocation timestamp if revoked

#### Verifier Integration
- [ ] Before accepting VC, call status endpoint
- [ ] Cache status (short TTL, e.g., 5 minutes)
- [ ] Reject revoked credentials
- [ ] Include status check in verification logs

#### Testing
- [ ] Issue credential → verify works
- [ ] Revoke credential → verify fails
- [ ] Status endpoint returns correct data
- [ ] Caching works correctly

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
