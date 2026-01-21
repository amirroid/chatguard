package ir.sysfail.chatguard.ui_models.message

import androidx.compose.runtime.Immutable

@Immutable
data class ChatMessageUiModel(
    val index: Int,
    val message: String,
    val isMessageFromMe: Boolean,
    val date: String,
    val isPublicKey: Boolean = false,
    val isDecryptedMessage: Boolean = false,
)