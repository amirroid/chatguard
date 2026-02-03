package ir.amirroid.chatguard.integration

import ir.amirroid.chatguard.FileUtils
import ir.amirroid.chatguard.core.crypto.abstraction.*
import ir.amirroid.chatguard.core.crypto.implementation.*
import ir.amirroid.chatguard.core.crypto.util.CryptoEnvelopeSerializer
import ir.amirroid.chatguard.core.file.implementation.ByteArrayFileSource
import ir.amirroid.chatguard.core.steganography.abstraction.CorpusProvider
import ir.amirroid.chatguard.core.steganography.abstraction.PoeticDecoder
import ir.amirroid.chatguard.core.steganography.abstraction.PoeticEncoder
import ir.amirroid.chatguard.core.steganography.implementation.CachedCorpusProvider
import ir.amirroid.chatguard.core.steganography.implementation.WordBasedDecoder
import ir.amirroid.chatguard.core.steganography.implementation.WordBasedEncoder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CryptoSteganographyIntegrationTest {

    private lateinit var keyManager: KeyManager
    private lateinit var cipherEngine: CipherEngine
    private lateinit var signatureValidator: SignatureValidator
    private lateinit var secretDeriver: SharedSecretDeriver
    private lateinit var cryptoOrchestrator: CryptoOrchestrator

    private lateinit var corpusProvider: CorpusProvider
    private lateinit var poeticEncoder: PoeticEncoder
    private lateinit var poeticDecoder: PoeticDecoder

    @Before
    fun setup() {
        keyManager = EcdhKeyManager()
        cipherEngine = AesGcmCipherEngine()
        signatureValidator = EcdsaSignatureValidator()
        secretDeriver = HkdfSecretDeriver()
        cryptoOrchestrator = DefaultCryptoOrchestrator(
            keyManager, cipherEngine, signatureValidator, secretDeriver
        )

        val wordsBytes = FileUtils.getWordsBytes()

        corpusProvider = CachedCorpusProvider(ByteArrayFileSource(wordsBytes))
        poeticEncoder = WordBasedEncoder(corpusProvider)
        poeticDecoder = WordBasedDecoder(corpusProvider)
    }

    @Test
    fun `complete flow - encrypt and encode Persian message`() = runTest {
        corpusProvider.load().getOrThrow()

        val alice = keyManager.generateIdentityKeyPair().getOrThrow()
        val bob = keyManager.generateIdentityKeyPair().getOrThrow()
        val message = "سلام! این یک پیام محرمانه است."

        val envelope = cryptoOrchestrator.encryptMessage(
            plaintext = message.toByteArray(),
            myIdentityPrivateKey = alice.privateKey,
            myIdentityPublicKey = alice.publicKey,
            theirIdentityPublicKey = bob.publicKey
        ).getOrThrow()

        val envelopeBytes = CryptoEnvelopeSerializer.serialize(envelope)
        val poeticText = poeticEncoder.encode(envelopeBytes).getOrThrow()

        val decodedBytes = poeticDecoder.decode(poeticText).getOrThrow()
        val decodedEnvelope = CryptoEnvelopeSerializer.deserialize(decodedBytes)

        val decrypted = cryptoOrchestrator.decryptMessage(
            envelope = decodedEnvelope,
            myIdentityPrivateKey = bob.privateKey,
            myIdentityPublicKey = bob.publicKey,
            theirIdentityPublicKey = alice.publicKey,
            iAmSender = false
        ).getOrThrow()

        assertEquals(message, String(decrypted))
    }

    @Test
    fun `sender can decrypt own message`() = runTest {
        corpusProvider.load().getOrThrow()

        val alice = keyManager.generateIdentityKeyPair().getOrThrow()
        val bob = keyManager.generateIdentityKeyPair().getOrThrow()
        val message = "سلام! این یک پیام محرمانه است."

        val envelope = cryptoOrchestrator.encryptMessage(
            plaintext = message.toByteArray(),
            myIdentityPrivateKey = alice.privateKey,
            myIdentityPublicKey = alice.publicKey,
            theirIdentityPublicKey = bob.publicKey
        ).getOrThrow()

        val envelopeBytes = CryptoEnvelopeSerializer.serialize(envelope)
        val poeticText = poeticEncoder.encode(envelopeBytes).getOrThrow()

        val decodedBytes = poeticDecoder.decode(poeticText).getOrThrow()
        val decodedEnvelope = CryptoEnvelopeSerializer.deserialize(decodedBytes)

        val bobDecrypted = cryptoOrchestrator.decryptMessage(
            envelope = decodedEnvelope,
            myIdentityPrivateKey = bob.privateKey,
            myIdentityPublicKey = bob.publicKey,
            theirIdentityPublicKey = alice.publicKey,
            iAmSender = false
        ).getOrThrow()

        assertEquals(message, String(bobDecrypted))

        val aliceDecrypted = cryptoOrchestrator.decryptMessage(
            envelope = decodedEnvelope,
            myIdentityPrivateKey = alice.privateKey,
            myIdentityPublicKey = alice.publicKey,
            theirIdentityPublicKey = bob.publicKey,
            iAmSender = true
        ).getOrThrow()

        assertEquals(message, String(aliceDecrypted))
    }

    @Test
    fun `tampering detection - modified poetic text should fail`() = runTest {
        corpusProvider.load().getOrThrow()

        val alice = keyManager.generateIdentityKeyPair().getOrThrow()
        val bob = keyManager.generateIdentityKeyPair().getOrThrow()
        val message = "Secret message"

        val envelope = cryptoOrchestrator.encryptMessage(
            plaintext = message.toByteArray(),
            myIdentityPrivateKey = alice.privateKey,
            myIdentityPublicKey = alice.publicKey,
            theirIdentityPublicKey = bob.publicKey
        ).getOrThrow()

        val envelopeBytes = CryptoEnvelopeSerializer.serialize(envelope)
        val poeticText = poeticEncoder.encode(envelopeBytes).getOrThrow()

        val words = poeticText.split(Regex("\\s+")).toMutableList()
        for (i in 0 until minOf(5, words.size)) {
            val idx = corpusProvider.getIndex(words[i])
            words[i] = corpusProvider.getWord((idx + 1) % corpusProvider.getWordCount())
        }
        val tampered = words.joinToString(" ")

        val decodedBytes = poeticDecoder.decode(tampered).getOrThrow()

        assertThrows(IllegalArgumentException::class.java) {
            CryptoEnvelopeSerializer.deserialize(decodedBytes)
        }
    }

    @Test
    fun `different messages produce different poetic outputs`() = runTest {
        corpusProvider.load().getOrThrow()

        val alice = keyManager.generateIdentityKeyPair().getOrThrow()
        val bob = keyManager.generateIdentityKeyPair().getOrThrow()

        val envelope1 = cryptoOrchestrator.encryptMessage(
            plaintext = "First message".toByteArray(),
            myIdentityPrivateKey = alice.privateKey,
            myIdentityPublicKey = alice.publicKey,
            theirIdentityPublicKey = bob.publicKey
        ).getOrThrow()

        val envelope2 = cryptoOrchestrator.encryptMessage(
            plaintext = "Second message".toByteArray(),
            myIdentityPrivateKey = alice.privateKey,
            myIdentityPublicKey = alice.publicKey,
            theirIdentityPublicKey = bob.publicKey
        ).getOrThrow()

        val poetic1 = poeticEncoder.encode(CryptoEnvelopeSerializer.serialize(envelope1)).getOrThrow()
        val poetic2 = poeticEncoder.encode(CryptoEnvelopeSerializer.serialize(envelope2)).getOrThrow()

        assertNotEquals(poetic1, poetic2)
    }

    @Test
    fun `complete flow - encrypt and encode very long Persian message`() = runTest {
        corpusProvider.load().getOrThrow()

        val alice = keyManager.generateIdentityKeyPair().getOrThrow()
        val bob = keyManager.generateIdentityKeyPair().getOrThrow()

        val message = buildString {
            repeat(500) {
                append("این یک پیام بسیار طولانی است که برای تست عملکرد سیستم انکریپت و استگانوگرافی استفاده می‌شود. ")
            }
        }

        val envelope = cryptoOrchestrator.encryptMessage(
            plaintext = message.toByteArray(),
            myIdentityPrivateKey = alice.privateKey,
            myIdentityPublicKey = alice.publicKey,
            theirIdentityPublicKey = bob.publicKey
        ).getOrThrow()

        val envelopeBytes = CryptoEnvelopeSerializer.serialize(envelope)
        val poeticText = poeticEncoder.encode(envelopeBytes).getOrThrow()

        assertTrue(poeticText.length > 1000)

        val decodedBytes = poeticDecoder.decode(poeticText).getOrThrow()
        val decodedEnvelope = CryptoEnvelopeSerializer.deserialize(decodedBytes)

        val decrypted = cryptoOrchestrator.decryptMessage(
            envelope = decodedEnvelope,
            myIdentityPrivateKey = bob.privateKey,
            myIdentityPublicKey = bob.publicKey,
            theirIdentityPublicKey = alice.publicKey,
            iAmSender = false
        ).getOrThrow()

        assertEquals(message, String(decrypted))
    }

    @Test
    fun `verify independent ephemeral keys for perfect forward secrecy`() = runTest {
        corpusProvider.load().getOrThrow()

        val alice = keyManager.generateIdentityKeyPair().getOrThrow()
        val bob = keyManager.generateIdentityKeyPair().getOrThrow()

        val envelope = cryptoOrchestrator.encryptMessage(
            plaintext = "test".toByteArray(),
            myIdentityPrivateKey = alice.privateKey,
            myIdentityPublicKey = alice.publicKey,
            theirIdentityPublicKey = bob.publicKey
        ).getOrThrow()

        assertNotEquals(
            envelope.receiverEphemeralPublicKey.contentHashCode(),
            envelope.senderEphemeralPublicKey.contentHashCode()
        )
    }

    @Test
    fun `MITM attack prevention - wrong sender key fails verification`() = runTest {
        corpusProvider.load().getOrThrow()

        val alice = keyManager.generateIdentityKeyPair().getOrThrow()
        val bob = keyManager.generateIdentityKeyPair().getOrThrow()
        val mallory = keyManager.generateIdentityKeyPair().getOrThrow()

        val envelope = cryptoOrchestrator.encryptMessage(
            plaintext = "Secret".toByteArray(),
            myIdentityPrivateKey = alice.privateKey,
            myIdentityPublicKey = alice.publicKey,
            theirIdentityPublicKey = bob.publicKey
        ).getOrThrow()

        val result = cryptoOrchestrator.decryptMessage(
            envelope = envelope,
            myIdentityPrivateKey = bob.privateKey,
            myIdentityPublicKey = bob.publicKey,
            theirIdentityPublicKey = mallory.publicKey,
            iAmSender = false
        )

        assertTrue(result.isFailure)
    }
}