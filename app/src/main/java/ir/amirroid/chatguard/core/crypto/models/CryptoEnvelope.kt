package ir.amirroid.chatguard.core.crypto.models

/**
 * Compact encrypted message payload.
 *
 * Encryption uses a shared secret derived once from both users' long-term identity keys
 * (ECDH), then per-message randomness comes from the AEAD nonce. Both the sender and
 * the recipient derive the same key material, so the sender can read their own messages
 * without a second envelope or key-wrapping layer.
 *
 * Serialized wire format (v2): 1-byte version, 12-byte nonce, 4-byte ciphertext length,
 * ciphertext, 16-byte GCM authentication tag. Fixed overhead is 33 bytes plus plaintext size.
 */
data class CryptoEnvelope(
    /** Random 12-byte value used as HKDF salt and AES-GCM nonce. */
    val nonce: ByteArray,

    /** AES-GCM ciphertext (plaintext length; tag stored separately). */
    val ciphertext: ByteArray,

    /** 16-byte GCM authentication tag; rejects tampered ciphertext on decrypt. */
    val authTag: ByteArray?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CryptoEnvelope
        if (!nonce.contentEquals(other.nonce)) return false
        if (!ciphertext.contentEquals(other.ciphertext)) return false
        if (!authTag.contentEquals(other.authTag)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = nonce.contentHashCode()
        result = 31 * result + ciphertext.contentHashCode()
        result = 31 * result + (authTag?.contentHashCode() ?: 0)
        return result
    }
}
