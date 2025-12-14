# Backend (Issuer) + Postgres: Runbook

## 0) Prereqs

- Docker Desktop running
- Java 17 installed
- Repo root: `finpass/`

## 1) Start Postgres

From repo root:

```bash
docker compose up -d
docker ps --format "table {{.Names}}\t{{.Ports}}"
```

Expected:

- `0.0.0.0:15432->5432/tcp`

(Optional DB check)

```bash
psql "postgresql://finpass:finpass@127.0.0.1:15432/finpass" -c "\dt"
```

## 2) Start issuer backend

From `finpass/issuer`:

```bash
./mvnw -DskipTests spring-boot:run
```

Expected logs include:

- Flyway validated/migrated
- `Tomcat started on port 8080`

## 3) Verify endpoints

```bash
curl "http://localhost:8080/.well-known/openid-credential-issuer"
```

### Issue a credential

NOTE: If you see `400 Bad Request`, your JSON payload probably doesn’t match what `/issue` expects. `/issue` expects:

- `holderDid` (required)
- `passportData` (required object/map)

Correct request:

```bash
curl -X POST "http://localhost:8080/issue" \
  -H "Content-Type: application/json" \
  -d '{
    "holderDid": "did:example:alice",
    "passportData": {
      "name": "Alice",
      "dob": "1990-01-01",
      "country": "US"
    }
  }'
```

### Check status

Replace `<credId>` with the UUID returned by `/issue`:

```bash
curl "http://localhost:8080/status/<credId>"
```

## Entry points (code paths)

### API endpoints

- `finpass/issuer/src/main/java/com/finpass/issuer/controller/IssuerController.java`
  - `GET /.well-known/openid-credential-issuer`
  - `POST /issue`
  - `GET /status/{credId}`

### Request/response DTOs

- `finpass/issuer/src/main/java/com/finpass/issuer/dto/IssueRequest.java`
  - Request shape for `/issue` (`holderDid`, `passportData`)
- `finpass/issuer/src/main/java/com/finpass/issuer/dto/IssueResponse.java`
  - Response fields (`credId`, `credentialJwt`, `commitmentHash`, `commitmentJwt`)
- `finpass/issuer/src/main/java/com/finpass/issuer/dto/StatusResponse.java`

### Core issuance logic

- `finpass/issuer/src/main/java/com/finpass/issuer/service/IssuerService.java`
  - `issuePassportCredential(holderDid, passportData)`
  - `signVcJwt(...)` (creates + signs `credentialJwt`)
  - `signCommitmentJwt(...)` (creates + signs `commitmentJwt`)

### Issuer signing key

- `finpass/issuer/src/main/java/com/finpass/issuer/service/IssuerKeyProvider.java`
  - Loads `issuer.privateJwk` (env: `ISSUER_PRIVATE_JWK`) or generates an Ed25519 key
  - Provides `getAlgorithm()`, `getKeyId()`, and `signer()` used for JWT signing

### Database schema

- `finpass/issuer/src/main/resources/db/migration/V1__init.sql`

## JWT notes: decode + signature verification

### Decode JWT (header/payload) locally

The issuer returns 2 JWTs:

- `credentialJwt` (JWT-VC)
- `commitmentJwt` (issuer-signed commitment)

You can decode without verifying signature using Node:

```bash
node -e 'const t=process.argv[1]; const [h,p]=t.split("."); const d=s=>JSON.parse(Buffer.from(s.replace(/-/g,"+").replace(/_/g,"/"),"base64").toString()); console.log("header:", d(h)); console.log("payload:", d(p));' "<JWT_HERE>"
```

### Verify issuer signature (EdDSA)

To *verify signature*, you need the issuer **public JWK**.

IMPORTANT: By default the issuer generates a random Ed25519 key at startup if `ISSUER_PRIVATE_JWK` is empty. That means the key changes across restarts.

Recommended: run issuer with a fixed key:

```bash
export ISSUER_PRIVATE_JWK='<PASTE_PRIVATE_OKP_JWK_JSON_HERE>'
./mvnw -DskipTests spring-boot:run
```

Then derive the **public JWK** by removing the private `d` value:

```bash
export ISSUER_PUBLIC_JWK='{"kty":"OKP","crv":"Ed25519","x":"<same-x-as-private>"}'
```

