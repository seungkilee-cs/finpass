import { KeyPair } from '../core/DIDManager';

export interface StoredKeys {
  privateKey: string;
  publicKey: string;
  address: string;
  did: string;
  mnemonic?: string;
  createdAt: string;
}

export interface StoredCredential {
  credId: string;
  issuerDid: string;
  status: string;
  credentialJwt: string;
  commitmentHash: string;
  commitmentJwt: string;
  holderDid: string;
  receivedAt: string;
}

export class StorageService {
  private static readonly KEYS_STORAGE_KEY = 'finpass_wallet_keys';
  private static readonly MNEMONIC_STORAGE_KEY = 'finpass_wallet_mnemonic';
  private static readonly CREDENTIALS_STORAGE_KEY = 'finpass_wallet_credentials';

  /**
   * Save keys to localStorage (encrypted for MVP)
   */
  static saveKeys(keys: KeyPair, did: string, mnemonic?: string): void {
    try {
      const storedKeys: StoredKeys = {
        ...keys,
        did,
        mnemonic,
        createdAt: new Date().toISOString()
      };

      // For MVP, we'll store as JSON (in production, use proper encryption)
      const encrypted = this.simpleEncrypt(JSON.stringify(storedKeys));
      localStorage.setItem(this.KEYS_STORAGE_KEY, encrypted);

      if (mnemonic) {
        const encryptedMnemonic = this.simpleEncrypt(mnemonic);
        localStorage.setItem(this.MNEMONIC_STORAGE_KEY, encryptedMnemonic);
      }
    } catch (error) {
      console.error('Failed to save keys:', error);
      throw new Error('Failed to save wallet keys');
    }
  }

  /**
   * Load keys from localStorage
   */
  static loadKeys(): StoredKeys | null {
    try {
      const encrypted = localStorage.getItem(this.KEYS_STORAGE_KEY);
      if (!encrypted) return null;

      const decrypted = this.simpleDecrypt(encrypted);
      return JSON.parse(decrypted) as StoredKeys;
    } catch (error) {
      console.error('Failed to load keys:', error);
      return null;
    }
  }

  /**
   * Clear all stored keys
   */
  static clearKeys(): void {
    localStorage.removeItem(this.KEYS_STORAGE_KEY);
    localStorage.removeItem(this.MNEMONIC_STORAGE_KEY);
    localStorage.removeItem(this.CREDENTIALS_STORAGE_KEY);
  }

  static storeCredential(credential: StoredCredential): void {
    try {
      const existing = this.getCredentials();
      const withoutDup = existing.filter(c => c.credId !== credential.credId);
      const updated = [credential, ...withoutDup];
      const encrypted = this.simpleEncrypt(JSON.stringify(updated));
      localStorage.setItem(this.CREDENTIALS_STORAGE_KEY, encrypted);
    } catch (error) {
      console.error('Failed to store credential:', error);
      throw new Error('Failed to store credential');
    }
  }

  static getCredentials(): StoredCredential[] {
    try {
      const encrypted = localStorage.getItem(this.CREDENTIALS_STORAGE_KEY);
      if (!encrypted) return [];
      const decrypted = this.simpleDecrypt(encrypted);
      const parsed = JSON.parse(decrypted);
      if (!Array.isArray(parsed)) return [];
      return parsed as StoredCredential[];
    } catch (error) {
      console.error('Failed to load credentials:', error);
      return [];
    }
  }

  static getCredentialById(credId: string): StoredCredential | null {
    const creds = this.getCredentials();
    return creds.find(c => c.credId === credId) || null;
  }

  /**
   * Export mnemonic phrase
   */
  static exportMnemonic(): string | null {
    try {
      const encrypted = localStorage.getItem(this.MNEMONIC_STORAGE_KEY);
      if (!encrypted) return null;

      return this.simpleDecrypt(encrypted);
    } catch (error) {
      console.error('Failed to export mnemonic:', error);
      return null;
    }
  }

