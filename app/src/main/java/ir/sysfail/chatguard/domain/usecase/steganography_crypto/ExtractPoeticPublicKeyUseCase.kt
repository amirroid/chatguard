package ir.sysfail.chatguard.domain.usecase.steganography_crypto

import ir.sysfail.chatguard.core.crypto.util.SignedPublicKeySerializer
import ir.sysfail.chatguard.domain.models.crypto.CryptoKey
import ir.sysfail.chatguard.domain.repository.KeyRepository
import ir.sysfail.chatguard.domain.repository.SteganographyRepository

class ExtractPoeticPublicKeyUseCase(
    private val steganographyRepository: SteganographyRepository,
    private val keyRepository: KeyRepository
) {
    suspend operator fun invoke(message: String): Result<CryptoKey> {
        return runCatching {
            val decodedBytes = steganographyRepository.decodeMessage(message).getOrThrow()
            val decodedKey = SignedPublicKeySerializer.deserialize(decodedBytes)

            keyRepository.reconstructPublicKey(decodedKey.publicKey).getOrThrow()

            CryptoKey(encoded = decodedKey.publicKey)
        }
    }
}