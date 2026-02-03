package ir.amirroid.chatguard.di

import ir.amirroid.chatguard.core.file.abstraction.FileSource
import ir.amirroid.chatguard.core.steganography.abstraction.CorpusProvider
import ir.amirroid.chatguard.core.steganography.abstraction.PoeticDecoder
import ir.amirroid.chatguard.core.steganography.abstraction.PoeticEncoder
import ir.amirroid.chatguard.core.steganography.implementation.CachedCorpusProvider
import ir.amirroid.chatguard.core.steganography.implementation.WordBasedDecoder
import ir.amirroid.chatguard.core.steganography.implementation.WordBasedEncoder
import ir.amirroid.chatguard.core.steganography.implementation.WordsFileSource
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