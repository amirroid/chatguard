package ir.sysfail.chatguard.di

import ir.sysfail.chatguard.core.file.abstraction.FileSource
import ir.sysfail.chatguard.core.steganography.abstraction.CorpusProvider
import ir.sysfail.chatguard.core.steganography.abstraction.PoeticDecoder
import ir.sysfail.chatguard.core.steganography.abstraction.PoeticEncoder
import ir.sysfail.chatguard.core.steganography.implementation.CachedCorpusProvider
import ir.sysfail.chatguard.core.steganography.implementation.WordBasedDecoder
import ir.sysfail.chatguard.core.steganography.implementation.WordBasedEncoder
import ir.sysfail.chatguard.core.steganography.implementation.WordsFileSource
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.qualifier
import org.koin.dsl.bind
import org.koin.dsl.module

private val wordsFileSourceQualifier = qualifier("words")


val steganographyModule = module {
    factory(wordsFileSourceQualifier) { WordsFileSource(androidContext()) }.bind<FileSource>()
    single {
        CachedCorpusProvider(
            fileSource = get(wordsFileSourceQualifier)
        )
    }.bind<CorpusProvider>()
    factoryOf(::WordBasedDecoder).bind<PoeticDecoder>()
    factoryOf(::WordBasedEncoder).bind<PoeticEncoder>()
}