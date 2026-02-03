package ir.amirroid.chatguard.data.repository

import ir.amirroid.chatguard.core.steganography.abstraction.CorpusProvider
import ir.amirroid.chatguard.core.steganography.abstraction.PoeticDecoder
import ir.amirroid.chatguard.core.steganography.abstraction.PoeticEncoder
import ir.amirroid.chatguard.domain.repository.SteganographyRepository

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