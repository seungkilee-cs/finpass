import React, { useEffect, useMemo, useState } from 'react';

import { PaymentClient, PaymentIntentResponse } from '../services/PaymentClient';
import { StorageService, StoredCredential } from '../services/storage';
import { VerificationFlow as VerificationFlowApi } from '../flows/VerificationFlow';

export const PaymentFlow: React.FC<{ verifierUrl: string }> = ({ verifierUrl }) => {
  const paymentBaseUrl = useMemo(() => verifierUrl.replace(/\/$/, ''), [verifierUrl]);

  const wallet = StorageService.getWalletInfo();
  const payerDid = wallet?.did || '';

  const [credentials, setCredentials] = useState<StoredCredential[]>(StorageService.getCredentials());
  const [selectedCredId, setSelectedCredId] = useState<string>(credentials[0]?.credId || '');

  const [receiverDid, setReceiverDid] = useState('did:key:RECEIVER_EXAMPLE');
  const [amount, setAmount] = useState<number>(10);

  const [intent, setIntent] = useState<PaymentIntentResponse | null>(null);
  const [payerBalance, setPayerBalance] = useState<number | null>(null);
  const [receiverBalance, setReceiverBalance] = useState<number | null>(null);

  const [isWorking, setIsWorking] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  useEffect(() => {
    const creds = StorageService.getCredentials();
    setCredentials(creds);
    if (!selectedCredId && creds.length > 0) setSelectedCredId(creds[0].credId);
  }, [selectedCredId]);

  const refreshBalances = async (currentReceiverDid?: string) => {
    if (!payerDid) return;
    const recv = currentReceiverDid ?? receiverDid;

    const [payer, recvBal] = await Promise.all([
      PaymentClient.getBalance(paymentBaseUrl, payerDid),
      PaymentClient.getBalance(paymentBaseUrl, recv)
    ]);
    setPayerBalance(payer.balance);
    setReceiverBalance(recvBal.balance);
  };

  const handleCreateIntent = async () => {
    setError('');
    setMessage('');

    if (!payerDid) {
      setError('Wallet not initialized');
      return;
    }
    if (!receiverDid.trim()) {
      setError('Receiver DID is required');
      return;
    }
    if (amount <= 0) {
      setError('Amount must be positive');
      return;
    }

    setIsWorking(true);
    try {
      const created = await PaymentClient.createIntent(paymentBaseUrl, payerDid, receiverDid.trim(), amount);
      setIntent(created);
      await refreshBalances(receiverDid.trim());
      setMessage('Payment intent created');
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || 'Failed to create payment intent');
    } finally {
      setIsWorking(false);
    }
  };

  const handleAttachKyc = async () => {
    setError('');
    setMessage('');

    if (!intent) {
      setError('Create a payment intent first');
      return;
    }
    if (!selectedCredId) {
      setError('Select a credential to present');
      return;
    }

    setIsWorking(true);
    try {
      let decision = StorageService.getValidDecisionToken({
        credId: selectedCredId,
        verifierUrl: paymentBaseUrl,
        requiredClaims: ['over_18']
      });

      if (!decision) {
        decision = await VerificationFlowApi.verify(selectedCredId, paymentBaseUrl);
      }

      const updated = await PaymentClient.attachKyc(paymentBaseUrl, intent.intentId, decision.decisionToken);
      setIntent(updated);
      setMessage('KYC attached');
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || 'Failed to attach KYC');
    } finally {
      setIsWorking(false);
    }
  };

  const handleConfirm = async () => {
    setError('');
    setMessage('');

    if (!intent) {
      setError('Create a payment intent first');
      return;
    }

    setIsWorking(true);
    try {
      const confirmed = await PaymentClient.confirmPayment(paymentBaseUrl, intent.intentId);
      setIntent({
        intentId: confirmed.intentId,
        status: confirmed.status,
        amount: confirmed.amount,
        payerDid: confirmed.payerDid,
        receiverDid: confirmed.receiverDid,
        kycVerified: true
      });
      setPayerBalance(confirmed.payerBalance);
      setReceiverBalance(confirmed.receiverBalance);
      setMessage('Payment confirmed');
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || 'Failed to confirm payment');
    } finally {
      setIsWorking(false);
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
      <h2 style={{ marginTop: 0, color: '#333' }}>Payments (M0.6)</h2>

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

      {message && (
        <div style={{
          background: '#d4edda',
          border: '1px solid #c3e6cb',
          color: '#155724',
          padding: '10px',
          borderRadius: '6px',
          marginBottom: '12px'
        }}>
          {message}
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
        <div>
          <label style={{ display: 'block', fontWeight: 'bold', marginBottom: '6px' }}>Receiver DID</label>
          <input
            value={receiverDid}
            onChange={(e) => setReceiverDid(e.target.value)}
            placeholder="did:key:..."
            style={{ width: '100%', padding: '10px', borderRadius: '6px', border: '1px solid #ccc' }}
          />
        </div>

        <div>
          <label style={{ display: 'block', fontWeight: 'bold', marginBottom: '6px' }}>Amount</label>
          <input
            type="number"
            value={amount}
            min={1}
            onChange={(e) => setAmount(parseInt(e.target.value || '0', 10))}
            style={{ width: '100%', padding: '10px', borderRadius: '6px', border: '1px solid #ccc' }}
          />
        </div>
      </div>

      <div style={{ marginTop: '12px' }}>
        <label style={{ display: 'block', fontWeight: 'bold', marginBottom: '6px' }}>Credential to present for KYC</label>
        <select
          value={selectedCredId}
          onChange={(e) => setSelectedCredId(e.target.value)}
          style={{ width: '100%', padding: '10px', borderRadius: '6px', border: '1px solid #ccc' }}
        >
          {credentials.length === 0 ? (
            <option value="">No credentials available</option>
          ) : (
            credentials.map(c => (
              <option key={c.credId} value={c.credId}>{c.credId}</option>
            ))
          )}
        </select>
      </div>

      <div style={{ marginTop: '14px', display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
        <button
          onClick={handleCreateIntent}
          disabled={isWorking}
          style={{
            background: isWorking ? '#6c757d' : '#007bff',
            color: 'white',
            border: 'none',
            padding: '10px 12px',
            borderRadius: '6px',
            cursor: isWorking ? 'not-allowed' : 'pointer'
          }}
        >
          Create Intent
        </button>

        <button
          onClick={handleAttachKyc}
          disabled={isWorking || !intent}
          style={{
            background: isWorking || !intent ? '#6c757d' : '#17a2b8',
            color: 'white',
            border: 'none',
            padding: '10px 12px',
            borderRadius: '6px',
            cursor: isWorking || !intent ? 'not-allowed' : 'pointer'
          }}
        >
          Attach KYC (over_18)
        </button>

        <button
          onClick={handleConfirm}
          disabled={isWorking || !intent}
          style={{
            background: isWorking || !intent ? '#6c757d' : '#28a745',
            color: 'white',
            border: 'none',
            padding: '10px 12px',
            borderRadius: '6px',
            cursor: isWorking || !intent ? 'not-allowed' : 'pointer'
          }}
        >
          Confirm Payment
        </button>

        <button
          onClick={() => refreshBalances()}
          disabled={isWorking || !payerDid}
          style={{
            background: '#6c757d',
            color: 'white',
            border: 'none',
            padding: '10px 12px',
            borderRadius: '6px',
            cursor: isWorking || !payerDid ? 'not-allowed' : 'pointer'
          }}
        >
          Refresh Balances
        </button>
      </div>

      <div style={{ marginTop: '14px', fontSize: '13px', color: '#444' }}>
        <div><strong>Payment API:</strong> {paymentBaseUrl}</div>
        <div style={{ marginTop: '6px' }}><strong>Payer DID:</strong> {payerDid || 'N/A'}</div>
        <div style={{ marginTop: '6px' }}><strong>Intent:</strong> {intent ? `${intent.intentId} (${intent.status})` : 'N/A'}</div>
        <div style={{ marginTop: '6px' }}><strong>Balances:</strong> payer={payerBalance ?? 'N/A'} receiver={receiverBalance ?? 'N/A'}</div>
      </div>

      <div style={{ marginTop: '10px', fontSize: '12px', color: '#666' }}>
        This demo requires an over_18 decision token. The wallet will re-use a valid token if present, otherwise it will run verification.
      </div>
    </div>
  );
};
