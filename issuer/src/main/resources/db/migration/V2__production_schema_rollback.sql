-- V2__production_schema_rollback.sql
-- Rollback script for V2 production schema changes

-- Drop triggers first
DROP TRIGGER IF EXISTS update_users_updated_at ON users;
DROP TRIGGER IF EXISTS update_credentials_updated_at ON credentials;
DROP TRIGGER IF EXISTS update_credential_status_updated_at ON credential_status;
DROP TRIGGER IF EXISTS update_payments_updated_at ON payments;

-- Drop new tables
DROP TABLE IF EXISTS audit_events;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS credential_status;

-- Remove new columns from credentials table
ALTER TABLE credentials 
DROP COLUMN IF EXISTS issuer_id,
DROP COLUMN IF EXISTS credential_type,
DROP COLUMN IF EXISTS expires_at,
DROP COLUMN IF EXISTS revoked_at,
DROP COLUMN IF EXISTS revocation_handle,
DROP COLUMN IF EXISTS revocation_reason,
DROP COLUMN IF EXISTS revoked_by,
DROP COLUMN IF EXISTS credential_hash,
DROP COLUMN IF EXISTS nonce,
DROP COLUMN IF EXISTS subject_did,
DROP COLUMN IF EXISTS verification_method,
DROP COLUMN IF EXISTS proof_type,
DROP COLUMN IF EXISTS proof_created_at,
DROP COLUMN IF EXISTS proof_purpose,
DROP COLUMN IF EXISTS metadata,
DROP COLUMN IF EXISTS updated_at;

-- Remove new columns from users table
ALTER TABLE users 
DROP COLUMN IF EXISTS email,
DROP COLUMN IF EXISTS phone,
DROP COLUMN IF EXISTS kyc_verified,
DROP COLUMN IF EXISTS kyc_verified_at,
DROP COLUMN IF EXISTS status,
DROP COLUMN IF EXISTS metadata,
DROP COLUMN IF EXISTS updated_at;

-- Drop indexes that were added
DROP INDEX IF EXISTS idx_users_status;
DROP INDEX IF EXISTS idx_users_kyc_verified;
DROP INDEX IF EXISTS idx_credentials_issuer_id;
DROP INDEX IF EXISTS idx_credentials_credential_type;
DROP INDEX IF EXISTS idx_credentials_expires_at;
DROP INDEX IF EXISTS idx_credentials_revoked_at;
DROP INDEX IF EXISTS idx_credentials_revocation_handle;
DROP INDEX IF EXISTS idx_credentials_credential_hash;
DROP INDEX IF EXISTS idx_credentials_subject_did;
DROP INDEX IF EXISTS idx_credential_status_status;
DROP INDEX IF EXISTS idx_credential_status_revoked_at;
DROP INDEX IF EXISTS idx_credential_status_revoked_by;
DROP INDEX IF EXISTS idx_payments_payer_did;
DROP INDEX IF EXISTS idx_payments_payee_did;
DROP INDEX IF EXISTS idx_payments_status;
DROP INDEX IF EXISTS idx_payments_payment_type;
DROP INDEX IF EXISTS idx_payments_created_at;
DROP INDEX IF EXISTS idx_payments_completed_at;
DROP INDEX IF EXISTS idx_payments_transaction_id;
DROP INDEX IF EXISTS idx_audit_events_event_type;
DROP INDEX IF EXISTS idx_audit_events_created_at;
DROP INDEX IF EXISTS idx_audit_events_user_id_hash;
DROP INDEX IF EXISTS idx_audit_events_session_id;
DROP INDEX IF EXISTS idx_audit_events_ip_address;
DROP INDEX IF EXISTS idx_audit_events_severity;
DROP INDEX IF EXISTS idx_audit_events_resource_id;
DROP INDEX IF EXISTS idx_audit_events_correlation_id;

-- Note: The trigger function update_updated_at_column() is left in place
-- as it might be used by other parts of the system
