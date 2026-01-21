package ir.sysfail.chatguard.core.initializer.implementation

import ir.sysfail.chatguard.core.initializer.abstraction.StartupInitializer
import ir.sysfail.chatguard.core.steganography.abstraction.CorpusProvider

class SteganographyStartupInitializer(
    private val corpusProvider: CorpusProvider
) : StartupInitializer {
    override suspend fun initialize() {
        corpusProvider.load()
    }
}