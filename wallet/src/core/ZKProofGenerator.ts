import { StoredCredential } from '../services/storage';

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

function computeOver18FromCredential(credential: StoredCredential): boolean {
  const payload = decodeJwtPayload(credential.credentialJwt);
  const subject = payload?.vc?.credentialSubject || null;
  const dob: string | undefined = subject?.dob || subject?.birthDate;
  if (!dob) return false;

  const dobMs = Date.parse(dob);
  if (!Number.isFinite(dobMs)) return false;

  const now = new Date();
  const birth = new Date(dobMs);

  let age = now.getFullYear() - birth.getFullYear();
  const m = now.getMonth() - birth.getMonth();
  if (m < 0 || (m === 0 && now.getDate() < birth.getDate())) {
    age -= 1;
  }

  return age >= 18;
}

export interface ZKProofResult {
  proof: string;
  publicSignals: Record<string, any>;
}

export class ZKProofGenerator {
  static generateOver18Proof(credential: StoredCredential, challenge: string): ZKProofResult {
    const result = computeOver18FromCredential(credential);

    return {
      proof: `poc-proof-${Date.now()}`,
      publicSignals: {
        predicate: 'over_18',
        result,
        challenge
      }
    };
  }
}
