package ir.amirroid.chatguard.core.crypto.abstraction

import ir.amirroid.chatguard.core.crypto.models.DerivedKeyMaterial
import ir.amirroid.chatguard.core.crypto.models.PrivateKey
import ir.amirroid.chatguard.core.crypto.models.PublicKey
import ir.amirroid.chatguard.core.crypto.models.SharedSecret

/**
 * Derives shared secrets using ECDH
 */
interface SharedSecretDeriver {

    // Perform ECDH key exchange
    suspend fun deriveSharedSecret(
        myPrivateKey: PrivateKey,
        theirPublicKey: PublicKey
    ): Result<SharedSecret>

    // Derive encryption key from shared secret using HKDF
    suspend fun deriveEncryptionKey(
        sharedSecret: SharedSecret,
        info: ByteArray = ByteArray(0),
        salt: ByteArray = ByteArray(0)
    ): Result<DerivedKeyMaterial>
}
