package ir.sysfail.chatguard.ui_models.message

import ir.sysfail.chatguard.core.messanger.models.ChatMessage
import ir.sysfail.chatguard.core.messanger.models.MessageSender

fun ChatMessage.toUiModel(index: Int) = ChatMessageUiModel(
    message = text,
    isMessageFromMe = sender == MessageSender.ME,
    date = date,
    index = index
)