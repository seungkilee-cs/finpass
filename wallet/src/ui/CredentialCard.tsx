import React, { useMemo, useState } from 'react';
import { StoredCredential } from '../services/storage';
import { VerificationFlow } from './VerificationFlow';

function decodeBase64Url(input: string): string {
  const padded = input.replace(/-/g, '+').replace(/_/g, '/');
  const padLen = (4 - (padded.length % 4)) % 4;
  const withPad = padded + '='.repeat(padLen);
  return atob(withPad);
}

function decodeJwtPayload(jwt: string): any {
  const parts = jwt.split('.');
  if (parts.length < 2) return null;
  try {
    return JSON.parse(decodeBase64Url(parts[1]));
  } catch {
    return null;
  }
}

export const CredentialCard: React.FC<{ credential: StoredCredential; verifierUrl: string }> = ({ credential, verifierUrl }) => {
  const [expanded, setExpanded] = useState(false);

  const subject = useMemo(() => {
    const payload = decodeJwtPayload(credential.credentialJwt);
    return payload?.vc?.credentialSubject || null;
  }, [credential.credentialJwt]);

  const displayName = subject?.name || 'Unknown';
  const displayDob = subject?.dob || subject?.birthDate || 'Unknown';
  const displayCountry = subject?.country || subject?.nationality || 'Unknown';

  return (
    <div style={{
      background: 'white',
      border: '1px solid #e9ecef',
      borderRadius: '8px',
      padding: '16px',
      marginBottom: '12px'
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <div style={{ fontWeight: 700, color: '#333' }}>Passport Credential</div>
          <div style={{ color: '#666', fontSize: '14px' }}>{displayName} · {displayCountry} · {displayDob}</div>
          <div style={{ color: '#666', fontSize: '12px', marginTop: '4px' }}>Status: {credential.status}</div>
        </div>
        <button
          onClick={() => setExpanded(!expanded)}
          style={{
            background: '#007bff',
            color: 'white',
            border: 'none',
            padding: '8px 10px',
            borderRadius: '6px',
            cursor: 'pointer'
          }}
        >
          {expanded ? 'Hide' : 'Details'}
        </button>
      </div>

      {expanded && (
        <div style={{ marginTop: '12px', fontSize: '13px', color: '#444' }}>
          <div><strong>credId:</strong> {credential.credId}</div>
          <div><strong>issuerDid:</strong> {credential.issuerDid}</div>
          <div><strong>commitmentHash:</strong> {credential.commitmentHash}</div>
          <div style={{ marginTop: '10px' }}>
            <button
              onClick={() => navigator.clipboard.writeText(credential.credentialJwt)}
              style={{
                background: '#28a745',
                color: 'white',
                border: 'none',
                padding: '6px 10px',
                borderRadius: '6px',
                cursor: 'pointer',
                marginRight: '8px'
              }}
            >
              Copy credentialJwt
            </button>
            <button
              onClick={() => navigator.clipboard.writeText(credential.commitmentJwt)}
              style={{
                background: '#6f42c1',
                color: 'white',
                border: 'none',
                padding: '6px 10px',
                borderRadius: '6px',
                cursor: 'pointer'
              }}
            >
              Copy commitmentJwt
            </button>
          </div>

          <VerificationFlow credential={credential} verifierUrl={verifierUrl} />
        </div>
      )}
    </div>
  );
};
