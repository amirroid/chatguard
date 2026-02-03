package ir.amirroid.chatguard.core.crypto.abstraction

import ir.amirroid.chatguard.core.crypto.models.CryptoEnvelope
import ir.amirroid.chatguard.core.crypto.models.PrivateKey
import ir.amirroid.chatguard.core.crypto.models.PublicKey

/**
 * High-level cryptographic orchestrator for end-to-end encrypted messaging
 * with perfect forward secrecy for both sent and received messages.
 *
 * This interface coordinates all cryptographic operations:
 * - Key generation and management
 * - ECDH key agreement
 * - AEAD encryption/decryption
 * - Digital signatures
 * - Key derivation
 *
 * Security properties:
 * - Perfect forward secrecy (both directions)
 * - Authentication via digital signatures
 * - Confidentiality via AEAD
 * - Integrity verification
 * - MITM protection
 */
interface CryptoOrchestrator {

    /**
     * Encrypt a message for a recipient with sender recovery capability
     *
     * This creates a dual-envelope structure:
     * 1. Receiver envelope: Standard E2EE for the recipient
     * 2. Sender envelope: Self-recovery with independent ephemeral key
     *
     * Both envelopes maintain perfect forward secrecy by using
     * independent ephemeral keys that are destroyed after encryption.
     *
     * @param plaintext The message to encrypt
     * @param myIdentityPrivateKey Sender's long-term identity private key
     * @param myIdentityPublicKey Sender's long-term identity public key (for self-recovery)
     * @param theirIdentityPublicKey Recipient's long-term identity public key
     * @return Result containing the encrypted envelope or error
     *
     * @throws SecurityException if cryptographic operation fails
     *
     * Example:
     * ```kotlin
     * val envelope = orchestrator.encryptMessage(
     *     plaintext = "Secret message".toByteArray(),
     *     myIdentityPrivateKey = alice.privateKey,
     *     myIdentityPublicKey = alice.publicKey,
     *     theirIdentityPublicKey = bob.publicKey
     * ).getOrThrow()
     * ```
     */
    suspend fun encryptMessage(
        plaintext: ByteArray,
        myIdentityPrivateKey: PrivateKey,
        myIdentityPublicKey: PublicKey,
        theirIdentityPublicKey: PublicKey
    ): Result<CryptoEnvelope>

    /**
     * Decrypt a message envelope
     *
     * Supports two decryption paths:
     * 1. As receiver: Verify sender signature, derive shared secret, decrypt
     * 2. As sender: Verify self-signature, unwrap key, decrypt
     *
     * Both paths verify signatures before decryption to ensure authenticity.
     *
     * @param envelope The encrypted message envelope
     * @param myIdentityPrivateKey My long-term identity private key
     * @param myIdentityPublicKey My long-term identity public key (for sender path)
     * @param theirIdentityPublicKey Sender's identity public key (for receiver path)
     * @param iAmSender True if decrypting own sent message, false if received message
     * @return Result containing plaintext or error
     *
     * @throws SecurityException if signature verification fails
     * @throws SecurityException if decryption fails
     *
     * Example (as receiver):
     * ```kotlin
     * val plaintext = orchestrator.decryptMessage(
     *     envelope = receivedEnvelope,
     *     myIdentityPrivateKey = bob.privateKey,
     *     myIdentityPublicKey = bob.publicKey,
     *     theirIdentityPublicKey = alice.publicKey,
     *     iAmSender = false
     * ).getOrThrow()
     * ```
     *
     * Example (as sender):
     * ```kotlin
     * val plaintext = orchestrator.decryptMessage(
     *     envelope = sentEnvelope,
     *     myIdentityPrivateKey = alice.privateKey,
     *     myIdentityPublicKey = alice.publicKey,
     *     theirIdentityPublicKey = bob.publicKey,
     *     iAmSender = true
     * ).getOrThrow()
     * ```
     */
    suspend fun decryptMessage(
        envelope: CryptoEnvelope,
        myIdentityPrivateKey: PrivateKey,
        myIdentityPublicKey: PublicKey,
        theirIdentityPublicKey: PublicKey,
        iAmSender: Boolean
    ): Result<ByteArray>
}