import axios from 'axios';
import { StorageService, StoredCredential } from '../services/storage';
import { ethers } from 'ethers';

export interface IssuerMetadata {
  issuer_did: string;
  credential_endpoint: string;
  credential_endpoint_with_proof?: string;
}

export interface IssueResponse {
  credId: string;
  issuerDid: string;
  status: string;
  credentialJwt: string;
  commitmentHash: string;
  commitmentJwt: string;
}

function canonicalize(value: any): any {
  if (Array.isArray(value)) {
    return value.map(v => canonicalize(v));
  }

  if (value && typeof value === 'object') {
    const keys = Object.keys(value).sort();
    const out: any = {};
    for (const k of keys) {
      out[k] = canonicalize(value[k]);
    }
    return out;
  }

  return value;
}

function canonicalStringify(value: any): string {
  return JSON.stringify(canonicalize(value));
}

export class CredentialHolder {
  static async fetchIssuerMetadata(issuerUrl: string): Promise<IssuerMetadata> {
    const url = `${issuerUrl.replace(/\/$/, '')}/.well-known/openid-credential-issuer`;
    const resp = await axios.get(url);
    return resp.data as IssuerMetadata;
  }

  static async requestCredential(issuerUrl: string, passportData: Record<string, any>): Promise<StoredCredential> {
    const keys = StorageService.loadKeys();
    if (!keys) {
      throw new Error('Wallet not initialized');
    }

    const metadata = await this.fetchIssuerMetadata(issuerUrl);
    const endpoint = (metadata.credential_endpoint_with_proof || `${issuerUrl.replace(/\/$/, '')}/issue-with-proof`).replace(/\/$/, '');

    const proofPayloadObj = {
      holderDid: keys.did,
      holderAddress: keys.address,
      passportData,
      timestamp: new Date().toISOString()
    };

    const proofPayload = canonicalStringify(proofPayloadObj);

    const wallet = new ethers.Wallet(keys.privateKey);
    const proofSignature = await wallet.signMessage(proofPayload);

    const body = {
      holderDid: keys.did,
      passportData,
      holderAddress: keys.address,
      proofPayload,
      proofSignature
    };

    const resp = await axios.post(endpoint, body, {
      headers: { 'Content-Type': 'application/json' }
    });

    const issueResp = resp.data as IssueResponse;
    const stored: StoredCredential = {
      ...issueResp,
      holderDid: keys.did,
      receivedAt: new Date().toISOString()
    };

    StorageService.storeCredential(stored);
    return stored;
  }

  static getCredentials(): StoredCredential[] {
    return StorageService.getCredentials();
  }

  static getCredentialById(credId: string): StoredCredential | null {
    return StorageService.getCredentialById(credId);
  }
}
