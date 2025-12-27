-- Database setup script for integration tests
-- This script creates test data and prepares the database for testing

-- Clean up existing test data
DELETE FROM audit_events WHERE user_id_hash LIKE 'test_%';
DELETE FROM payments WHERE payer_did LIKE 'did:example:test%' OR payee_did LIKE 'did:example:test%';
DELETE FROM credentials WHERE holder_did LIKE 'did:example:test%';
DELETE FROM users WHERE did LIKE 'did:example:test%';

-- Create test users
INSERT INTO users (id, did, email, name, phone_number, date_of_birth, nationality, address, created_at, updated_at) VALUES
('user_test_001', 'did:example:testuser001', 'testuser001@test.com', 'Test User 001', '+1-555-0000001', '1990-01-01', 'US', '123 Test St, Test City, TC 12345', NOW(), NOW()),
('user_test_002', 'did:example:testuser002', 'testuser002@test.com', 'Test User 002', '+1-555-0000002', '1985-05-15', 'US', '456 Test Ave, Test Town, TC 67890', NOW(), NOW()),
('user_test_003', 'did:example:testuser003', 'testuser003@test.com', 'Test User 003', '+1-555-0000003', '1992-12-25', 'US', '789 Test Blvd, Test Village, TC 13579', NOW(), NOW()),
('user_test_004', 'did:example:testpayee001', 'testpayee001@test.com', 'Test Payee 001', '+1-555-0000101', '1980-03-10', 'US', '321 Payee St, Payee City, PC 24680', NOW(), NOW()),
('user_test_005', 'did:example:testpayee002', 'testpayee002@test.com', 'Test Payee 002', '+1-555-0000102', '1988-07-20', 'US', '654 Payee Ave, Payee Town, PC 97531', NOW(), NOW());

-- Create test credentials
INSERT INTO credentials (id, holder_did, issuer_did, credential_type, credential_data, credential_jwt, status, issued_at, expires_at, revocation_id, created_at, updated_at) VALUES
('cred_test_001', 'did:example:testuser001', 'did:example:gov001', 'PASSPORT', 
 '{"passportNumber":"P123456789","fullName":"Test User 001","dateOfBirth":"1990-01-01","nationality":"US","issuingCountry":"US","expirationDate":"2030-01-01"}',
 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjcmVkZW50aWFsSWQiOiJjcmVkX3Rlc3RfMDAxIiwiaG9sZGVyRpZCI6ImRpZDpleGFtcGxlOnRlc3R1c2VyMDAxIiwidHlwZSI6IlBBU1BPUlQiLCJpYXQiOjE2MzUyODgwMDAsImV4cCI6MTk1MDY0ODAwMH0.test_signature_001',
 'ISSUED', NOW(), '2030-12-31 23:59:59', 'rev_test_001', NOW(), NOW()),

('cred_test_002', 'did:example:testuser001', 'did:example:gov002', 'DRIVERS_LICENSE',
 '{"licenseNumber":"DL123456789","fullName":"Test User 001","dateOfBirth":"1990-01-01","nationality":"US","issuingState":"CA","expirationDate":"2028-01-01"}',
 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjcmVkZW50aWFsSWQiOiJjcmVkX3Rlc3RfMDAyIiwiaG9sZGVyRpZCI6ImRpZDpleGFtcGxlOnRlc3R1c2VyMDAxIiwidHlwZSI6IkRSSVZFUlNfTElDRU5TRSIsImlhdCI6MTYzNTI4ODAwMCwiZXhwIjoxOTMwNjQ4MDAwfQ.test_signature_002',
 'ISSUED', NOW(), '2028-12-31 23:59:59', 'rev_test_002', NOW(), NOW()),

