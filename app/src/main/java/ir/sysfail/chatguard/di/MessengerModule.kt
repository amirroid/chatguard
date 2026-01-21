package ir.sysfail.chatguard.di

import ir.sysfail.chatguard.core.messanger.abstraction.MessengerNodeReader
import ir.sysfail.chatguard.core.messanger.implementation.BaleMessengerNodeReader
import ir.sysfail.chatguard.core.messanger.implementation.EitaaMessengerNodeReader
import ir.sysfail.chatguard.core.messanger.implementation.SoroushMessengerNodeReader
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val messengerModule = module {
    factoryOf(::EitaaMessengerNodeReader).bind<MessengerNodeReader>()
    factoryOf(::BaleMessengerNodeReader).bind<MessengerNodeReader>()
    factoryOf(::SoroushMessengerNodeReader).bind<MessengerNodeReader>()
}