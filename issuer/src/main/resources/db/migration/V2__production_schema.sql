-- V2__production_schema.sql
-- Enhanced production schema for FinPass M1.7

-- Add new columns to existing users table
ALTER TABLE users 
ADD COLUMN IF NOT EXISTS email VARCHAR(255),
ADD COLUMN IF NOT EXISTS phone VARCHAR(50),
ADD COLUMN IF NOT EXISTS kyc_verified BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS kyc_verified_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
ADD COLUMN IF NOT EXISTS metadata TEXT,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Create additional indexes for users table
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);
CREATE INDEX IF NOT EXISTS idx_users_kyc_verified ON users(kyc_verified);

-- Enhance credentials table with new columns
ALTER TABLE credentials 
ADD COLUMN IF NOT EXISTS issuer_id TEXT NOT NULL DEFAULT 'finpass-issuer',
ADD COLUMN IF NOT EXISTS credential_type TEXT NOT NULL DEFAULT 'PassportCredential',
ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS revocation_handle VARCHAR(255),
ADD COLUMN IF NOT EXISTS revocation_reason VARCHAR(100),
ADD COLUMN IF NOT EXISTS revoked_by VARCHAR(255),
ADD COLUMN IF NOT EXISTS credential_hash VARCHAR(255),
ADD COLUMN IF NOT EXISTS nonce VARCHAR(255),
ADD COLUMN IF NOT EXISTS subject_did VARCHAR(255),
ADD COLUMN IF NOT EXISTS verification_method VARCHAR(100),
ADD COLUMN IF NOT EXISTS proof_type VARCHAR(50),
ADD COLUMN IF NOT EXISTS proof_created_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS proof_purpose VARCHAR(50),
ADD COLUMN IF NOT EXISTS metadata TEXT,
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Create additional indexes for credentials table
CREATE INDEX IF NOT EXISTS idx_credentials_issuer_id ON credentials(issuer_id);
CREATE INDEX IF NOT EXISTS idx_credentials_credential_type ON credentials(credential_type);
CREATE INDEX IF NOT EXISTS idx_credentials_expires_at ON credentials(expires_at);
CREATE INDEX IF NOT EXISTS idx_credentials_revoked_at ON credentials(revoked_at);
CREATE INDEX IF NOT EXISTS idx_credentials_revocation_handle ON credentials(revocation_handle);
CREATE INDEX IF NOT EXISTS idx_credentials_credential_hash ON credentials(credential_hash);
CREATE INDEX IF NOT EXISTS idx_credentials_subject_did ON credentials(subject_did);

