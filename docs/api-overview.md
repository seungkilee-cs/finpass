# FinPass API Overview

## Introduction

The FinPass API provides a comprehensive set of endpoints for decentralized identity management, credential issuance, verification, payment processing, and audit logging. This document provides a complete overview of all available endpoints with detailed examples.

## Base URLs

- **Production**: `https://api.finpass.io`
- **Staging**: `https://staging-api.finpass.io`  
- **Development**: `http://localhost:8080`

## Authentication

All API endpoints require JWT authentication. Include the JWT token in the Authorization header:

```http
Authorization: Bearer <your-jwt-token>
```

### Getting a JWT Token

```bash
curl -X POST https://auth.finpass.io/oauth/token \
  -H "Content-Type: application/json" \
  -d '{
    "grant_type": "client_credentials",
    "client_id": "your_client_id",
    "client_secret": "your_client_secret"
  }'
```

## Error Handling

All errors follow a standardized format:

```json
{
  "error": "ERROR_CODE",
  "error_description": "Human-readable description",
  "timestamp": "2023-12-27T10:00:00.000Z",
  "correlation_id": "abc123def456",
  "path": "/api/endpoint",
  "details": {
    "field": "value"
  }
}
```

## Issuer API Endpoints

### Issue Credential

Issues a new verifiable credential to a specified holder.

**Endpoint**: `POST /api/issuer/credentials`

**Request Body**:
```json
{
  "holderDid": "did:example:123456789abcdefghi",
  "credentialType": "PASSPORT",
  "credentialData": {
    "passportNumber": "P123456789",
    "fullName": "John Doe",
    "dateOfBirth": "1990-01-01",
    "nationality": "US",
    "issuingCountry": "US",
    "expirationDate": "2030-01-01"
  },
  "livenessProof": {
    "score": 0.95,
    "timestamp": "2023-12-27T10:00:00.000Z",
    "proofData": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

**Response**:
```json
{
  "credentialId": "123e4567-e89b-12d3-a456-426614174000",
  "credentialJwt": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "status": "ISSUED",
  "issuedAt": "2023-12-27T10:00:00.000Z",
  "expiresAt": "2030-12-27T10:00:00.000Z",
  "revocationId": "rev_123456789"
}
```

**Example Request**:
```bash
curl -X POST http://localhost:8080/api/issuer/credentials \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "holderDid": "did:example:123456789abcdefghi",
    "credentialType": "PASSPORT",
    "credentialData": {
      "passportNumber": "P123456789",
      "fullName": "John Doe",
      "dateOfBirth": "1990-01-01",
      "nationality": "US",
      "issuingCountry": "US",
      "expirationDate": "2030-01-01"
    },
    "livenessProof": {
      "score": 0.95,
      "timestamp": "2023-12-27T10:00:00.000Z",
      "proofData": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
    }
  }'
```

### Get Credential Details

Retrieves details of a specific credential by ID.

**Endpoint**: `GET /api/issuer/credentials/{credentialId}`

**Path Parameters**:
- `credentialId` (string, UUID): The credential identifier

**Response**:
```json
{
  "credentialId": "123e4567-e89b-12d3-a456-426614174000",
  "credentialJwt": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "status": "ISSUED",
  "issuedAt": "2023-12-27T10:00:00.000Z",
  "expiresAt": "2030-12-27T10:00:00.000Z",
  "revocationId": "rev_123456789"
}
```

**Example Request**:
```bash
curl -X GET http://localhost:8080/api/issuer/credentials/123e4567-e89b-12d3-a456-426614174000 \
  -H "Authorization: Bearer <token>"
```

### Revoke Credential

Revokes a previously issued credential.

**Endpoint**: `DELETE /api/issuer/credentials/{credentialId}`

**Path Parameters**:
- `credentialId` (string, UUID): The credential identifier to revoke

**Response**:
```json
{
  "message": "Credential revoked successfully",
  "revokedAt": "2023-12-27T10:00:00.000Z"
}
```

**Example Request**:
```bash
curl -X DELETE http://localhost:8080/api/issuer/credentials/123e4567-e89b-12d3-a456-426614174000 \
  -H "Authorization: Bearer <token>"
