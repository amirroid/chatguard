package ir.sysfail.chatguard.domain.usecase.key

import ir.sysfail.chatguard.domain.repository.IdentityKeyRepository

class CheckIdentityKeyExistsUseCase(
    private val identityKeyRepository: IdentityKeyRepository
) {
    suspend operator fun invoke() = identityKeyRepository.exists()
}