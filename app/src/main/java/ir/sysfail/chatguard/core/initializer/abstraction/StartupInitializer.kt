package ir.sysfail.chatguard.core.initializer.abstraction

fun interface StartupInitializer {
    suspend fun initialize()
}