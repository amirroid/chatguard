package ir.amirroid.chatguard.domain.usecase.key

import ir.amirroid.chatguard.domain.repository.IdentityKeyRepository

class CheckIdentityKeyExistsUseCase(
    private val identityKeyRepository: IdentityKeyRepository
) {
    suspend operator fun invoke() = identityKeyRepository.exists()
}