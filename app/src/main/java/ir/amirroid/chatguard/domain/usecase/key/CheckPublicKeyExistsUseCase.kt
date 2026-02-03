package ir.amirroid.chatguard.domain.usecase.key

import ir.amirroid.chatguard.domain.repository.KeyRepository

class CheckPublicKeyExistsUseCase(
    private val repository: KeyRepository
) {
    operator fun invoke(username: String, packageName: String) =
        repository.checkExistsKey(username = username, packageName = packageName)
}