```

## Verifier API Endpoints

### Verify Credential

Verifies a verifiable credential and returns verification results.

**Endpoint**: `POST /api/verifier/verify`

**Request Body**:
```json
{
  "credentialJwt": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "verifierDid": "did:example:verifier123",
  "verificationType": "IDENTITY_VERIFICATION",
  "verificationData": {
    "minimumAge": 18,
    "requiredFields": ["fullName", "dateOfBirth"]
  }
}
```

**Response**:
```json
{
  "valid": true,
  "verifiedAt": "2023-12-27T10:00:00.000Z",
  "verificationResult": {
    "credentialStatus": "VALID",
    "issuerVerified": true,
    "claimsVerified": {
      "fullName": true,
      "dateOfBirth": true,
      "nationality": true
    },
    "verificationScore": 0.98
  }
}
```

**Example Request**:
```bash
curl -X POST http://localhost:8081/api/verifier/verify \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "credentialJwt": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "verifierDid": "did:example:verifier123",
    "verificationType": "IDENTITY_VERIFICATION",
    "verificationData": {
      "minimumAge": 18,
      "requiredFields": ["fullName", "dateOfBirth"]
    }
  }'
```

### Get Verification Policy

Retrieves a verification policy by ID.

**Endpoint**: `GET /api/verifier/policies/{policyId}`

**Path Parameters**:
- `policyId` (string, UUID): The policy identifier

**Response**:
```json
{
  "policyId": "policy-123456789",
  "name": "Age Verification Policy",
  "description": "Verifies that the holder is above minimum age",
  "verificationType": "AGE_VERIFICATION",
  "requirements": {
    "minimumAge": 18,
    "requiredClaims": ["dateOfBirth"],
    "trustedIssuers": ["did:example:gov1", "did:example:gov2"]
  },
  "createdAt": "2023-12-27T10:00:00.000Z",
  "updatedAt": "2023-12-27T10:00:00.000Z"
}
```

**Example Request**:
```bash
curl -X GET http://localhost:8081/api/verifier/policies/policy-123456789 \
  -H "Authorization: Bearer <token>"
```

### Create Verification Challenge

Creates a new verification challenge for credential presentation.

**Endpoint**: `POST /api/verifier/challenges`

**Request Body**:
```json
{
  "verifierDid": "did:example:verifier123",
  "challengeType": "PRESENTATION",
  "requestedCredentials": ["PASSPORT", "DRIVERS_LICENSE"],
  "expirationTime": "2023-12-27T11:00:00.000Z"
}
```

**Response**:
```json
{
  "challengeId": "challenge-123456789",
  "challengeJwt": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresAt": "2023-12-27T11:00:00.000Z",
  "status": "PENDING"
}
```

**Example Request**:
```bash
curl -X POST http://localhost:8081/api/verifier/challenges \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "verifierDid": "did:example:verifier123",
    "challengeType": "PRESENTATION",
    "requestedCredentials": ["PASSPORT", "DRIVERS_LICENSE"],
    "expirationTime": "2023-12-27T11:00:00.000Z"
  }'
```

## Payment API Endpoints

### Initiate Payment

Creates and initiates a new payment transaction.

**Endpoint**: `POST /api/payments`

**Request Body**:
```json
{
  "payerDid": "did:example:payer123",
  "payeeDid": "did:example:payee456",
  "amount": 100.50,
  "currency": "USD",
  "paymentMethod": "BANK_TRANSFER",
  "description": "Payment for digital identity verification",
  "metadata": {
    "invoiceId": "INV-12345",
    "orderId": "ORDER-67890"
  }
}
```

**Response**:
```json
{
  "paymentId": "pay_123e4567-e89b-12d3-a456-426614174000",
  "status": "PENDING",
  "createdAt": "2023-12-27T10:00:00.000Z",
  "amount": 100.50,
  "currency": "USD",
  "fee": 0.50
}
```

**Example Request**:
```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "payerDid": "did:example:payer123",
    "payeeDid": "did:example:payee456",
    "amount": 100.50,
    "currency": "USD",
    "paymentMethod": "BANK_TRANSFER",
    "description": "Payment for digital identity verification",
    "metadata": {
      "invoiceId": "INV-12345",
      "orderId": "ORDER-67890"
    }
  }'
