package ir.sysfail.chatguard.domain.usecase.crypto

import ir.sysfail.chatguard.domain.repository.KeyRepository

class CheckPublicKeyExistsUseCase(
    private val repository: KeyRepository
) {
    operator fun invoke(username: String, packageName: String) =
        repository.checkExistsKey(username = username, packageName = packageName)
}