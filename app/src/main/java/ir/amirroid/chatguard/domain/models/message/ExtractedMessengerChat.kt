package ir.amirroid.chatguard.domain.models.message

import ir.amirroid.chatguard.core.messanger.models.ChatMessage

data class ExtractedMessengerChat(
    val title: String = "",
    val packageName: String = "",
    val messages: List<ChatMessage> = emptyList()
)