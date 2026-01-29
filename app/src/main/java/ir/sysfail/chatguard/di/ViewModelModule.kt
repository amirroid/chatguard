package ir.sysfail.chatguard.di

import ir.sysfail.chatguard.features.home.HomeViewModel
import ir.sysfail.chatguard.features.intro.IntroViewModel
import ir.sysfail.chatguard.features.main.MainViewModel
import ir.sysfail.chatguard.features.messages.MessagesViewModel
import ir.sysfail.chatguard.features.web_frame.WebFrameViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::MainViewModel)
    viewModelOf(::MessagesViewModel)
    viewModelOf(::WebFrameViewModel)
    viewModelOf(::IntroViewModel)
    viewModelOf(::HomeViewModel)
}