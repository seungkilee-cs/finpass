import React, { useState } from 'react';
import { DIDManager } from '../core/DIDManager';
import { StorageService } from '../services/storage';

interface OnboardingProps {
  onComplete: (did: string) => void;
}

export const Onboarding: React.FC<OnboardingProps> = ({ onComplete }) => {
  const [isCreating, setIsCreating] = useState(false);
  const [isImporting, setIsImporting] = useState(false);
  const [importMnemonic, setImportMnemonic] = useState('');
  const [error, setError] = useState('');
  const [showMnemonic, setShowMnemonic] = useState(false);
  const [confirmedBackup, setConfirmedBackup] = useState(false);
  const [createdDID, setCreatedDID] = useState<string | null>(null);
  const [createdMnemonic, setCreatedMnemonic] = useState<string | null>(null);

  const handleCreateWallet = async () => {
    setIsCreating(true);
    setError('');
    
    try {
      // Generate new DID and key pair
      const { did, keyPair, mnemonic } = await DIDManager.createDID();
      
      // Save to storage
      StorageService.saveKeys(keyPair, did, mnemonic);
      
      // Show mnemonic backup prompt
      setCreatedDID(did);
      setCreatedMnemonic(mnemonic);
      setConfirmedBackup(false);
      setShowMnemonic(true);
    } catch (err) {
      setError('Failed to create wallet. Please try again.');
      console.error('Wallet creation failed:', err);
    } finally {
      setIsCreating(false);
    }
  };

  const handleConfirmMnemonic = () => {
    if (createdDID) {
      setShowMnemonic(false);
      onComplete(createdDID);
    }
  };

  const handleImportWallet = async () => {
    if (!importMnemonic.trim()) {
      setError('Please enter your mnemonic phrase');
      return;
    }

    setIsImporting(true);
    setError('');
    
    try {
      const storedKeys = await StorageService.importFromMnemonic(importMnemonic.trim());
      onComplete(storedKeys.did);
    } catch (err) {
      setError('Invalid mnemonic phrase. Please check and try again.');
      console.error('Wallet import failed:', err);
    } finally {
      setIsImporting(false);
    }
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text).then(() => {
      // Could add a toast notification here
    });
  };

  if (showMnemonic && createdMnemonic) {
    return (
      <div style={{ maxWidth: '600px', margin: '0 auto', padding: '20px' }}>
        <div style={{ 
          background: '#fff3cd', 
          border: '1px solid #ffeaa7', 
          borderRadius: '8px', 
          padding: '20px',
          marginBottom: '20px'
        }}>
          <h2 style={{ color: '#856404', marginTop: 0 }}>🔐 Save Your Recovery Phrase</h2>
          <p style={{ color: '#856404' }}>
            This is the <strong>only way</strong> to recover your wallet if you lose access to this device.
            Write it down and store it safely.
          </p>
          
          <div style={{
            background: '#fff',
            border: '2px dashed #ffeaa7',
            borderRadius: '8px',
            padding: '15px',
            marginBottom: '15px',
            fontFamily: 'monospace',
            fontSize: '14px',
            wordBreak: 'break-all',
            lineHeight: '1.5'
          }}>
            {createdMnemonic}
          </div>
          
          <button
            onClick={() => copyToClipboard(createdMnemonic)}
            style={{
              background: '#007bff',
              color: 'white',
              border: 'none',
              padding: '8px 16px',
              borderRadius: '4px',
              cursor: 'pointer',
              marginRight: '10px'
            }}
          >
            📋 Copy to Clipboard
          </button>
        </div>

        <div style={{ marginBottom: '20px' }}>
          <label style={{ display: 'block', marginBottom: '10px', fontWeight: 'bold' }}>
            <input
              type="checkbox"
              checked={confirmedBackup}
              onChange={(e) => setConfirmedBackup(e.target.checked)}
              style={{ marginRight: '8px' }}
            />
            I have safely stored my recovery phrase
          </label>
        </div>

        <button
          onClick={handleConfirmMnemonic}
          disabled={!confirmedBackup}
          style={{
            background: '#28a745',
            color: 'white',
            border: 'none',
            padding: '12px 24px',
            borderRadius: '6px',
            cursor: confirmedBackup ? 'pointer' : 'not-allowed',
            fontSize: '16px',
            width: '100%'
          }}
        >
          Continue to Wallet
        </button>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: '500px', margin: '0 auto', padding: '20px' }}>
      <div style={{ textAlign: 'center', marginBottom: '30px' }}>
        <h1 style={{ color: '#333', marginBottom: '10px' }}>FinPass Wallet</h1>
        <p style={{ color: '#666' }}>Your Digital Financial Passport</p>
      </div>

      {error && (
        <div style={{
          background: '#f8d7da',
          border: '1px solid #f5c6cb',
          color: '#721c24',
          padding: '12px',
          borderRadius: '4px',
          marginBottom: '20px'
        }}>
          {error}
        </div>
      )}

      <div style={{ marginBottom: '20px' }}>
        <button
          onClick={handleCreateWallet}
          disabled={isCreating}
          style={{
            background: '#007bff',
            color: 'white',
            border: 'none',
            padding: '15px 30px',
            borderRadius: '6px',
            cursor: isCreating ? 'not-allowed' : 'pointer',
            fontSize: '16px',
            width: '100%',
            marginBottom: '10px'
          }}
        >
          {isCreating ? 'Creating Wallet...' : '🆕 Create New Wallet'}
        </button>

        <div style={{ textAlign: 'center', color: '#666', margin: '15px 0' }}>OR</div>

        <div>
          <label style={{ display: 'block', marginBottom: '8px', fontWeight: 'bold' }}>
            Import Existing Wallet
          </label>
          <textarea
            value={importMnemonic}
            onChange={(e) => setImportMnemonic(e.target.value)}
            placeholder="Enter your 12 or 24 word recovery phrase..."
            style={{
              width: '100%',
              height: '80px',
              padding: '10px',
              border: '1px solid #ddd',
              borderRadius: '4px',
              fontSize: '14px',
              fontFamily: 'monospace',
              resize: 'vertical'
            }}
          />
          <button
            onClick={handleImportWallet}
            disabled={isImporting || !importMnemonic.trim()}
            style={{
              background: '#28a745',
              color: 'white',
              border: 'none',
              padding: '12px 24px',
              borderRadius: '6px',
              cursor: isImporting || !importMnemonic.trim() ? 'not-allowed' : 'pointer',
              fontSize: '16px',
              width: '100%',
              marginTop: '10px'
            }}
          >
            {isImporting ? 'Importing...' : '📥 Import Wallet'}
          </button>
        </div>
      </div>

      <div style={{
        background: '#f8f9fa',
        border: '1px solid #e9ecef',
        borderRadius: '6px',
        padding: '15px',
        fontSize: '12px',
        color: '#6c757d'
      }}>
        <strong>🔒 Security Note:</strong> Your wallet keys are stored locally on this device. 
        Never share your recovery phrase with anyone. We cannot recover your wallet if you lose it.
      </div>
    </div>
  );
};
