# FinPass — Implementation Plan & Milestones

**Project:** Minimal Blockchain Financial Passport PoC  
**Timeline:** 4–6 weeks (PoC: 2 weeks, MVP: 4 weeks)  
**Team Size:** 3–4 engineers (1 TS/Frontend, 2 Java/Backend, 1 DevOps/Infra)

---

## Executive Summary

This plan follows a phased approach:
1. **Phase 0 (PoC):** End-to-end proof that SSI + payments work together (Weeks 1–2)
2. **Phase 1 (MVP):** Production-ready system with blockchain anchoring and standards compliance (Weeks 3–6)

---

## Phase 0: Proof of Concept (Weeks 1–2)

### Goal
Demonstrate end-to-end flow: User signs up → gets credential → verifies identity → makes payment.

### Scope

| Component | In Scope | Notes |
|-----------|----------|-------|
| Wallet DID creation | ✅ | localStorage; Ed25519 keys |
| Passport upload + VC issuance | ✅ | Manual validation; no ML liveness |
| ZKP-based verification | ✅ | Minimal predicate proof (e.g., prove over-18) without sending raw passport PII |
| Verifier check | ✅ | Signature validation + decision token |
| Payment flow | ✅ | Demo balance transfer (closed-loop) |
| Trust registry | ❌ | Hardcoded issuer list |
| Blockchain anchoring | ❌ | Database only |
| Liveness detection | ❌ | Manual validation |
| Revocation | ❌ | All credentials valid |
| Real PSP | ❌ | Demo in-app balance |

### Architecture

```
┌─────────────────┐
│  React Wallet   │ (TypeScript)
│  - DID creation │
│  - VC holder    │
│  - Payment UI   │
└────────┬────────┘
         │ HTTP/REST
┌────────▼────────────────────┐
│  Spring Boot API (Java)     │
│  • POST /issue              │
│  • POST /verify             │ (accept ZKP proof + issuer commitment)
│  • POST /payment            │
└────────┬────────────────────┘
         │
┌────────▼────────┐
│  PostgreSQL     │
│  (users, creds, │
│   payments)     │
└─────────────────┘
```

### Milestones

#### M0.1: Wallet Setup (Days 1–2)
**Deliverable:** Wallet app displays user's DID

| Task | Owner | Effort |
|------|-------|--------|
| Create React app with TypeScript | Frontend | 4h |
| Install Veramo + ethers + jose | Frontend | 2h |
| Implement DID Manager (create/load) | Frontend | 6h |
| Key storage in localStorage | Frontend | 4h |
| Basic UI: "Create Wallet" button | Frontend | 4h |

---

#### M0.2: Issuer Scaffold (Days 2–3)
**Deliverable:** POST `/issue` returns signed JWT-VC

| Task | Owner | Effort |
|------|-------|--------|
| Initialize Spring Boot app | Backend | 3h |
| PostgreSQL schema: users, credentials | Backend | 4h |
| Security config (CORS, validation) | Backend | 3h |
| Issuer metadata endpoint | Backend | 2h |
| POST /issue endpoint | Backend | 6h |
| Ed25519 issuer key management | Backend | 4h |
| VC generation (JWT signing) | Backend | 4h |

---

#### M0.3: Wallet ↔ Issuer Integration (Days 3–4)
**Deliverable:** "Passport Credential" card visible in wallet

| Task | Owner | Effort |
|------|-------|--------|
| Fetch issuer metadata | Frontend | 2h |
| Credential request with wallet proof | Frontend | 4h |
| Store VC in wallet | Frontend | 3h |
| Issuer validates wallet proof | Backend | 4h |
| UI: Upload photo → receive VC | Frontend | 4h |

---

#### M0.4: Verifier Scaffold (Days 4–5)
**Deliverable:** Decision token JWT returned from verifier

| Task | Owner | Effort |
|------|-------|--------|
| POST /verify endpoint | Backend | 4h |
| Verifier metadata endpoint | Backend | 2h |
| Verify issuer signature over commitment | Backend | 4h |
| ZKP validation (proof + nonce) | Backend | 6h |
| Decision token minting (JWT) | Backend | 4h |
| Trusted issuer check (hardcoded) | Backend | 2h |

---

#### M0.5: Wallet ↔ Verifier Integration (Days 5–6)
**Deliverable:** Wallet shows "Verified" status

| Task | Owner | Effort |
|------|-------|--------|
| Create ZKP witness inputs from credential (local) | Frontend | 4h |
| Generate ZKP proof (e.g., Groth16 via snarkjs) | Frontend | 6h |
| Fetch verifier nonce/challenge (e.g., `GET /verify/challenge`) and bind proof | Frontend | 2h |
| POST proof + public signals + commitment to verifier | Frontend | 2h |
| Store decision token | Frontend | 2h |
| UI: "Prove Over 18" flow (no DOB reveal) | Frontend | 4h |

---

#### M0.6: Payment Demo (Days 6–7)
**Deliverable:** Working payment with identity verification

