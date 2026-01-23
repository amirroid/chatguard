package ir.sysfail.chatguard.core.web_content_extractor.models

/**
 * Post-processing configuration for extracted text
 */
data class ProcessingConfig(
    val trimWhitespace: Boolean = true,
    val removeEmptyLines: Boolean = true,
    val normalizeSpaces: Boolean = false,
    val removeUrls: Boolean = false,
    val customProcessor: ((String) -> String)? = null
)
