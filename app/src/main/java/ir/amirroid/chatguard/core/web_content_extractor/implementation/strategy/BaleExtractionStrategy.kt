package ir.amirroid.chatguard.core.web_content_extractor.implementation.strategy

import androidx.core.net.toUri
import ir.amirroid.chatguard.core.messanger.models.MessengerPlatform
import ir.amirroid.chatguard.core.web_content_extractor.abstraction.PlatformExtractionStrategy
import ir.amirroid.chatguard.core.web_content_extractor.models.*
import kotlin.text.isNullOrBlank

class BaleExtractionStrategy : PlatformExtractionStrategy {
    override val platform = MessengerPlatform.BALE

    override fun getSelectorConfig() = SelectorConfig(
        containerSelector = "#message_list_scroller_id",
        messageParentSelector = ".message-item",
        messageSelector = "[data-sid] > div:nth-child(1) > div:nth-child(1) > div:nth-child(1) > div:nth-child(1) > div:nth-child(1) span.p",
        messageIdData = MESSAGE_ID_DATA,
        ignoreSelectors = listOf(".time"),
        attributesToExtract = listOf(MESSAGE_ID_DATA),
        sendButtonSelector = "#chat_footer > :has(#main-message-input) div[aria-label=\"send-button\"]:nth-child(5)",
        submitSendClick = "click",
        inputFieldSelector = "#main-message-input",
        messageMetaSelector = ".time",
        backgroundColorVariable = "--color-neutrals-n-00",
        buttonInjectionConfig = ButtonInjectionConfig(
            targetSelector = "[data-sid] > div:nth-child(1) > div:nth-child(1) > div:nth-child(1) > div:nth-child(1) > div:nth-child(1)",
            insertPosition = InsertPosition.APPEND,
        ),
        beforeSendPublicKeySelector = ".main-section-container > div[aria-label=\"ChatAppBar\"]:nth-child(1) > div:nth-child(1) > div:nth-child(4)",
        userInfoSelector = UserInfoSelector(
            fullNameSelector = ".main-section-container > div[aria-label=\"ChatAppBar\"]:nth-child(1) > div:nth-child(1) > div:nth-child(3) > div:nth-child(1) > p:nth-child(1)"
        ),
        infoMessageConfig = InfoMessageConfig(
            targetSelector = ".main-section-container > div[aria-label=\"ChatAppBar\"]:nth-child(1)"
        ),
        chatHeader = ".main-section-container > div[aria-label=\"ChatAppBar\"]:nth-child(1) > div:nth-child(1) > div:nth-child(3) > div:nth-child(1) > p:nth-child(1)"
    )

    override fun getProcessingConfig() = ProcessingConfig(
        trimWhitespace = true,
        removeEmptyLines = true,
        normalizeSpaces = true,
        findMessageId = ::findElementId,
        checkIsOwnMessage = { url, data ->
            val uri = url.toUri()
            val contactUid = uri.getQueryParameter("uid") ?: return@ProcessingConfig false
            val messageId = findElementId(data) ?: return@ProcessingConfig false

            messageId.split("-").lastOrNull() != contactUid
        }
    )

    private fun findElementId(data: ElementData): String? {
        return data.customAttributes[MESSAGE_ID_DATA].takeIf { !it.isNullOrBlank() }
    }

    override fun validateElement(text: String, html: String, attributes: Map<String, String>) =
        text.length > 2

    override fun transformData(data: ElementData) =
        data.copy(text = data.text.replace(Regex("\\s+"), " ").trim())

    companion object {
        private const val MESSAGE_ID_DATA = "data-sid"
    }
}