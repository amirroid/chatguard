package ir.sysfail.chatguard.domain.usecase.key

import ir.sysfail.chatguard.domain.models.crypto.CryptoKey
import ir.sysfail.chatguard.domain.repository.KeyRepository

class AddUserPublicKeyUseCase(
    private val keyRepository: KeyRepository
) {
    suspend operator fun invoke(
        packageName: String,
        username: String,
        key: CryptoKey
    ): Result<Unit> {
        return runCatching {
            keyRepository.insetUserKey(username = username, packageName = packageName, key = key)
        }
    }
}