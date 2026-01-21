package ir.sysfail.chatguard.domain.usecase.crypto

import ir.sysfail.chatguard.domain.models.crypto.CryptoKey
import ir.sysfail.chatguard.domain.repository.KeyRepository

class GetPublicKeyUseCase(
    private val repository: KeyRepository,
) {
    suspend operator fun invoke(username: String, packageName: String): Result<CryptoKey> {
        return runCatching {
            val encodedKey =
                repository.getPublicKey(username = username, packageName = packageName)!!

            repository.reconstructPublicKey(encodedKey.encoded).getOrThrow()
        }
    }
}