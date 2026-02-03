package ir.amirroid.chatguard.domain.usecase.steganography_crypto

import ir.amirroid.chatguard.core.crypto.util.SignedPublicKeySerializer
import ir.amirroid.chatguard.domain.models.crypto.CryptoKey
import ir.amirroid.chatguard.domain.repository.KeyRepository
import ir.amirroid.chatguard.domain.repository.SteganographyRepository

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