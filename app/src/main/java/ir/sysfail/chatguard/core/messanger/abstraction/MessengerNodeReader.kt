package ir.sysfail.chatguard.core.messanger.abstraction

import android.view.accessibility.AccessibilityNodeInfo
import ir.sysfail.chatguard.core.messanger.models.ChatMessage
import ir.sysfail.chatguard.core.messanger.models.MessengerPlatform

/**
 * Interface for reading chat information from messaging app accessibility nodes
 */
interface MessengerNodeReader {

    /**
     * Platform of the messaging app
     */
    val platform: MessengerPlatform

    /**
     * Get the current chat title
     */
    fun getChatTitle(rootNode: AccessibilityNodeInfo?): String?

    /**
     * Get all messages from the current chat screen
     */
    fun getMessages(rootNode: AccessibilityNodeInfo?): List<ChatMessage>

    /**
     * Check if the current screen is a chat conversation
     */
    fun isChatScreen(rootNode: AccessibilityNodeInfo?): Boolean

    /**
     * Send a message to the current chat
     */
    fun sendMessage(rootNode: AccessibilityNodeInfo?, message: String) : Boolean
}