package ir.sysfail.chatguard.data.repository

import ir.sysfail.chatguard.core.steganography.abstraction.CorpusProvider
import ir.sysfail.chatguard.core.steganography.abstraction.PoeticDecoder
import ir.sysfail.chatguard.core.steganography.abstraction.PoeticEncoder
import ir.sysfail.chatguard.domain.repository.SteganographyRepository

class SteganographyRepositoryImpl(
    private val corpusProvider: CorpusProvider,
    private val encoder: PoeticEncoder,
    private val decoder: PoeticDecoder
) : SteganographyRepository {
    override suspend fun initialize() {
        if (!corpusProvider.isLoaded()) {
            corpusProvider.load()
        }
    }

    override suspend fun encodeMessage(data: ByteArray): Result<String> = encoder.encode(data)

    override suspend fun decodeMessage(poeticText: String): Result<ByteArray> =
        decoder.decode(poeticText)
}