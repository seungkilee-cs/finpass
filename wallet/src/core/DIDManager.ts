import { ethers } from 'ethers';
import * as jose from 'jose';

export interface KeyPair {
  privateKey: string;
  publicKey: string;
  address: string;
}

export interface DIDDocument {
  '@context': string[];
  id: string;
  verificationMethod: Array<{
    id: string;
    type: string;
    controller: string;
    publicKeyJwk: JsonWebKey;
  }>;
  authentication: string[];
  assertionMethod: string[];
}

export class DIDManager {
  private static readonly DID_METHOD = 'did:key';
  private static readonly KEY_TYPE = 'Ed25519';
  
  /**
   * Generate a new Ed25519 key pair and create a DID
   */
  static async createDID(): Promise<{ did: string; keyPair: KeyPair }> {
    // Generate Ed25519 key pair using ethers
    const wallet = ethers.Wallet.createRandom();
    
    // Get the public key from the private key using the correct ethers method
    const publicKeyBytes = ethers.SigningKey.computePublicKey(wallet.privateKey, false);
    
    const keyPair: KeyPair = {
      privateKey: wallet.privateKey,
      publicKey: publicKeyBytes,
      address: wallet.address
    };

    // Create did:key format
    const pubKeyBytes = ethers.getBytes(publicKeyBytes);
    const multicodecKey = new Uint8Array([0xed, 0x01]);
    const combinedKey = new Uint8Array(multicodecKey.length + pubKeyBytes.length);
    combinedKey.set(multicodecKey);
    combinedKey.set(pubKeyBytes, multicodecKey.length);
    const multibaseKey = this.bytesToBase58btc(combinedKey);
    const did = `${this.DID_METHOD}:${multibaseKey}`;

    return { did, keyPair };
  }

  /**
   * Load DID from stored keys
   */
  static async loadDID(privateKey: string): Promise<{ did: string; keyPair: KeyPair }> {
    const wallet = new ethers.Wallet(privateKey);
    
    // Get the public key from the private key using the correct ethers method
    const publicKeyBytes = ethers.SigningKey.computePublicKey(privateKey, false);
    
    const keyPair: KeyPair = {
      privateKey: wallet.privateKey,
      publicKey: publicKeyBytes,
      address: wallet.address
    };

    // Recreate did:key format
    const pubKeyBytes = ethers.getBytes(publicKeyBytes);
    const multicodecKey = new Uint8Array([0xed, 0x01]);
    const combinedKey = new Uint8Array(multicodecKey.length + pubKeyBytes.length);
    combinedKey.set(multicodecKey);
    combinedKey.set(pubKeyBytes, multicodecKey.length);
    const multibaseKey = this.bytesToBase58btc(combinedKey);
    const did = `${this.DID_METHOD}:${multibaseKey}`;

    return { did, keyPair };
  }

  /**
   * Get DID string from key pair
   */
  static getDID(keyPair: KeyPair): string {
    const publicKeyBytes = ethers.getBytes(keyPair.publicKey);
    const multicodecKey = new Uint8Array([0xed, 0x01]);
    const combinedKey = new Uint8Array(multicodecKey.length + publicKeyBytes.length);
    combinedKey.set(multicodecKey);
    combinedKey.set(publicKeyBytes, multicodecKey.length);
    const multibaseKey = this.bytesToBase58btc(combinedKey);
    return `${this.DID_METHOD}:${multibaseKey}`;
  }

  /**
   * Sign a payload with the private key
   */
  static async signPayload(privateKey: string, payload: any): Promise<string> {
    const wallet = new ethers.Wallet(privateKey);
    const message = JSON.stringify(payload);
    const signature = await wallet.signMessage(message);
    return signature;
  }

  /**
   * Verify a signature
   */
  static async verifySignature(publicKey: string, payload: any, signature: string): Promise<boolean> {
    try {
      const wallet = new ethers.Wallet(publicKey);
      const message = JSON.stringify(payload);
      const recoveredAddress = ethers.verifyMessage(message, signature);
      return recoveredAddress.toLowerCase() === wallet.address.toLowerCase();
    } catch (error) {
      console.error('Signature verification failed:', error);
      return false;
    }
  }

  /**
   * Generate a mnemonic phrase for backup/restore
   */
  static generateMnemonic(): string {
    const wallet = ethers.Wallet.createRandom();
    return (wallet as any).mnemonic?.phrase || '';
  }

  /**
   * Create wallet from mnemonic
   */
  static async fromMnemonic(mnemonic: string): Promise<{ did: string; keyPair: KeyPair }> {
    const wallet = ethers.Wallet.fromPhrase(mnemonic);
    
    const keyPair: KeyPair = {
      privateKey: wallet.privateKey,
      publicKey: wallet.publicKey,
      address: wallet.address
    };

    const did = this.getDID(keyPair);
    return { did, keyPair };
  }

  /**
   * Convert bytes to base58btc (simplified implementation)
   */
  private static bytesToBase58btc(bytes: Uint8Array): string {
    // This is a simplified base58 implementation
    // In production, use a proper base58btc library
    const alphabet = '123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz';
    let num = BigInt(0);
    
    for (let i = 0; i < bytes.length; i++) {
      const byte = bytes[i];
      num = (num << BigInt(8)) | BigInt(byte);
    }
    
    let result = '';
    while (num > BigInt(0)) {
      result = alphabet[Number(num % BigInt(58))] + result;
      num = num / BigInt(58);
    }
    
    // Add 'z' prefix for base58btc
    return 'z' + result;
  }

  /**
   * Create DID Document (simplified)
   */
  static createDIDDocument(did: string, publicKey: string): DIDDocument {
    // Convert public key to JWK format (simplified)
    const publicKeyJwk: JsonWebKey = {
      kty: 'OKP',
      crv: 'Ed25519',
      x: Buffer.from(ethers.getBytes(publicKey)).toString('base64url')
    };

    return {
      '@context': ['https://www.w3.org/ns/did/v1'],
      id: did,
      verificationMethod: [
        {
          id: `${did}#key-1`,
          type: 'Ed25519VerificationKey2018',
          controller: did,
          publicKeyJwk
        }
      ],
      authentication: [`${did}#key-1`],
      assertionMethod: [`${did}#key-1`]
    };
  }
}