Verify with Node (from `finpass/wallet` since it already has `jose` installed):

```bash
node -e '
  const { importJWK, jwtVerify } = require("jose");
  (async () => {
    const publicJwk = JSON.parse(process.env.ISSUER_PUBLIC_JWK);
    const jwt = process.env.JWT;
    const key = await importJWK(publicJwk, "EdDSA");
    const res = await jwtVerify(jwt, key);
    console.log("verified payload:", res.payload);
    console.log("protected header:", res.protectedHeader);
  })().catch(e => { console.error(e); process.exit(1); });
' \
JWT='<JWT_HERE>' ISSUER_PUBLIC_JWK="$ISSUER_PUBLIC_JWK"
```

Notes:

- The JWT header contains `kid` (key id). In this PoC, it’s mainly for debugging/identification.
- Signature verification succeeds only if the public JWK matches the key used by the running issuer.

## 4) Stop everything

- Stop issuer: `Ctrl+C` in the issuer terminal
- Stop Postgres:

```bash
docker compose down
```

(Full reset DB data if needed)

```bash
docker compose down -v
```


## Output
```bash
❯ curl -X POST "http://localhost:8080/issue" \
  -H "Content-Type: application/json" \
  -d '{
    "holderDid": "did:example:alice",
    "passportData": {
      "name": "Alice",
      "dob": "1990-01-01",
      "country": "US"
    }
  }'
{"credId":"c68c2dbb-b68b-4e6c-ac40-c8f3b4a2aab3","issuerDid":"did:example:issuer","status":"valid","credentialJwt":"eyJraWQiOiIzZDE0ODZkYS1hODZiLTRhNWItOThhNy05NjhhMDI3ZmYzM2UiLCJ0eXAiOiJKV1QiLCJhbGciOiJFZERTQSJ9.eyJpc3MiOiJkaWQ6ZXhhbXBsZTppc3N1ZXIiLCJzdWIiOiJkaWQ6ZXhhbXBsZTphbGljZSIsImlhdCI6MTc2NTcwMDI5NSwidmMiOnsiY3JlZGVudGlhbFN1YmplY3QiOnsibmFtZSI6IkFsaWNlIiwiZG9iIjoiMTk5MC0wMS0wMSIsImNvdW50cnkiOiJVUyJ9LCJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiUGFzc3BvcnRDcmVkZW50aWFsIl0sIkBjb250ZXh0IjpbImh0dHBzOi8vd3d3LnczLm9yZy8yMDE4L2NyZWRlbnRpYWxzL3YxIl19LCJqdGkiOiJkMGU2YWY5Ni1lN2YyLTQ0OGYtOGMyNy1hOWU1MTYzODgzNTkifQ._4w8NwQbrDebkUKzeGV7JyYRlLlQpZYnInQlTIop3fVljxkF_MIDzBdDLKK3eibTjdA1o_2tHQ9_E0yn7BTTAQ","commitmentHash":"537ab0594632117f2478e006c691745dfa1ebbf6a1d86b8ce7bb7ded1697588d","commitmentJwt":"eyJraWQiOiIzZDE0ODZkYS1hODZiLTRhNWItOThhNy05NjhhMDI3ZmYzM2UiLCJ0eXAiOiJKV1QiLCJhbGciOiJFZERTQSJ9.eyJpc3MiOiJkaWQ6ZXhhbXBsZTppc3N1ZXIiLCJzdWIiOiJkaWQ6ZXhhbXBsZTphbGljZSIsImNvbW1pdG1lbnRfaGFzaCI6IjUzN2FiMDU5NDYzMjExN2YyNDc4ZTAwNmM2OTE3NDVkZmExZWJiZjZhMWQ4NmI4Y2U3YmI3ZGVkMTY5NzU4OGQiLCJpYXQiOjE3NjU3MDAyOTUsImp0aSI6IjcwY2FlMTczLWIwNWMtNGQyOS1hYTFkLTIxZGRiMzIxODI4OCJ9.oNH58AwgivzhA-cnuPfU_YmZf3OI1GUhPMfQa8lAOCRakGHrDZsurTsFYGUBgGcaid6mkQU6idjTFUCoGlrjDA"}%   
```