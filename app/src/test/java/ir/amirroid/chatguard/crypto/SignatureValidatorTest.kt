package ir.amirroid.chatguard.crypto

import ir.amirroid.chatguard.core.crypto.abstraction.*
import ir.amirroid.chatguard.core.crypto.implementation.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SignatureValidatorTest {

    private lateinit var signatureValidator: SignatureValidator
    private lateinit var keyManager: KeyManager

    @Before
    fun setup() {
        keyManager = EcdhKeyManager()
        signatureValidator = EcdsaSignatureValidator()
    }

    @Test
    fun `sign and verify should succeed with correct key`() = runTest {
        val keyPair = keyManager.generateIdentityKeyPair().getOrThrow()
        val data = "Test data".toByteArray()

        val signature = signatureValidator.sign(keyPair.privateKey, data).getOrThrow()
        val isValid = signatureValidator.verify(keyPair.publicKey, data, signature).getOrThrow()

        assertTrue(isValid)
    }

    @Test
    fun `verify should fail with wrong public key`() = runTest {
        val keyPair1 = keyManager.generateIdentityKeyPair().getOrThrow()
        val keyPair2 = keyManager.generateIdentityKeyPair().getOrThrow()
        val data = "Test data".toByteArray()

        val signature = signatureValidator.sign(keyPair1.privateKey, data).getOrThrow()
        val isValid = signatureValidator.verify(keyPair2.publicKey, data, signature).getOrThrow()

        assertFalse(isValid)
    }

    @Test
    fun `verify should fail with tampered data`() = runTest {
        val keyPair = keyManager.generateIdentityKeyPair().getOrThrow()
        val originalData = "Original data".toByteArray()
        val tamperedData = "Tampered data".toByteArray()

        val signature = signatureValidator.sign(keyPair.privateKey, originalData).getOrThrow()
        val isValid =
            signatureValidator.verify(keyPair.publicKey, tamperedData, signature).getOrThrow()

        assertFalse(isValid)
    }

    @Test
    fun `signEphemeralKey should sign public key bytes`() = runTest {
        val identityKeys = keyManager.generateIdentityKeyPair().getOrThrow()
        val ephemeralKeys = keyManager.generateEphemeralKeyPair().getOrThrow()

        val signature = signatureValidator.signEphemeralKey(
            identityKeys.privateKey,
            ephemeralKeys.publicKey
        ).getOrThrow()

        assertNotNull(signature)
        assertTrue(signature.isNotEmpty())
    }
}