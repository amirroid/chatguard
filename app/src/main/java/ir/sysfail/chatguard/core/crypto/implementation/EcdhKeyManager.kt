package ir.sysfail.chatguard.core.crypto.implementation

import ir.sysfail.chatguard.core.crypto.abstraction.KeyManager
import ir.sysfail.chatguard.core.crypto.models.EphemeralKeyPair
import ir.sysfail.chatguard.core.crypto.models.IdentityKeyPair
import ir.sysfail.chatguard.core.crypto.models.PrivateKey
import ir.sysfail.chatguard.core.crypto.models.PublicKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

class EcdhKeyManager : KeyManager {

    companion object {
        private const val CURVE_NAME = "secp256r1"
        private const val ALGORITHM = "EC"
    }

    override suspend fun generateIdentityKeyPair(): Result<IdentityKeyPair> =
        withContext(Dispatchers.Default) {
            runCatching {
                val keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM)
                keyPairGenerator.initialize(ECGenParameterSpec(CURVE_NAME), SecureRandom())

                val keyPair = keyPairGenerator.generateKeyPair()

                IdentityKeyPair(
                    privateKey = PrivateKey(keyPair.private.encoded, ALGORITHM),
                    publicKey = PublicKey(keyPair.public.encoded, ALGORITHM)
                )
            }
        }

    override suspend fun generateEphemeralKeyPair(): Result<EphemeralKeyPair> =
        withContext(Dispatchers.Default) {
            runCatching {
                val keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM)
                keyPairGenerator.initialize(ECGenParameterSpec(CURVE_NAME), SecureRandom())

                val keyPair = keyPairGenerator.generateKeyPair()

                EphemeralKeyPair(
                    privateKey = PrivateKey(keyPair.private.encoded, ALGORITHM),
                    publicKey = PublicKey(keyPair.public.encoded, ALGORITHM)
                )
            }
        }

    override suspend fun reconstructPublicKey(encodedKey: ByteArray): Result<PublicKey> =
        withContext(Dispatchers.Default) {
            runCatching {
                val keyFactory = KeyFactory.getInstance(ALGORITHM)
                val keySpec = X509EncodedKeySpec(encodedKey)
                val javaPublicKey = keyFactory.generatePublic(keySpec)

                PublicKey(javaPublicKey.encoded, ALGORITHM)
            }
        }

    override suspend fun serializePublicKey(publicKey: PublicKey): Result<ByteArray> =
        withContext(Dispatchers.Default) {
            runCatching {
                publicKey.encoded
            }
        }

    override suspend fun calculateFingerprint(publicKey: PublicKey): Result<String> =
        withContext(Dispatchers.Default) {
            runCatching {
                val digest = MessageDigest.getInstance("SHA-256")
                val hash = digest.digest(publicKey.encoded)

                hash.take(8).joinToString(":") { byte ->
                    "%02X".format(byte)
                }
            }
        }

    override fun validateKeyPair(privateKeyBytes: ByteArray, publicKeyBytes: ByteArray): Boolean {
        return runCatching {
            val keyFactory = KeyFactory.getInstance(ALGORITHM)

            val privateKeySpec = PKCS8EncodedKeySpec(privateKeyBytes)
            keyFactory.generatePrivate(privateKeySpec)

            val publicKeySpec = X509EncodedKeySpec(publicKeyBytes)
            keyFactory.generatePublic(publicKeySpec)

            true
        }.getOrDefault(false)
    }
}