package ir.sysfail.chatguard.core.web_content_extractor.implementation.strategy

import ir.sysfail.chatguard.core.messanger.models.MessengerPlatform
import ir.sysfail.chatguard.core.web_content_extractor.abstraction.PlatformExtractionStrategy
import ir.sysfail.chatguard.core.web_content_extractor.models.*

class SoroushExtractionStrategy : PlatformExtractionStrategy {
    override val platform = MessengerPlatform.SOROUSH

    override fun getSelectorConfig() = SelectorConfig(
        containerSelector = ".MessageList",
        messageParentSelector = ".Message.message-list-item",
        messageSelector = ".text-content.clearfix.with-meta",
        messageIdData = MESSAGE_ID_DATA,
        ignoreSelectors = listOf(
            "[data-ignore-on-paste=\"true\"]",
            ".MessageMeta",
            ".message-time",
            ".MessageOutgoingStatus",
            ".Transition",
            ".icon",
            ".sender-title"
        ),
        attributesToExtract = listOf(MESSAGE_ID_DATA),
        sendButtonSelector = ".send.main-button",
        inputFieldSelector = "#editable-message-text",
        messageMetaSelector = ".MessageMeta",
        backgroundColorVariable = "--color-background",
        buttonInjectionConfig = ButtonInjectionConfig(
            targetSelector = ".text-content.clearfix.with-meta",
            insertPosition = InsertPosition.AFTER,
        ),
        userInfoSelector = UserInfoSelector(
            fullNameSelector = ".chat-info-wrapper .info .title .fullName"
        ),
        infoMessageConfig = InfoMessageConfig(
            targetSelector = ".messages-layout .MiddleHeader"
        )
    )

    override fun getProcessingConfig() = ProcessingConfig(
        trimWhitespace = true,
        removeEmptyLines = true,
        normalizeSpaces = true,
        findMessageId = { data ->
            data.customAttributes[MESSAGE_ID_DATA]?.toLongOrNull()
        },
        checkIsOwnMessage = { data ->
            data.className.split(" ").contains("own")
        }
    )

    override fun validateElement(text: String, html: String, attributes: Map<String, String>) =
        text.length > 2

    override fun transformData(data: ElementData) =
        data.copy(text = data.text.replace(Regex("\\s+"), " ").trim())

    override fun getPreExtractionScript(): String? = null

    override fun getIsChatPageScript() = """
        var chatHeader = document.querySelector('.ChatInfo');
        var inputField = document.querySelector('#editable-message-text');
        return chatHeader !== null && inputField !== null;
    """.trimIndent()

    companion object {
        private const val MESSAGE_ID_DATA = "data-message-id"
    }
}