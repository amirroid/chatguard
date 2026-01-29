// core/storage/impl/DataStorePreferenceStorage.kt
package ir.sysfail.chatguard.core.preferences.implementation

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import ir.sysfail.chatguard.core.preferences.abstraction.PreferenceKey
import ir.sysfail.chatguard.core.preferences.abstraction.PreferenceStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class DataStorePreferenceStorage(
    private val dataStore: DataStore<Preferences>
) : PreferenceStorage {

    override suspend fun <T> read(key: PreferenceKey<T>): T? {
        return dataStore.data.map { prefs ->
            val raw = prefs[stringPreferencesKey(key.name)]
            raw?.let { key.serializer.deserialize(it) }
        }.first()
    }

    override suspend fun <T> write(key: PreferenceKey<T>, value: T) {
        val serialized = key.serializer.serialize(value)
        dataStore.edit { prefs ->
            prefs[stringPreferencesKey(key.name)] = serialized
        }
    }

    override suspend fun <T> contains(key: PreferenceKey<T>): Boolean {
        return dataStore.data.map { prefs ->
            prefs.contains(stringPreferencesKey(key.name))
        }.first()
    }

    override suspend fun <T> remove(key: PreferenceKey<T>) {
        dataStore.edit { prefs ->
            prefs.remove(stringPreferencesKey(key.name))
        }
    }
}
