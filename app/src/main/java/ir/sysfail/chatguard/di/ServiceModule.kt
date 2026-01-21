package ir.sysfail.chatguard.di

import ir.sysfail.chatguard.core.floating_button.FloatingButtonController
import ir.sysfail.chatguard.services.MessengerAccessibilityStateManager
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