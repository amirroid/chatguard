package ir.amirroid.chatguard.core.steganography.implementation

import ir.amirroid.chatguard.core.steganography.abstraction.CorpusProvider
import ir.amirroid.chatguard.core.steganography.abstraction.PoeticDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.log2

class WordBasedDecoder(
    private val corpusProvider: CorpusProvider
) : PoeticDecoder {

    override suspend fun decode(poeticText: String): Result<ByteArray> =
        withContext(Dispatchers.Default) {
            runCatching {
                // Extract words (ignore newlines and multiple spaces)
                val words = poeticText
                    .split(Regex("\\s+"))
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                if (words.isEmpty()) {
                    throw IllegalArgumentException("No words found in poetic text")
                }

                val corpusSize = corpusProvider.getWordCount()
                val bitsPerWord = log2(corpusSize.toDouble()).toInt()

                // Convert words back to bit string
                val bitString = buildString {
                    words.forEach { word ->
                        val index = corpusProvider.getIndex(word)

                        if (index == -1) {
                            throw IllegalArgumentException("Word '$word' not found in corpus")
                        }

                        // Convert index to binary
                        append(index.toString(2).padStart(bitsPerWord, '0'))
                    }
                }

                // Convert bit string back to bytes
                val bytes = mutableListOf<Byte>()
                var index = 0

                while (index + 8 <= bitString.length) {
                    val byteBits = bitString.substring(index, index + 8)
                    val byteValue = byteBits.toInt(2).toByte()
                    bytes.add(byteValue)
                    index += 8
                }

                bytes.toByteArray()
            }
        }

    override suspend fun validate(poeticText: String): Result<Boolean> =
        withContext(Dispatchers.Default) {
            runCatching {
                // Extract words
                val words = poeticText
                    .split(Regex("\\s+"))
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                if (words.isEmpty()) {
                    return@runCatching false
                }

                // Check if all words exist in corpus
                words.all { word ->
                    corpusProvider.getIndex(word) != -1
                }
            }
        }
}