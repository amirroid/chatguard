package ir.sysfail.chatguard.core.web_content_extractor.models

/**
 * Selector configuration for extracting content from DOM
 */
data class SelectorConfig(
    val containerSelector: String,
    val messageParentSelector: String,
    val messageSelector: String,
    val messageMetaSelector: String,
    val ignoreSelectors: List<String> = emptyList(),
    val attributesToExtract: List<String> = emptyList(),
    val sendButtonSelector: String = "",
    val inputFieldSelector: String = "",
)