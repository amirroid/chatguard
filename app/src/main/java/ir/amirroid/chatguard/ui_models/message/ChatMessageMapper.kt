package ir.amirroid.chatguard.ui_models.message

import ir.amirroid.chatguard.core.messanger.models.ChatMessage
import ir.amirroid.chatguard.core.messanger.models.MessageSender

fun ChatMessage.toUiModel(index: Int) = ChatMessageUiModel(
    message = text,
    isMessageFromMe = sender == MessageSender.ME,
    date = date,
    index = index
)