package ir.amirroid.chatguard

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationManagerCompat
import ir.amirroid.chatguard.core.exception.GlobalExceptionHandler
import ir.amirroid.chatguard.core.initializer.abstraction.StartupInitializer
import ir.amirroid.chatguard.di.applicationModule
import ir.amirroid.chatguard.di.bridgeModule
import ir.amirroid.chatguard.di.cryptoModule
import ir.amirroid.chatguard.di.databaseModule
import ir.amirroid.chatguard.di.messengerModule
import ir.amirroid.chatguard.di.preferencesModule
import ir.amirroid.chatguard.di.repositoryModule
import ir.amirroid.chatguard.di.serviceModule
import ir.amirroid.chatguard.di.steganographyModule
import ir.amirroid.chatguard.di.useCaseModule
import ir.amirroid.chatguard.di.viewModelModule
import ir.amirroid.chatguard.di.webExtractionModule
import ir.amirroid.chatguard.utils.Constants
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

        NotificationManagerCompat.from(this)
            .createNotificationChannel(
                NotificationChannel(
                    Constants.CHANNEL_ID,
                    "برای استفاده از بکگراند",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )

        if (!BuildConfig.DEBUG) setupGlobalExceptionHandler()

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

    private fun setupGlobalExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(
            GlobalExceptionHandler(
                applicationContext = applicationContext,
                oldHandler = Thread.getDefaultUncaughtExceptionHandler(),
                onCrash = {}
            )
        )
    }
}