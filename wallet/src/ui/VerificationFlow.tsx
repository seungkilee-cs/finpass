import React, { useEffect, useMemo, useState } from 'react';

import { VerificationFlow as VerificationFlowApi } from '../flows/VerificationFlow';
import { StorageService, StoredCredential, StoredDecisionToken } from '../services/storage';

export const VerificationFlow: React.FC<{ credential: StoredCredential; verifierUrl: string }> = ({
  credential,
  verifierUrl
}) => {
  const normalizedVerifierUrl = useMemo(() => verifierUrl.replace(/\/$/, ''), [verifierUrl]);

  const [isVerifying, setIsVerifying] = useState(false);
  const [error, setError] = useState('');
  const [token, setToken] = useState<StoredDecisionToken | null>(null);

  useEffect(() => {
    const existing = StorageService.getValidDecisionToken({
      credId: credential.credId,
      verifierUrl: normalizedVerifierUrl,
      requiredClaims: ['over_18']
    });
    setToken(existing);
  }, [credential.credId, normalizedVerifierUrl]);

  const handleVerify = async () => {
    setError('');

    const ok = window.confirm('Prove you are over 18 to the verifier?');
    if (!ok) return;

    setIsVerifying(true);
    try {
      const t = await VerificationFlowApi.verify(credential.credId, normalizedVerifierUrl);
      setToken(t);
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || 'Verification failed');
    } finally {
      setIsVerifying(false);
    }
  };

  return (
    <div style={{ marginTop: '12px' }}>
      <div style={{ fontWeight: 700, color: '#333', marginBottom: '6px' }}>Verification</div>

      {token ? (
        <div style={{
          background: '#d4edda',
          border: '1px solid #c3e6cb',
          borderRadius: '6px',
          padding: '10px',
          color: '#155724'
        }}>
          <div style={{ fontWeight: 700 }}>✓ Verified (over_18)</div>
          <div style={{ fontSize: '12px', marginTop: '4px' }}>
            Assurance: {token.assuranceLevel} · Expires: {new Date(token.expiresAt).toLocaleString()}
          </div>

          <div style={{ marginTop: '10px' }}>
            <button
              onClick={() => navigator.clipboard.writeText(token.decisionToken)}
              style={{
                background: '#17a2b8',
                color: 'white',
                border: 'none',
                padding: '6px 10px',
                borderRadius: '6px',
                cursor: 'pointer'
              }}
            >
              Copy decisionToken
            </button>
          </div>
        </div>
      ) : (
        <div>
          {error && (
            <div style={{
              background: '#f8d7da',
              border: '1px solid #f5c6cb',
              color: '#721c24',
              padding: '10px',
              borderRadius: '6px',
              marginBottom: '10px'
            }}>
              {error}
            </div>
          )}

          <button
            onClick={handleVerify}
            disabled={isVerifying}
            style={{
              background: isVerifying ? '#6c757d' : '#0069d9',
              color: 'white',
              border: 'none',
              padding: '8px 12px',
              borderRadius: '6px',
              cursor: isVerifying ? 'not-allowed' : 'pointer'
            }}
          >
            {isVerifying ? 'Verifying...' : 'Prove Over 18'}
          </button>

          <div style={{ marginTop: '8px', fontSize: '12px', color: '#666' }}>
            This sends a proof bound to a one-time verifier challenge and stores a decision token locally.
          </div>
        </div>
      )}
    </div>
  );
};
