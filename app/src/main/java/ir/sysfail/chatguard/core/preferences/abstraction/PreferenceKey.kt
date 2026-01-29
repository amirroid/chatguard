package ir.sysfail.chatguard.core.preferences.abstraction

data class PreferenceKey<T>(
    val name: String,
    val serializer: PreferenceSerializer<T>
)