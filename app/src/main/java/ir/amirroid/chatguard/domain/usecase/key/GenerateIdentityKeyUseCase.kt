package ir.amirroid.chatguard.domain.usecase.key

import ir.amirroid.chatguard.domain.repository.IdentityKeyRepository

class GenerateIdentityKeyUseCase(
    private val identityKeyRepository: IdentityKeyRepository
) {
    suspend operator fun invoke() = identityKeyRepository.generate()
}