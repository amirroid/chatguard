package ir.sysfail.chatguard.domain.usecase.steganography_crypto

import ir.sysfail.chatguard.core.crypto.models.SignedPublicKey
import ir.sysfail.chatguard.core.crypto.util.SignedPublicKeySerializer
import ir.sysfail.chatguard.domain.repository.IdentityKeyRepository
import ir.sysfail.chatguard.domain.repository.KeyRepository
import ir.sysfail.chatguard.domain.repository.SteganographyRepository

class GetPoeticSignedPublicKeyUseCase(
    private val identityKeyRepository: IdentityKeyRepository,
    private val keyRepository: KeyRepository,
    private val steganographyRepository: SteganographyRepository,
) {
    suspend operator fun invoke(): Result<String> {
        return runCatching {
            val key = identityKeyRepository.getOrGenerate().getOrThrow()

            val signature = keyRepository.signPublicKey(
                privateKey = key.privateKey,
                publicKey = key.publicKey
            ).getOrThrow()

            val signedKey = SignedPublicKey(
                publicKey = key.publicKey.encoded,
                signature = signature
            )

            steganographyRepository.encodeMessage(
                SignedPublicKeySerializer.serialize(signedKey)
            ).getOrThrow()
        }
    }
}