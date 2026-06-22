# ChatGuard: Simple End-to-End Encryption

## Overview

ChatGuard is a privacy bridge: it encrypts messages on your device and disguises them as poetic Persian text so you can send them through existing apps (Eitaa, Bale, Soroush, Telegram, and similar). Keys stay on your phone. There are no ChatGuard servers or accounts.

The design favors **small messages** and **simple code** over Signal-level features such as perfect forward secrecy or double ratchets.

## How It Works

### Key exchange (once per contact)

1. Each user generates a **long-term identity keypair** (stored locally).
2. Users exchange **public keys** over a trusted channel (often the same chat app).
3. Public keys can be encoded as poetic text (steganography) before sharing.

Public keys may be **signed** with the owner’s identity key so the recipient can detect tampering during exchange. That signature is separate from message encryption.

### Sending a message (Alice → Bob)

1. Compute a **shared secret**: ECDH(Alice’s private key, Bob’s public key).
2. Derive a conversation AES-256 key with **HKDF-SHA256** (shared secret + fixed salt).
3. Encrypt the plaintext with **AES-256-GCM** using a random **12-byte nonce** per message.
5. Build a compact envelope: `nonce | ciphertext | auth tag`.
6. Serialize the envelope to bytes, then encode as **poetic text** for sending.

### Reading a message

**Bob (recipient):**

1. Decode poetic text → binary envelope.
2. Compute the same shared secret: ECDH(Bob’s private key, Alice’s public key).
3. Derive the message key with the envelope nonce.
4. Decrypt with AES-GCM (tampering fails the auth tag).

**Alice (re-reading what she sent):**

Uses the **same steps** as Bob. ECDH(Alice’s private key, Bob’s public key) equals ECDH(Bob’s private key, Alice’s public key), so no second envelope or key-wrapping layer is needed.

## Message envelope (v2)

| Field        | Size        | Role                          |
|-------------|-------------|-------------------------------|
| Version     | 1 byte      | Format identifier (v2)        |
| Nonce       | 12 bytes    | HKDF salt + GCM IV            |
| Ciphertext  | Variable    | Encrypted message             |
| Auth tag    | 16 bytes    | GCM integrity                 |

**Fixed overhead:** 33 bytes (+ poetic encoding expansion).

Compared with the previous dual-ephemeral design (~400+ bytes of crypto overhead per message), payloads are much smaller, so more plaintext fits under messenger length limits.

## Cryptographic building blocks

| Component        | Choice              | Purpose                          |
|------------------|---------------------|----------------------------------|
| Key agreement    | ECDH (P-256)        | Shared secret per peer pair      |
| Key derivation   | HKDF-SHA256         | Per-message AES key from nonce   |
| Encryption       | AES-256-GCM         | Confidentiality + integrity      |
| Key exchange auth| ECDSA (optional)    | Sign exported public keys only   |

## Security properties

**What you get**

- Messaging platforms and their servers cannot read ciphertext without your private key.
- GCM authentication tags detect modified ciphertext.
- Each message uses a fresh nonce, so identical plaintexts produce different ciphertexts.

**What you do not get**

- **Perfect forward secrecy:** If a long-term private key is stolen, past messages encrypted with that peer can be decrypted. Ephemeral per-message keys were removed to save space.
- **Group chat:** One-to-one only.
- **MITM after bad key exchange:** You must verify public keys out-of-band (fingerprints, QR, in-person).

## Poetic encoding

Binary envelopes are embedded in Persian word sequences (word index = data). Encoding efficiency depends on corpus size (more words → fewer words per byte). This layer is unchanged in purpose; smaller envelopes mean shorter poems.

## Limitations

- One-to-one chats only.
- Not a replacement for Signal or WhatsApp’s full protocol stack.
- Security depends on safe initial public-key exchange.

---

**Bottom line:** ChatGuard adds a lightweight ECDH + AES-GCM privacy layer and poetic disguise on top of apps you already use, with minimal per-message overhead.
