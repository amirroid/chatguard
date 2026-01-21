package ir.sysfail.chatguard.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import ir.sysfail.chatguard.core.crypto.models.IdentityKeyPair
import ir.sysfail.chatguard.core.crypto.models.PrivateKey
import ir.sysfail.chatguard.core.crypto.models.PublicKey
import ir.sysfail.chatguard.data.mappers.crypto.toIdentityCryptoKeyPair
import ir.sysfail.chatguard.domain.models.crypto.CryptoKey
import ir.sysfail.chatguard.domain.models.crypto.IdentityCryptoKeyPair
import ir.sysfail.chatguard.domain.repository.IdentityKeyRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

class DataStoreIdentityKeyRepository(
    private val dataStore: DataStore<Preferences>
) : IdentityKeyRepository {

    @Volatile
    private var cachedKeyPair: IdentityCryptoKeyPair? = null
    private val mutex = Mutex()

    override suspend fun getOrGenerate(): Result<IdentityCryptoKeyPair> {
        return try {
            mutex.withLock {
                cachedKeyPair?.let { return@withLock Result.success(it) }

                val existing = getInternal()
                if (existing != null) {
                    cachedKeyPair = existing
                    return@withLock Result.success(existing)
                }

                val generated = generateKeyPair()
                saveInternal(generated)
                val keyPair = generated.toIdentityCryptoKeyPair()
                cachedKeyPair = keyPair
                Result.success(keyPair)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun get(): Result<IdentityCryptoKeyPair?> {
        return try {
            mutex.withLock {
                cachedKeyPair?.let { return@withLock Result.success(it) }

                val keyPair = getInternal()
                if (keyPair != null) {
                    cachedKeyPair = keyPair
                }
                Result.success(keyPair)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getInternal(): IdentityCryptoKeyPair? {
        val prefs = dataStore.data.first()

        val privateKeyBase64 = prefs[KEY_PRIVATE] ?: return null
        val publicKeyBase64 = prefs[KEY_PUBLIC] ?: return null

        val privateKeyBytes = android.util.Base64.decode(
            privateKeyBase64,
            android.util.Base64.DEFAULT
        )

        val publicKeyBytes = android.util.Base64.decode(
            publicKeyBase64,
            android.util.Base64.DEFAULT
        )

        val keyFactory = KeyFactory.getInstance(ALGORITHM)

        val privateKeySpec = PKCS8EncodedKeySpec(privateKeyBytes)
        keyFactory.generatePrivate(privateKeySpec)

        val publicKeySpec = X509EncodedKeySpec(publicKeyBytes)
        keyFactory.generatePublic(publicKeySpec)

        return IdentityCryptoKeyPair(
            privateKey = CryptoKey(privateKeyBytes, ALGORITHM),
            publicKey = CryptoKey(publicKeyBytes, ALGORITHM)
        )
    }

    private suspend fun saveInternal(keyPair: IdentityKeyPair) {
        val privateKeyBase64 = android.util.Base64.encodeToString(
            keyPair.privateKey.encoded,
            android.util.Base64.DEFAULT
        )
        val publicKeyBase64 = android.util.Base64.encodeToString(
            keyPair.publicKey.encoded,
            android.util.Base64.DEFAULT
        )

        dataStore.edit { prefs ->
            prefs[KEY_PRIVATE] = privateKeyBase64
            prefs[KEY_PUBLIC] = publicKeyBase64
        }
    }

    override suspend fun exists(): Result<Boolean> {
        return try {
            val cached = cachedKeyPair
            if (cached != null) {
                Result.success(true)
            } else {
                val prefs = dataStore.data.first()
                val exists = prefs.contains(KEY_PRIVATE) && prefs.contains(KEY_PUBLIC)
                Result.success(exists)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generateKeyPair(): IdentityKeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM)
        keyPairGenerator.initialize(ECGenParameterSpec(CURVE_NAME), SecureRandom())

        val keyPair = keyPairGenerator.generateKeyPair()

        return IdentityKeyPair(
            privateKey = PrivateKey(keyPair.private.encoded, ALGORITHM),
            publicKey = PublicKey(keyPair.public.encoded, ALGORITHM)
        )
    }

    fun clearCache() {
        cachedKeyPair = null
    }

    companion object {
        private const val CURVE_NAME = "secp256r1"
        private const val ALGORITHM = "EC"
        private val KEY_PRIVATE = stringPreferencesKey("ec_private")
        private val KEY_PUBLIC = stringPreferencesKey("ec_public")
    }
}