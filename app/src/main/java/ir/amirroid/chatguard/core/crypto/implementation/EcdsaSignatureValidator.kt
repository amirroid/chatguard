package ir.amirroid.chatguard.core.crypto.implementation

import ir.amirroid.chatguard.core.crypto.abstraction.SignatureValidator
import ir.amirroid.chatguard.core.crypto.models.PrivateKey
import ir.amirroid.chatguard.core.crypto.models.PublicKey
import ir.amirroid.chatguard.core.crypto.util.KeyConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.Signature
import java.security.SignatureException

class EcdsaSignatureValidator : SignatureValidator {

    companion object {
        private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
    }

    override suspend fun sign(
        privateKey: PrivateKey,
        data: ByteArray
    ): Result<ByteArray> = withContext(Dispatchers.Default) {
        runCatching {
            val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
            val javaPrivateKey = KeyConverter.toJavaPrivateKey(privateKey)

            signature.initSign(javaPrivateKey)
            signature.update(data)

            signature.sign()
        }
    }

    override suspend fun verify(
        publicKey: PublicKey,
        data: ByteArray,
        signature: ByteArray
    ): Result<Boolean> = withContext(Dispatchers.Default) {
        runCatching {
            try {
                val verifier = Signature.getInstance(SIGNATURE_ALGORITHM)
                val javaPublicKey = KeyConverter.toJavaPublicKey(publicKey)

                verifier.initVerify(javaPublicKey)
                verifier.update(data)

                verifier.verify(signature)
            } catch (e: SignatureException) {
                // Invalid signature format or verification failed
                false
            }
        }
    }

    override suspend fun signEphemeralKey(
        identityPrivateKey: PrivateKey,
        ephemeralPublicKey: PublicKey
    ): Result<ByteArray> = withContext(Dispatchers.Default) {
        sign(identityPrivateKey, ephemeralPublicKey.encoded)
    }
}