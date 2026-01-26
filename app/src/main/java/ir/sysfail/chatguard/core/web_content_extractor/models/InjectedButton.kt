package ir.sysfail.chatguard.core.web_content_extractor.models

/**
 * Represents a button to be injected into messages
 */
data class InjectedButton(
    val id: String,
    val text: String,
    val buttonType: ButtonType,
    val enabled: Boolean = true,
)

/**
 * Types of buttons that can be injected
 */
enum class ButtonType {
    CHOOSE_KEY,
}

/**
 * Callback data when button is clicked
 */
data class ButtonClickData(
    val buttonType: ButtonType,
    val messageId: Long?,
    val messageText: String
)