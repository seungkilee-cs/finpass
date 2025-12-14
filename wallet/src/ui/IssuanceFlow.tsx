import React, { useState } from 'react';
import { CredentialHolder } from '../core/CredentialHolder';
import { StoredCredential } from '../services/storage';

export const IssuanceFlow: React.FC<{ issuerUrl: string; onIssued: (cred: StoredCredential) => void }> = ({
  issuerUrl,
  onIssued
}) => {
  const [name, setName] = useState('');
  const [dob, setDob] = useState('');
  const [country, setCountry] = useState('');
  const [isIssuing, setIsIssuing] = useState(false);
  const [error, setError] = useState('');

  const handleIssue = async () => {
    setError('');
    setIsIssuing(true);

    try {
      const passportData = {
        name,
        dob,
        country
      };

      const cred = await CredentialHolder.requestCredential(issuerUrl, passportData);
      onIssued(cred);
      setName('');
      setDob('');
      setCountry('');
    } catch (e: any) {
      setError(e?.message || 'Failed to issue credential');
    } finally {
      setIsIssuing(false);
    }
  };

  return (
    <div style={{
      background: 'white',
      border: '1px solid #e9ecef',
      borderRadius: '8px',
      padding: '20px',
      marginBottom: '20px'
    }}>
      <h2 style={{ marginTop: 0, color: '#333' }}>Get Passport Credential</h2>

      {error && (
        <div style={{
          background: '#f8d7da',
          border: '1px solid #f5c6cb',
          color: '#721c24',
          padding: '10px',
          borderRadius: '6px',
          marginBottom: '12px'
        }}>
          {error}
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '12px' }}>
        <div>
          <label style={{ display: 'block', fontWeight: 'bold', marginBottom: '6px' }}>Name</label>
          <input
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Alice"
            style={{ width: '100%', padding: '10px', borderRadius: '6px', border: '1px solid #ccc' }}
          />
        </div>

        <div>
          <label style={{ display: 'block', fontWeight: 'bold', marginBottom: '6px' }}>DOB</label>
          <input
            value={dob}
            onChange={(e) => setDob(e.target.value)}
            placeholder="1990-01-01"
            style={{ width: '100%', padding: '10px', borderRadius: '6px', border: '1px solid #ccc' }}
          />
        </div>

        <div>
          <label style={{ display: 'block', fontWeight: 'bold', marginBottom: '6px' }}>Country</label>
          <input
            value={country}
            onChange={(e) => setCountry(e.target.value)}
            placeholder="US"
            style={{ width: '100%', padding: '10px', borderRadius: '6px', border: '1px solid #ccc' }}
          />
        </div>
      </div>

      <div style={{ marginTop: '14px' }}>
        <button
          onClick={handleIssue}
          disabled={isIssuing || !name || !dob || !country}
          style={{
            background: isIssuing ? '#6c757d' : '#007bff',
            color: 'white',
            border: 'none',
            padding: '12px 16px',
            borderRadius: '6px',
            cursor: isIssuing ? 'not-allowed' : 'pointer'
          }}
        >
          {isIssuing ? 'Issuing...' : 'Get Credential'}
        </button>
      </div>

      <div style={{ marginTop: '10px', fontSize: '12px', color: '#666' }}>
        This uses a wallet proof signature and sends the request to the issuer.
      </div>
    </div>
  );
};
