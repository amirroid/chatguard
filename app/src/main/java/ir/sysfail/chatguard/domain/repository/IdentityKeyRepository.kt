package ir.sysfail.chatguard.domain.repository

import ir.sysfail.chatguard.domain.models.crypto.IdentityCryptoKeyPair

interface IdentityKeyRepository {
    suspend fun getOrGenerate(): Result<IdentityCryptoKeyPair>
    suspend fun generate(): Result<IdentityCryptoKeyPair>
    suspend fun get(): Result<IdentityCryptoKeyPair?>
    suspend fun exists(): Result<Boolean>
    suspend fun checkIsValid(keyPair: IdentityCryptoKeyPair): Boolean
    suspend fun saveInternal(keyPair: IdentityCryptoKeyPair): Result<Unit>
    suspend fun removeCurrentKeys()
}