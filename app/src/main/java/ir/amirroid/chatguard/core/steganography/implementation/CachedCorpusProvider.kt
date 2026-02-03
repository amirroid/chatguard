package ir.amirroid.chatguard.core.steganography.implementation

import ir.amirroid.chatguard.core.file.abstraction.FileSource
import ir.amirroid.chatguard.core.steganography.abstraction.CorpusProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CachedCorpusProvider(
    private val fileSource: FileSource
) : CorpusProvider {

    // In-memory cache
    private var words: List<String> = emptyList()
    private val wordToIndex: MutableMap<String, Int> = mutableMapOf()

    override suspend fun load(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val newWords = fileSource.open().use { stream ->
                stream.readBytes()
                    .decodeToString()
                    .lines()
            }

            if (newWords.isEmpty()) {
                throw IllegalStateException("Corpus file is empty")
            }

            // Cache words
            words = newWords

            // Build index map
            wordToIndex.clear()
            newWords.forEachIndexed { index, word ->
                wordToIndex[word] = index
            }
        }
    }

    override fun getWord(index: Int): String {
        if (words.isEmpty()) {
            throw IllegalStateException("Corpus not loaded")
        }

        if (index < 0 || index >= words.size) {
            throw IndexOutOfBoundsException("Index $index out of bounds (size: ${words.size})")
        }

        return words[index]
    }

    override fun getIndex(word: String): Int {
        if (words.isEmpty()) {
            throw IllegalStateException("Corpus not loaded")
        }

        return wordToIndex[word] ?: -1
    }

    override fun isLoaded(): Boolean = words.isNotEmpty()

    override suspend fun reload(): Result<Unit> = load()

    override fun getWordCount(): Int = words.size
}