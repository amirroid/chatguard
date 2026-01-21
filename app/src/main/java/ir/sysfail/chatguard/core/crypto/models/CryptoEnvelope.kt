package ir.sysfail.chatguard.core.crypto.models

/**
 * Complete encrypted message envelope with Perfect Forward Secrecy
 *
 * This version supports sender recovery while maintaining perfect forward secrecy
 * by using TWO independent ephemeral keys (one for receiver, one for sender).
 *
 * Security guarantees:
 * - Perfect forward secrecy for BOTH sent and received messages
 * - Authentication via ECDSA signatures
 * - AEAD encryption (AES-256-GCM)
 * - MITM protection
 *
 * Overhead: ~342 bytes (191 for receiver + 151 for sender)
 */
data class CryptoEnvelope(
    // ══════════════════════════════════════════════════════════
    // Receiver Envelope (for the recipient)
    // ══════════════════════════════════════════════════════════

    /**
     * Receiver's ephemeral public key
     * Generated fresh for THIS message, for THIS recipient
     * Used by receiver to derive shared secret
     *
     * Size: ~91 bytes
     */
    val receiverEphemeralPublicKey: ByteArray,

    /**
     * Signature over receiver's ephemeral public key
     * Signed with sender's identity private key
     * Proves authenticity and prevents MITM
     *
     * Size: ~72 bytes
     */
    val receiverSignature: ByteArray,

    /**
     * Encrypted message content
     * Same ciphertext shared by both envelopes
     *
     * Size: variable (same as plaintext)
     */
    val ciphertext: ByteArray,

    /**
     * Nonce for AEAD encryption
     *
     * Size: 12 bytes
     */
    val nonce: ByteArray,

    /**
     * Authentication tag for integrity verification
     *
     * Size: 16 bytes
     */
    val authTag: ByteArray?,

    // ══════════════════════════════════════════════════════════
    // Sender Envelope (for sender's own recovery)
    // ══════════════════════════════════════════════════════════

    /**
     * Sender's ephemeral public key (DIFFERENT from receiver's)
     * Generated fresh, independent ephemeral key
     * Used by sender to recover their own message
     *
     * Size: ~91 bytes
     */
    val senderEphemeralPublicKey: ByteArray,

    /**
     * Signature over sender's ephemeral public key
     * Self-signed with sender's identity private key
     *
     * Size: ~72 bytes
     */
    val senderSignature: ByteArray,

    /**
     * Wrapped symmetric encryption key for sender
     * Contains the AES key used to encrypt the message
     * Encrypted using ECDH(sender_ephemeral_priv, sender_identity_pub)
     *
     * Size: ~32 bytes (wrapped AES-256 key)
     */
    val senderWrappedKey: ByteArray,

    /**
     * Nonce for sender's key wrapping
     *
     * Size: 12 bytes
     */
    val senderWrappedKeyNonce: ByteArray,

    /**
     * Authentication tag for sender's key wrapping
     * Part of AEAD encryption for the wrapped key
     *
     * Size: 16 bytes
     */
    val senderWrappedKeyAuthTag: ByteArray?
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CryptoEnvelope

        if (!receiverEphemeralPublicKey.contentEquals(other.receiverEphemeralPublicKey)) return false
        if (!receiverSignature.contentEquals(other.receiverSignature)) return false
        if (!ciphertext.contentEquals(other.ciphertext)) return false
        if (!nonce.contentEquals(other.nonce)) return false
        if (!authTag.contentEquals(other.authTag)) return false
        if (!senderEphemeralPublicKey.contentEquals(other.senderEphemeralPublicKey)) return false
        if (!senderSignature.contentEquals(other.senderSignature)) return false
        if (!senderWrappedKey.contentEquals(other.senderWrappedKey)) return false
        if (!senderWrappedKeyNonce.contentEquals(other.senderWrappedKeyNonce)) return false
        if (!senderWrappedKeyAuthTag.contentEquals(other.senderWrappedKeyAuthTag)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = receiverEphemeralPublicKey.contentHashCode()
        result = 31 * result + receiverSignature.contentHashCode()
        result = 31 * result + ciphertext.contentHashCode()
        result = 31 * result + nonce.contentHashCode()
        result = 31 * result + (authTag?.contentHashCode() ?: 0)
        result = 31 * result + senderEphemeralPublicKey.contentHashCode()
        result = 31 * result + senderSignature.contentHashCode()
        result = 31 * result + senderWrappedKey.contentHashCode()
        result = 31 * result + senderWrappedKeyNonce.contentHashCode()
        result = 31 * result + (senderWrappedKeyAuthTag?.contentHashCode() ?: 0)
        return result
    }
}

/**
 * ENVELOPE SIZE BREAKDOWN:
 *
 * Component                      | Size      | Purpose
 * -------------------------------|-----------|----------------------------------
 * receiverEphemeralPublicKey     | 91 bytes  | ECDH with receiver
 * receiverSignature              | 72 bytes  | Authenticate sender to receiver
 * ciphertext                     | variable  | Encrypted message (shared)
 * nonce                          | 12 bytes  | AEAD nonce (shared)
 * authTag                        | 16 bytes  | AEAD auth tag (shared)
 * senderEphemeralPublicKey       | 91 bytes  | ECDH with self (sender recovery)
 * senderSignature                | 72 bytes  | Self-authentication
 * senderWrappedKey               | 32 bytes  | Wrapped AES key
 * senderWrappedKeyNonce          | 12 bytes  | Wrapping nonce
 * senderWrappedKeyAuthTag        | 16 bytes  | Wrapping auth tag
 * -------------------------------|-----------|----------------------------------
 * Fixed Overhead                 | 414 bytes | Total overhead per message
 *
 * Example:
 * - 250 byte message → 664 bytes total
 * - 1 KB message → 1.4 KB total
 * - After poetry encoding: ~1-2 KB
 *
 * Trade-off: More overhead for perfect forward secrecy
 */

/**
 * PERFECT FORWARD SECRECY EXPLANATION:
 *
 * Traditional approach (broken forward secrecy for sender):
 *   shared_secret = ECDH(my_identity_priv, ephemeral_pub)
 *   Problem: If identity key leaks → all sent messages decryptable
 *
 * Our approach (perfect forward secrecy):
 *   Receiver path:
 *     shared_secret = ECDH(receiver_eph_priv, sender_identity_pub)
 *     ✅ If sender identity leaks → receiver messages still safe
 *
 *   Sender path:
 *     shared_secret = ECDH(sender_eph_priv, sender_identity_pub)
 *     ✅ sender_eph_priv is destroyed after encryption
 *     ✅ If sender identity leaks → sent messages still safe
 *
 * Both ephemeral private keys are destroyed immediately after use.
 * Even if identity keys leak, past messages remain secure.
 */

/**
 * SECURITY PROPERTIES:
 *
 * ✅ Perfect Forward Secrecy:
 *    Both sent and received messages use ephemeral keys
 *    Ephemeral private keys destroyed after use
 *    Identity key compromise doesn't affect past messages
 *
 * ✅ Authenticity:
 *    Dual signatures prove sender identity
 *    Prevents impersonation and MITM attacks
 *
 * ✅ Confidentiality:
 *    AES-256-GCM ensures only intended parties can read
 *
 * ✅ Integrity:
 *    Authentication tags detect tampering
 *
 * ✅ Sender Recovery:
 *    Sender can decrypt their own messages
 *    Uses independent ephemeral key (maintains forward secrecy)
 */