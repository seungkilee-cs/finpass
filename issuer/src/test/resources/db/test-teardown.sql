-- Database teardown script for integration tests
-- This script cleans up all test data and resets the database state

-- Disable foreign key constraints temporarily for faster cleanup
SET session_replication_role = replica;

-- Clean up audit events (delete first due to foreign key dependencies)
DELETE FROM audit_events WHERE user_id_hash LIKE 'hash_test_%'
   OR correlation_id LIKE 'corr_test_%'
   OR session_id LIKE 'sess_test_%'
   OR details LIKE '%test_%'
   OR details LIKE '%TEST_%';

-- Clean up verification challenges
DELETE FROM verification_challenges WHERE id LIKE 'challenge_test_%'
   OR verifier_did LIKE 'did:example:verifier%'
   OR challenge_jwt LIKE '%test_%';

-- Clean up verification policies
DELETE FROM verification_policies WHERE id LIKE 'policy_test_%'
   OR name LIKE '%Test%'
   OR description LIKE '%test%';

-- Clean up payments
DELETE FROM payments WHERE id LIKE 'pay_test_%'
   OR payer_did LIKE 'did:example:test%'
   OR payee_did LIKE 'did:example:test%'
   OR description LIKE '%Test%'
   OR transaction_id LIKE '%test%';

-- Clean up credentials
DELETE FROM credentials WHERE id LIKE 'cred_test_%'
   OR holder_did LIKE 'did:example:test%'
   OR issuer_did LIKE 'did:example:test%'
   OR credential_jwt LIKE '%test_%'
   OR revocation_id LIKE 'rev_test_%';

-- Clean up users
DELETE FROM users WHERE did LIKE 'did:example:test%'
   OR email LIKE '%@test.com'
   OR name LIKE 'Test%'
   OR phone_number LIKE '+1-555-%';

-- Clean up trusted issuers
DELETE FROM trusted_issuers WHERE did LIKE 'did:example:test%'
   OR did LIKE 'did:example:gov%'
   OR did LIKE 'did:example:untrusted'
   OR email LIKE '%@test%'
   OR name LIKE '%Test%';

-- Clean up verifiers
DELETE FROM verifiers WHERE did LIKE 'did:example:test%'
   OR did LIKE 'did:example:verifier%'
   OR email LIKE '%@test%'
   OR name LIKE '%Test%';

-- Re-enable foreign key constraints
SET session_replication_role = DEFAULT;

-- Clean up any remaining test-related data with broader patterns
DELETE FROM audit_events WHERE 
    user_id_hash LIKE '%test%' OR
    correlation_id LIKE '%test%' OR
    session_id LIKE '%test%' OR
    ip_address LIKE '192.168.1.%' OR
    user_agent LIKE '%Test%' OR
    path LIKE '%test%' OR
    details ILIKE '%test%';

DELETE FROM payments WHERE 
    id LIKE '%test%' OR
    payer_did LIKE '%test%' OR
    payee_did LIKE '%test%' OR
    description ILIKE '%test%' OR
    metadata ILIKE '%test%';

DELETE FROM credentials WHERE 
    id LIKE '%test%' OR
    holder_did LIKE '%test%' OR
    issuer_did LIKE '%test%' OR
    credential_data ILIKE '%test%' OR
    credential_jwt LIKE '%test%';

DELETE FROM users WHERE 
    did LIKE '%test%' OR
    email LIKE '%test%' OR
    name ILIKE '%test%' OR
    phone_number LIKE '%test%' OR
    address ILIKE '%test%';

-- Reset sequences to start from 1 for clean test runs
ALTER SEQUENCE users_id_seq RESTART WITH 1;
ALTER SEQUENCE credentials_id_seq RESTART WITH 1;
ALTER SEQUENCE payments_id_seq RESTART WITH 1;
ALTER SEQUENCE audit_events_id_seq RESTART WITH 1;
ALTER SEQUENCE verification_policies_id_seq RESTART WITH 1;
ALTER SEQUENCE verification_challenges_id_seq RESTART WITH 1;

-- Vacuum and analyze tables to reclaim space and update statistics
VACUUM ANALYZE users;
VACUUM ANALYZE credentials;
VACUUM ANALYZE payments;
VACUUM ANALYZE audit_events;
VACUUM ANALYZE verification_policies;
VACUUM ANALYZE verification_challenges;
VACUUM ANALYZE trusted_issuers;
VACUUM ANALYZE verifiers;

-- Commit the cleanup
COMMIT;

-- Verify cleanup was successful
SELECT 'Users cleaned: ' || COUNT(*) as users_remaining FROM users WHERE 
    did LIKE '%test%' OR email LIKE '%test%' OR name ILIKE '%test%';

