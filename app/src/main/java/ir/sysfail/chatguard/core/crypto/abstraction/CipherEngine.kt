package ir.sysfail.chatguard.core.crypto.abstraction

import ir.sysfail.chatguard.core.crypto.models.CiphertextBundle
import ir.sysfail.chatguard.core.crypto.models.SymmetricKey

/**
 * AEAD symmetric encryption engine
 */
interface CipherEngine {

    // Encrypt plaintext with authenticated encryption
    suspend fun encrypt(
        key: SymmetricKey,
        plaintext: ByteArray,
        associatedData: ByteArray = ByteArray(0)
    ): Result<CiphertextBundle>

    // Decrypt and verify ciphertext
    suspend fun decrypt(
        key: SymmetricKey,
        bundle: CiphertextBundle,
        associatedData: ByteArray = ByteArray(0)
    ): Result<ByteArray>

    // Generate secure random nonce
    suspend fun generateNonce(): Result<ByteArray>
}