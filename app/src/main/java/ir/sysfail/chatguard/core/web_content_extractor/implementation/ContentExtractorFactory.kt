package ir.sysfail.chatguard.core.web_content_extractor.implementation

import ir.sysfail.chatguard.core.web_content_extractor.abstraction.PlatformExtractionStrategy
import ir.sysfail.chatguard.core.web_content_extractor.abstraction.WebContentExtractor

/**
 * Factory for creating ContentExtractor instances
 */
object ContentExtractorFactory {
    /**
     * Create an extractor with a custom strategy
     */
    fun createWithStrategy(strategy: PlatformExtractionStrategy): WebContentExtractor {
        return DefaultWebContentExtractor(strategy)
    }
}