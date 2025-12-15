import axios from 'axios';

export interface CreateIntentResponse {
  intentId: string;
  status: string;
  amount: number;
  payerDid: string;
  receiverDid: string;
  kycVerified: boolean;
}

export interface PaymentIntentResponse {
  intentId: string;
  status: string;
  amount: number;
  payerDid: string;
  receiverDid: string;
  kycVerified: boolean;
}

export interface ConfirmPaymentResponse {
  intentId: string;
  status: string;
  amount: number;
  payerDid: string;
  receiverDid: string;
  payerBalance: number;
  receiverBalance: number;
}

export interface BalanceResponse {
  did: string;
  balance: number;
}

function normalizeBaseUrl(url: string): string {
  return url.replace(/\/$/, '');
}

export class PaymentClient {
  static async createIntent(paymentBaseUrl: string, payerDid: string, receiverDid: string, amount: number): Promise<CreateIntentResponse> {
    const base = normalizeBaseUrl(paymentBaseUrl);
    const resp = await axios.post(`${base}/payments/intents`, {
      payerDid,
      receiverDid,
      amount
    }, {
      headers: { 'Content-Type': 'application/json' }
    });
    return resp.data as CreateIntentResponse;
  }

  static async getIntent(paymentBaseUrl: string, intentId: string): Promise<PaymentIntentResponse> {
    const base = normalizeBaseUrl(paymentBaseUrl);
    const resp = await axios.get(`${base}/payments/intents/${intentId}`);
    return resp.data as PaymentIntentResponse;
  }

  static async attachKyc(paymentBaseUrl: string, intentId: string, decisionToken: string): Promise<PaymentIntentResponse> {
    const base = normalizeBaseUrl(paymentBaseUrl);
    const resp = await axios.post(`${base}/payments/${intentId}/verify-kyc`, {
      decisionToken
    }, {
      headers: { 'Content-Type': 'application/json' }
    });
    return resp.data as PaymentIntentResponse;
  }

  static async confirmPayment(paymentBaseUrl: string, intentId: string): Promise<ConfirmPaymentResponse> {
    const base = normalizeBaseUrl(paymentBaseUrl);
    const resp = await axios.post(`${base}/payments/${intentId}/confirm`, {}, {
      headers: { 'Content-Type': 'application/json' }
    });
    return resp.data as ConfirmPaymentResponse;
  }

  static async getBalance(paymentBaseUrl: string, did: string): Promise<BalanceResponse> {
    const base = normalizeBaseUrl(paymentBaseUrl);
    const resp = await axios.get(`${base}/payments/balance/${encodeURIComponent(did)}`);
    return resp.data as BalanceResponse;
  }
}
