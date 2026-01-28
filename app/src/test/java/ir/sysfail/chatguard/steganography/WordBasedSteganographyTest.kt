package ir.sysfail.chatguard.steganography

import ir.sysfail.chatguard.core.file.implementation.ByteArrayFileSource
import ir.sysfail.chatguard.core.steganography.abstraction.CorpusProvider
import ir.sysfail.chatguard.core.steganography.abstraction.PoeticDecoder
import ir.sysfail.chatguard.core.steganography.abstraction.PoeticEncoder
import ir.sysfail.chatguard.core.steganography.implementation.CachedCorpusProvider
import ir.sysfail.chatguard.core.steganography.implementation.WordBasedDecoder
import ir.sysfail.chatguard.core.steganography.implementation.WordBasedEncoder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

class WordBasedSteganographyTest {

    private lateinit var corpusProvider: CorpusProvider
    private lateinit var encoder: PoeticEncoder
    private lateinit var decoder: PoeticDecoder

    @Before
    fun setup() {
        val wordsBytes = javaClass.getResourceAsStream("/words.txt")
            ?.use { it.readBytes() }
            ?: throw IllegalStateException("words.txt not found in resources")


        corpusProvider = CachedCorpusProvider(ByteArrayFileSource(wordsBytes))
        encoder = WordBasedEncoder(corpusProvider)
        decoder = WordBasedDecoder(corpusProvider)
    }

    @Test
    fun `corpus should load successfully`() = runTest {
        val result = corpusProvider.load()

        assertTrue(result.isSuccess)
        assertTrue(corpusProvider.isLoaded())
        assertTrue(corpusProvider.getWordCount() > 0)
    }

    @Test
    fun `encode and decode should restore original data`() = runTest {
        corpusProvider.load().getOrThrow()

        val originalData = "Hello World!".toByteArray()

        // Encode to Persian words
        val poeticText = encoder.encode(originalData).getOrThrow()

        println("Original: ${String(originalData)}")
        println("Encoded as Persian:\n$poeticText")

        // Should only contain Persian words
        assertFalse(poeticText.contains("Hello"))
        assertFalse(poeticText.contains("World"))

        // Decode back
        val decodedData = decoder.decode(poeticText).getOrThrow()

        assertArrayEquals(originalData, decodedData)
    }

    @Test
    fun `encode and decode should restore long message`() = runTest {
        corpusProvider.load().getOrThrow()

        val originalText = buildString {
            repeat(2000) {
                append("This is a long test message number $it. ")
                if (it % 10 == 0) append("\n")
            }
        }

        val originalData = originalText.toByteArray()

        val poeticText = encoder.encode(originalData).getOrThrow()

        println("Original length: ${originalData.size}")
        println("Encoded length: ${poeticText.length}")

        val decodedData = decoder.decode(poeticText).getOrThrow()

        assertArrayEquals(originalData, decodedData)
    }

    @Test
    fun `encoded text should only contain corpus words`() = runTest {
        corpusProvider.load().getOrThrow()

        val data = "Test message".toByteArray()
        val poeticText = encoder.encode(data).getOrThrow()

        // Extract all words
        val words = poeticText.split(Regex("\\s+")).filter { it.isNotEmpty() }

        // All words should exist in corpus
        words.forEach { word ->
            val index = corpusProvider.getIndex(word)
            assertTrue("Word '$word' not in corpus", index >= 0)
        }
    }

    @Test
    fun `decoder should validate correct format`() = runTest {
        corpusProvider.load().getOrThrow()

        val data = "Test".toByteArray()
        val poeticText = encoder.encode(data).getOrThrow()

        val isValid = decoder.validate(poeticText).getOrThrow()
        assertTrue(isValid)
    }

    @Test
    fun `decoder should reject text with unknown words`() = runTest {
        corpusProvider.load().getOrThrow()

        val invalidText = "این کلمات نامعتبر هستند xyz123"

        val isValid = decoder.validate(invalidText).getOrThrow()
        assertFalse(isValid)
    }

    @Test
    fun `should handle empty data`() = runTest {
        corpusProvider.load().getOrThrow()

        val emptyData = ByteArray(0)
        val poeticText = encoder.encode(emptyData).getOrThrow()

        // Should produce empty or minimal output
        assertTrue(poeticText.isBlank() || poeticText.split(Regex("\\s+")).size <= 1)
    }

    @Test
    fun `should handle large data`() = runTest {
        corpusProvider.load().getOrThrow()

        // 1KB of random data
        val largeData = ByteArray(1024) { (it % 256).toByte() }

        val poeticText = encoder.encode(largeData).getOrThrow()
        val decodedData = decoder.decode(poeticText).getOrThrow()

        assertArrayEquals(largeData, decodedData)
    }

    @Test
    fun `different data should produce different output`() = runTest {
        corpusProvider.load().getOrThrow()

        val data1 = "Message 1".toByteArray()
        val data2 = "Message 2".toByteArray()

        val poetic1 = encoder.encode(data1).getOrThrow()
        val poetic2 = encoder.encode(data2).getOrThrow()

        assertNotEquals(poetic1, poetic2)
    }

    @Test
    fun `word count calculation should be accurate`() = runTest {
        corpusProvider.load().getOrThrow()

        val data = "سلام چطوری مشتی؟".toByteArray()
        val expectedWords = encoder.calculateWordCount(data.size)

        val poeticText = encoder.encode(data).getOrThrow()
        val actualWords = poeticText.split(Regex("\\s+")).filter { it.isNotEmpty() }.size

        // Should be close (within 1 due to padding)
        assertTrue(abs(actualWords - expectedWords) <= 1)
    }
}