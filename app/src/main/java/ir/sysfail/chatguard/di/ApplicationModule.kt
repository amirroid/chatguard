package ir.sysfail.chatguard.di

import ir.sysfail.chatguard.core.initializer.abstraction.StartupInitializer
import ir.sysfail.chatguard.core.initializer.implementation.SteganographyStartupInitializer
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val applicationModule = module {
    factoryOf(::SteganographyStartupInitializer).bind<StartupInitializer>()
}