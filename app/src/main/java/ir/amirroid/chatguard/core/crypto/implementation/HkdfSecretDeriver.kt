package ir.amirroid.chatguard.core.crypto.implementation

import ir.amirroid.chatguard.core.crypto.abstraction.SharedSecretDeriver
import ir.amirroid.chatguard.core.crypto.models.DerivedKeyMaterial
import ir.amirroid.chatguard.core.crypto.models.PrivateKey
import ir.amirroid.chatguard.core.crypto.models.PublicKey
import ir.amirroid.chatguard.core.crypto.models.SharedSecret
import ir.amirroid.chatguard.core.crypto.models.SymmetricKey
import ir.amirroid.chatguard.core.crypto.util.KeyConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import javax.crypto.KeyAgreement
import javax.crypto.spec.SecretKeySpec

class HkdfSecretDeriver : SharedSecretDeriver {

    companion object {
        private const val KEY_AGREEMENT_ALGORITHM = "ECDH"
        private const val HASH_ALGORITHM = "SHA-256"
        private const val AES_KEY_LENGTH = 32 // 256 bits
        private const val NONCE_LENGTH = 12
    }

    override suspend fun deriveSharedSecret(
        myPrivateKey: PrivateKey,
        theirPublicKey: PublicKey
    ): Result<SharedSecret> = withContext(Dispatchers.Default) {
        runCatching {
            val keyAgreement = KeyAgreement.getInstance(KEY_AGREEMENT_ALGORITHM)

            val javaPrivateKey = KeyConverter.toJavaPrivateKey(myPrivateKey)
            val javaPublicKey = KeyConverter.toJavaPublicKey(theirPublicKey)

            keyAgreement.init(javaPrivateKey)
            keyAgreement.doPhase(javaPublicKey, true)

            val secret = keyAgreement.generateSecret()
            SharedSecret(secret)
        }
    }

    override suspend fun deriveEncryptionKey(
        sharedSecret: SharedSecret,
        info: ByteArray,
        salt: ByteArray
    ): Result<DerivedKeyMaterial> = withContext(Dispatchers.Default) {
        runCatching {
            // Simple HKDF implementation
            val digest = MessageDigest.getInstance(HASH_ALGORITHM)

            // Extract phase (simplified - using hash directly if no salt)
            val prk = if (salt.isNotEmpty()) {
                hmacSha256(salt, sharedSecret.secretBytes)
            } else {
                digest.digest(sharedSecret.secretBytes)
            }

            // Expand phase
            val okm = hkdfExpand(prk, info, AES_KEY_LENGTH + NONCE_LENGTH)

            val encryptionKeyBytes = okm.copyOfRange(0, AES_KEY_LENGTH)
            val nonce = okm.copyOfRange(AES_KEY_LENGTH, AES_KEY_LENGTH + NONCE_LENGTH)

            DerivedKeyMaterial(
                encryptionKey = SymmetricKey(encryptionKeyBytes, "AES"),
                nonce = nonce
            )
        }
    }

    // HMAC-SHA256 for HKDF extract
    private fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(key, "HmacSHA256")
        mac.init(secretKey)
        return mac.doFinal(data)
    }

    // HKDF expand phase
    private fun hkdfExpand(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
        val hashLen = 32 // SHA-256 output length
        val n = (length + hashLen - 1) / hashLen

        val okm = ByteArrayOutputStream()
        var t = ByteArray(0)

        for (i in 1..n) {
            val input = t + info + byteArrayOf(i.toByte())
            t = hmacSha256(prk, input)
            okm.write(t)
        }

        return okm.toByteArray().copyOfRange(0, length)
    }
}