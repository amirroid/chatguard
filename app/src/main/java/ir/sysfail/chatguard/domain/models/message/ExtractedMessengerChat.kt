package ir.sysfail.chatguard.domain.models.message

import ir.sysfail.chatguard.core.messanger.models.ChatMessage

data class ExtractedMessengerChat(
    val title: String = "",
    val packageName: String = "",
    val messages: List<ChatMessage> = emptyList()
)