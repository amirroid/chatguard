package ir.sysfail.chatguard.domain.repository

import ir.sysfail.chatguard.domain.models.crypto.IdentityCryptoKeyPair

class SaveInternalIdentityKeyUseCase(
    private val identityKeyRepository: IdentityKeyRepository
) {
    suspend operator fun invoke(keyPair: IdentityCryptoKeyPair) =
        identityKeyRepository.saveInternal(keyPair)
}