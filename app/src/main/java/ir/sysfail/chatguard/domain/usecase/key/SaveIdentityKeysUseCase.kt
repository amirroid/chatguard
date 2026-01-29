package ir.sysfail.chatguard.domain.usecase.key

import android.net.Uri
import ir.sysfail.chatguard.core.crypto.util.IdentityKeyPairSerializer
import ir.sysfail.chatguard.data.mappers.crypto.toIdentityKeyPair
import ir.sysfail.chatguard.domain.repository.IdentityKeyRepository
import ir.sysfail.chatguard.domain.repository.StorageRepository

class SaveIdentityKeysUseCase(
    private val identityKeyRepository: IdentityKeyRepository,
    private val storageRepository: StorageRepository
) {

    suspend operator fun invoke(uri: Uri): Result<Unit> =
        runCatching {
            val keyPair = identityKeyRepository.get()
                .getOrNull()
                ?: error("Identity key pair not found")

            val bytes = IdentityKeyPairSerializer
                .serialize(keyPair.toIdentityKeyPair())

            storageRepository.writeFile(uri, bytes)
        }
}