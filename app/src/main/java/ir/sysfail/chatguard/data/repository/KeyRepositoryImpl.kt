package ir.sysfail.chatguard.data.repository

import ir.sysfail.chatguard.core.crypto.abstraction.KeyManager
import ir.sysfail.chatguard.core.crypto.abstraction.SignatureValidator
import ir.sysfail.chatguard.core.database.dao.PublicKeyDao
import ir.sysfail.chatguard.core.database.entity.PublicKeyEntity
import ir.sysfail.chatguard.data.mappers.crypto.toCryptoKey
import ir.sysfail.chatguard.data.mappers.crypto.toPrivateKey
import ir.sysfail.chatguard.data.mappers.crypto.toPublicKey
import ir.sysfail.chatguard.domain.models.crypto.CryptoKey
import ir.sysfail.chatguard.domain.repository.KeyRepository
import kotlinx.coroutines.flow.Flow

class KeyRepositoryImpl(
    private val publicKeyDao: PublicKeyDao,
    private val keyManager: KeyManager,
    private val signatureValidator: SignatureValidator
) : KeyRepository {
    override fun checkExistsKey(
        username: String,
        packageName: String
    ): Flow<Boolean> = publicKeyDao.keyExists(packageName, username)

    override suspend fun getPublicKey(
        username: String,
        packageName: String
    ): CryptoKey? = publicKeyDao.getKey(username, packageName)?.toCryptoKey()

    override suspend fun insetUserKey(username: String, packageName: String, key: CryptoKey) {
        publicKeyDao.insertKey(
            PublicKeyEntity(
                username = username,
                appPackageName = packageName,
                publicKey = key.encoded,
                algorithm = key.algorithm
            )
        )
    }

    override suspend fun reconstructPublicKey(encodedKey: ByteArray): Result<CryptoKey> {
        return keyManager.reconstructPublicKey(encodedKey).map { it.toCryptoKey() }
    }

    override suspend fun verifyPublicKey(
        key: CryptoKey,
        data: ByteArray,
        signature: ByteArray
    ): Result<Boolean> {
        return signatureValidator.verify(key.toPublicKey(), data, signature)
    }

    override suspend fun signPublicKey(
        publicKey: CryptoKey,
        privateKey: CryptoKey
    ): Result<ByteArray> {
        return signatureValidator.sign(
            privateKey = privateKey.toPrivateKey(),
            data = publicKey.encoded
        )
    }
}