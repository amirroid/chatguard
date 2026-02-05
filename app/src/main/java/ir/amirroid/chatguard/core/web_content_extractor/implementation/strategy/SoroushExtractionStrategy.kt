package ir.amirroid.chatguard.core.web_content_extractor.implementation.strategy

import ir.amirroid.chatguard.core.messanger.models.MessengerPlatform
import ir.amirroid.chatguard.core.web_content_extractor.abstraction.PlatformExtractionStrategy
import ir.amirroid.chatguard.core.web_content_extractor.models.*
import androidx.core.net.toUri

class SoroushExtractionStrategy : PlatformExtractionStrategy {
    override val platform = MessengerPlatform.SOROUSH

    override fun getSelectorConfig() = SelectorConfig(
        containerSelector = ".MessageList",
        messageParentSelector = ".Message.message-list-item",
        messageSelector = ".text-content.clearfix.with-meta",
        messageIdData = MESSAGE_ID_DATA,
        ignoreSelectors = listOf(
            "[data-ignore-on-paste=\"true\"]",
            ".message-time",
            ".MessageMeta",
            ".MessageOutgoingStatus",
            ".Transition",
            ".icon",
            ".sender-title"
        ),
        attributesToExtract = listOf(MESSAGE_ID_DATA),
        sendButtonSelector = ".send.main-button.click-allowed",
        submitSendClick = "click",
        inputFieldSelector = "#editable-message-text",
        messageMetaSelector = ".MessageMeta",
        backgroundColorVariable = "--color-background",
        buttonInjectionConfig = ButtonInjectionConfig(
            targetSelector = ".text-content.clearfix.with-meta",
            insertPosition = InsertPosition.AFTER,
        ),
        beforeSendPublicKeySelector = ".messages-layout .MiddleHeader .HeaderActions",
        userInfoSelector = UserInfoSelector(
            fullNameSelector = ".chat-info-wrapper .info .title .fullName"
        ),
        infoMessageConfig = InfoMessageConfig(
            targetSelector = ".messages-layout .MiddleHeader"
        ),
        chatHeader = ".chat-info-wrapper .info .title .fullName"
    )

    override fun getProcessingConfig() = ProcessingConfig(
        trimWhitespace = true,
        removeEmptyLines = true,
        normalizeSpaces = true,
        findMessageId = { data ->
            data.customAttributes[MESSAGE_ID_DATA].takeIf { !it.isNullOrBlank() }
        },
        checkIsOwnMessage = { _, data ->
            data.className.split(" ").contains("own")
        }
    )

    override fun validateElement(text: String, html: String, attributes: Map<String, String>) =
        text.length > 2

    override fun transformData(data: ElementData) =
        data.copy(text = data.text.replace(Regex("\\s+"), " ").trim())

    override fun isChatUrl(url: String): Boolean {
        return try {
            val uri = url.toUri()
            val fragment = uri.fragment ?: return false
            val chatId = fragment.toLongOrNull() ?: return false
            chatId > 0
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        private const val MESSAGE_ID_DATA = "data-message-id"
    }
}