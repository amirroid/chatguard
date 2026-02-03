package ir.amirroid.chatguard.core.initializer.abstraction

fun interface StartupInitializer {
    suspend fun initialize()
}