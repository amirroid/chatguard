package ir.amirroid.chatguard.core.web_content_extractor.models

data class ExtractedElementMessage(
    val message: String,
    val id: String,
    val isMyMessage: Boolean
)