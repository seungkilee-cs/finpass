<img src="https://r2cdn.perplexity.ai/pplx-full-logo-primary-dark%402x.png" style="height:64px;margin-right:32px"/>

# If I were doing an elevator pitch about this PoC, how would I detailedly explain this?

This PoC is a “digital passport for finance”: a user can prove who they are once, then reuse that proof to unlock a payment flow without repeatedly sharing personal data. It combines an SSI-style identity wallet with a KYC-gated mini payment ledger.

## One-minute pitch script

“This PoC turns identity into a reusable, privacy-first credential instead of a repeated upload process.”
“A user gets a digitally signed ‘passport credential’ in their wallet, then proves eligibility to a merchant using a short-lived verification token—so payments can be allowed only after verification, without exposing raw passport data.”

## The problem

Cross-border users (tourists, expats, remote workers) get blocked by repeated KYC steps, unfamiliar local systems, and merchants that can’t easily trust foreign IDs.
Merchants also don’t want to store sensitive passport data, yet they still need a reliable ‘is this person verified?’ signal.

## The solution

The PoC issues a Verifiable Credential (VC) to the user’s wallet and uses standard-ish flows that can evolve into OpenID4VCI (issuance) and OpenID4VP (presentation/verification), so it’s not a one-off proprietary design.[^1][^2]
The verifier returns a short-lived “decision token” (think: “KYC passed, valid for 5 minutes”), and the payment service accepts only that token—not the passport credential itself.
A minimal blockchain piece can anchor issuer trust/public keys (not PII), aligning with common “wallet + trust infrastructure” reference architectures.[^3]

## What you demo in PoC

-User creates a wallet: the app generates a DID + keys locally.
-User receives a “digital passport” VC: the issuer service signs a synthetic passport credential (fake data, no real PII).
-User proves eligibility: the wallet creates a presentation and the verifier checks signatures + policy, then returns a short-lived decision token.[^2]
-User pays: the payment service processes a simple ledger transfer only if a valid decision token is attached (KYC-gated payment).

## Why this is compelling

It separates responsibilities cleanly: identity proof stays in the wallet, verification produces an auditable yes/no token, and payments never need to touch passport data.
It’s easy to extend later (real passport proofing, selective disclosure/ZKP, real PSP integrations) because the core “issue → present → verify → gate payment” loop stays the same.[^1][^2]

If a 30-second version is needed (investor-style) or a 2-minute version is needed (technical stakeholder), say which audience to target.

<div align="center">⁂</div>

[^1]: https://docs.walt.id/concepts/data-exchange-protocols/openid4vci

[^2]: https://docs.walt.id/enterprise-stack/services/verifier-service/credential-verification/vc-oid4vc

[^3]: https://github.com/openwallet-foundation/architecture-sig/blob/main/docs/papers/architecture-whitepaper.md

