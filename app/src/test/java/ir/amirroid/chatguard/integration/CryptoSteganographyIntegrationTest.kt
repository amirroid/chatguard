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

    @Test
    fun `find maximum Persian message size that encrypts under 4096 characters`() = runTest {
        corpusProvider.load().getOrThrow()

        val alice = keyManager.generateIdentityKeyPair().getOrThrow()
        val bob = keyManager.generateIdentityKeyPair().getOrThrow()

        val persianTexts = listOf(
            "در دنیای امروز، امنیت اطلاعات یکی از مهم‌ترین دغدغه‌های بشر است. ",
            "زمانی که خورشید از پشت کوه‌های البرز طلوع می‌کند، نور طلایی آن تمام دشت را فرا می‌گیرد. ",
            "تکنولوژی رمزنگاری پیشرفت‌های چشمگیری در سال‌های اخیر داشته و امنیت بیشتری را فراهم کرده است. ",
            "شعر و ادبیات فارسی یکی از غنی‌ترین میراث‌های فرهنگی ایران زمین محسوب می‌شود که قرن‌ها الهام‌بخش بوده است. ",
            "علم و دانش همواره کلید پیشرفت جوامع بشری بوده و خواهد بود، چرا که بدون دانش نمی‌توان به تکامل رسید. "
        )

        println("\n${"=".repeat(100)}")
        println("Testing Maximum Persian Message Size (Target: Poetic Output < 4096 chars)")
        println("=".repeat(100))

        val results = mutableListOf<SizeTestResult>()
        val testSizes = listOf(50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000)

        persianTexts.forEachIndexed { textIndex, baseText ->
            println("\nBase Text ${textIndex + 1}: ${baseText.take(50)}...")
            println("-".repeat(100))

            testSizes.forEach { targetSize ->
                val message = buildString {
                    while (length < targetSize) {
                        append(baseText)
                    }
                }.take(targetSize)

                try {
                    val envelope = cryptoOrchestrator.encryptMessage(
                        plaintext = message.toByteArray(),
                        myIdentityPrivateKey = alice.privateKey,
                        myIdentityPublicKey = alice.publicKey,
                        theirIdentityPublicKey = bob.publicKey
                    ).getOrThrow()

                    val envelopeBytes = CryptoEnvelopeSerializer.serialize(envelope)
                    val poeticText = poeticEncoder.encode(envelopeBytes).getOrThrow()

                    val poeticLength = poeticText.length
                    val isUnderLimit = poeticLength < 4096
                    val status = if (isUnderLimit) "PASS" else "FAIL"

                    results.add(
                        SizeTestResult(
                            textId = textIndex + 1,
                            originalLength = message.length,
                            encryptedBytes = envelopeBytes.size,
                            poeticLength = poeticLength,
                            ratio = poeticLength.toDouble() / message.length,
                            isUnderLimit = isUnderLimit
                        )
                    )

                    println("Original: %4d | Encrypted: %5d bytes | Poetic: %4d chars | Ratio: %.2fx | %s".format(
                        message.length,
                        envelopeBytes.size,
                        poeticLength,
                        poeticLength.toDouble() / message.length,
                        status
                    ))

                } catch (e: Exception) {
                    println("Original: %4d | ERROR: ${e.message}".format(targetSize))
                }
            }
        }

        println("\n${"=".repeat(100)}")
        println("RESULTS SUMMARY")
        println("=".repeat(100))

        val validResults = results.filter { it.isUnderLimit }
        val maxValidResult = validResults.maxByOrNull { it.originalLength }

        println("\nTotal Tests: ${results.size}")
        println("Passed (< 4096): ${validResults.size}")
        println("Failed (>= 4096): ${results.size - validResults.size}")

        println("\nMAXIMUM ALLOWED MESSAGE SIZE:")
        maxValidResult?.let {
            println("  Original Length: ${it.originalLength} chars")
            println("  Encrypted Size: ${it.encryptedBytes} bytes")
            println("  Poetic Output: ${it.poeticLength} chars")
            println("  Inflation Ratio: %.2fx".format(it.ratio))
            println("  Usage: %.2f%%".format((it.poeticLength.toDouble() / 4096) * 100))
        }

        val closestResult = validResults.maxByOrNull { it.poeticLength }
        println("\nCLOSEST TO LIMIT (4096):")
        closestResult?.let {
            println("  Original Length: ${it.originalLength} chars")
            println("  Poetic Output: ${it.poeticLength} chars")
            println("  Remaining: ${4096 - it.poeticLength} chars")
        }

        val avgRatio = validResults.map { it.ratio }.average()
        println("\nAverage Inflation Ratio: %.2fx".format(avgRatio))

        println("\n${"=".repeat(100)}")
        println("PER BASE TEXT SUMMARY:")
        println("=".repeat(100))

        persianTexts.indices.forEach { textIndex ->
            val textResults = results.filter { it.textId == textIndex + 1 && it.isUnderLimit }
            val maxForThisText = textResults.maxByOrNull { it.originalLength }

            if (maxForThisText != null) {
                println("\nText ${textIndex + 1}:")
                println("  Max Original: ${maxForThisText.originalLength} chars")
                println("  Poetic Output: ${maxForThisText.poeticLength} chars")
                println("  Ratio: %.2fx".format(maxForThisText.ratio))
            }
        }

        println("\n${"=".repeat(100)}")
        println("FINAL RESULT: Maximum Persian message size = ${maxValidResult?.originalLength} chars")
        println("              Results in ${maxValidResult?.poeticLength} poetic chars")
        println("=".repeat(100))
    }

    data class SizeTestResult(
        val textId: Int,
        val originalLength: Int,
        val encryptedBytes: Int,
        val poeticLength: Int,
        val ratio: Double,
        val isUnderLimit: Boolean
    )
}