SELECT 'Credentials cleaned: ' || COUNT(*) as credentials_remaining FROM credentials WHERE 
    id LIKE '%test%' OR holder_did LIKE '%test%' OR issuer_did LIKE '%test%';

SELECT 'Payments cleaned: ' || COUNT(*) as payments_remaining FROM payments WHERE 
    id LIKE '%test%' OR payer_did LIKE '%test%' OR payee_did LIKE '%test%';

SELECT 'Audit events cleaned: ' || COUNT(*) as audit_events_remaining FROM audit_events WHERE 
    user_id_hash LIKE '%test%' OR correlation_id LIKE '%test%' OR session_id LIKE '%test%';

SELECT 'Verification policies cleaned: ' || COUNT(*) as policies_remaining FROM verification_policies WHERE 
    id LIKE '%test%' OR name ILIKE '%test%';

SELECT 'Verification challenges cleaned: ' || COUNT(*) as challenges_remaining FROM verification_challenges WHERE 
    id LIKE '%test%' OR verifier_did LIKE '%test%';

SELECT 'Trusted issuers cleaned: ' || COUNT(*) as issuers_remaining FROM trusted_issuers WHERE 
    did LIKE '%test%' OR email LIKE '%test%' OR name ILIKE '%test%';

SELECT 'Verifiers cleaned: ' || COUNT(*) as verifiers_remaining FROM verifiers WHERE 
    did LIKE '%test%' OR email LIKE '%test%' OR name ILIKE '%test%';

-- Check for any orphaned records that might have been missed
SELECT 'Checking for orphaned audit events...' as status;
SELECT COUNT(*) as orphaned_audit_events FROM audit_events ae 
LEFT JOIN users u ON ae.user_id_hash = 'hash_' || SUBSTRING(u.did FROM 10)
WHERE u.did IS NULL AND ae.user_id_hash LIKE 'hash_%';

SELECT 'Checking for orphaned credentials...' as status;
SELECT COUNT(*) as orphaned_credentials FROM credentials c 
LEFT JOIN users u ON c.holder_did = u.did
WHERE u.did IS NULL AND c.holder_did LIKE 'did:%';

SELECT 'Checking for orphaned payments...' as status;
SELECT COUNT(*) as orphaned_payments FROM payments p 
LEFT JOIN users u ON p.payer_did = u.did
WHERE u.did IS NULL AND p.payer_did LIKE 'did:%';

-- Final cleanup of any orphaned records found
DELETE FROM audit_events WHERE id IN (
    SELECT ae.id FROM audit_events ae 
    LEFT JOIN users u ON ae.user_id_hash = 'hash_' || SUBSTRING(u.did FROM 10)
    WHERE u.did IS NULL AND ae.user_id_hash LIKE 'hash_%'
);

DELETE FROM credentials WHERE id IN (
    SELECT c.id FROM credentials c 
    LEFT JOIN users u ON c.holder_did = u.did
    WHERE u.did IS NULL AND c.holder_did LIKE 'did:%'
);

DELETE FROM payments WHERE id IN (
    SELECT p.id FROM payments p 
    LEFT JOIN users u ON p.payer_did = u.did
    WHERE u.did IS NULL AND p.payer_did LIKE 'did:%'
);

-- Final verification
SELECT 'Final cleanup verification:' as status;
SELECT 'Users remaining: ' || COUNT(*) as final_users_count FROM users WHERE 
    did LIKE '%test%' OR email LIKE '%test%' OR name ILIKE '%test%';

SELECT 'Credentials remaining: ' || COUNT(*) as final_credentials_count FROM credentials WHERE 
    id LIKE '%test%' OR holder_did LIKE '%test%' OR issuer_did LIKE '%test%';

SELECT 'Payments remaining: ' || COUNT(*) as final_payments_count FROM payments WHERE 
    id LIKE '%test%' OR payer_did LIKE '%test%' OR payee_did LIKE '%test%';

SELECT 'Audit events remaining: ' || COUNT(*) as final_audit_events_count FROM audit_events WHERE 
    user_id_hash LIKE '%test%' OR correlation_id LIKE '%test%' OR session_id LIKE '%test%';

SELECT 'Teardown completed successfully!' as status;

-- Reset database statistics
ANALYZE;

-- Check table sizes after cleanup
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables 
WHERE schemaname = 'public' 
    AND tablename IN ('users', 'credentials', 'payments', 'audit_events', 'verification_policies', 'verification_challenges', 'trusted_issuers', 'verifiers')
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
