package ir.sysfail.chatguard.core.web_content_extractor.implementation.strategy

import ir.sysfail.chatguard.core.messanger.models.MessengerPlatform
import ir.sysfail.chatguard.core.web_content_extractor.abstraction.PlatformExtractionStrategy
import ir.sysfail.chatguard.core.web_content_extractor.models.ElementData
import ir.sysfail.chatguard.core.web_content_extractor.models.ProcessingConfig
import ir.sysfail.chatguard.core.web_content_extractor.models.SelectorConfig
import org.json.JSONObject

class SoroushStrategy : PlatformExtractionStrategy {
    override val platform = MessengerPlatform.SOROUSH

    override fun getSelectorConfig() = SelectorConfig(
        messageSelector = ".text-content.clearfix.with-meta.with-outgoing-icon",
        ignoreSelectors = listOf(
            "[data-ignore-on-paste='true']",
            ".MessageMeta",
            ".message-time",
            ".MessageOutgoingStatus",
            ".Transition",
            ".icon"
        ),
        attributesToExtract = listOf("dir", "class"),
        sendButtonSelector = ".send-message-button, .btn-send",
        inputFieldSelector = ".input-message-text, #message-input-text",
        chatContainerSelector = ".messages-container, .chat-container"
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
        (function() {
            var chatContainer = document.querySelector('.messages-container, .chat-container');
            var inputField = document.querySelector('.input-message-text, #message-input-text');
            return chatContainer !== null && inputField !== null;
        })();
    """.trimIndent()

    override fun getSendMessageScript(message: String) = """
        (function() {
            var input = document.findElementById('editable-message-text');
            var sendBtn = document.querySelector('.send-message-button, .btn-send');
            
            if (input && sendBtn) {
                input.value = ${JSONObject.quote(message)};
                input.dispatchEvent(new Event('input', { bubbles: true }));
                sendBtn.click();
                return true;
            }
            return false;
        })();
    """.trimIndent()
}