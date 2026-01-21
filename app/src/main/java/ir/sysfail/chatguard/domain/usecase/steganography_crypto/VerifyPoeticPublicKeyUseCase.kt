package ir.sysfail.chatguard.domain.usecase.steganography_crypto

import ir.sysfail.chatguard.core.crypto.util.SignedPublicKeySerializer
import ir.sysfail.chatguard.domain.repository.KeyRepository
import ir.sysfail.chatguard.domain.repository.SteganographyRepository

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