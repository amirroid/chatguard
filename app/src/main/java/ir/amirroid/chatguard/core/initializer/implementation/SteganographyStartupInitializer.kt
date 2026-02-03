package ir.amirroid.chatguard.core.initializer.implementation

import ir.amirroid.chatguard.core.initializer.abstraction.StartupInitializer
import ir.amirroid.chatguard.core.steganography.abstraction.CorpusProvider

class SteganographyStartupInitializer(
    private val corpusProvider: CorpusProvider
) : StartupInitializer {
    override suspend fun initialize() {
        corpusProvider.load()
    }
}