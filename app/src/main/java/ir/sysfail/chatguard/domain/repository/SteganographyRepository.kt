package ir.sysfail.chatguard.domain.repository

interface SteganographyRepository {
    suspend fun initialize()
    suspend fun encodeMessage(data: ByteArray): Result<String>
    suspend fun decodeMessage(poeticText: String): Result<ByteArray>
}