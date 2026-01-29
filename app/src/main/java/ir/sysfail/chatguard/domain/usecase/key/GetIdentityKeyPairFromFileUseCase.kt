package ir.sysfail.chatguard.domain.usecase.key

import android.net.Uri
import ir.sysfail.chatguard.core.crypto.util.IdentityKeyPairSerializer
import ir.sysfail.chatguard.data.mappers.crypto.toIdentityCryptoKeyPair
import ir.sysfail.chatguard.domain.models.crypto.IdentityCryptoKeyPair
import ir.sysfail.chatguard.domain.repository.IdentityKeyRepository
import ir.sysfail.chatguard.domain.repository.StorageRepository

class GetIdentityKeyPairFromFileUseCase(
    private val identityKeyRepository: IdentityKeyRepository,
    private val storageRepository: StorageRepository
) {

    suspend operator fun invoke(uri: Uri): Result<IdentityCryptoKeyPair> {
        return runCatching {
            val bytes = storageRepository.readFile(uri)
                ?: throw IllegalStateException("Failed to read key file")

            val identityCryptoKeyPair =
                IdentityKeyPairSerializer
                    .deserialize(bytes)
                    .toIdentityCryptoKeyPair()

            if (!identityKeyRepository.checkIsValid(identityCryptoKeyPair)) {
                throw IllegalArgumentException("Invalid identity key pair")
            }

            identityCryptoKeyPair
        }
    }
}