```

### Get Payment Details

Retrieves details of a specific payment by ID.

**Endpoint**: `GET /api/payments/{paymentId}`

**Path Parameters**:
- `paymentId` (string, UUID): The payment identifier

**Response**:
```json
{
  "paymentId": "pay_123e4567-e89b-12d3-a456-426614174000",
  "transactionId": "0x1234567890abcdef1234567890abcdef12345678",
  "status": "COMPLETED",
  "createdAt": "2023-12-27T10:00:00.000Z",
  "completedAt": "2023-12-27T10:05:00.000Z",
  "amount": 100.50,
  "currency": "USD",
  "fee": 0.50,
  "blockchainConfirmation": 12
}
```

**Example Request**:
```bash
curl -X GET http://localhost:8080/api/payments/pay_123e4567-e89b-12d3-a456-426614174000 \
  -H "Authorization: Bearer <token>"
```

### Confirm Payment

Confirms a payment after external processing.

**Endpoint**: `POST /api/payments/{paymentId}/confirm`

**Path Parameters**:
- `paymentId` (string, UUID): The payment identifier

**Request Body**:
```json
{
  "confirmationCode": "CONF-123456789",
  "transactionHash": "0x1234567890abcdef1234567890abcdef12345678"
}
```

**Response**:
```json
{
  "paymentId": "pay_123e4567-e89b-12d3-a456-426614174000",
  "status": "COMPLETED",
  "confirmedAt": "2023-12-27T10:05:00.000Z",
  "transactionId": "0x1234567890abcdef1234567890abcdef12345678"
}
```

**Example Request**:
```bash
curl -X POST http://localhost:8080/api/payments/pay_123e4567-e89b-12d3-a456-426614174000/confirm \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "confirmationCode": "CONF-123456789",
    "transactionHash": "0x1234567890abcdef1234567890abcdef12345678"
  }'
