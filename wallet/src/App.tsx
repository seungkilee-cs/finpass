import React, { useState, useEffect } from 'react';
import { Onboarding } from './ui/Onboarding';
import { StorageService } from './services/storage';
import { IssuanceFlow } from './ui/IssuanceFlow';
import { CredentialCard } from './ui/CredentialCard';
import './App.css';

interface WalletInfo {
  did: string;
  createdAt: string;
}

const WalletDashboard: React.FC<{ walletInfo: WalletInfo }> = ({ walletInfo }) => {
  const [copied, setCopied] = useState(false);
  const [credentials, setCredentials] = useState(StorageService.getCredentials());

  const issuerUrl = 'http://localhost:8080';
  const verifierUrl = 'http://localhost:8090';

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    });
  };

  const formatDID = (did: string) => {
    if (did.length > 40) {
      return `${did.substring(0, 20)}...${did.substring(did.length - 20)}`;
    }
    return did;
  };

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto', padding: '20px' }}>
      <div style={{ textAlign: 'center', marginBottom: '30px' }}>
        <h1 style={{ color: '#333', marginBottom: '10px' }}>FinPass Wallet</h1>
        <p style={{ color: '#666' }}>Your Digital Financial Passport</p>
      </div>

      <div style={{
        background: 'white',
        border: '1px solid #e9ecef',
        borderRadius: '8px',
        padding: '20px',
        marginBottom: '20px'
      }}>
        <h2 style={{ marginTop: 0, color: '#333' }}>🔐 Your Identity</h2>
        
        <div style={{ marginBottom: '15px' }}>
          <label style={{ display: 'block', fontWeight: 'bold', marginBottom: '5px' }}>
            Your DID:
          </label>
          <div style={{
            background: '#f8f9fa',
            border: '1px solid #e9ecef',
            borderRadius: '4px',
            padding: '10px',
            fontFamily: 'monospace',
            fontSize: '14px',
            wordBreak: 'break-all',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center'
          }}>
            <span title={walletInfo.did}>{formatDID(walletInfo.did)}</span>
            <button
              onClick={() => copyToClipboard(walletInfo.did)}
              style={{
                background: copied ? '#28a745' : '#007bff',
                color: 'white',
                border: 'none',
                padding: '4px 8px',
                borderRadius: '3px',
                cursor: 'pointer',
                fontSize: '12px'
              }}
            >
              {copied ? '✓ Copied' : '📋 Copy'}
            </button>
          </div>
        </div>

        <div style={{ marginBottom: '15px' }}>
          <label style={{ display: 'block', fontWeight: 'bold', marginBottom: '5px' }}>
            Wallet Created:
          </label>
          <div style={{ color: '#666' }}>
            {new Date(walletInfo.createdAt).toLocaleString()}
          </div>
        </div>
      </div>

      <div style={{
        background: '#e7f3ff',
        border: '1px solid #b3d9ff',
        borderRadius: '8px',
        padding: '20px',
        marginBottom: '20px'
      }}>
        <h3 style={{ marginTop: 0, color: '#0066cc' }}>📋 Next Steps</h3>
        <p style={{ marginBottom: '15px' }}>Your wallet is ready! Here's what you can do next:</p>
        <ul style={{ paddingLeft: '20px', margin: 0 }}>
          <li>Get your passport credential</li>
          <li>Prove you're over 18 to a verifier without sharing PII</li>
          <li>Make payments with KYC verification (coming soon)</li>
        </ul>
      </div>

      <IssuanceFlow
        issuerUrl={issuerUrl}
        onIssued={() => {
          setCredentials(StorageService.getCredentials());
        }}
      />

      <div style={{
        background: 'white',
        border: '1px solid #e9ecef',
        borderRadius: '8px',
        padding: '20px',
        marginBottom: '20px'
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h2 style={{ marginTop: 0, color: '#333' }}>Your Credentials</h2>
          <button
            onClick={() => setCredentials(StorageService.getCredentials())}
            style={{
              background: '#6c757d',
              color: 'white',
              border: 'none',
              padding: '8px 12px',
              borderRadius: '6px',
              cursor: 'pointer'
            }}
          >
            Refresh
          </button>
        </div>

        {credentials.length === 0 ? (
          <div style={{ color: '#666' }}>No credentials stored yet.</div>
        ) : (
          <div>
            {credentials.map(c => (
              <CredentialCard key={c.credId} credential={c} verifierUrl={verifierUrl} />
            ))}
          </div>
        )}
      </div>

      <div style={{
        background: '#f8f9fa',
        border: '1px solid #e9ecef',
        borderRadius: '6px',
        padding: '15px',
        fontSize: '14px',
        color: '#6c757d'
      }}>
        <strong>🔐 Security:</strong> Your keys are stored locally. Keep your recovery phrase safe and never share it with anyone.
      </div>

      <div style={{ marginTop: '30px', textAlign: 'center' }}>
        <button
          onClick={() => {
            if (window.confirm('Are you sure you want to clear your wallet? Make sure you have saved your recovery phrase!')) {
              StorageService.clearKeys();
              window.location.reload();
            }
          }}
          style={{
            background: '#dc3545',
            color: 'white',
            border: 'none',
            padding: '10px 20px',
            borderRadius: '4px',
            cursor: 'pointer'
          }}
        >
          🗑️ Clear Wallet
        </button>
      </div>
    </div>
  );
};

function App() {
  const [walletInfo, setWalletInfo] = useState<WalletInfo | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Check if wallet already exists
    const existingWallet = StorageService.getWalletInfo();
    if (existingWallet) {
      setWalletInfo(existingWallet);
    }
    setLoading(false);
  }, []);

  const handleWalletComplete = (did: string) => {
    const info = StorageService.getWalletInfo();
    if (info) {
      setWalletInfo(info);
    }
  };

  if (loading) {
    return (
      <div style={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        height: '100vh',
        flexDirection: 'column'
      }}>
        <div style={{ fontSize: '18px', marginBottom: '10px' }}>Loading FinPass Wallet...</div>
        <div style={{ color: '#666' }}>Please wait</div>
      </div>
    );
  }

  return (
    <div className="App">
      {walletInfo ? (
        <WalletDashboard walletInfo={walletInfo} />
      ) : (
        <Onboarding onComplete={handleWalletComplete} />
      )}
    </div>
  );
}

export default App;
