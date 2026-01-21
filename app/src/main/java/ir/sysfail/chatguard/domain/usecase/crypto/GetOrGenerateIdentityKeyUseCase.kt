package ir.sysfail.chatguard.domain.usecase.crypto

import ir.sysfail.chatguard.domain.repository.IdentityKeyRepository

class GetOrGenerateIdentityKeyUseCase(
    private val identityKeyRepository: IdentityKeyRepository
) {
    suspend operator fun invoke() = identityKeyRepository.getOrGenerate()
}