```

## Audit API Endpoints

### Get Audit Events

Retrieves audit events with filtering and pagination.

**Endpoint**: `GET /api/audit/events`

**Query Parameters**:
- `userIdHash` (string, optional): Filter by user ID hash
- `eventType` (string, optional): Filter by event type
- `from` (string, optional): Filter events from this timestamp (ISO 8601)
- `to` (string, optional): Filter events to this timestamp (ISO 8601)
- `page` (integer, optional): Page number (0-based, default: 0)
- `size` (integer, optional): Page size (1-100, default: 20)

**Response**:
```json
{
  "content": [
    {
      "eventId": "evt_123e4567-e89b-12d3-a456-426614174000",
      "eventType": "CREDENTIAL_ISSUED",
      "userIdHash": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
      "timestamp": "2023-12-27T10:00:00.000Z",
      "outcome": "SUCCESS",
      "severity": "LOW",
      "ipAddress": "192.168.1.100",
      "userAgent": "FinPass-App/1.0.0",
      "sessionId": "sess_123456789",
      "details": {
        "credentialId": "cred_123456789",
        "credentialType": "PASSPORT",
        "issuerDid": "did:example:issuer123"
      }
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8,
  "first": true,
  "last": false
}
```

**Example Request**:
```bash
curl -X GET "http://localhost:8080/api/audit/events?eventType=CREDENTIAL_ISSUED&from=2023-12-01T00:00:00.000Z&page=0&size=20" \
  -H "Authorization: Bearer <token>"
```

### Get Audit Metrics

Retrieves audit metrics and analytics.

**Endpoint**: `GET /api/audit/metrics`

**Query Parameters**:
- `from` (string, required): Metrics from this timestamp (ISO 8601)
- `to` (string, required): Metrics to this timestamp (ISO 8601)

**Response**:
```json
{
  "period": {
    "from": "2023-12-01T00:00:00.000Z",
    "to": "2023-12-31T23:59:59.999Z"
  },
  "totalEvents": 15000,
  "eventsByType": {
    "CREDENTIAL_ISSUED": 5000,
    "PRESENTATION_VERIFIED": 3000,
    "PAYMENT_COMPLETED": 2000,
    "SECURITY_ALERT": 100,
    "USER_LOGIN": 4000,
    "SYSTEM_ERROR": 50
  },
  "errorRate": 0.02,
  "criticalEvents": 5,
  "topUsers": [
    {
      "userIdHash": "user123hash",
      "eventCount": 150
    },
    {
      "userIdHash": "user456hash",
      "eventCount": 120
    }
  ],
  "topIssuers": [
    {
      "issuerDid": "did:example:gov1",
      "credentialCount": 2000
    },
    {
      "issuerDid": "did:example:bank1",
      "credentialCount": 1500
    }
  ]
}
```

**Example Request**:
```bash
curl -X GET "http://localhost:8080/api/audit/metrics?from=2023-12-01T00:00:00.000Z&to=2023-12-31T23:59:59.999Z" \
  -H "Authorization: Bearer <token>"
```

### Search Audit Logs

Searches audit logs with advanced filters.

**Endpoint**: `POST /api/audit/search`

**Request Body**:
```json
{
  "filters": {
    "eventTypes": ["CREDENTIAL_ISSUED", "PRESENTATION_VERIFIED"],
    "outcomes": ["SUCCESS"],
    "severity": ["LOW", "MEDIUM"],
    "ipAddresses": ["192.168.1.100", "192.168.1.101"],
    "userAgents": ["FinPass-App/1.0.0"]
  },
  "timeRange": {
    "from": "2023-12-01T00:00:00.000Z",
    "to": "2023-12-31T23:59:59.999Z"
  },
  "pagination": {
    "page": 0,
    "size": 50
  },
  "sortBy": "timestamp",
  "sortOrder": "desc"
}
```

**Response**:
```json
{
  "content": [
    {
      "eventId": "evt_123e4567-e89b-12d3-a456-426614174000",
      "eventType": "CREDENTIAL_ISSUED",
      "userIdHash": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
      "timestamp": "2023-12-27T10:00:00.000Z",
      "outcome": "SUCCESS",
      "severity": "LOW",
      "ipAddress": "192.168.1.100",
      "userAgent": "FinPass-App/1.0.0",
      "sessionId": "sess_123456789",
      "details": {
        "credentialId": "cred_123456789",
        "credentialType": "PASSPORT",
        "issuerDid": "did:example:issuer123"
      }
    }
  ],
  "page": 0,
  "size": 50,
  "totalElements": 75,
  "totalPages": 2,
  "first": true,
  "last": false
}
```

**Example Request**:
```bash
curl -X POST http://localhost:8080/api/audit/search \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "filters": {
      "eventTypes": ["CREDENTIAL_ISSUED", "PRESENTATION_VERIFIED"],
      "outcomes": ["SUCCESS"],
      "severity": ["LOW", "MEDIUM"]
    },
    "timeRange": {
      "from": "2023-12-01T00:00:00.000Z",
      "to": "2023-12-31T23:59:59.999Z"
    },
    "pagination": {
      "page": 0,
      "size": 50
    },
    "sortBy": "timestamp",
    "sortOrder": "desc"
  }'
```

## SDK Examples

### JavaScript/TypeScript SDK

```typescript
import { FinPassClient } from '@finpass/sdk';

const client = new FinPassClient({
  baseURL: 'https://api.finpass.io',
  apiKey: 'your-api-key'
});

// Issue a credential
const credential = await client.credentials.issue({
  holderDid: 'did:example:123456789',
  credentialType: 'PASSPORT',
  credentialData: {
    passportNumber: 'P123456789',
    fullName: 'John Doe',
    dateOfBirth: '1990-01-01',
    nationality: 'US'
  }
});

// Verify a credential
const verification = await client.verifier.verify({
  credentialJwt: credential.credentialJwt,
  verifierDid: 'did:example:verifier123',
  verificationType: 'IDENTITY_VERIFICATION'
});

// Process a payment
const payment = await client.payments.initiate({
  payerDid: 'did:example:payer123',
  payeeDid: 'did:example:payee456',
  amount: 100.50,
  currency: 'USD',
  paymentMethod: 'BANK_TRANSFER'
});
```

### Python SDK

```python
from finpass import FinPassClient

client = FinPassClient(
    base_url='https://api.finpass.io',
    api_key='your-api-key'
)

# Issue a credential
credential = client.credentials.issue({
    'holderDid': 'did:example:123456789',
    'credentialType': 'PASSPORT',
    'credentialData': {
        'passportNumber': 'P123456789',
        'fullName': 'John Doe',
        'dateOfBirth': '1990-01-01',
        'nationality': 'US'
    }
})