| Task | Owner | Effort |
|------|-------|--------|
| Payment intent endpoint | Backend | 4h |
| KYC verification endpoint | Backend | 3h |
| Payment confirm endpoint | Backend | 4h |
| Demo balance ledger | Backend | 4h |
| UI: Payment flow with verification | Frontend | 6h |
| UI: Balance display | Frontend | 3h |

---

#### M0.7: Integration & Demo (Day 7)
**Deliverable:** PoC fully working; demo-ready

| Task | Owner | Effort |
|------|-------|--------|
| End-to-end walkthrough testing | All | 4h |
| Error case testing | All | 3h |
| Demo script preparation | All | 2h |
| README with setup instructions | All | 2h |
| Code cleanup | All | 3h |

---

## Phase 1: MVP (Weeks 3–6)

### Goal
Production-ready system with blockchain anchoring, standards compliance, and proper infrastructure.

### New Features

| Feature | Purpose | Effort |
|---------|---------|--------|
| OpenID4VCI compliance | Standards-based issuance | Medium |
| OpenID4VP compliance | Standards-based verification | Medium |
| Blockchain DID registry | Issuer key anchoring | Medium |
| Trust registry (on-chain) | Immutable issuer whitelist | Medium |
| Liveness detection | Basic anti-spoofing | Low |
| Revocation service | Credential lifecycle | Low |
| Production database schema | Proper data model | Low |
| Audit logging | Compliance trail | Low |
| Error handling | Production-grade responses | Low |
| API documentation | OpenAPI/Swagger | Low |

### Milestones

#### M1.1: Blockchain DID Registry (Days 8–10)
**Deliverable:** Issuer DID anchored on testnet

| Task | Owner | Effort |
|------|-------|--------|
| Deploy DIDRegistry.sol (Hardhat) | Backend | 6h |
| Java web3j integration | Backend | 4h |
| BlockchainService implementation | Backend | 6h |
| Issuer key publishing | Backend | 4h |
| On-chain verification check | Backend | 4h |

---

#### M1.2: Trust Registry On-Chain (Days 10–12)
**Deliverable:** Verifier checks on-chain trust registry

| Task | Owner | Effort |
|------|-------|--------|
| Deploy TrustRegistry.sol | Backend | 4h |
| Admin issuer management | Backend | 4h |
| TrustRegistryService | Backend | 6h |
| Verifier integration | Backend | 4h |
| Registry caching (1h TTL) | Backend | 3h |

---

#### M1.3: OpenID4VCI Implementation (Days 12–15)
**Deliverable:** Standards-compliant issuance flow

| Task | Owner | Effort |
|------|-------|--------|
| Issuer metadata per spec | Backend | 4h |
| OAuth token endpoint | Backend | 6h |
| Credential request format | Backend | 6h |
| Wallet OpenID4VCI client | Frontend | 8h |
| Proof validation | Backend | 4h |

---

#### M1.4: OpenID4VP Implementation (Days 15–18)
**Deliverable:** Standards-compliant verification flow

| Task | Owner | Effort |
|------|-------|--------|
| Verifier metadata per spec | Backend | 4h |
| Presentation definition endpoint | Backend | 6h |
| Response endpoint (callback) | Backend | 6h |
| Wallet OpenID4VP client | Frontend | 8h |
| Claim filtering support | Frontend | 4h |

---

#### M1.5: Liveness Detection (Days 18–20)
**Deliverable:** Issuance requires liveness check

| Task | Owner | Effort |
|------|-------|--------|
| Integrate face-api.js | Frontend | 6h |
| Capture video frames | Frontend | 4h |
| Liveness score calculation | Frontend | 4h |
| Issuer liveness validation | Backend | 4h |
| UI: Camera prompt flow | Frontend | 4h |

---

#### M1.6: Revocation Service (Days 20–21)
**Deliverable:** Credentials can be revoked

| Task | Owner | Effort |
|------|-------|--------|
| credential_status table | Backend | 2h |
| POST /revoke endpoint | Backend | 4h |
| GET /status endpoint | Backend | 3h |
| Verifier revocation check | Backend | 4h |

---

#### M1.7: Production Database Schema (Days 21–22)
**Deliverable:** Full PostgreSQL schema deployed

| Task | Owner | Effort |
|------|-------|--------|
| Final schema design | Backend | 4h |
| Flyway migrations | Backend | 4h |
| Repository layer updates | Backend | 6h |
| Index optimization | Backend | 2h |

---

#### M1.8: Audit Logging (Days 22–23)
**Deliverable:** Event trail for all operations

| Task | Owner | Effort |
|------|-------|--------|
| AuditService implementation | Backend | 6h |
| Event definitions | Backend | 3h |
| Query endpoints | Backend | 4h |
| Integration across services | Backend | 4h |

---

#### M1.9: Error Handling (Days 23–24)
**Deliverable:** Consistent error responses

| Task | Owner | Effort |
|------|-------|--------|
| Input validation layer | Backend | 4h |
| HTTP error code mapping | Backend | 3h |
| Error response format | Backend | 3h |
| Frontend error handling | Frontend | 4h |

