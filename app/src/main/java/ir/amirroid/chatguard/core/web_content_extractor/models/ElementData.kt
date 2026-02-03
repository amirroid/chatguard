package ir.amirroid.chatguard.core.web_content_extractor.models

data class ElementData(
    val text: String,
    val html: String,
    val className: String,
    val id: String,
    val dir: String,
    val customAttributes: Map<String, String> = emptyMap()
)