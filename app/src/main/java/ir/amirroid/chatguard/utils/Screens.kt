package ir.amirroid.chatguard.utils

import ir.amirroid.chatguard.core.messanger.models.MessengerPlatform
import kotlinx.serialization.Serializable

sealed interface Screens {
    @Serializable
    data object Intro : Screens

    @Serializable
    data object Home : Screens

    @Serializable
    data object Guides : Screens

    @Serializable
    data class Web(
        val platform: MessengerPlatform
    ) : Screens
}