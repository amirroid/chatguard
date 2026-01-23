package ir.sysfail.chatguard

import android.app.Application
import ir.sysfail.chatguard.core.initializer.abstraction.StartupInitializer
import ir.sysfail.chatguard.di.applicationModule
import ir.sysfail.chatguard.di.bridgeModule
import ir.sysfail.chatguard.di.cryptoModule
import ir.sysfail.chatguard.di.databaseModule
import ir.sysfail.chatguard.di.messengerModule
import ir.sysfail.chatguard.di.preferencesModule
import ir.sysfail.chatguard.di.repositoryModule
import ir.sysfail.chatguard.di.serviceModule
import ir.sysfail.chatguard.di.steganographyModule
import ir.sysfail.chatguard.di.useCaseModule
import ir.sysfail.chatguard.di.viewModelModule
import ir.sysfail.chatguard.di.webExtractionModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class ChatGuardApp : Application() {
    private val startupScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@ChatGuardApp)
            modules(
                cryptoModule, steganographyModule, messengerModule, serviceModule, bridgeModule,
                databaseModule, preferencesModule, repositoryModule, useCaseModule, viewModelModule,
                applicationModule, webExtractionModule
            )
        }


        startupScope.launch {
            getKoin().getAll<StartupInitializer>().forEach { initializer ->
                initializer.initialize()
            }

            startupScope.cancel()
        }
    }
}