('cred_test_003', 'did:example:testuser002', 'did:example:gov001', 'PASSPORT',
 '{"passportNumber":"P987654321","fullName":"Test User 002","dateOfBirth":"1985-05-15","nationality":"US","issuingCountry":"US","expirationDate":"2030-05-15"}',
 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjcmVkZW50aWFsSWQiOiJjcmVkX3Rlc3RfMDAzIiwiaG9sZGVyRpZCI6ImRpZDpleGFtcGxlOnRlc3R1c2VyMDAyIiwidHlwZSI6IlBBU1BPUlQiLCJpYXQiOjE2MzUyODgwMDAsImV4cCI6MTk1MDY0ODAwMH0.test_signature_003',
 'ISSUED', NOW(), '2030-12-31 23:59:59', 'rev_test_003', NOW(), NOW()),

('cred_test_004', 'did:example:testuser003', 'did:example:gov003', 'NATIONAL_ID',
 '{"nationalIdNumber":"NID123456789","fullName":"Test User 003","dateOfBirth":"1992-12-25","nationality":"US","issuingAuthority":"Social Security Administration","expirationDate":"2035-12-25"}',
 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjcmVkZW50aWFsSWQiOiJjcmVkX3Rlc3RfMDA0IiwiaG9sZGVyRpZCI6ImRpZDpleGFtcGxlOnRlc3R1c2VyMDAzIiwidHlwZSI6Ik5BVElPTkFMX0lEIiwiaWF0IjoxNjM1Mjg4MDAwLCJleHAiOjIwMDY0ODgwMDB9.test_signature_004',
 'ISSUED', NOW(), '2035-12-31 23:59:59', 'rev_test_004', NOW(), NOW()),

('cred_test_005', 'did:example:testuser001', 'did:example:untrusted', 'PASSPORT',
 '{"passportNumber":"P555555555","fullName":"Test User 001","dateOfBirth":"1990-01-01","nationality":"US","issuingCountry":"US","expirationDate":"2030-01-01"}',
 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjcmVkZW50aWFsSWQiOiJjcmVkX3Rlc3RfMDA1IiwiaG9sZGVyRpZCI6ImRpZDpleGFtcGxlOnRlc3R1c2VyMDAxIiwidHlwZSI6IlBBU1BPUlQiLCJpYXQiOjE2MzUyODgwMDAsImV4cCI6MTk1MDY0ODAwMH0.test_signature_005',
 'ISSUED', NOW(), '2030-12-31 23:59:59', 'rev_test_005', NOW(), NOW()),

('cred_test_006', 'did:example:testuser002', 'did:example:gov001', 'PASSPORT',
 '{"passportNumber":"P111111111","fullName":"Test User 002","dateOfBirth":"1985-05-15","nationality":"US","issuingCountry":"US","expirationDate":"2020-05-15"}',
 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjcmVkZW50aWFsSWQiOiJjcmVkX3Rlc3RfMDA2IiwiaG9sZGVyRpZCI6ImRpZDpleGFtcGxlOnRlc3R1c2VyMDAyIiwidHlwZSI6IlBBU1BPUlQiLCJpYXQiOjE2MzUyODgwMDAsImV4cCI6MTk1MDY0ODAwMH0.test_signature_006',
 'REVOKED', NOW(), '2020-12-31 23:59:59', 'rev_test_006', NOW(), NOW());

