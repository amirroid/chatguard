# ChatGuard: End-to-End Encryption with Perfect Forward Secrecy

## Overview

ChatGuard encrypts messages that can be sent through any messaging platform (Eitaa, Bale, Soroush). It stores keys locally on your device—no servers, no accounts. Messages are disguised as poetic text to avoid detection.

## How It Works

### Key Exchange

1. Each user generates an **identity keypair** (one-time, stored locally)
2. Users exchange **public keys** through a trusted channel
3. Public keys are encoded as poetic text and shared directly peer-to-peer

### Sending a Message (Alice → Bob)

**For Bob to decrypt:**
1. Generate temporary **ephemeral keypair #1**
2. Combine ephemeral private key with Bob's public key (ECDH)
3. Derive encryption key (HKDF)
4. Encrypt message (AES-256-GCM)
5. Sign the ephemeral public key (proves it's from Alice)
6. **Destroy ephemeral private key**

**For Alice to re-read later:**
1. Generate separate **ephemeral keypair #2**
2. Combine ephemeral private key with Alice's own public key (ECDH)
3. Derive wrapping key (HKDF)
4. Encrypt the encryption key from step 3
5. Sign the ephemeral public key
6. **Destroy ephemeral private key**

**Result:** One message envelope containing both encrypted paths, converted to poetic text and sent.

### Receiving a Message

**Bob decrypts:**
1. Decode poetic text to binary envelope
2. Verify Alice's signature (confirms sender)
3. Combine his private key with ephemeral public key
4. Derive decryption key
5. Decrypt message

**Alice re-reads:**
1. Verify her own signature
2. Combine her private key with sender's ephemeral public key
3. Decrypt wrapped encryption key
4. Use it to decrypt the message

## Message Envelope Contains

- **Receiver path:** ephemeral public key, signature, ciphertext, nonce, auth tag
- **Sender path:** ephemeral public key, signature, wrapped key, nonce, auth tag

Each component serves authentication, encryption, or integrity verification.

## Security Properties

**Forward Secrecy:** If Alice's phone is stolen months later, past messages remain secure—the ephemeral private keys needed to decrypt them were destroyed after sending.

**Authentication:** Signatures prove who sent each message, preventing impersonation.

**Integrity:** Authentication tags detect any tampering with ciphertext.

**No Server Trust:** Everything happens on-device; no third party can access keys or plaintext.

## Key Innovation

Standard encryption with sender-copy breaks forward secrecy. ChatGuard uses **two independent ephemeral keypairs**—one for receiver, one for sender recovery—both destroyed after encryption. This achieves forward secrecy even for messages you sent yourself.

## Cryptographic Algorithms

- ECDH (P-256 curve) for key agreement
- HKDF-SHA256 for key derivation
- AES-256-GCM for encryption
- ECDSA-SHA256 for signatures

## Limitations

- **No group chat:** Designed for one-to-one messaging only

---

**Bottom line:** ChatGuard provides Signal-level encryption over any text channel, with the unique ability to decrypt your own sent messages while maintaining perfect forward secrecy.