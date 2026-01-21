package ir.sysfail.chatguard.crypto

import ir.sysfail.chatguard.core.crypto.abstraction.*
import ir.sysfail.chatguard.core.crypto.implementation.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class KeyManagerTest {

    private lateinit var keyManager: KeyManager

    @Before
    fun setup() {
        keyManager = EcdhKeyManager()
    }

    @Test
    fun `generateIdentityKeyPair should create valid key pair`() = runTest {
        val result = keyManager.generateIdentityKeyPair()

        assertTrue(result.isSuccess)
        val keyPair = result.getOrThrow()
        assertNotNull(keyPair.privateKey)
        assertNotNull(keyPair.publicKey)
        assertTrue(keyPair.privateKey.encoded.isNotEmpty())
        assertTrue(keyPair.publicKey.encoded.isNotEmpty())
    }

    @Test
    fun `generateEphemeralKeyPair should create different keys each time`() = runTest {
        val keyPair1 = keyManager.generateEphemeralKeyPair().getOrThrow()
        val keyPair2 = keyManager.generateEphemeralKeyPair().getOrThrow()

        assertFalse(keyPair1.publicKey.encoded.contentEquals(keyPair2.publicKey.encoded))
        assertFalse(keyPair1.privateKey.encoded.contentEquals(keyPair2.privateKey.encoded))
    }

    @Test
    fun `reconstructPublicKey should restore key from bytes`() = runTest {
        val original = keyManager.generateIdentityKeyPair().getOrThrow()
        val serialized = original.publicKey.encoded

        val reconstructed = keyManager.reconstructPublicKey(serialized).getOrThrow()

        assertArrayEquals(original.publicKey.encoded, reconstructed.encoded)
    }

    @Test
    fun `calculateFingerprint should generate consistent fingerprint`() = runTest {
        val keyPair = keyManager.generateIdentityKeyPair().getOrThrow()

        val fingerprint1 = keyManager.calculateFingerprint(keyPair.publicKey).getOrThrow()
        val fingerprint2 = keyManager.calculateFingerprint(keyPair.publicKey).getOrThrow()

        assertEquals(fingerprint1, fingerprint2)
        assertTrue(fingerprint1.matches(Regex("[0-9A-F]{2}(:[0-9A-F]{2}){7}")))
    }
}