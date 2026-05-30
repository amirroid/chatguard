package ir.amirroid.chatguard.integration

import ir.amirroid.chatguard.FileUtils
import ir.amirroid.chatguard.core.crypto.abstraction.CipherEngine
import ir.amirroid.chatguard.core.crypto.abstraction.CryptoOrchestrator
import ir.amirroid.chatguard.core.crypto.abstraction.KeyManager
import ir.amirroid.chatguard.core.crypto.abstraction.SharedSecretDeriver
import ir.amirroid.chatguard.core.crypto.implementation.AesGcmCipherEngine
import ir.amirroid.chatguard.core.crypto.implementation.DefaultCryptoOrchestrator
import ir.amirroid.chatguard.core.crypto.implementation.EcdhKeyManager
import ir.amirroid.chatguard.core.crypto.implementation.HkdfSecretDeriver
import ir.amirroid.chatguard.core.crypto.util.CryptoEnvelopeSerializer
import ir.amirroid.chatguard.core.file.implementation.ByteArrayFileSource
import ir.amirroid.chatguard.core.steganography.abstraction.CorpusProvider
import ir.amirroid.chatguard.core.steganography.abstraction.PoeticDecoder
import ir.amirroid.chatguard.core.steganography.abstraction.PoeticEncoder
import ir.amirroid.chatguard.core.steganography.implementation.CachedCorpusProvider
import ir.amirroid.chatguard.core.steganography.implementation.WordBasedDecoder
import ir.amirroid.chatguard.core.steganography.implementation.WordBasedEncoder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CryptoSteganographyIntegrationTest {

    private lateinit var keyManager: KeyManager
    private lateinit var cryptoOrchestrator: CryptoOrchestrator
    private lateinit var corpusProvider: CorpusProvider
    private lateinit var poeticEncoder: PoeticEncoder
    private lateinit var poeticDecoder: PoeticDecoder

    @Before
    fun setup() {
        keyManager = EcdhKeyManager()
        cryptoOrchestrator = DefaultCryptoOrchestrator(
            cipherEngine = AesGcmCipherEngine(),
            secretDeriver = HkdfSecretDeriver(),
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
            theirIdentityPublicKey = bob.publicKey,
        ).getOrThrow()

        val poeticText = poeticEncoder.encode(CryptoEnvelopeSerializer.serialize(envelope)).getOrThrow()

        println(poeticText)
        println(poeticText.length)
        println(message.length)

        val decodedEnvelope = CryptoEnvelopeSerializer.deserialize(poeticDecoder.decode(poeticText).getOrThrow())

        val decrypted = cryptoOrchestrator.decryptMessage(
            envelope = decodedEnvelope,
            myIdentityPrivateKey = bob.privateKey,
            myIdentityPublicKey = bob.publicKey,
            theirIdentityPublicKey = alice.publicKey,
            iAmSender = false,
        ).getOrThrow()

        assertEquals(message, String(decrypted))
    }

    @Test
    fun `sender can decrypt own message after poetic round-trip`() = runTest {
        corpusProvider.load().getOrThrow()

        val alice = keyManager.generateIdentityKeyPair().getOrThrow()
        val bob = keyManager.generateIdentityKeyPair().getOrThrow()
        val message = "سلام! این یک پیام محرمانه است."

        val envelope = cryptoOrchestrator.encryptMessage(
            plaintext = message.toByteArray(),
            myIdentityPrivateKey = alice.privateKey,
            myIdentityPublicKey = alice.publicKey,
            theirIdentityPublicKey = bob.publicKey,
        ).getOrThrow()

        val poeticText = poeticEncoder.encode(CryptoEnvelopeSerializer.serialize(envelope)).getOrThrow()
        val decodedEnvelope = CryptoEnvelopeSerializer.deserialize(poeticDecoder.decode(poeticText).getOrThrow())

        assertEquals(
            message,
            String(
                cryptoOrchestrator.decryptMessage(
                    decodedEnvelope,
                    bob.privateKey,
                    bob.publicKey,
                    alice.publicKey,
                    iAmSender = false,
                ).getOrThrow(),
            ),
        )
        assertEquals(
            message,
            String(
                cryptoOrchestrator.decryptMessage(
                    decodedEnvelope,
                    alice.privateKey,
                    alice.publicKey,
                    bob.publicKey,
                    iAmSender = true,
                ).getOrThrow(),
            ),
        )
    }

    @Test
    fun `tampering poetic text prevents successful decryption`() = runTest {
        corpusProvider.load().getOrThrow()

        val alice = keyManager.generateIdentityKeyPair().getOrThrow()
        val bob = keyManager.generateIdentityKeyPair().getOrThrow()
        val message = "Secret message"

        val envelope = cryptoOrchestrator.encryptMessage(
            plaintext = message.toByteArray(),
            myIdentityPrivateKey = alice.privateKey,
            myIdentityPublicKey = alice.publicKey,
            theirIdentityPublicKey = bob.publicKey,
        ).getOrThrow()

        val poeticText = poeticEncoder.encode(CryptoEnvelopeSerializer.serialize(envelope)).getOrThrow()
        val words = poeticText.split(Regex("\\s+")).toMutableList()
        for (i in 0 until minOf(5, words.size)) {
            val idx = corpusProvider.getIndex(words[i])
            words[i] = corpusProvider.getWord((idx + 1) % corpusProvider.getWordCount())
        }

        val decodedBytes = poeticDecoder.decode(words.joinToString(" ")).getOrThrow()

        // v2 envelopes may still parse after bit-flips; AES-GCM must reject tampered ciphertext.
        val tamperingDetected = runCatching {
            val tamperedEnvelope = CryptoEnvelopeSerializer.deserialize(decodedBytes)
            cryptoOrchestrator.decryptMessage(
                envelope = tamperedEnvelope,
                myIdentityPrivateKey = bob.privateKey,
                myIdentityPublicKey = bob.publicKey,
                theirIdentityPublicKey = alice.publicKey,
                iAmSender = false,
            ).getOrThrow()
        }.isFailure

        assertTrue(
            "Tampered poetic text must not recover the original plaintext",
            tamperingDetected,
        )
    }

    @Test
    fun `different messages produce different poetic outputs`() = runTest {
        corpusProvider.load().getOrThrow()

        val alice = keyManager.generateIdentityKeyPair().getOrThrow()
        val bob = keyManager.generateIdentityKeyPair().getOrThrow()

        suspend fun encryptToPoetry(plaintext: String): String {
            val envelope = cryptoOrchestrator.encryptMessage(
                plaintext.toByteArray(),
                alice.privateKey,
                alice.publicKey,
                bob.publicKey,
            ).getOrThrow()
            return poeticEncoder.encode(CryptoEnvelopeSerializer.serialize(envelope)).getOrThrow()
        }

        assertNotEquals(encryptToPoetry("First message"), encryptToPoetry("Second message"))
    }

    @Test
    fun `complete flow - long Persian message`() = runTest {
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
            theirIdentityPublicKey = bob.publicKey,
        ).getOrThrow()

        val poeticText = poeticEncoder.encode(CryptoEnvelopeSerializer.serialize(envelope)).getOrThrow()
        assertTrue(poeticText.length > 1000)

        println(poeticText)
        println(poeticText.length)
        println(message.length)

        val decodedEnvelope = CryptoEnvelopeSerializer.deserialize(poeticDecoder.decode(poeticText).getOrThrow())
        val decrypted = cryptoOrchestrator.decryptMessage(
            decodedEnvelope,
            bob.privateKey,
            bob.publicKey,
            alice.publicKey,
            iAmSender = false,
        ).getOrThrow()

        assertEquals(message, String(decrypted))
    }

    @Test
    fun `decryption fails when peer public key does not match encryption`() = runTest {
        corpusProvider.load().getOrThrow()

        val alice = keyManager.generateIdentityKeyPair().getOrThrow()
        val bob = keyManager.generateIdentityKeyPair().getOrThrow()
        val mallory = keyManager.generateIdentityKeyPair().getOrThrow()

        val envelope = cryptoOrchestrator.encryptMessage(
            plaintext = "Secret".toByteArray(),
            myIdentityPrivateKey = alice.privateKey,
            myIdentityPublicKey = alice.publicKey,
            theirIdentityPublicKey = bob.publicKey,
        ).getOrThrow()

        val result = cryptoOrchestrator.decryptMessage(
            envelope = envelope,
            myIdentityPrivateKey = bob.privateKey,
            myIdentityPublicKey = bob.publicKey,
            theirIdentityPublicKey = mallory.publicKey,
            iAmSender = false,
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `compact envelope stays under poetic limit for larger plaintext`() = runTest {
        corpusProvider.load().getOrThrow()

        val alice = keyManager.generateIdentityKeyPair().getOrThrow()
        val bob = keyManager.generateIdentityKeyPair().getOrThrow()
        val base = "در دنیای امروز، امنیت اطلاعات یکی از مهم‌ترین دغدغه‌های بشر است. "

        var maxUnderLimit = 0
        for (targetSize in listOf(100, 200, 400, 600, 800, 1000, 1200, 1500)) {
            val message = buildString {
                while (length < targetSize) append(base)
            }.take(targetSize)

            val envelope = cryptoOrchestrator.encryptMessage(
                message.toByteArray(),
                alice.privateKey,
                alice.publicKey,
                bob.publicKey,
            ).getOrThrow()
            val poeticLength = poeticEncoder.encode(
                CryptoEnvelopeSerializer.serialize(envelope),
            ).getOrThrow().length

            if (poeticLength < 4096) {
                maxUnderLimit = targetSize
            }
        }

        assertTrue(
            "Simplified crypto should allow at least 600 chars under 4096 poetic limit, got max $maxUnderLimit",
            maxUnderLimit >= 600,
        )
    }
}
