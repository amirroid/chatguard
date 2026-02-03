package ir.amirroid.chatguard.crypto

import ir.amirroid.chatguard.core.crypto.abstraction.CipherEngine
import ir.amirroid.chatguard.core.crypto.implementation.AesGcmCipherEngine
import ir.amirroid.chatguard.core.crypto.models.SymmetricKey
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CipherEngineTest {

    private lateinit var cipherEngine: CipherEngine

    @Before
    fun setup() {
        cipherEngine = AesGcmCipherEngine()
    }

    @Test
    fun `encrypt and decrypt should restore original plaintext`() = runTest {
        val plaintext = "Hello, secure world!".toByteArray()
        val key = SymmetricKey(ByteArray(32) { it.toByte() })

        val encrypted = cipherEngine.encrypt(key, plaintext).getOrThrow()
        val decrypted = cipherEngine.decrypt(key, encrypted).getOrThrow()

        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun `decrypt with wrong key should fail`() = runTest {
        val plaintext = "Secret message".toByteArray()
        val key1 = SymmetricKey(ByteArray(32) { it.toByte() })
        val key2 = SymmetricKey(ByteArray(32) { (it + 1).toByte() })

        val encrypted = cipherEngine.encrypt(key1, plaintext).getOrThrow()
        val result = cipherEngine.decrypt(key2, encrypted)

        assertTrue(result.isFailure)
    }

    @Test
    fun `encrypt should produce different ciphertext each time`() = runTest {
        val plaintext = "Same message".toByteArray()
        val key = SymmetricKey(ByteArray(32) { it.toByte() })

        val encrypted1 = cipherEngine.encrypt(key, plaintext).getOrThrow()
        val encrypted2 = cipherEngine.encrypt(key, plaintext).getOrThrow()

        assertFalse(encrypted1.ciphertext.contentEquals(encrypted2.ciphertext))
        assertFalse(encrypted1.nonce.contentEquals(encrypted2.nonce))
    }

    @Test
    fun `generateNonce should create unique nonces`() = runTest {
        val nonce1 = cipherEngine.generateNonce().getOrThrow()
        val nonce2 = cipherEngine.generateNonce().getOrThrow()

        assertEquals(12, nonce1.size)
        assertEquals(12, nonce2.size)
        assertFalse(nonce1.contentEquals(nonce2))
    }
}