  /**
   * Import from mnemonic phrase
   */
  static async importFromMnemonic(mnemonic: string): Promise<StoredKeys> {
    try {
      // Validate mnemonic format (basic check)
      if (!mnemonic || mnemonic.split(' ').length < 12) {
        throw new Error('Invalid mnemonic phrase');
      }

      // Clear existing keys
      this.clearKeys();

      // Import using DIDManager
      const { did, keyPair } = await import('../core/DIDManager').then(module => 
        module.DIDManager.fromMnemonic(mnemonic)
      );

      const storedKeys: StoredKeys = {
        ...keyPair,
        did,
        mnemonic,
        createdAt: new Date().toISOString()
      };

      this.saveKeys(keyPair, did, mnemonic);
      return storedKeys;
    } catch (error) {
      console.error('Failed to import from mnemonic:', error);
      throw new Error('Failed to import wallet from mnemonic');
    }
  }

  /**
   * Check if wallet exists
   */
  static walletExists(): boolean {
    return localStorage.getItem(this.KEYS_STORAGE_KEY) !== null;
  }

  /**
   * Get wallet info
   */
  static getWalletInfo(): { did: string; createdAt: string } | null {
    const keys = this.loadKeys();
    if (!keys) return null;

    return {
      did: keys.did,
      createdAt: keys.createdAt
    };
  }

  /**
   * Simple encryption for MVP (XOR-based)
   * In production, use proper encryption libraries
   */
  private static simpleEncrypt(data: string): string {
    // Simple XOR encryption with a fixed key (MVP only)
    const key = 'finpass-mvp-key-2024';
    let encrypted = '';
    
    for (let i = 0; i < data.length; i++) {
      encrypted += String.fromCharCode(
        data.charCodeAt(i) ^ key.charCodeAt(i % key.length)
      );
    }
    
    return btoa(encrypted); // Base64 encode
  }

  /**
   * Simple decryption for MVP
   */
  private static simpleDecrypt(encryptedData: string): string {
    try {
      const key = 'finpass-mvp-key-2024';
      const decoded = atob(encryptedData); // Base64 decode
      let decrypted = '';
      
      for (let i = 0; i < decoded.length; i++) {
        decrypted += String.fromCharCode(
          decoded.charCodeAt(i) ^ key.charCodeAt(i % key.length)
        );
      }
      
      return decrypted;
    } catch (error) {
      console.error('Decryption failed:', error);
      throw new Error('Failed to decrypt wallet data');
    }
  }

  /**
   * Backup wallet data as JSON
   */
  static exportWallet(): string | null {
    try {
      const keys = this.loadKeys();
      if (!keys) return null;

      // Create backup object
      const backup = {
        version: '1.0',
        exportedAt: new Date().toISOString(),
        wallet: {
          did: keys.did,
          privateKey: keys.privateKey,
          publicKey: keys.publicKey,
          address: keys.address,
          mnemonic: keys.mnemonic,
          createdAt: keys.createdAt
        }
      };

      return JSON.stringify(backup, null, 2);
    } catch (error) {
      console.error('Failed to export wallet:', error);
      return null;
    }
  }

  /**
   * Import wallet from backup JSON
   */
  static async importWallet(backupJson: string): Promise<StoredKeys> {
    try {
      const backup = JSON.parse(backupJson);
      
      if (!backup.wallet || !backup.wallet.privateKey || !backup.wallet.did) {
        throw new Error('Invalid backup format');
      }

      // Validate the backup by recreating the DID
      const { did: restoredDid, keyPair } = await import('../core/DIDManager').then(module => 
        module.DIDManager.loadDID(backup.wallet.privateKey)
      );

      if (restoredDid !== backup.wallet.did) {
        throw new Error('Backup integrity check failed');
      }

      const storedKeys: StoredKeys = {
        privateKey: backup.wallet.privateKey,
        publicKey: backup.wallet.publicKey,
        address: backup.wallet.address,
        did: backup.wallet.did,
        mnemonic: backup.wallet.mnemonic,
        createdAt: backup.wallet.createdAt
      };

      this.saveKeys(keyPair, backup.wallet.did, backup.wallet.mnemonic);
      return storedKeys;
    } catch (error) {
      console.error('Failed to import wallet backup:', error);
      throw new Error('Failed to import wallet backup');
    }
  }
}
