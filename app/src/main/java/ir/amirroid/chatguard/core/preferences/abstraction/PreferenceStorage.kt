package ir.amirroid.chatguard.core.preferences.abstraction

interface PreferenceStorage {
    suspend fun <T> read(key: PreferenceKey<T>): T?
    suspend fun <T> write(key: PreferenceKey<T>, value: T)
    suspend fun <T> contains(key: PreferenceKey<T>): Boolean
    suspend fun <T> remove(key: PreferenceKey<T>)
}