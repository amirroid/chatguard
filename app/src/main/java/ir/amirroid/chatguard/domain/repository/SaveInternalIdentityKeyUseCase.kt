package ir.amirroid.chatguard.domain.repository

import ir.amirroid.chatguard.domain.models.crypto.IdentityCryptoKeyPair

class SaveInternalIdentityKeyUseCase(
    private val identityKeyRepository: IdentityKeyRepository
) {
    suspend operator fun invoke(keyPair: IdentityCryptoKeyPair) =
        identityKeyRepository.saveInternal(keyPair)
}