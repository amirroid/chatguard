package ir.amirroid.chatguard.di

import ir.amirroid.chatguard.core.initializer.abstraction.StartupInitializer
import ir.amirroid.chatguard.core.initializer.implementation.SteganographyStartupInitializer
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val applicationModule = module {
    factoryOf(::SteganographyStartupInitializer).bind<StartupInitializer>()
}