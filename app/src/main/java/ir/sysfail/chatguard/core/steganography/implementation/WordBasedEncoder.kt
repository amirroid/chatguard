package ir.sysfail.chatguard.core.steganography.implementation

import ir.sysfail.chatguard.core.steganography.abstraction.CorpusProvider
import ir.sysfail.chatguard.core.steganography.abstraction.PoeticEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.log2

/**
 * Encodes binary data as Persian words using positional encoding
 *
 * Algorithm:
 * - Each word represents a number (its index in corpus)
 * - Multiple bytes encoded per word based on corpus size
 * - Example with 256 words: each word = 1 byte
 * - Example with 4096 words: each word = ~1.5 bytes
 */
class WordBasedEncoder(
    private val corpusProvider: CorpusProvider
) : PoeticEncoder {

    companion object {
        private const val WORDS_PER_LINE = 6
    }

    override suspend fun encode(data: ByteArray): Result<String> =
        withContext(Dispatchers.Default) {
            runCatching {
                val corpusSize = corpusProvider.getWordCount()

                // Calculate bits per word
                val bitsPerWord = log2(corpusSize.toDouble()).toInt()

                // Convert bytes to bit string
                val bitString = data.joinToString("") { byte ->
                    byte.toInt().and(0xFF).toString(2).padStart(8, '0')
                }

                // Split into chunks of bitsPerWord
                val words = mutableListOf<String>()
                var index = 0

                while (index < bitString.length) {
                    val endIndex = minOf(index + bitsPerWord, bitString.length)
                    val chunk = bitString.substring(index, endIndex)

                    // Pad last chunk if needed
                    val paddedChunk = if (chunk.length < bitsPerWord) {
                        chunk.padEnd(bitsPerWord, '0')
                    } else {
                        chunk
                    }

                    // Convert binary chunk to word index
                    val wordIndex = paddedChunk.toInt(2) % corpusSize
                    val word = corpusProvider.getWord(wordIndex)
                    words.add(word)

                    index += bitsPerWord
                }

                // Format as lines (looks more natural)
                words.chunked(WORDS_PER_LINE)
                    .joinToString("\n") { line ->
                        line.joinToString(" ")
                    }
            }
        }

    override fun calculateWordCount(dataSize: Int): Int {
        val corpusSize = corpusProvider.getWordCount()
        val bitsPerWord = log2(corpusSize.toDouble()).toInt()
        val totalBits = dataSize * 8
        return ceil(totalBits.toDouble() / bitsPerWord).toInt()
    }
}