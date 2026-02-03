package ir.amirroid.chatguard.crypto

import ir.amirroid.chatguard.core.crypto.abstraction.*
import ir.amirroid.chatguard.core.crypto.implementation.*
import ir.amirroid.chatguard.core.crypto.models.SharedSecret
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Before
import org.junit.Test

class SharedSecretDeriverTest {

    private lateinit var secretDeriver: SharedSecretDeriver
    private lateinit var keyManager: KeyManager

    @Before
    fun setup() {
        keyManager = EcdhKeyManager()
        secretDeriver = HkdfSecretDeriver()
    }

    @Test
    fun `deriveSharedSecret should produce same secret for both parties`() = runTest {
        val aliceKeys = keyManager.generateEphemeralKeyPair().getOrThrow()
        val bobKeys = keyManager.generateEphemeralKeyPair().getOrThrow()

        val aliceSecret = secretDeriver.deriveSharedSecret(
            aliceKeys.privateKey,
            bobKeys.publicKey
        ).getOrThrow()

        val bobSecret = secretDeriver.deriveSharedSecret(
            bobKeys.privateKey,
            aliceKeys.publicKey
        ).getOrThrow()

        assertArrayEquals(aliceSecret.secretBytes, bobSecret.secretBytes)
    }

    @Test
    fun `deriveEncryptionKey should produce key and nonce`() = runTest {
        val sharedSecret = SharedSecret(ByteArray(32) { it.toByte() })

        val keyMaterial = secretDeriver.deriveEncryptionKey(sharedSecret).getOrThrow()

        assertEquals(32, keyMaterial.encryptionKey.keyBytes.size)
        assertEquals(12, keyMaterial.nonce.size)
    }

    @Test
    fun `deriveEncryptionKey should be deterministic with same inputs`() = runTest {
        val sharedSecret = SharedSecret(ByteArray(32) { it.toByte() })

        val keyMaterial1 = secretDeriver.deriveEncryptionKey(sharedSecret).getOrThrow()
        val keyMaterial2 = secretDeriver.deriveEncryptionKey(sharedSecret).getOrThrow()

        assertArrayEquals(keyMaterial1.encryptionKey.keyBytes, keyMaterial2.encryptionKey.keyBytes)
        assertArrayEquals(keyMaterial1.nonce, keyMaterial2.nonce)
    }
}