package ir.sysfail.chatguard.features.messages

import androidx.compose.runtime.Immutable
import ir.sysfail.chatguard.ui_models.message.ChatMessageUiModel

@Immutable
data class MessagesScreenState(
    val messages: List<ChatMessageUiModel> = emptyList(),
    val currentNewMessageText: String = "",
    val hasPublicKey: Boolean = true
)