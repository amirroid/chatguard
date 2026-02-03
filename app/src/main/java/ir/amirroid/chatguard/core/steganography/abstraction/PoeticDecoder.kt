package ir.amirroid.chatguard.core.steganography.abstraction

/**
 * Decodes Persian words back to binary data
 */
interface PoeticDecoder {
    
    // Extract original bytes from Persian words
    suspend fun decode(poeticText: String): Result<ByteArray>
    
    // Validate that text can be decoded
    suspend fun validate(poeticText: String): Result<Boolean>
}
