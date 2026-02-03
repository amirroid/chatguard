package ir.amirroid.chatguard.core.steganography.abstraction

/**
 * Provides Persian word corpus for encoding/decoding
 */
interface CorpusProvider {
    
    // Load words from assets
    suspend fun load(): Result<Unit>
    
    // Get word by index
    fun getWord(index: Int): String
    
    // Get index of word (returns -1 if not found)
    fun getIndex(word: String): Int
    
    // Check if corpus is loaded
    fun isLoaded(): Boolean
    
    // Reload corpus
    suspend fun reload(): Result<Unit>
    
    // Get total word count
    fun getWordCount(): Int
}