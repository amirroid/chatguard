package ir.sysfail.chatguard.features.web_frame

import ir.sysfail.chatguard.core.web_content_extractor.models.ButtonType
import ir.sysfail.chatguard.core.web_content_extractor.models.InfoMessageType

sealed interface WebFrameEvent {
    data class ShowInfoMessage(val message: String, val type: InfoMessageType) : WebFrameEvent
    data class ShowInfoMessageResource(val message: Int, val type: InfoMessageType) : WebFrameEvent
    data object ClearInfoMessage : WebFrameEvent
    data object RefreshWebView : WebFrameEvent

    data class InjectButton(
        val text: Int,
        val buttonId: String,
        val messageId: String,
        val buttonType: ButtonType
    ) : WebFrameEvent

    data class UpdateMessageText(val messageId: String, val newText: String) : WebFrameEvent
    data class SendMessage(val message: String) : WebFrameEvent
}
