package ir.amirroid.chatguard.core.messanger.implementation

import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import ir.amirroid.chatguard.core.messanger.abstraction.MessengerNodeReader
import ir.amirroid.chatguard.core.messanger.models.ChatMessage
import ir.amirroid.chatguard.core.messanger.models.MessageSender
import ir.amirroid.chatguard.core.messanger.models.MessengerPlatform

class SoroushMessengerNodeReader : MessengerNodeReader {
    override val platform: MessengerPlatform = MessengerPlatform.SOROUSH

    override fun getChatTitle(rootNode: AccessibilityNodeInfo?): String? {
        return rootNode?.window?.title?.toString()
    }

    override fun getMessages(rootNode: AccessibilityNodeInfo?): List<ChatMessage> {
        if (rootNode == null) return emptyList()

        val recyclerView = findRecyclerView(rootNode) ?: return emptyList()

        return extractMessagesFromRecyclerView(recyclerView)
    }

    override fun isChatScreen(rootNode: AccessibilityNodeInfo?): Boolean {
        if (rootNode == null) return false

        val title = rootNode.window?.title?.toString()
        if (title.isNullOrEmpty()) return false

        return findMessageInputField(rootNode) != null
    }

    override fun sendMessage(rootNode: AccessibilityNodeInfo?, message: String): Boolean {
        if (rootNode == null) return false

        val inputField = findMessageInputField(rootNode) ?: return false

        val textSet = setTextInField(inputField, message)
        if (!textSet) return false

        val sendButton = findSendButton(rootNode) ?: return false

        return sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun setTextInField(inputField: AccessibilityNodeInfo, message: String): Boolean {
        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                message
            )
        }

        return inputField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    private fun findSendButton(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        node ?: return null

        if (node.className == "android.view.View") {
            val contentDesc = node.contentDescription?.toString()
            if (contentDesc in SEND_BUTTON_DESCRIPTIONS && node.isClickable) {
                return node
            }
        }

        for (i in 0 until node.childCount) {
            val result = findSendButton(node.getChild(i))
            if (result != null) return result
        }

        return null
    }

    private fun findRecyclerView(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        node ?: return null

        if (node.className == RecyclerView::class.java.name) {
            return node
        }

        for (i in 0 until node.childCount) {
            val result = findRecyclerView(node.getChild(i))
            if (result != null) return result
        }

        return null
    }

    private fun findMessageInputField(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        node ?: return null

        val directPath = tryDirectPath(node)
        if (directPath != null) return directPath

        return findMessageInputFieldRecursive(node)
    }

    private fun tryDirectPath(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        try {
            val child0 = rootNode.getChild(0) ?: return null
            val child2 = child0.getChild(2) ?: return null
            val child1 = child2.getChild(1) ?: return null

            if (child1.className == EditText::class.java.name) {
                return child1
            }
        } catch (_: Exception) {
            return null
        }

        return null
    }

    private fun findMessageInputFieldRecursive(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        node ?: return null

        if (node.className == EditText::class.java.name) {
            val text = node.text?.toString()
            val hintText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                node.hintText?.toString()
            } else {
                null
            }

            val isValidField = text in MESSAGE_INPUT_HINTS ||
                    hintText in MESSAGE_INPUT_HINTS

            if (isValidField) {
                return node
            }
        }

        for (i in 0 until node.childCount) {
            val result = findMessageInputFieldRecursive(node.getChild(i))
            if (result != null) return result
        }

        return null
    }

    private fun extractMessagesFromRecyclerView(recyclerView: AccessibilityNodeInfo): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()

        for (i in 0 until recyclerView.childCount) {
            val item = recyclerView.getChild(i) ?: continue
            extractMessageFromItem(item)?.let { messages.add(it) }
        }

        return messages.reversed()
    }

    private fun extractMessageFromItem(item: AccessibilityNodeInfo): ChatMessage? {
        val text = item.text?.toString() ?: return null

        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return null

        val lastLine = lines.last()

        if (!hasStatusMarker(lastLine)) return null

        val time = extractTime(lastLine) ?: return null
        val sender = determineSender(lastLine)
        val messageText = extractMessageText(lines)

        return ChatMessage(
            text = messageText,
            date = time,
            sender = sender
        )
    }

    private fun hasStatusMarker(text: String): Boolean {
        return STATUS_MARKERS.any { marker -> text.contains(marker) }
    }

    private fun extractTime(text: String): String? {
        for (pattern in TIME_PATTERNS) {
            val match = pattern.find(text)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }

    private fun determineSender(lastLine: String): MessageSender {
        return when {
            SENT_MARKERS.any { lastLine.contains(it) } -> MessageSender.ME
            RECEIVED_MARKERS.any { lastLine.contains(it) } -> MessageSender.OTHER
            else -> MessageSender.OTHER
        }
    }

    private fun extractMessageText(lines: List<String>): String {
        return lines.dropLast(1).joinToString("\n")
    }

    companion object {

        private val MESSAGE_INPUT_HINTS = setOf(
            "پیامت را بنویس",
            "Write your message"
        )

        private val SEND_BUTTON_DESCRIPTIONS = setOf(
            "ارسال",
            "Send",
            "ارسال پیام",
            "Send message"
        )

        private val TIME_PATTERNS = listOf(
            "⁨در ([۰-۹٠-٩\\d]{2}:[۰-۹٠-٩\\d]{2})⁩".toRegex(),
            "در ([۰-۹٠-٩\\d]{2}:[۰-۹٠-٩\\d]{2})".toRegex(),
            "at (\\d{2}:\\d{2})".toRegex()
        )

        private val SENT_MARKERS = setOf(
            "Seen",
            "Not seen",
            "ارسال شد",
        )

        private val RECEIVED_MARKERS = setOf(
            "Received",
            "دریافت شد",
        )

        private val STATUS_MARKERS = setOf(
            "Not seen",
            "Received",
            "Seen",
            "ارسال شد",
            "دریافت شد",
            "دیده شد",
            "دیده نشده",
        )
    }
}