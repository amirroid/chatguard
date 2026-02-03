package ir.amirroid.chatguard.data.repository

import ir.amirroid.chatguard.core.crypto.abstraction.CryptoOrchestrator
import ir.amirroid.chatguard.core.crypto.util.CryptoEnvelopeSerializer
import ir.amirroid.chatguard.data.mappers.crypto.toIdentityKeyPair
import ir.amirroid.chatguard.data.mappers.crypto.toPublicKey
import ir.amirroid.chatguard.domain.models.crypto.CryptoKey
import ir.amirroid.chatguard.domain.models.crypto.IdentityCryptoKeyPair
import ir.amirroid.chatguard.domain.repository.CryptoRepository

class CryptoRepositoryImpl(
    private val cryptoOrchestrator: CryptoOrchestrator
) : CryptoRepository {
    override suspend fun encryptMessage(
        message: ByteArray,
        myIdentityCryptoKeyPair: IdentityCryptoKeyPair,
        theirPublicKey: CryptoKey
    ): Result<ByteArray> {
        return runCatching {
            val keyPair = myIdentityCryptoKeyPair.toIdentityKeyPair()

            cryptoOrchestrator.encryptMessage(
                plaintext = message,
                myIdentityPrivateKey = keyPair.privateKey,
                myIdentityPublicKey = keyPair.publicKey,
                theirIdentityPublicKey = theirPublicKey.toPublicKey()
            ).map { CryptoEnvelopeSerializer.serialize(it) }.getOrThrow()
        }
    }

    override suspend fun decryptMessage(
        message: ByteArray,
        myIdentityCryptoKeyPair: IdentityCryptoKeyPair,
        theirPublicKey: CryptoKey,
        isAmSender: Boolean
    ): Result<ByteArray> {
        return runCatching {
            val keyPair = myIdentityCryptoKeyPair.toIdentityKeyPair()

            cryptoOrchestrator.decryptMessage(
                envelope = CryptoEnvelopeSerializer.deserialize(message),
                myIdentityPrivateKey = keyPair.privateKey,
                myIdentityPublicKey = keyPair.publicKey,
                theirIdentityPublicKey = theirPublicKey.toPublicKey(),
                iAmSender = isAmSender
            ).getOrThrow()
        }
    }
}