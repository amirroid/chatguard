package ir.amirroid.chatguard.data.repository

import ir.amirroid.chatguard.core.crypto.abstraction.KeyManager
import ir.amirroid.chatguard.core.crypto.models.IdentityKeyPair
import ir.amirroid.chatguard.core.preferences.abstraction.PreferenceKey
import ir.amirroid.chatguard.core.preferences.abstraction.PreferenceStorage
import ir.amirroid.chatguard.core.preferences.implementation.Base64ByteArraySerializer
import ir.amirroid.chatguard.data.mappers.crypto.toIdentityCryptoKeyPair
import ir.amirroid.chatguard.data.mappers.crypto.toIdentityKeyPair
import ir.amirroid.chatguard.domain.models.crypto.CryptoKey
import ir.amirroid.chatguard.domain.models.crypto.IdentityCryptoKeyPair
import ir.amirroid.chatguard.domain.repository.IdentityKeyRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DataStoreIdentityKeyRepository(
    private val preferences: PreferenceStorage,
    private val keyManager: KeyManager
) : IdentityKeyRepository {

    @Volatile
    private var cachedKeyPair: IdentityCryptoKeyPair? = null

    private val mutex = Mutex()

    override suspend fun getOrGenerate(): Result<IdentityCryptoKeyPair> =
        runCatching {
            mutex.withLock {
                cachedKeyPair
                    ?: loadFromStorage()
                    ?: generateAndPersistLocked()
            }
        }

    override suspend fun generate(): Result<IdentityCryptoKeyPair> =
        runCatching {
            mutex.withLock {
                generateAndPersistLocked()
            }
        }

    override suspend fun get(): Result<IdentityCryptoKeyPair?> =
        runCatching {
            mutex.withLock {
                cachedKeyPair ?: loadFromStorage()
            }
        }

    override suspend fun exists(): Result<Boolean> =
        runCatching {
            mutex.withLock {
                cachedKeyPair != null || loadFromStorage() != null
            }
        }

    override suspend fun checkIsValid(keyPair: IdentityCryptoKeyPair): Boolean {
        return keyManager.validateKeyPair(
            keyPair.privateKey.encoded,
            keyPair.publicKey.encoded
        )
    }

    override suspend fun saveInternal(
        keyPair: IdentityCryptoKeyPair
    ): Result<Unit> =
        runCatching {
            mutex.withLock {
                persistIdentityKeys(keyPair.toIdentityKeyPair())
                cachedKeyPair = keyPair
            }
        }

    override suspend fun removeCurrentKeys() {
        mutex.withLock {
            preferences.remove(privatePreferencesKey)
            preferences.remove(publicPreferencesKey)
            cachedKeyPair = null
        }
    }

    private suspend fun loadFromStorage(): IdentityCryptoKeyPair? {
        val privateBytes = preferences.read(privatePreferencesKey) ?: return null
        val publicBytes = preferences.read(publicPreferencesKey) ?: return null

        if (!keyManager.validateKeyPair(privateBytes, publicBytes)) {
            return null
        }

        return IdentityCryptoKeyPair(
            privateKey = CryptoKey(privateBytes),
            publicKey = CryptoKey(publicBytes)
        ).also {
            cachedKeyPair = it
        }
    }

    private suspend fun generateAndPersistLocked(): IdentityCryptoKeyPair {
        val generated = keyManager
            .generateIdentityKeyPair()
            .getOrThrow()

        persistIdentityKeys(generated)

        return generated
            .toIdentityCryptoKeyPair()
            .also { cachedKeyPair = it }
    }

    private suspend fun persistIdentityKeys(keyPair: IdentityKeyPair) {
        preferences.write(
            privatePreferencesKey,
            keyPair.privateKey.encoded
        )
        preferences.write(
            publicPreferencesKey,
            keyPair.publicKey.encoded
        )
    }


    companion object {
        private val privatePreferencesKey =
            PreferenceKey("ec_private", Base64ByteArraySerializer)

        private val publicPreferencesKey =
            PreferenceKey("ec_public", Base64ByteArraySerializer)
    }
}