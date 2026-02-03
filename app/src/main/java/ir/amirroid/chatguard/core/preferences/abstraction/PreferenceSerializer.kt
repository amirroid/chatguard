package ir.amirroid.chatguard.core.preferences.abstraction

interface PreferenceSerializer<T> {
    fun serialize(value: T): String
    fun deserialize(data: String): T
}