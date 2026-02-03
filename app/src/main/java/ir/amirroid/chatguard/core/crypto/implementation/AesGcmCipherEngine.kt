package ir.amirroid.chatguard.core.crypto.implementation

import ir.amirroid.chatguard.core.crypto.abstraction.CipherEngine
import ir.amirroid.chatguard.core.crypto.models.CiphertextBundle
import ir.amirroid.chatguard.core.crypto.models.SymmetricKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class AesGcmCipherEngine : CipherEngine {

    companion object {
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val TAG_LENGTH_BITS = 128
        private const val NONCE_LENGTH_BYTES = 12
    }

    override suspend fun encrypt(
        key: SymmetricKey,
        plaintext: ByteArray,
        associatedData: ByteArray
    ): Result<CiphertextBundle> = withContext(Dispatchers.Default) {
        runCatching {
            val cipher = Cipher.getInstance(ALGORITHM)
            val keySpec = SecretKeySpec(key.keyBytes, "AES")
            val nonce = generateNonceSync()
            val gcmSpec = GCMParameterSpec(TAG_LENGTH_BITS, nonce)

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

            if (associatedData.isNotEmpty()) {
                cipher.updateAAD(associatedData)
            }

            val ciphertext = cipher.doFinal(plaintext)

            // GCM includes auth tag at the end of ciphertext
            val authTagStart = ciphertext.size - (TAG_LENGTH_BITS / 8)
            val actualCiphertext = ciphertext.copyOfRange(0, authTagStart)
            val authTag = ciphertext.copyOfRange(authTagStart, ciphertext.size)

            CiphertextBundle(
                ciphertext = actualCiphertext,
                nonce = nonce,
                authTag = authTag
            )
        }
    }

    override suspend fun decrypt(
        key: SymmetricKey,
        bundle: CiphertextBundle,
        associatedData: ByteArray
    ): Result<ByteArray> = withContext(Dispatchers.Default) {
        runCatching {
            val cipher = Cipher.getInstance(ALGORITHM)
            val keySpec = SecretKeySpec(key.keyBytes, "AES")
            val gcmSpec = GCMParameterSpec(TAG_LENGTH_BITS, bundle.nonce)

            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

            if (associatedData.isNotEmpty()) {
                cipher.updateAAD(associatedData)
            }

            // Combine ciphertext and auth tag for GCM
            val fullCiphertext = if (bundle.authTag != null) {
                bundle.ciphertext + bundle.authTag
            } else {
                bundle.ciphertext
            }

            cipher.doFinal(fullCiphertext)
        }
    }

    override suspend fun generateNonce(): Result<ByteArray> = withContext(Dispatchers.Default) {
        runCatching {
            generateNonceSync()
        }
    }

    private fun generateNonceSync(): ByteArray {
        val nonce = ByteArray(NONCE_LENGTH_BYTES)
        SecureRandom().nextBytes(nonce)
        return nonce
    }
}