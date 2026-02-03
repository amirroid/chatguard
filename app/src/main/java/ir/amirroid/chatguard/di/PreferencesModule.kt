package ir.amirroid.chatguard.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import ir.amirroid.chatguard.core.preferences.abstraction.PreferenceStorage
import ir.amirroid.chatguard.core.preferences.implementation.DataStorePreferenceStorage
import ir.amirroid.chatguard.utils.Constants
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = Constants.PREFS_NAME
)

val preferencesModule = module {
    single { androidContext().dataStore }
    singleOf(::DataStorePreferenceStorage).bind<PreferenceStorage>()
}