-- Create credential_status table if not exists
CREATE TABLE IF NOT EXISTS credential_status (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    credential_id UUID NOT NULL UNIQUE REFERENCES credentials(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'VALID',
    revoked_at TIMESTAMP,
    revocation_reason VARCHAR(50),
    revoked_by VARCHAR(255),
    reason_description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for credential_status table
CREATE INDEX IF NOT EXISTS idx_credential_status_status ON credential_status(status);
CREATE INDEX IF NOT EXISTS idx_credential_status_revoked_at ON credential_status(revoked_at);
CREATE INDEX IF NOT EXISTS idx_credential_status_revoked_by ON credential_status(revoked_by);

-- Create payments table
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payer_did TEXT NOT NULL,
    payee_did TEXT NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_type VARCHAR(20) NOT NULL DEFAULT 'ONE_TIME',
    kyc_decision_token VARCHAR(500),
    payment_method VARCHAR(50),
    transaction_id VARCHAR(255) UNIQUE,
    reference_id VARCHAR(255),
    description VARCHAR(500),
    fee_amount DECIMAL(18,2),
    tax_amount DECIMAL(18,2),
    total_amount DECIMAL(18,2),
    authorized_at TIMESTAMP,
    captured_at TIMESTAMP,
    failed_at TIMESTAMP,
    refunded_at TIMESTAMP,
    failure_reason VARCHAR(500),
    refund_reason VARCHAR(500),
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

-- Create indexes for payments table
CREATE INDEX IF NOT EXISTS idx_payments_payer_did ON payments(payer_did);
CREATE INDEX IF NOT EXISTS idx_payments_payee_did ON payments(payee_did);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);
CREATE INDEX IF NOT EXISTS idx_payments_payment_type ON payments(payment_type);
CREATE INDEX IF NOT EXISTS idx_payments_created_at ON payments(created_at);
CREATE INDEX IF NOT EXISTS idx_payments_completed_at ON payments(completed_at);
CREATE INDEX IF NOT EXISTS idx_payments_transaction_id ON payments(transaction_id);

-- Create audit_events table
CREATE TABLE IF NOT EXISTS audit_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(50) NOT NULL,
    user_id_hash VARCHAR(64),
    session_id VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    tenant_id VARCHAR(255),
    resource_id VARCHAR(255),
    resource_type VARCHAR(100),
    action VARCHAR(100),
    outcome VARCHAR(20),
    severity VARCHAR(20) DEFAULT 'INFO',
    description VARCHAR(1000),
    details TEXT,
    previous_values TEXT,
    new_values TEXT,
    performed_by VARCHAR(255),
    performed_by_role VARCHAR(100),
    source_system VARCHAR(100),
    correlation_id VARCHAR(255),
    request_id VARCHAR(255),
    duration_ms BIGINT,
    error_code VARCHAR(100),
    error_message VARCHAR(1000),
    stack_trace TEXT,
    client_version VARCHAR(50),
    api_version VARCHAR(50),
    geolocation VARCHAR(255),
    device_fingerprint VARCHAR(255),
    compliance_flags VARCHAR(255),
    retention_days INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for audit_events table
CREATE INDEX IF NOT EXISTS idx_audit_events_event_type ON audit_events(event_type);
CREATE INDEX IF NOT EXISTS idx_audit_events_created_at ON audit_events(created_at);
CREATE INDEX IF NOT EXISTS idx_audit_events_user_id_hash ON audit_events(user_id_hash);
CREATE INDEX IF NOT EXISTS idx_audit_events_session_id ON audit_events(session_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_ip_address ON audit_events(ip_address);
CREATE INDEX IF NOT EXISTS idx_audit_events_severity ON audit_events(severity);
CREATE INDEX IF NOT EXISTS idx_audit_events_resource_id ON audit_events(resource_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_correlation_id ON audit_events(correlation_id);

-- Create or replace update timestamp trigger function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for updated_at columns
DROP TRIGGER IF EXISTS update_users_updated_at ON users;
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_credentials_updated_at ON credentials;
CREATE TRIGGER update_credentials_updated_at BEFORE UPDATE ON credentials
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_credential_status_updated_at ON credential_status;
CREATE TRIGGER update_credential_status_updated_at BEFORE UPDATE ON credential_status
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_payments_updated_at ON payments;
CREATE TRIGGER update_payments_updated_at BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Update existing users with default values
UPDATE users SET 
    status = COALESCE(status, 'ACTIVE'),
    kyc_verified = COALESCE(kyc_verified, FALSE),
    updated_at = CURRENT_TIMESTAMP
WHERE status IS NULL OR kyc_verified IS NULL;

-- Update existing credentials with default values
UPDATE credentials SET 
    issuer_id = COALESCE(issuer_id, 'finpass-issuer'),
    credential_type = COALESCE(credential_type, 'PassportCredential'),
    status = COALESCE(status, 'VALID'),
    updated_at = CURRENT_TIMESTAMP
WHERE issuer_id IS NULL OR credential_type IS NULL OR status IS NULL;

-- Initialize credential_status for existing credentials
INSERT INTO credential_status (credential_id, status, created_at, updated_at)
SELECT id, 'VALID', created_at, CURRENT_TIMESTAMP 
FROM credentials c 
WHERE NOT EXISTS (SELECT 1 FROM credential_status cs WHERE cs.credential_id = c.id);

-- Insert system user if not exists
INSERT INTO users (id, did, created_at, status, updated_at) 
SELECT '00000000-0000-0000-0000-000000000000', 'did:example:system', CURRENT_TIMESTAMP, 'ACTIVE', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = '00000000-0000-0000-0000-000000000000');

-- Add enhanced comments
COMMENT ON TABLE users IS 'Enhanced user accounts with KYC and status tracking';
COMMENT ON TABLE credentials IS 'Enhanced verifiable credentials with full lifecycle support';
COMMENT ON TABLE payments IS 'Payment transactions with comprehensive tracking';
COMMENT ON TABLE audit_events IS 'Comprehensive audit logging for compliance and security';

COMMENT ON COLUMN users.kyc_verified IS 'KYC verification status for regulatory compliance';
COMMENT ON COLUMN users.status IS 'User account status (ACTIVE, SUSPENDED, TERMINATED)';
COMMENT ON COLUMN credentials.credential_hash IS 'SHA-256 hash for credential integrity verification';
COMMENT ON COLUMN credentials.revocation_handle IS 'Unique handle for revocation list references';
COMMENT ON COLUMN payments.kyc_decision_token IS 'Token from KYC service for payment authorization';
COMMENT ON COLUMN audit_events.user_id_hash IS 'SHA-256 hash of user DID for privacy compliance';
COMMENT ON COLUMN audit_events.severity IS 'Event severity level (INFO, WARNING, ERROR, CRITICAL)';
