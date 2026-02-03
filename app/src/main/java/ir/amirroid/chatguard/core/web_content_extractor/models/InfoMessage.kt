package ir.amirroid.chatguard.core.web_content_extractor.models

data class InfoMessage(
    val text: String,
    val type: InfoMessageType
)

enum class InfoMessageType {
    INFO,
    ERROR
}