---

#### M1.10: API Documentation (Days 24–25)
**Deliverable:** OpenAPI spec + Swagger UI

| Task | Owner | Effort |
|------|-------|--------|
| OpenAPI 3.0 spec | Backend | 6h |
| Swagger UI setup | Backend | 2h |
| README documentation | All | 4h |
| Architecture diagrams | All | 3h |

---

#### M1.11: Integration Tests (Days 25–26)
**Deliverable:** Full test suite passing

| Task | Owner | Effort |
|------|-------|--------|
| Happy path tests | All | 6h |
| Error case tests | All | 4h |
| Postman collection | Backend | 3h |
| E2E UI tests (Playwright) | Frontend | 6h |
| Backend unit tests | Backend | 6h |

---

#### M1.12: Deployment & Docs (Days 27–28)
**Deliverable:** Docker deployment working

| Task | Owner | Effort |
|------|-------|--------|
| Dockerfile for backend | DevOps | 3h |
| docker-compose.yaml | DevOps | 4h |
| Environment configuration | DevOps | 3h |
| Health check endpoints | Backend | 2h |
| Deployment documentation | All | 4h |

---

## Dependency Graph

```
Week 1:
  [Frontend: DID + Wallet] ─┐
  [Backend: Spring + DB] ───┤─→ M0.1, M0.2
  [Backend: Issuer endpoints] ┘

Week 2:
  [Wallet ↔ Issuer] ───┐
  [Verifier endpoints] ─┼─→ M0.3, M0.4, M0.5, M0.6
  [Payment service] ────┘
  [Integration Test] ──────→ M0.7

Week 3:
  [Blockchain DID] ───────┐
  [Trust Registry] ───────┼─→ M1.1, M1.2, M1.3
  [OpenID4VCI] ───────────┘

Week 4:
  [OpenID4VP] ────────────┐
  [Liveness Detection] ───┼─→ M1.4, M1.5, M1.6
  [Revocation Service] ───┘

Week 5-6:
  [Database Schema] ──────┐
  [Audit Logging] ────────┤
  [Error Handling] ───────┼─→ M1.7–M1.12
  [API Docs] ─────────────┤
  [Testing] ──────────────┤
  [Deployment] ───────────┘
```

---

## Resource Allocation

### Frontend Engineer (TypeScript)
| Week | Focus | Hours |
|------|-------|-------|
| 1 | Wallet scaffold, DID manager | 20h |
| 2 | Issuer/Verifier integration, Payment UI | 35h |
| 3 | OpenID4VCI client, Liveness UI | 20h |
| 4 | OpenID4VP client | 20h |
| 5-6 | Bug fixes, testing, polish | 20h |
| **Total** | | **115h** |

### Backend Engineer A (Core Services)
| Week | Focus | Hours |
|------|-------|-------|
| 1 | Spring Boot, Issuer, VC signing | 25h |
| 2 | Verifier, Payment service | 25h |
| 3 | Blockchain integration | 20h |
| 4 | OpenID4VCI/VP, Trust registry | 25h |
| 5-6 | Revocation, Error handling | 20h |
| **Total** | | **115h** |

### Backend Engineer B (Data & Infra)
| Week | Focus | Hours |
|------|-------|-------|
| 1-2 | PostgreSQL, JPA, Migrations | 30h |
| 3-4 | Audit logging, Status service | 25h |
| 5-6 | Docker, API docs, Testing | 30h |
| **Total** | | **85h** |

---

## Risk Mitigation

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Blockchain testnet issues | Medium | Use Polygon (faster); pre-test deploys |
| OpenID spec confusion | Medium | Review spec early; use reference implementations |
| Key management bugs | High | Use battle-tested Veramo library |
| Integration failures | High | Daily integration testing; mock dependencies |
| Scope creep | Medium | Strict scope control; defer to "future" |

---

## Success Criteria

### PoC (End of Week 2)
- [ ] Wallet app runs locally
- [ ] Issuer issues VCs successfully
- [ ] Verifier validates ZK proofs + returns decision token
- [ ] Payment flow works with demo balance
- [ ] End-to-end demo script works
- [ ] Basic README exists

### MVP (End of Week 6)
- [ ] All PoC requirements met
- [ ] OpenID4VCI & OpenID4VP compliant
- [ ] Blockchain DID + trust registry functional
- [ ] Liveness detection working
- [ ] Revocation service operational
- [ ] Production database schema deployed
- [ ] Audit logging complete
- [ ] Consistent error handling
- [ ] OpenAPI spec + Swagger UI available
- [ ] 70%+ test coverage
- [ ] Docker deployment working

---

## Performance Targets

| Metric | PoC | MVP |
|--------|-----|-----|
| VC issuance time | <10s | <5s |
| Verification time (ZKP/VP) | <5s | <2s |
| End-to-end payment | <30s | <20s |
| Uptime | 95% | 99.5% |
| Test coverage | 40% | 70% |
