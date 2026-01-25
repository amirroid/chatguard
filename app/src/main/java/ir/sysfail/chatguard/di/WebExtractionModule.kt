package ir.sysfail.chatguard.di

import ir.sysfail.chatguard.core.messanger.models.MessengerPlatform
import ir.sysfail.chatguard.core.web_content_extractor.abstraction.PlatformExtractionStrategy
import ir.sysfail.chatguard.core.web_content_extractor.abstraction.WebContentExtractor
import ir.sysfail.chatguard.core.web_content_extractor.implementation.ContentExtractorFactory
import ir.sysfail.chatguard.core.web_content_extractor.implementation.strategy.SoroushExtractionStrategy
import org.koin.core.qualifier.qualifier
import org.koin.dsl.module

val webExtractionModule = module {
    factory<PlatformExtractionStrategy>(qualifier = qualifier(MessengerPlatform.SOROUSH)) {
        SoroushExtractionStrategy()
    }

    factory<WebContentExtractor> { (platform: MessengerPlatform) ->
        val strategy = get<PlatformExtractionStrategy>(qualifier(platform))

        ContentExtractorFactory.createWithStrategy(strategy)
    }
}