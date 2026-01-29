package ir.sysfail.chatguard.core.messanger.implementation

import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import ir.sysfail.chatguard.core.messanger.abstraction.MessengerNodeReader
import ir.sysfail.chatguard.core.messanger.models.ChatMessage
import ir.sysfail.chatguard.core.messanger.models.MessageSender
import ir.sysfail.chatguard.core.messanger.models.MessengerPlatform
import ir.sysfail.chatguard.core.messanger.utils.hasNodeWithId

class BaleMessengerNodeReader : MessengerNodeReader {
    override val platform: MessengerPlatform = MessengerPlatform.BALE

    override fun getChatTitle(rootNode: AccessibilityNodeInfo?): String? {
        return rootNode
            ?.findAccessibilityNodeInfosByViewId(VIEW_ID_TITLE)
            ?.firstOrNull()
            ?.text
            ?.toString()
    }

    override fun getMessages(rootNode: AccessibilityNodeInfo?): List<ChatMessage> {
        if (rootNode == null) return emptyList()

        return rootNode
            .findAccessibilityNodeInfosByViewId(VIEW_ID_BUBBLE)
            ?.mapNotNull(::extractChatMessage)
            ?.reversed()
            ?: emptyList()
    }

    override fun isChatScreen(rootNode: AccessibilityNodeInfo?): Boolean {
        if (rootNode == null) return false

        return rootNode.hasNodeWithId(VIEW_ID_COMPOSE_MENU)
                && rootNode.hasNodeWithId(VIEW_ID_MESSAGE_INPUT)
                && rootNode.hasNodeWithId(VIEW_ID_VIDEO_CALL)
    }

    override fun sendMessage(rootNode: AccessibilityNodeInfo?, message: String): Boolean {
        if (rootNode == null) return false

        val inputNode =
            rootNode.findAccessibilityNodeInfosByViewId(VIEW_ID_MESSAGE_INPUT)
                ?.firstOrNull()
                ?: return false

        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                message
            )
        }

        inputNode.performAction(
            AccessibilityNodeInfo.ACTION_SET_TEXT,
            args
        )

        val sendButton =
            rootNode.findAccessibilityNodeInfosByViewId(VIEW_ID_SEND_BUTTON)
                ?.firstOrNull()
                ?: return false

        sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        return true
    }


    private fun extractChatMessage(node: AccessibilityNodeInfo): ChatMessage? {
        val raw = node.contentDescription?.toString() ?: return null
        val content = raw.trim()

        val time = extractTime(content).takeIf { it.isNotEmpty() } ?: return null
        val text = extractMessageText(content)

        return ChatMessage(
            text = text,
            date = time,
            sender = determineSender(content)
        )
    }

    private fun determineSender(content: String): MessageSender {
        return if (SENT_MARKERS.any { content.contains(it) }) {
            MessageSender.ME
        } else {
            MessageSender.OTHER
        }
    }

    private fun extractTime(content: String): String {
        val patterns = listOf(" در ", "در")

        for (pattern in patterns) {
            val index = content.lastIndexOf(pattern)
            if (index != -1) {
                val afterMarker = content.substring(index + pattern.length).trim()

                val timePattern = "[۰-۹\\d]{1,2}:[۰-۹\\d]{2}".toRegex()
                val match = timePattern.find(afterMarker)
                if (match != null) {
                    return match.value
                }
            }
        }

        return ""
    }

    private fun extractMessageText(content: String): String {
        val textAfterPrefix = removeMessagePrefix(content)

        val patterns = listOf("at", " در ", "در")
        for (pattern in patterns) {
            val index = textAfterPrefix.lastIndexOf(pattern)
            if (index != -1) {
                return textAfterPrefix.take(index).trim()
            }
        }

        return textAfterPrefix.trim()
    }

    private fun removeMessagePrefix(content: String): String {
        val withTextIndex = content.indexOf(WITH_TEXT_MARKER)

        return if (withTextIndex == -1) {
            content
        } else {
            content.substring(withTextIndex + WITH_TEXT_MARKER.length)
        }
    }

    companion object {
        private const val VIEW_ID_TITLE = "ir.nasim:id/title"
        private const val VIEW_ID_BUBBLE = "ir.nasim:id/bubbleStub"
        private const val VIEW_ID_COMPOSE_MENU = "ir.nasim:id/chat_toolbar_compose_menu"
        private const val VIEW_ID_MESSAGE_INPUT = "ir.nasim:id/et_message"
        private const val VIEW_ID_VIDEO_CALL = "ir.nasim:id/video_call_item"
        private const val VIEW_ID_SEND_BUTTON = "ir.nasim:id/ib_send"

        private const val WITH_TEXT_MARKER = "بامتن"

        private val SENT_MARKERS = setOf("seen", "ارسال شده", "دیده شده")
        private val RECEIVED_MARKERS = setOf("received", "دریافت شده")
    }
}