package ir.sysfail.chatguard.core.steganography.abstraction

/**
 * Encodes binary data into Persian words
 * Each word represents a numeric value from the corpus index
 */
interface PoeticEncoder {

    // Transform encrypted bytes into Persian words
    suspend fun encode(data: ByteArray): Result<String>

    // Calculate required words for given data size
    fun calculateWordCount(dataSize: Int): Int
}