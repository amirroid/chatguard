package ir.amirroid.chatguard.domain.usecase.steganography_crypto

import ir.amirroid.chatguard.domain.models.crypto.CryptoKey
import ir.amirroid.chatguard.domain.repository.CryptoRepository
import ir.amirroid.chatguard.domain.repository.IdentityKeyRepository
import ir.amirroid.chatguard.domain.repository.SteganographyRepository

class PoeticMessageDecryptionUseCase(
    private val identityKeyRepository: IdentityKeyRepository,
    private val cryptoRepository: CryptoRepository,
    private val steganographyRepository: SteganographyRepository
) {
    suspend operator fun invoke(
        message: String,
        theirPublicKey: CryptoKey,
        isAmSender: Boolean
    ): Result<String> {
        return runCatching {
            val myKeyPair = identityKeyRepository.getOrGenerate().getOrThrow()
            val decodedBytes = steganographyRepository.decodeMessage(message).getOrThrow()

            cryptoRepository.decryptMessage(
                message = decodedBytes,
                myIdentityCryptoKeyPair = myKeyPair,
                theirPublicKey = theirPublicKey,
                isAmSender = isAmSender
            ).getOrThrow().decodeToString()
        }
    }
}