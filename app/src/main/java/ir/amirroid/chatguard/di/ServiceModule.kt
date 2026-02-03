package ir.amirroid.chatguard.di

import ir.amirroid.chatguard.core.floating_button.FloatingButtonController
import ir.amirroid.chatguard.services.MessengerAccessibilityStateManager
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val serviceModule = module {
    factoryOf(::FloatingButtonController)
    factory {
        MessengerAccessibilityStateManager(
            readers = getAll(),
            checkPublicKeyExistsUseCase = get(),
            messageStateBridge = get(),
        )
    }
}