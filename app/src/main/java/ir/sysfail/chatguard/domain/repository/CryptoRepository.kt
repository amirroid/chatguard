package ir.sysfail.chatguard.domain.repository

import ir.sysfail.chatguard.domain.models.crypto.CryptoKey
import ir.sysfail.chatguard.domain.models.crypto.IdentityCryptoKeyPair

interface CryptoRepository {
    suspend fun encryptMessage(
        message: ByteArray,
        myIdentityCryptoKeyPair: IdentityCryptoKeyPair,
        theirPublicKey: CryptoKey
    ): Result<ByteArray>

    suspend fun decryptMessage(
        message: ByteArray,
        myIdentityCryptoKeyPair: IdentityCryptoKeyPair,
        theirPublicKey: CryptoKey,
        isAmSender: Boolean
    ): Result<ByteArray>
}
