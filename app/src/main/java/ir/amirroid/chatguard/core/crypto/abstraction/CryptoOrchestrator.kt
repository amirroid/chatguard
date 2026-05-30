package ir.amirroid.chatguard.core.crypto.abstraction

import ir.amirroid.chatguard.core.crypto.models.CryptoEnvelope
import ir.amirroid.chatguard.core.crypto.models.PrivateKey
import ir.amirroid.chatguard.core.crypto.models.PublicKey

/**
 * High-level API for peer-to-peer message encryption.
 *
 * Uses long-term identity keys (ECDH P-256) plus per-message AEAD nonces. This is a
 * practical privacy layer for third-party chat apps, not a full Signal-style protocol.
 */
interface CryptoOrchestrator {

    /**
     * Encrypts [plaintext] for the peer identified by [theirIdentityPublicKey].
     *
     * The returned envelope is small: random nonce, ciphertext, and GCM tag only.
     */
    suspend fun encryptMessage(
        plaintext: ByteArray,
        myIdentityPrivateKey: PrivateKey,
        myIdentityPublicKey: PublicKey,
        theirIdentityPublicKey: PublicKey,
    ): Result<CryptoEnvelope>

    /**
     * Decrypts an envelope produced by [encryptMessage].
     *
     * Sender and receiver use the same key derivation; [iAmSender] is retained for callers
     * that track message direction but does not change the crypto path.
     */
    suspend fun decryptMessage(
        envelope: CryptoEnvelope,
        myIdentityPrivateKey: PrivateKey,
        myIdentityPublicKey: PublicKey,
        theirIdentityPublicKey: PublicKey,
        iAmSender: Boolean,
    ): Result<ByteArray>
}
