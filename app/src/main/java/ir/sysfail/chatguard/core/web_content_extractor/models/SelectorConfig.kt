package ir.sysfail.chatguard.core.web_content_extractor.models

/**
 * Selector configuration for extracting content from DOM
 */
data class SelectorConfig(
    val containerSelector: String,
    val messageParentSelector: String,
    val messageSelector: String,
    val messageMetaSelector: String,
    val userInfoSelector: UserInfoSelector = UserInfoSelector(),
    val ignoreSelectors: List<String> = emptyList(),
    val attributesToExtract: List<String> = emptyList(),
    val messageIdData: String = "",
    val sendButtonSelector: String = "",
    val submitSendClick: String = "",
    val inputFieldSelector: String = "",
    val beforeSendPublicKeySelector: String = "",
    val backgroundColorVariable: String,
    val buttonInjectionConfig: ButtonInjectionConfig? = null,
    val infoMessageConfig: InfoMessageConfig? = null,
    val chatHeader: String = ""
)

data class ButtonInjectionConfig(
    val targetSelector: String,
    val insertPosition: InsertPosition = InsertPosition.AFTER,
    val shouldInject: (ElementData) -> Boolean = { true }
)

enum class InsertPosition {
    BEFORE,      // Before the target element
    AFTER,       // After the target element
    PREPEND,     // As first child of target
    APPEND       // As last child of target
}


data class UserInfoSelector(
    val fullNameSelector: String = ""
)

data class InfoMessageConfig(
    val targetSelector: String,
    val insertPosition: InsertPosition = InsertPosition.AFTER
)