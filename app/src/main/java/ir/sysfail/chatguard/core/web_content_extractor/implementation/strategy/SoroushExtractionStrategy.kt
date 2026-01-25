package ir.sysfail.chatguard.core.web_content_extractor.implementation.strategy

import ir.sysfail.chatguard.core.messanger.models.MessengerPlatform
import ir.sysfail.chatguard.core.web_content_extractor.abstraction.PlatformExtractionStrategy
import ir.sysfail.chatguard.core.web_content_extractor.models.ElementData
import ir.sysfail.chatguard.core.web_content_extractor.models.ProcessingConfig
import ir.sysfail.chatguard.core.web_content_extractor.models.SelectorConfig

class SoroushExtractionStrategy : PlatformExtractionStrategy {
    override val platform = MessengerPlatform.SOROUSH

    override fun getSelectorConfig() = SelectorConfig(
        containerSelector = ".MessageList",
        messageParentSelector = ".Message.message-list-item",
        messageSelector = ".text-content.clearfix.with-meta",
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
        messageMetaSelector = ".MessageMeta"
    )

    override fun getProcessingConfig() = ProcessingConfig(
        trimWhitespace = true,
        removeEmptyLines = true,
        normalizeSpaces = true,
        customProcessor = { text ->
            text.split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .joinToString("\n")
        },
        findMessageId = { data ->
            data.customAttributes[MESSAGE_ID_DATA]?.toLongOrNull()
        }
    )

    override fun validateElement(text: String, html: String, attributes: Map<String, String>) =
        text.length > 2

    override fun transformData(data: ElementData) =
        data.copy(text = data.text.replace(Regex("\\s+"), " ").trim())

    override fun getPreExtractionScript() = """
        document.querySelectorAll('.loading-indicator, .typing-indicator').forEach(el => el.remove());
    """.trimIndent()

    override fun getIsChatPageScript() = """
        var chatHeader = document.querySelector('.ChatInfo');
        var inputField = document.querySelector('#editable-message-text');
        return chatHeader !== null && inputField !== null;
    """.trimIndent()


    companion object {
        private const val MESSAGE_ID_DATA = "data-message-id"
    }
}