package ir.sysfail.chatguard.core.crypto.abstraction

import ir.sysfail.chatguard.core.crypto.models.PrivateKey
import ir.sysfail.chatguard.core.crypto.models.PublicKey

/**
 * Digital signature validation for authentication
 */
interface SignatureValidator {

    // Sign data with identity private key
    suspend fun sign(
        privateKey: PrivateKey,
        data: ByteArray
    ): Result<ByteArray>

    // Verify signature authenticity
    suspend fun verify(
        publicKey: PublicKey,
        data: ByteArray,
        signature: ByteArray
    ): Result<Boolean>

    // Sign ephemeral key with identity (MITM prevention)
    suspend fun signEphemeralKey(
        identityPrivateKey: PrivateKey,
        ephemeralPublicKey: PublicKey
    ): Result<ByteArray>
}