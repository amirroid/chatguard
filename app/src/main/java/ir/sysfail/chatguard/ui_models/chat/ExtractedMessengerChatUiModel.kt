package ir.sysfail.chatguard.ui_models.chat

import ir.sysfail.chatguard.core.floating_button.FloatingButtonController

enum class WindowType(val color: Int) {
    NON_MESSENGER(FloatingButtonController.IDLE_COLOR),
    NON_CHAT(FloatingButtonController.IDLE_COLOR),
    DECRYPTABLE_CHAT(FloatingButtonController.GREEN_SUCCESS_COLOR),
    UNDECRYPTABLE_CHAT(FloatingButtonController.RED_FAIL_COLOR)
}

data class ExtractedMessengerChatUiModel(
    val windowType: WindowType = WindowType.NON_MESSENGER,
)