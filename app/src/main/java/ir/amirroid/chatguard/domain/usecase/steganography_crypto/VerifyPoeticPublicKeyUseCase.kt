package ir.amirroid.chatguard.domain.usecase.steganography_crypto

import ir.amirroid.chatguard.core.crypto.util.SignedPublicKeySerializer
import ir.amirroid.chatguard.domain.repository.KeyRepository
import ir.amirroid.chatguard.domain.repository.SteganographyRepository

class VerifyPoeticPublicKeyUseCase(
    private val steganographyRepository: SteganographyRepository,
    private val keyRepository: KeyRepository
) {
    suspend operator fun invoke(message: String): Boolean {
        val decodedBytes =
            steganographyRepository.decodeMessage(message).getOrNull() ?: return false

        return runCatching {
            val decodedKey = SignedPublicKeySerializer.deserialize(decodedBytes)

            val reconstructedPublicKey = keyRepository.reconstructPublicKey(
                decodedKey.publicKey
            ).getOrThrow()

            keyRepository.verifyPublicKey(
                reconstructedPublicKey,
                decodedKey.publicKey,
                decodedKey.signature
            ).getOrThrow()
        }.getOrDefault(false)
    }
}