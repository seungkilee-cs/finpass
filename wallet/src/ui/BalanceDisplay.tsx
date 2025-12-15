import React, { useCallback, useEffect, useState } from 'react';

import { PaymentClient } from '../services/PaymentClient';

export const BalanceDisplay: React.FC<{
  paymentBaseUrl: string;
  payerDid: string;
  receiverDid: string;
  refreshNonce?: number;
}> = ({ paymentBaseUrl, payerDid, receiverDid, refreshNonce }) => {
  const [payerBalance, setPayerBalance] = useState<number | null>(null);
  const [receiverBalance, setReceiverBalance] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const refresh = useCallback(async () => {
    if (!payerDid) return;
    if (!receiverDid) return;

    setIsLoading(true);
    setError('');
    try {
      const [payer, recv] = await Promise.all([
        PaymentClient.getBalance(paymentBaseUrl, payerDid),
        PaymentClient.getBalance(paymentBaseUrl, receiverDid)
      ]);
      setPayerBalance(payer.balance);
      setReceiverBalance(recv.balance);
    } catch (e: any) {
      setError(e?.response?.data?.message || e?.message || 'Failed to load balances');
    } finally {
      setIsLoading(false);
    }
  }, [payerDid, receiverDid, paymentBaseUrl]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  useEffect(() => {
    void refresh();
  }, [refreshNonce, refresh]);

  return (
    <div style={{
      background: '#f8f9fa',
      border: '1px solid #e9ecef',
      borderRadius: '8px',
      padding: '12px',
      marginTop: '14px'
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '10px' }}>
        <div style={{ fontWeight: 'bold', color: '#333' }}>Balances</div>
        <button
          onClick={() => refresh()}
          disabled={isLoading || !payerDid || !receiverDid}
          style={{
            background: '#6c757d',
            color: 'white',
            border: 'none',
            padding: '8px 10px',
            borderRadius: '6px',
            cursor: isLoading || !payerDid || !receiverDid ? 'not-allowed' : 'pointer'
          }}
        >
          {isLoading ? 'Refreshing…' : 'Refresh'}
        </button>
      </div>

      {error && (
        <div style={{ marginTop: '10px', color: '#721c24', fontSize: '12px' }}>{error}</div>
      )}

      <div style={{ marginTop: '10px', fontSize: '13px', color: '#444' }}>
        <div><strong>Payer:</strong> {payerBalance ?? 'N/A'}</div>
        <div style={{ marginTop: '6px' }}><strong>Receiver:</strong> {receiverBalance ?? 'N/A'}</div>
      </div>

      <div style={{ marginTop: '10px', fontSize: '12px', color: '#666' }}>
        <div><strong>Payer DID:</strong> {payerDid || 'N/A'}</div>
        <div style={{ marginTop: '6px' }}><strong>Receiver DID:</strong> {receiverDid || 'N/A'}</div>
      </div>
    </div>
  );
};
