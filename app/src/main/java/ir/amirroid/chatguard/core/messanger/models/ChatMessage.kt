package ir.amirroid.chatguard.core.messanger.models

/**
 * Domain model representing a chat message
 */
data class ChatMessage(
    val text: String,
    val date: String,
    val sender: MessageSender
)

/**
 * Message sender type
 */
enum class MessageSender {
    ME,
    OTHER
}