-- Create test payments
INSERT INTO payments (id, payer_did, payee_did, amount, currency, payment_method, status, description, transaction_id, created_at, updated_at, confirmed_at, blockchain_confirmation) VALUES
('pay_test_001', 'did:example:testuser001', 'did:example:testpayee001', 100.50, 'USD', 'BANK_TRANSFER', 'COMPLETED', 'Test payment 001', '0x1234567890abcdef1234567890abcdef12345678', NOW() - INTERVAL '1 hour', NOW() - INTERVAL '55 minutes', NOW() - INTERVAL '50 minutes', 12),
('pay_test_002', 'did:example:testuser002', 'did:example:testpayee001', 250.75, 'USD', 'DIGITAL_WALLET', 'COMPLETED', 'Test payment 002', '0xabcdef1234567890abcdef1234567890abcdef12', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour 55 minutes', NOW() - INTERVAL '1 hour 50 minutes', 8),
('pay_test_003', 'did:example:testuser003', 'did:example:testpayee002', 75.25, 'USD', 'CREDIT_CARD', 'PENDING', 'Test payment 003', NULL, NOW() - INTERVAL '30 minutes', NOW() - INTERVAL '30 minutes', NULL, 0),
('pay_test_004', 'did:example:testuser001', 'did:example:testpayee002', 500.00, 'USD', 'BANK_TRANSFER', 'FAILED', 'Test payment 004 - insufficient balance', NULL, NOW() - INTERVAL '3 hours', NOW() - INTERVAL '3 hours', NULL, 0);

-- Create test audit events
INSERT INTO audit_events (id, event_type, user_id_hash, timestamp, outcome, severity, ip_address, user_agent, session_id, correlation_id, path, details, created_at) VALUES
('evt_test_001', 'USER_LOGIN', 'hash_test_user_001', NOW() - INTERVAL '4 hours', 'SUCCESS', 'LOW', '192.168.1.100', 'FinPass-App/1.0.0', 'sess_test_001', 'corr_test_001', '/api/auth/login', '{"loginMethod":"JWT","deviceId":"device_001"}', NOW() - INTERVAL '4 hours'),
('evt_test_002', 'CREDENTIAL_ISSUED', 'hash_test_user_001', NOW() - INTERVAL '3 hours 30 minutes', 'SUCCESS', 'LOW', '192.168.1.100', 'FinPass-App/1.0.0', 'sess_test_001', 'corr_test_002', '/api/issuer/credentials', '{"credentialId":"cred_test_001","credentialType":"PASSPORT","issuerDid":"did:example:gov001"}', NOW() - INTERVAL '3 hours 30 minutes'),
('evt_test_003', 'CREDENTIAL_ISSUED', 'hash_test_user_001', NOW() - INTERVAL '3 hours', 'SUCCESS', 'LOW', '192.168.1.100', 'FinPass-App/1.0.0', 'sess_test_001', 'corr_test_003', '/api/issuer/credentials', '{"credentialId":"cred_test_002","credentialType":"DRIVERS_LICENSE","issuerDid":"did:example:gov002"}', NOW() - INTERVAL '3 hours'),
('evt_test_004', 'PRESENTATION_VERIFIED', 'hash_test_user_001', NOW() - INTERVAL '2 hours 30 minutes', 'SUCCESS', 'LOW', '192.168.1.101', 'FinPass-App/1.0.0', 'sess_test_001', 'corr_test_004', '/api/verifier/verify', '{"credentialId":"cred_test_001","verifierDid":"did:example:verifier001","verificationScore":0.95}', NOW() - INTERVAL '2 hours 30 minutes'),
('evt_test_005', 'PAYMENT_COMPLETED', 'hash_test_user_001', NOW() - INTERVAL '2 hours', 'SUCCESS', 'MEDIUM', '192.168.1.101', 'FinPass-App/1.0.0', 'sess_test_001', 'corr_test_005', '/api/payments/pay_test_001/confirm', '{"paymentId":"pay_test_001","amount":100.50,"currency":"USD","transactionId":"0x1234567890abcdef1234567890abcdef12345678"}', NOW() - INTERVAL '2 hours'),
('evt_test_006', 'CREDENTIAL_ISSUED', 'hash_test_user_002', NOW() - INTERVAL '1 hour 45 minutes', 'SUCCESS', 'LOW', '192.168.1.102', 'FinPass-App/1.0.0', 'sess_test_002', 'corr_test_006', '/api/issuer/credentials', '{"credentialId":"cred_test_003","credentialType":"PASSPORT","issuerDid":"did:example:gov001"}', NOW() - INTERVAL '1 hour 45 minutes'),
('evt_test_007', 'PRESENTATION_VERIFIED', 'hash_test_user_002', NOW() - INTERVAL '1 hour 30 minutes', 'SUCCESS', 'LOW', '192.168.1.102', 'FinPass-App/1.0.0', 'sess_test_002', 'corr_test_007', '/api/verifier/verify', '{"credentialId":"cred_test_003","verifierDid":"did:example:verifier002","verificationScore":0.92}', NOW() - INTERVAL '1 hour 30 minutes'),
('evt_test_008', 'PAYMENT_COMPLETED', 'hash_test_user_002', NOW() - INTERVAL '1 hour 15 minutes', 'SUCCESS', 'MEDIUM', '192.168.1.102', 'FinPass-App/1.0.0', 'sess_test_002', 'corr_test_008', '/api/payments/pay_test_002/confirm', '{"paymentId":"pay_test_002","amount":250.75,"currency":"USD","transactionId":"0xabcdef1234567890abcdef1234567890abcdef12"}', NOW() - INTERVAL '1 hour 15 minutes'),
('evt_test_009', 'CREDENTIAL_ISSUED', 'hash_test_user_003', NOW() - INTERVAL '45 minutes', 'SUCCESS', 'LOW', '192.168.1.103', 'FinPass-App/1.0.0', 'sess_test_003', 'corr_test_009', '/api/issuer/credentials', '{"credentialId":"cred_test_004","credentialType":"NATIONAL_ID","issuerDid":"did:example:gov003"}', NOW() - INTERVAL '45 minutes'),
('evt_test_010', 'PAYMENT_FAILED', 'hash_test_user_001', NOW() - INTERVAL '3 hours', 'FAILURE', 'HIGH', '192.168.1.100', 'FinPass-App/1.0.0', 'sess_test_001', 'corr_test_010', '/api/payments', '{"paymentId":"pay_test_004","amount":500.00,"currency":"USD","error":"INSUFFICIENT_BALANCE"}', NOW() - INTERVAL '3 hours'),
('evt_test_011', 'CREDENTIAL_REVOKED', 'hash_test_user_002', NOW() - INTERVAL '1 hour', 'SUCCESS', 'MEDIUM', '192.168.1.102', 'FinPass-App/1.0.0', 'sess_test_002', 'corr_test_011', '/api/issuer/credentials/cred_test_006', '{"credentialId":"cred_test_006","revocationId":"rev_test_006","reason":"EXPIRED"}', NOW() - INTERVAL '1 hour'),
('evt_test_012', 'PRESENTATION_FAILED', 'hash_test_user_002', NOW() - INTERVAL '50 minutes', 'FAILURE', 'HIGH', '192.168.1.102', 'FinPass-App/1.0.0', 'sess_test_002', 'corr_test_012', '/api/verifier/verify', '{"credentialId":"cred_test_006","verifierDid":"did:example:verifier003","error":"CREDENTIAL_REVOKED"}', NOW() - INTERVAL '50 minutes');

-- Create verification policies
INSERT INTO verification_policies (id, name, description, verification_type, requirements, created_at, updated_at) VALUES
('policy_test_001', 'Age Verification Policy', 'Verifies that the holder is above minimum age', 'AGE_VERIFICATION',
 '{"minimumAge":18,"requiredClaims":["dateOfBirth"],"trustedIssuers":["did:example:gov001","did:example:gov002","did:example:gov003"]}',
 NOW(), NOW()),

('policy_test_002', 'Identity Verification Policy', 'Comprehensive identity verification', 'IDENTITY_VERIFICATION',
 '{"minimumAge":18,"requiredClaims":["fullName","dateOfBirth","nationality"],"trustedIssuers":["did:example:gov001","did:example:gov002","did:example:gov003"]}',
 NOW(), NOW()),

('policy_test_003', 'Address Verification Policy', 'Verifies residential address', 'ADDRESS_VERIFICATION',
 '{"requiredClaims":["address","dateOfBirth"],"trustedIssuers":["did:example:gov001","did:example:gov002"]}',
 NOW(), NOW());

-- Create verification challenges
INSERT INTO verification_challenges (id, verifier_did, challenge_type, requested_credentials, challenge_jwt, expires_at, status, created_at, updated_at) VALUES
('challenge_test_001', 'did:example:verifier001', 'PRESENTATION', '["PASSPORT","DRIVERS_LICENSE"]',
 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjaGFsbGVuZ2VJZCI6ImNoYWxsZW5nZV90ZXN0XzAwMSIsInZlcmlmaWVyRpZCI6ImRpZDpleGFtcGxlOnZlcmlmaWVyMDAxIiwiaWF0IjoxNjM1Mjg4MDAwLCJleHAiOjE2MzUyOTE2MDB9.challenge_signature_001',
 NOW() + INTERVAL '1 hour', 'PENDING', NOW(), NOW()),

('challenge_test_002', 'did:example:verifier002', 'PRESENTATION', '["PASSPORT"]',
 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjaGFsbGVuZ2VJZCI6ImNoYWxsZW5nZV90ZXN0XzAwMiIsInZlcmlmaWVyRpZCI6ImRpZDpleGFtcGxlOnZlcmlmaWVyMDAyIiwiaWF0IjoxNjM1Mjg4MDAwLCJleHAiOjE2MzUyOTE2MDB9.challenge_signature_002',
 NOW() + INTERVAL '2 hours', 'EXPIRED', NOW() - INTERVAL '1 hour', NOW() - INTERVAL '1 hour');

-- Create test issuers
INSERT INTO trusted_issuers (did, name, email, trusted, established_date, jurisdiction, created_at, updated_at) VALUES
('did:example:gov001', 'Government Issuer 001', 'gov001@gov.test', true, '2020-01-01', 'US', NOW(), NOW()),
('did:example:gov002', 'Government Issuer 002', 'gov002@gov.test', true, '2020-01-01', 'US', NOW(), NOW()),
('did:example:gov003', 'Government Issuer 003', 'gov003@gov.test', true, '2020-01-01', 'US', NOW(), NOW()),
('did:example:untrusted', 'Untrusted Issuer', 'untrusted@fake.test', false, '2023-01-01', 'XX', NOW(), NOW());

-- Create test verifiers
INSERT INTO verifiers (did, name, email, verification_types, created_at, updated_at) VALUES
('did:example:verifier001', 'Test Verifier 001', 'verifier001@test.com', '["IDENTITY_VERIFICATION","AGE_VERIFICATION"]', NOW(), NOW()),
('did:example:verifier002', 'Test Verifier 002', 'verifier002@test.com', '["IDENTITY_VERIFICATION","ADDRESS_VERIFICATION"]', NOW(), NOW()),
('did:example:verifier003', 'Test Verifier 003', 'verifier003@test.com', '["AGE_VERIFICATION","ADDRESS_VERIFICATION"]', NOW(), NOW());

-- Update sequences to avoid conflicts
SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id::integer), 0) + 1 FROM users WHERE id ~ '^\\d+$'));
SELECT setval('credentials_id_seq', (SELECT COALESCE(MAX(id::integer), 0) + 1 FROM credentials WHERE id ~ '^\\d+$'));
SELECT setval('payments_id_seq', (SELECT COALESCE(MAX(id::integer), 0) + 1 FROM payments WHERE id ~ '^\\d+$'));
SELECT setval('audit_events_id_seq', (SELECT COALESCE(MAX(id::integer), 0) + 1 FROM audit_events WHERE id ~ '^\\d+$'));

-- Commit the changes
COMMIT;

-- Verify test data creation
SELECT 'Test users created: ' || COUNT(*) as test_users_count FROM users WHERE did LIKE 'did:example:test%';
SELECT 'Test credentials created: ' || COUNT(*) as test_credentials_count FROM credentials WHERE holder_did LIKE 'did:example:test%';
SELECT 'Test payments created: ' || COUNT(*) as test_payments_count FROM payments WHERE payer_did LIKE 'did:example:test%' OR payee_did LIKE 'did:example:test%';
SELECT 'Test audit events created: ' || COUNT(*) as test_audit_events_count FROM audit_events WHERE user_id_hash LIKE 'hash_test_%';

SELECT 'Setup completed successfully!' as status;
