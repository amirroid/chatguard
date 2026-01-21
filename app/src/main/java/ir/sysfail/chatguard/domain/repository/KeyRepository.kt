package ir.sysfail.chatguard.domain.repository

import ir.sysfail.chatguard.domain.models.crypto.CryptoKey
import kotlinx.coroutines.flow.Flow

interface KeyRepository {
    fun checkExistsKey(username: String, packageName: String): Flow<Boolean>
    suspend fun getPublicKey(username: String, packageName: String): CryptoKey?
    suspend fun insetUserKey(username: String, packageName: String, key: CryptoKey)

    suspend fun reconstructPublicKey(encodedKey: ByteArray): Result<CryptoKey>
    suspend fun verifyPublicKey(
        key: CryptoKey,
        data: ByteArray,
        signature: ByteArray
    ): Result<Boolean>

    suspend fun signPublicKey(
        publicKey: CryptoKey,
        privateKey: CryptoKey,
    ): Result<ByteArray>
}