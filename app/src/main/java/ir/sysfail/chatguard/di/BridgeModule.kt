package ir.sysfail.chatguard.di

import ir.sysfail.chatguard.core.bridge.implementation.MessengerStateBridge
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val bridgeModule = module {
    singleOf(::MessengerStateBridge)
}