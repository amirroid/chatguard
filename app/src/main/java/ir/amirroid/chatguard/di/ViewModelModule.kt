package ir.amirroid.chatguard.di

import ir.amirroid.chatguard.features.home.HomeViewModel
import ir.amirroid.chatguard.features.intro.IntroViewModel
import ir.amirroid.chatguard.features.main.MainViewModel
import ir.amirroid.chatguard.features.messages.MessagesViewModel
import ir.amirroid.chatguard.features.web_frame.WebFrameViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::MainViewModel)
    viewModelOf(::MessagesViewModel)
    viewModelOf(::WebFrameViewModel)
    viewModelOf(::IntroViewModel)
    viewModelOf(::HomeViewModel)
}