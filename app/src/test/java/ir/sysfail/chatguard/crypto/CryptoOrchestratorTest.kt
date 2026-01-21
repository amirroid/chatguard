package ir.sysfail.chatguard.crypto

import ir.sysfail.chatguard.core.crypto.abstraction.CryptoOrchestrator
import ir.sysfail.chatguard.core.crypto.abstraction.KeyManager
import ir.sysfail.chatguard.core.crypto.implementation.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CryptoOrchestratorTest {

    private lateinit var orchestrator: CryptoOrchestrator
    private lateinit var keyManager: KeyManager

    @Before
    fun setup() {
        keyManager = EcdhKeyManager()
        val cipherEngine = AesGcmCipherEngine()
        val signatureValidator = EcdsaSignatureValidator()
        val secretDeriver = HkdfSecretDeriver()

        orchestrator = DefaultCryptoOrchestrator(
            keyManager,
            cipherEngine,
            signatureValidator,
            secretDeriver
        )
    }

    @Test
    fun `decryption should fail with tampered signature`() = runTest {
        val aliceIdentity = keyManager.generateIdentityKeyPair().getOrThrow()
        val bobIdentity = keyManager.generateIdentityKeyPair().getOrThrow()
        val originalMessage = "Test message".toByteArray()

        val envelope = orchestrator.encryptMessage(
            originalMessage,
            aliceIdentity.privateKey,
            aliceIdentity.publicKey,
            bobIdentity.publicKey
        ).getOrThrow()

        // Tamper with signature
        val tamperedEnvelope = envelope.copy(
            receiverSignature = ByteArray(envelope.receiverSignature.size) { 0 }
        )

        val result = orchestrator.decryptMessage(
            tamperedEnvelope,
            bobIdentity.privateKey,
            bobIdentity.publicKey,
            aliceIdentity.publicKey,
            false
        )

        // Should fail with SecurityException
        assertTrue("Result should be failure", result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull("Exception should not be null", exception)

        // Check that it's wrapped as SecurityException
        assertTrue(
            "Expected SecurityException but got ${exception?.javaClass?.simpleName}: ${exception?.message}",
            exception is SecurityException
        )
        assertTrue(
            "Exception message should mention signature verification",
            exception?.message?.contains("Signature verification failed", ignoreCase = true) == true
        )
    }

    @Test
    fun `decryption should fail with tampered ciphertext`() = runTest {
        val aliceIdentity = keyManager.generateIdentityKeyPair().getOrThrow()
        val bobIdentity = keyManager.generateIdentityKeyPair().getOrThrow()
        val originalMessage = "Test message".toByteArray()

        val envelope = orchestrator.encryptMessage(
            originalMessage,
            aliceIdentity.privateKey,
            aliceIdentity.publicKey,
            bobIdentity.publicKey
        ).getOrThrow()

        // Tamper with ciphertext (signature is still valid)
        val tamperedEnvelope = envelope.copy(
            ciphertext = ByteArray(envelope.ciphertext.size) { 0 }
        )

        val result = orchestrator.decryptMessage(
            tamperedEnvelope,
            bobIdentity.privateKey,
            bobIdentity.publicKey,
            aliceIdentity.publicKey,
            false
        )

        // Should fail during AEAD decryption (tag mismatch)
        assertTrue("Result should be failure", result.isFailure)
    }

    @Test
    fun `decryption should fail with tampered nonce`() = runTest {
        val aliceIdentity = keyManager.generateIdentityKeyPair().getOrThrow()
        val bobIdentity = keyManager.generateIdentityKeyPair().getOrThrow()
        val originalMessage = "Test message".toByteArray()

        val envelope = orchestrator.encryptMessage(
            originalMessage,
            aliceIdentity.privateKey,
            aliceIdentity.publicKey,
            bobIdentity.publicKey
        ).getOrThrow()

        // Tamper with nonce
        val tamperedEnvelope = envelope.copy(
            nonce = ByteArray(envelope.nonce.size) { 0 }
        )

        val result = orchestrator.decryptMessage(
            tamperedEnvelope,
            bobIdentity.privateKey,
            bobIdentity.publicKey,
            aliceIdentity.publicKey,
            false
        )

        // Should fail during AEAD decryption
        assertTrue("Result should be failure", result.isFailure)
    }

    @Test
    fun `full encryption and decryption flow should work`() = runTest {
        val aliceIdentity = keyManager.generateIdentityKeyPair().getOrThrow()
        val bobIdentity = keyManager.generateIdentityKeyPair().getOrThrow()

        val originalMessage = "سلام! این یک پیام رمزنگاری شده است.".toByteArray()

        // Alice encrypts for Bob
        val envelope = orchestrator.encryptMessage(
            originalMessage,
            aliceIdentity.privateKey,
            aliceIdentity.publicKey,
            bobIdentity.publicKey
        ).getOrThrow()

        println(envelope.ciphertext)

        // Bob decrypts from Alice
        val decryptResult = orchestrator.decryptMessage(
            envelope,
            bobIdentity.privateKey,
            bobIdentity.publicKey,
            aliceIdentity.publicKey,
            false
        )

        assertTrue("Decryption should succeed", decryptResult.isSuccess)
        val decryptedMessage = decryptResult.getOrThrow()
        assertArrayEquals(originalMessage, decryptedMessage)
    }

    @Test
    fun `encryption with same plaintext should produce different ciphertext`() = runTest {
        val aliceIdentity = keyManager.generateIdentityKeyPair().getOrThrow()
        val bobIdentity = keyManager.generateIdentityKeyPair().getOrThrow()
        val message = "Same message".toByteArray()

        val envelope1 = orchestrator.encryptMessage(
            message,
            aliceIdentity.privateKey,
            aliceIdentity.publicKey,
            bobIdentity.publicKey
        ).getOrThrow()

        val envelope2 = orchestrator.encryptMessage(
            message,
            aliceIdentity.privateKey,
            aliceIdentity.publicKey,
            bobIdentity.publicKey,
        ).getOrThrow()

        // Different ephemeral keys and nonces should produce different ciphertexts
        assertFalse(
            "Ephemeral keys should be different",
            envelope1.receiverEphemeralPublicKey.contentEquals(envelope2.receiverEphemeralPublicKey)
        )
        assertFalse(
            "Ciphertexts should be different",
            envelope1.ciphertext.contentEquals(envelope2.ciphertext)
        )
        assertFalse(
            "Nonces should be different",
            envelope1.nonce.contentEquals(envelope2.nonce)
        )
    }

    @Test
    fun `decryption with wrong recipient key should fail`() = runTest {
        val aliceIdentity = keyManager.generateIdentityKeyPair().getOrThrow()
        val bobIdentity = keyManager.generateIdentityKeyPair().getOrThrow()
        val eveIdentity = keyManager.generateIdentityKeyPair().getOrThrow()

        val message = "Secret for Bob".toByteArray()

        // Alice encrypts for Bob
        val envelope = orchestrator.encryptMessage(
            message,
            aliceIdentity.privateKey,
            aliceIdentity.publicKey,
            bobIdentity.publicKey
        ).getOrThrow()

        // Eve tries to decrypt (should fail)
        val result = orchestrator.decryptMessage(
            envelope,
            eveIdentity.privateKey,
            eveIdentity.publicKey,
            aliceIdentity.publicKey,
            false
        )

        assertTrue("Decryption should fail", result.isFailure)
    }

    @Test
    fun `sender should be able to decrypt their own sent message`() = runTest {
        val aliceIdentity = keyManager.generateIdentityKeyPair().getOrThrow()
        val bobIdentity = keyManager.generateIdentityKeyPair().getOrThrow()
        val originalMessage = "Alice sends to Bob".toByteArray()

        val envelope = orchestrator.encryptMessage(
            originalMessage,
            aliceIdentity.privateKey,
            aliceIdentity.publicKey,
            bobIdentity.publicKey
        ).getOrThrow()

        val aliceDecryptResult = orchestrator.decryptMessage(
            envelope,
            aliceIdentity.privateKey,
            aliceIdentity.publicKey,
            bobIdentity.publicKey,
            iAmSender = true
        )

        assertTrue("Alice should decrypt her own message", aliceDecryptResult.isSuccess)
        val aliceDecrypted = aliceDecryptResult.getOrThrow()
        assertArrayEquals(originalMessage, aliceDecrypted)

        val bobDecryptResult = orchestrator.decryptMessage(
            envelope,
            bobIdentity.privateKey,
            bobIdentity.publicKey,
            aliceIdentity.publicKey,
            iAmSender = false
        )

        assertTrue("Bob should also decrypt the message", bobDecryptResult.isSuccess)
        val bobDecrypted = bobDecryptResult.getOrThrow()
        assertArrayEquals(originalMessage, bobDecrypted)
    }
}