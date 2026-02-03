package ir.amirroid.chatguard.di

import ir.amirroid.chatguard.core.bridge.implementation.MessengerStateBridge
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val bridgeModule = module {
    singleOf(::MessengerStateBridge)
}