package ir.sysfail.chatguard.domain.repository

import ir.sysfail.chatguard.domain.models.crypto.IdentityCryptoKeyPair

interface IdentityKeyRepository {
    suspend fun getOrGenerate(): Result<IdentityCryptoKeyPair>
    suspend fun get(): Result<IdentityCryptoKeyPair?>
    suspend fun exists(): Result<Boolean>
}