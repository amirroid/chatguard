package ir.sysfail.chatguard.core.crypto.abstraction

import ir.sysfail.chatguard.core.crypto.models.EphemeralKeyPair
import ir.sysfail.chatguard.core.crypto.models.IdentityKeyPair
import ir.sysfail.chatguard.core.crypto.models.PublicKey

/**
 * Manages identity and ephemeral key pairs
 */
interface KeyManager {

    // Generate long-term identity key pair (once per installation)
    suspend fun generateIdentityKeyPair(): Result<IdentityKeyPair>

    // Generate ephemeral key pair (per message, for forward secrecy)
    suspend fun generateEphemeralKeyPair(): Result<EphemeralKeyPair>

    // Reconstruct public key from bytes
    suspend fun reconstructPublicKey(encodedKey: ByteArray): Result<PublicKey>

    // Serialize public key to transferable bytes
    suspend fun serializePublicKey(publicKey: PublicKey): Result<ByteArray>

    // Calculate fingerprint for manual verification
    suspend fun calculateFingerprint(publicKey: PublicKey): Result<String>

    // Validate key pair bytes by attempting to reconstruct the keys
    fun validateKeyPair(privateKeyBytes: ByteArray, publicKeyBytes: ByteArray): Boolean
}