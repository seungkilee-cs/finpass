import axios from 'axios';

import { ZKProofGenerator } from '../core/ZKProofGenerator';
import { StorageService, StoredCredential, StoredDecisionToken } from '../services/storage';

export interface ChallengeResponse {
  challenge: string;
  expiresIn: number;
}

export interface VerifyResponse {
  decisionToken: string;
  assuranceLevel: string;
  verifiedClaims: string[];
  expiresIn: number;
}

function normalizeBaseUrl(url: string): string {
  return url.replace(/\/$/, '');
}

export class VerificationFlow {
  static async verify(credId: string, verifierUrl: string): Promise<StoredDecisionToken> {
    const verifierBase = normalizeBaseUrl(verifierUrl);

    const credential: StoredCredential | null = StorageService.getCredentialById(credId);
    if (!credential) {
      throw new Error('Credential not found');
    }

    const cached = StorageService.getValidDecisionToken({
      credId,
      verifierUrl: verifierBase,
      requiredClaims: ['over_18']
    });
    if (cached) {
      return cached;
    }

    const challengeResp = await axios.get<ChallengeResponse>(`${verifierBase}/verify/challenge`);
    const challenge = challengeResp.data.challenge;

    const { proof, publicSignals } = ZKProofGenerator.generateOver18Proof(credential, challenge);

    const verifyBody = {
      holderDid: credential.holderDid,
      challenge,
      commitmentJwt: credential.commitmentJwt,
      proof,
      publicSignals,
      requestedClaims: ['over_18']
    };

    const verifyResp = await axios.post<VerifyResponse>(`${verifierBase}/verify`, verifyBody, {
      headers: { 'Content-Type': 'application/json' }
    });

    const now = Date.now();
    const expiresMs = (verifyResp.data.expiresIn || 0) * 1000;

    const stored: StoredDecisionToken = {
      credId,
      verifierUrl: verifierBase,
      decisionToken: verifyResp.data.decisionToken,
      assuranceLevel: verifyResp.data.assuranceLevel,
      verifiedClaims: verifyResp.data.verifiedClaims,
      expiresAt: new Date(now + expiresMs).toISOString(),
      receivedAt: new Date(now).toISOString()
    };

    StorageService.storeDecisionToken(stored);
    return stored;
  }
}
