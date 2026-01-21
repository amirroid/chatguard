package ir.sysfail.chatguard.di

import ir.sysfail.chatguard.features.main.MainViewModel
import ir.sysfail.chatguard.features.messages.MessagesViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::MainViewModel)
    viewModelOf(::MessagesViewModel)
}