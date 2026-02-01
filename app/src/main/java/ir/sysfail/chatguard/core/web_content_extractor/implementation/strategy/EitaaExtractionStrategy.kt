package ir.sysfail.chatguard.core.web_content_extractor.implementation.strategy

import ir.sysfail.chatguard.core.messanger.models.MessengerPlatform
import ir.sysfail.chatguard.core.web_content_extractor.abstraction.PlatformExtractionStrategy
import ir.sysfail.chatguard.core.web_content_extractor.models.*

class EitaaExtractionStrategy : PlatformExtractionStrategy {
    override val platform = MessengerPlatform.EITAA

    override fun getSelectorConfig() = SelectorConfig(
        containerSelector = ".bubbles",
        messageParentSelector = ".bubble",
        messageSelector = ".bubble-content .message",
        messageIdData = MESSAGE_ID_DATA,
        ignoreSelectors = listOf(
            ".time",
        ),
        attributesToExtract = listOf(MESSAGE_ID_DATA),
        sendButtonSelector = ".btn-send-container .btn-icon.btn-send.send",
        submitSendClick = "mousedown",
        inputFieldSelector = ".input-message-container .input-message-input",
        messageMetaSelector = ".time",
        backgroundColorVariable = "--surface-color",
        buttonInjectionConfig = ButtonInjectionConfig(
            targetSelector = ".bubble-content .message",
            insertPosition = InsertPosition.APPEND,
        ),
        beforeSendPublicKeySelector = ".chat-utils .tgico-more",
        userInfoSelector = UserInfoSelector(
            fullNameSelector = ".chat-info .peer-title"
        ),
        infoMessageConfig = InfoMessageConfig(
            targetSelector = ".sidebar-header.topbar"
        ),
        chatHeader = ".sidebar-header.topbar .chat-info"
    )

    override fun getProcessingConfig() = ProcessingConfig(
        trimWhitespace = true,
        removeEmptyLines = true,
        normalizeSpaces = true,
        findMessageId = { data ->
            data.customAttributes[MESSAGE_ID_DATA].takeIf { !it.isNullOrBlank() }
        },
        checkIsOwnMessage = { data ->
            data.className.split(" ").contains("is-out")
        }
    )

    override fun validateElement(text: String, html: String, attributes: Map<String, String>) =
        text.length > 2

    override fun transformData(data: ElementData) =
        data.copy(text = data.text.replace(Regex("\\s+"), " ").trim())

    override fun getInitialExecutionScript(): String {
        return """
       const header = document.querySelector('.sidebar-header.topbar');
       if (!header) return;
       
       header.addEventListener('mousedown', function(e) {
           const button = e.target.closest('[data-chatguard-public-key-input]');
           if (button) {
               e.preventDefault();
               e.stopPropagation();
               e.stopImmediatePropagation();
               return false;
           }
       }, true);
    """.trimIndent()
    }

    companion object {
        private const val MESSAGE_ID_DATA = "data-mid"
    }
}