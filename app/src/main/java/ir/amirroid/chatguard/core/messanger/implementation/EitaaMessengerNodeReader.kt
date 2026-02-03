package ir.amirroid.chatguard.core.messanger.implementation

import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.EditText
import ir.amirroid.chatguard.core.messanger.abstraction.MessengerNodeReader
import ir.amirroid.chatguard.core.messanger.models.ChatMessage
import ir.amirroid.chatguard.core.messanger.models.MessageSender
import ir.amirroid.chatguard.core.messanger.models.MessengerPlatform

class EitaaMessengerNodeReader : MessengerNodeReader {
    override val platform: MessengerPlatform = MessengerPlatform.EITAA

    override fun getChatTitle(rootNode: AccessibilityNodeInfo?): String? {
        return rootNode?.window?.title?.toString()
    }

    override fun getMessages(rootNode: AccessibilityNodeInfo?): List<ChatMessage> {
        if (rootNode == null) return emptyList()

        return findMessageNodes(rootNode)
            .mapNotNull(::extractChatMessage)
            .reversed()
    }

    override fun isChatScreen(rootNode: AccessibilityNodeInfo?): Boolean {
        if (rootNode == null) return false

        val title = rootNode.window?.title?.toString()
        if (title.isNullOrEmpty() || title == "ایتا") return false

        return findEditText(rootNode) != null
    }

    override fun sendMessage(rootNode: AccessibilityNodeInfo?, message: String): Boolean {
        if (rootNode == null) return false

        val inputField = findEditText(rootNode) ?: return false

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

    private fun findEditText(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        node ?: return null

        if (node.className == EditText::class.java.name) {
            val hintTextValidate =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    node.hintText?.toString() in SEND_MESSAGE_EDIT_TEXT_HINTS
                } else true

            if (hintTextValidate) {
                return node
            }
        }

        for (i in 0 until node.childCount) {
            val result = findEditText(node.getChild(i))
            if (result != null) return result
        }

        return null
    }

    private fun findMessageNodes(rootNode: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val messageNodes = mutableListOf<AccessibilityNodeInfo>()
        traverseForMessages(rootNode, messageNodes)
        return messageNodes
    }

    private fun traverseForMessages(
        node: AccessibilityNodeInfo?,
        result: MutableList<AccessibilityNodeInfo>
    ) {
        if (node == null) return

        if (isMessageNode(node)) {
            result.add(node)
        }

        for (i in 0 until node.childCount) {
            traverseForMessages(node.getChild(i), result)
        }
    }

    private fun isMessageNode(node: AccessibilityNodeInfo): Boolean {
        val content = node.contentDescription?.toString() ?: return false
        val lines = content.lines().map { it.trim() }.filter { it.isNotEmpty() }

        if (lines.size < 2) return false

        val lastLine = lines.last()
        return hasTimePattern(lastLine) && hasStatusPattern(lastLine)
    }

    private fun hasTimePattern(text: String): Boolean {
        return TIME_PATTERNS.any { it.containsMatchIn(text) }
    }

    private fun hasStatusPattern(text: String): Boolean {
        val lowerText = text.lowercase()
        return SENT_MARKERS.any { lowerText.contains(it) } ||
                RECEIVED_MARKERS.any { lowerText.contains(it) } ||
                SENDING_MARKERS.any { lowerText.contains(it) }
    }

    private fun extractChatMessage(node: AccessibilityNodeInfo): ChatMessage? {
        val content = node.contentDescription?.toString() ?: return null
        val lines = content.lines().map { it.trim() }.filter { it.isNotEmpty() }

        if (lines.size < 2) return null

        val lastLine = lines.last()
        val time = extractTime(lastLine) ?: return null
        val sender = determineSender(lastLine)
        val text = extractMessageText(node, lines)

        return ChatMessage(
            text = text,
            date = time,
            sender = sender
        )
    }

    private fun extractMessageText(node: AccessibilityNodeInfo, lines: List<String>): String {
        val firstLine = lines.first()

        if (isFileWithLink(firstLine)) {
            return if (lines.size > 2) {
                lines.drop(1).dropLast(1).joinToString("\n")
            } else {
                firstLine
            }
        }

        if (isFileMessage(firstLine)) {
            val fileName = firstLine.substringBefore(",").trim()
            return if (lines.size > 2) {
                val caption = lines.drop(1).dropLast(1).joinToString("\n")
                "$fileName\n$caption"
            } else {
                fileName
            }
        }

        if (LINK_MARKERS.contains(firstLine)) {
            return extractTextFromChildren(node)
        }

        if (PHOTO_MARKERS.contains(firstLine) || VIDEO_MARKERS.contains(firstLine)) {
            return if (lines.size > 2) {
                lines.drop(1).dropLast(1).joinToString("\n")
            } else {
                firstLine
            }
        }

        return lines.dropLast(1).joinToString("\n")
    }

    private fun isFileWithLink(text: String): Boolean {
        return LINK_MARKERS.any { text.startsWith(it) } && text.contains("MB")
    }

    private fun isFileMessage(text: String): Boolean {
        return text.contains(",") && text.contains("MB")
    }

    private fun extractTextFromChildren(node: AccessibilityNodeInfo): String {
        val texts = mutableListOf<String>()

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val childText = child.text?.toString() ?: child.contentDescription?.toString()

            if (!childText.isNullOrEmpty() && !LINK_MARKERS.contains(childText)) {
                texts.add(childText)
            }
        }

        return texts.joinToString("\n").ifEmpty { LINK_MARKERS.first() }
    }

    private fun extractTime(lastLine: String): String? {
        for (pattern in TIME_PATTERNS) {
            val match = pattern.find(lastLine)
            if (match != null) {
                return match.groupValues[1]
            }
        }

        return null
    }

    private fun determineSender(lastLine: String): MessageSender {
        val lowerLine = lastLine.lowercase()

        return when {
            SENT_MARKERS.any { lowerLine.contains(it) } || SENDING_MARKERS.any {
                lowerLine.contains(it)
            } -> MessageSender.ME

            RECEIVED_MARKERS.any { lowerLine.contains(it) } -> MessageSender.OTHER
            else -> MessageSender.OTHER
        }
    }

    companion object {
        private val TIME_PATTERNS = listOf(
            "at (\\d{2}:\\d{2})".toRegex(),
            "\u202Aدر ([۰-۹٠-٩\\d]{2}:[۰-۹٠-٩\\d]{2})\u202C".toRegex(),
            "در ([۰-۹٠-٩\\d]{2}:[۰-۹٠-٩\\d]{2})".toRegex()
        )

        private val LINK_MARKERS = setOf("Link", "لینک")
        private val PHOTO_MARKERS = setOf("Photo", "عکس")
        private val VIDEO_MARKERS = setOf("Video", "ویدیو")

        private val SENT_MARKERS = setOf("sent", "ارسال شد")
        private val RECEIVED_MARKERS = setOf("received", "دریافت شد")
        private val SENDING_MARKERS = setOf("sending", "در حال ارسال")

        private val SEND_MESSAGE_EDIT_TEXT_HINTS = setOf("پیام", "Message")

        private val SEND_BUTTON_DESCRIPTIONS = setOf(
            "ارسال",
            "Send"
        )
    }
}