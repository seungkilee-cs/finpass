import React from 'react';

export interface PaymentReceipt {
  id: string;
  intentId: string;
  payerDid: string;
  receiverDid: string;
  amount: number;
  confirmedAt: string;
}

export const TransactionHistory: React.FC<{
  receipts: PaymentReceipt[];
  onClear?: () => void;
}> = ({ receipts, onClear }) => {
  return (
    <div style={{
      background: 'white',
      border: '1px solid #e9ecef',
      borderRadius: '8px',
      padding: '12px',
      marginTop: '14px'
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '10px' }}>
        <div style={{ fontWeight: 'bold', color: '#333' }}>Transaction History</div>
        <button
          onClick={onClear}
          disabled={!onClear || receipts.length === 0}
          style={{
            background: receipts.length === 0 ? '#adb5bd' : '#dc3545',
            color: 'white',
            border: 'none',
            padding: '8px 10px',
            borderRadius: '6px',
            cursor: !onClear || receipts.length === 0 ? 'not-allowed' : 'pointer'
          }}
        >
          Clear
        </button>
      </div>

      {receipts.length === 0 ? (
        <div style={{ marginTop: '10px', fontSize: '13px', color: '#666' }}>
          No confirmed payments yet.
        </div>
      ) : (
        <div style={{ marginTop: '10px', display: 'grid', gap: '10px' }}>
          {receipts.map(r => (
            <div key={r.id} style={{
              border: '1px solid #e9ecef',
              borderRadius: '8px',
              padding: '10px',
              background: '#f8f9fa'
            }}>
              <div style={{ fontSize: '13px', color: '#333' }}><strong>Amount:</strong> {r.amount}</div>
              <div style={{ marginTop: '6px', fontSize: '12px', color: '#444' }}><strong>Intent:</strong> {r.intentId}</div>
              <div style={{ marginTop: '6px', fontSize: '12px', color: '#444' }}><strong>From:</strong> {r.payerDid}</div>
              <div style={{ marginTop: '6px', fontSize: '12px', color: '#444' }}><strong>To:</strong> {r.receiverDid}</div>
              <div style={{ marginTop: '6px', fontSize: '12px', color: '#666' }}><strong>Confirmed:</strong> {r.confirmedAt}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
