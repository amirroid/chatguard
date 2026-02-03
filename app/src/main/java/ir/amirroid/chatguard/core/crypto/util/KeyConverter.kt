package ir.amirroid.chatguard.core.crypto.util

import ir.amirroid.chatguard.core.crypto.models.PrivateKey
import ir.amirroid.chatguard.core.crypto.models.PublicKey
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec


/**
 * Converts between our data models and Java security keys
 */
object KeyConverter {

    private const val ALGORITHM = "EC"

    fun toJavaPrivateKey(privateKey: PrivateKey): java.security.PrivateKey {
        val keyFactory = KeyFactory.getInstance(ALGORITHM)
        val keySpec = PKCS8EncodedKeySpec(privateKey.encoded)
        return keyFactory.generatePrivate(keySpec)
    }

    fun toJavaPublicKey(publicKey: PublicKey): java.security.PublicKey {
        val keyFactory = KeyFactory.getInstance(ALGORITHM)
        val keySpec = X509EncodedKeySpec(publicKey.encoded)
        return keyFactory.generatePublic(keySpec)
    }

    fun fromJavaPrivateKey(javaKey: java.security.PrivateKey): PrivateKey {
        return PrivateKey(javaKey.encoded, javaKey.algorithm)
    }

    fun fromJavaPublicKey(javaKey: java.security.PublicKey): PublicKey {
        return PublicKey(javaKey.encoded, javaKey.algorithm)
    }
}