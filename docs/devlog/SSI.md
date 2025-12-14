DID -> recovery phrase yielding different private key

```
Wallet Phrase 1 - sell stomach fuel language hospital man picnic lava device canyon flavor survey
Original DID  1 - did:key:z82T5WZ51U4XQp8vPSKdZ8xfcjsqigVPGPa8prPdo6RpcQVwxdLGpCVPyS64zt1obdwBUsGiKZZoZrj1Sv7wCSnieVVBL
Recovered DID 1 - did:key:zQebf4z2EYGhEkL7RBV1tdLujQ6HXhP5WC4D4ZVjuFMK2NWPg

Wallet Phrase 2 - scan mean labor short slender pair slight pistol crowd entry weather rebuild
Original DID  2 - did:key:z82T5XQkPhUwP6biGT5jmMdbr4nvbmFGCSUbjeKv27aLS5sRBVEb2oY86xuxy9S2cGUL8WMdnjGFa7q4pq4tMinBjjmL5
Recovered DID 2 - did:key:zQebf6J9FhNVAmuvC9vCxH2upYds7AqUX3tMdPBz7sUcex45N

Wallet Phrase 3 - jazz armed tiny very glance identify shiver company poem tongue label adult
Original DID  3 - did:key:z82T5XJqtDGzz7q9SzDfTixiHS89WMVm23m73ugRNxPbHiQ7hZYLdXpNDneFkKDBmezpRgneZ9uCzQpXNQFTxtTvf1J1B
Recovered DID 3 - did:key:zQebfCuzPohie8hRzPjJ8SdAF5ZEfyDAmrrPTqXcqsnTSvYwi
```

## Recovered DID differs from original DID
Root cause #1 (main): When creating a wallet we were doing:
Create random wallet walletA → DID derived from walletA.privateKey
Then generate another random mnemonic from walletB (generateMnemonic())
Store DID/keys from walletA but store mnemonic from walletB
So when I later imported the mnemonic, I was restoring walletB, not walletA → I get a different private key → different DID.
That exactly matches logs: the recovered DIDs are shorter/different because they’re derived from a different key.

Root cause #2: We also had inconsistent public key derivation in different code paths (one path used SigningKey.computePublicKey, another used a wallet field). That can also cause mismatches even with the same private key.

Changes:

DIDManager.createDID() now returns { did, keyPair, mnemonic } where mnemonic is from the same wallet used to create the DID.
fromMnemonic() now derives the public key using the same method (ethers.SigningKey.computePublicKey(privateKey, false)) to match create/load.
Onboarding now uses the mnemonic returned from createDID() and stores that exact mnemonic.
Result: Importing the saved phrase should now recreate the same private key → same DID.

FIX: current localStorage likely contains “bad” entries created with the old logic (wrong mnemonic saved). Those can’t be “fixed” automatically because the stored phrase truly corresponds to a different wallet.

FIX: Clean reset + re-test steps
In the wallet UI click Clear Wallet (or clear site data in DevTools).
Create a new wallet again.
Save the shown phrase.
Import it after clearing, and verify the DID matches.
