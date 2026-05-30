package ir.amirroid.chatguard.crypto

import ir.amirroid.chatguard.core.crypto.abstraction.CryptoOrchestrator
import ir.amirroid.chatguard.core.crypto.abstraction.KeyManager
import ir.amirroid.chatguard.core.crypto.implementation.AesGcmCipherEngine
import ir.amirroid.chatguard.core.crypto.implementation.DefaultCryptoOrchestrator
import ir.amirroid.chatguard.core.crypto.implementation.EcdhKeyManager
import ir.amirroid.chatguard.core.crypto.implementation.HkdfSecretDeriver
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CryptoOrchestratorTest {

    private lateinit var orchestrator: CryptoOrchestrator
    private lateinit var keyManager: KeyManager

    @Before
    fun setup() {
        keyManager = EcdhKeyManager()
        orchestrator = DefaultCryptoOrchestrator(
            cipherEngine = AesGcmCipherEngine(),
            secretDeriver = HkdfSecretDeriver(),
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
            bobIdentity.publicKey,
        ).getOrThrow()

        val tamperedEnvelope = envelope.copy(
            ciphertext = ByteArray(envelope.ciphertext.size) { 0 },
        )

        val result = orchestrator.decryptMessage(
            tamperedEnvelope,
            bobIdentity.privateKey,
            bobIdentity.publicKey,
            aliceIdentity.publicKey,
            iAmSender = false,
        )

        assertTrue(result.isFailure)
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
            bobIdentity.publicKey,
        ).getOrThrow()

        val tamperedEnvelope = envelope.copy(
            nonce = ByteArray(envelope.nonce.size) { 0 },
        )

        val result = orchestrator.decryptMessage(
            tamperedEnvelope,
            bobIdentity.privateKey,
            bobIdentity.publicKey,
            aliceIdentity.publicKey,
            iAmSender = false,
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `full encryption and decryption flow should work`() = runTest {
        val aliceIdentity = keyManager.generateIdentityKeyPair().getOrThrow()
        val bobIdentity = keyManager.generateIdentityKeyPair().getOrThrow()
        val originalMessage = "سلام! این یک پیام رمزنگاری شده است.".toByteArray()

        val envelope = orchestrator.encryptMessage(
            originalMessage,
            aliceIdentity.privateKey,
            aliceIdentity.publicKey,
            bobIdentity.publicKey,
        ).getOrThrow()

        val decryptedMessage = orchestrator.decryptMessage(
            envelope,
            bobIdentity.privateKey,
            bobIdentity.publicKey,
            aliceIdentity.publicKey,
            iAmSender = false,
        ).getOrThrow()

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
            bobIdentity.publicKey,
        ).getOrThrow()

        val envelope2 = orchestrator.encryptMessage(
            message,
            aliceIdentity.privateKey,
            aliceIdentity.publicKey,
            bobIdentity.publicKey,
        ).getOrThrow()

        assertFalse(
            "Nonces should differ per message",
            envelope1.nonce.contentEquals(envelope2.nonce),
        )
        assertFalse(
            "Ciphertexts should differ",
            envelope1.ciphertext.contentEquals(envelope2.ciphertext),
        )
    }

    @Test
    fun `decryption with wrong recipient key should fail`() = runTest {
        val aliceIdentity = keyManager.generateIdentityKeyPair().getOrThrow()
        val bobIdentity = keyManager.generateIdentityKeyPair().getOrThrow()
        val eveIdentity = keyManager.generateIdentityKeyPair().getOrThrow()
        val message = "Secret for Bob".toByteArray()

        val envelope = orchestrator.encryptMessage(
            message,
            aliceIdentity.privateKey,
            aliceIdentity.publicKey,
            bobIdentity.publicKey,
        ).getOrThrow()

        val result = orchestrator.decryptMessage(
            envelope,
            eveIdentity.privateKey,
            eveIdentity.publicKey,
            aliceIdentity.publicKey,
            iAmSender = false,
        )

        assertTrue(result.isFailure)
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
            bobIdentity.publicKey,
        ).getOrThrow()

        val aliceDecrypted = orchestrator.decryptMessage(
            envelope,
            aliceIdentity.privateKey,
            aliceIdentity.publicKey,
            bobIdentity.publicKey,
            iAmSender = true,
        ).getOrThrow()

        val bobDecrypted = orchestrator.decryptMessage(
            envelope,
            bobIdentity.privateKey,
            bobIdentity.publicKey,
            aliceIdentity.publicKey,
            iAmSender = false,
        ).getOrThrow()

        assertArrayEquals(originalMessage, aliceDecrypted)
        assertArrayEquals(originalMessage, bobDecrypted)
    }
}
