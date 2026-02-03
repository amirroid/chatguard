package ir.amirroid.chatguard.domain.usecase.steganography_crypto

import ir.amirroid.chatguard.domain.models.crypto.CryptoKey
import ir.amirroid.chatguard.domain.repository.CryptoRepository
import ir.amirroid.chatguard.domain.repository.IdentityKeyRepository
import ir.amirroid.chatguard.domain.repository.SteganographyRepository

class PoeticMessageEncryptionUseCase(
    private val identityKeyRepository: IdentityKeyRepository,
    private val cryptoRepository: CryptoRepository,
    private val steganographyRepository: SteganographyRepository
) {
    suspend operator fun invoke(message: String, theirPublicKey: CryptoKey): Result<String> {
        return runCatching {
            val myKeyPair = identityKeyRepository.getOrGenerate().getOrThrow()

            val envelope = cryptoRepository.encryptMessage(
                message = message.toByteArray(),
                myIdentityCryptoKeyPair = myKeyPair,
                theirPublicKey = theirPublicKey
            ).getOrThrow()

            steganographyRepository.encodeMessage(envelope).getOrThrow()
        }
    }
}