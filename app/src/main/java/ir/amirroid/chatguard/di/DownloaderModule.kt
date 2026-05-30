package ir.amirroid.chatguard.di

import ir.amirroid.chatguard.core.download.abstraction.Downloader
import ir.amirroid.chatguard.core.download.implementation.DownloadManagerDownloaderImpl
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val downloaderModule = module {
    factoryOf(::DownloadManagerDownloaderImpl).bind<Downloader>()
}