# Verify a credential
verification = client.verifier.verify({
    'credentialJwt': credential['credentialJwt'],
    'verifierDid': 'did:example:verifier123',
    'verificationType': 'IDENTITY_VERIFICATION'
})

# Process a payment
payment = client.payments.initiate({
    'payerDid': 'did:example:payer123',
    'payeeDid': 'did:example:payee456',
    'amount': 100.50,
    'currency': 'USD',
    'paymentMethod': 'BANK_TRANSFER'
})
```

### Java SDK

```java
import com.finpass.client.FinPassClient;
import com.finpass.model.*;

FinPassClient client = new FinPassClient.Builder()
    .baseUrl("https://api.finpass.io")
    .apiKey("your-api-key")
    .build();

// Issue a credential
CredentialRequest request = new CredentialRequest.Builder()
    .holderDid("did:example:123456789")
    .credentialType(CredentialType.PASSPORT)
    .credentialData(Map.of(
        "passportNumber", "P123456789",
        "fullName", "John Doe",
        "dateOfBirth", "1990-01-01",
        "nationality", "US"
    ))
    .build();

CredentialResponse credential = client.credentials().issue(request);

// Verify a credential
VerificationRequest verificationRequest = new VerificationRequest.Builder()
    .credentialJwt(credential.getCredentialJwt())
    .verifierDid("did:example:verifier123")
    .verificationType(VerificationType.IDENTITY_VERIFICATION)
    .build();

VerificationResponse verification = client.verifier().verify(verificationRequest);

// Process a payment
PaymentRequest paymentRequest = new PaymentRequest.Builder()
    .payerDid("did:example:payer123")
    .payeeDid("did:example:payee456")
    .amount(new BigDecimal("100.50"))
    .currency("USD")
    .paymentMethod(PaymentMethod.BANK_TRANSFER)
    .build();

PaymentResponse payment = client.payments().initiate(paymentRequest);
```

## Rate Limits

The API implements rate limiting to ensure fair usage:

| Endpoint | Rate Limit | Time Window |
|----------|------------|-------------|
| Credential Issuance | 100 requests | 1 hour |
| Credential Verification | 1000 requests | 1 hour |
| Payment Processing | 500 requests | 1 hour |
| Audit Queries | 200 requests | 1 hour |

Rate limit headers are included in responses:
```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1703673600
```

## Webhooks

Configure webhooks to receive real-time notifications:

### Supported Events

- `credential.issued` - New credential issued
- `credential.revoked` - Credential revoked
- `payment.completed` - Payment completed
- `payment.failed` - Payment failed
- `verification.completed` - Verification completed

### Webhook Configuration

```bash
curl -X POST https://api.finpass.io/webhooks \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "url": "https://your-app.com/webhooks",
    "events": ["credential.issued", "payment.completed"],
    "secret": "your-webhook-secret"
  }'
```

### Webhook Payload Example

```json
{
  "eventId": "evt_123456789",
  "eventType": "credential.issued",
  "timestamp": "2023-12-27T10:00:00.000Z",
  "data": {
    "credentialId": "123e4567-e89b-12d3-a456-426614174000",
    "holderDid": "did:example:123456789",
    "credentialType": "PASSPORT"
  },
  "signature": "sha256=5d41402abc4b2a76b9719d911017c592"
}
```

## Testing

### Test Environment

Use the staging environment for testing:
- **Base URL**: `https://staging-api.finpass.io`
- **Test Credentials**: Available in developer portal

### Test Data

The API provides test endpoints for creating test data:

```bash
# Create test user
curl -X POST https://staging-api.finpass.io/test/users \
  -H "Authorization: Bearer <test-token>" \
  -d '{"email": "test@example.com"}'

# Create test credential
curl -X POST https://staging-api.finpass.io/test/credentials \
  -H "Authorization: Bearer <test-token>" \
  -d '{"type": "PASSPORT", "holderDid": "did:example:test"}'
```

## Support

- **Documentation**: https://docs.finpass.io
- **API Reference**: https://api.finpass.io/docs
- **Support Email**: api-support@finpass.io
- **Status Page**: https://status.finpass.io
- **GitHub Issues**: https://github.com/finpass/finpass/issues
