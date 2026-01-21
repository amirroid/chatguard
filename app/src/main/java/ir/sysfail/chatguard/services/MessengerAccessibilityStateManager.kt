package ir.sysfail.chatguard.services

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import ir.sysfail.chatguard.BuildConfig
import ir.sysfail.chatguard.core.bridge.implementation.MessengerStateBridge
import ir.sysfail.chatguard.core.event.EventBus
import ir.sysfail.chatguard.core.messanger.abstraction.MessengerNodeReader
import ir.sysfail.chatguard.domain.events.SendMessageEvent
import ir.sysfail.chatguard.domain.models.message.ExtractedMessengerChat
import ir.sysfail.chatguard.domain.usecase.crypto.CheckPublicKeyExistsUseCase
import ir.sysfail.chatguard.ui_models.chat.ExtractedMessengerChatUiModel
import ir.sysfail.chatguard.ui_models.chat.WindowType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MessengerAccessibilityStateManager(
    private val readers: List<MessengerNodeReader>,
    private val checkPublicKeyExistsUseCase: CheckPublicKeyExistsUseCase,
    private val messageStateBridge: MessengerStateBridge,
) {

    private var currentReader: MessengerNodeReader? = null
    private var lastNode: AccessibilityNodeInfo? = null
    private var missingReaderEventCount = 0
    private var missingMessengerEventCount = 0
    private var keyCheckJob: Job? = null

    private var pendingMessage: String? = null
    private var lastChatPackage: String? = null

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private val _state = MutableStateFlow(ExtractedMessengerChatUiModel())
    val state: StateFlow<ExtractedMessengerChatUiModel> = _state

    init {
        collectToSendMessageEvent()
    }

    private fun collectToSendMessageEvent() = scope.launch {
        EventBus.subscribe<SendMessageEvent> { event ->
            handleSendMessage(event.message)
        }
    }

    private fun handleSendMessage(message: String) = scope.launch {
        pendingMessage = message
        lastChatPackage = currentReader?.platform?.packageName
    }


    fun onNodeChanged(
        event: AccessibilityEvent,
        rootNode: AccessibilityNodeInfo
    ) {
        val eventPackageName = event.packageName?.toString()

        if (eventPackageName == SYSTEM_UI_PACKAGE) {
            handleMissingMessenger()
            return
        }

        if (packageFilters.any { it.shouldSkip(eventPackageName.orEmpty()) } || eventPackageName == null) {
            return
        }

        lastNode = rootNode

        val reader = readers.firstOrNull {
            it.platform.packageName == eventPackageName
        } ?: run {
            handleMissingReader()
            handleMissingMessenger()
            return
        }

        missingReaderEventCount = 0
        missingMessengerEventCount = 0
        currentReader = reader

        handlePendingMessage(reader, rootNode)

        if (!reader.isChatScreen(rootNode)) {
            setNonChatState()
            return
        }

        val chatTitle = reader.getChatTitle(rootNode) ?: run {
            setNonChatState()
            return
        }

        keyCheckJob?.cancel()

        keyCheckJob = scope.launch {
            checkPublicKeyExistsUseCase(
                chatTitle,
                eventPackageName
            ).collectLatest { hasKey ->
                val windowType = if (hasKey) {
                    WindowType.DECRYPTABLE_CHAT
                } else {
                    WindowType.UNDECRYPTABLE_CHAT
                }

                _state.value = ExtractedMessengerChatUiModel(
                    windowType = windowType,
                )
            }
        }
    }

    private fun handlePendingMessage(
        reader: MessengerNodeReader,
        rootNode: AccessibilityNodeInfo
    ) {
        val message = pendingMessage ?: return
        val targetPackage = lastChatPackage ?: return

        if (reader.platform.packageName == targetPackage && reader.isChatScreen(rootNode)) {
            val success = reader.sendMessage(rootNode, message)
            if (success) {
                pendingMessage = null
                lastChatPackage = null
            }
        }
    }

    private fun handleMissingMessenger() {
        missingMessengerEventCount++

        if (missingMessengerEventCount >= 5) {
            _state.value = _state.value.copy(windowType = WindowType.NON_MESSENGER)
        }
    }

    private fun handleMissingReader() {
        missingReaderEventCount++
        if (missingReaderEventCount >= 5) {
            resetReader()
        }
    }

    private fun resetReader() {
        missingReaderEventCount = 0
        currentReader = null
        lastNode = null
        keyCheckJob?.cancel()
        _state.value = _state.value.copy(windowType = WindowType.NON_MESSENGER)
        setNonChatState()
    }

    private fun setNonChatState() {
        keyCheckJob?.cancel()
        _state.value = ExtractedMessengerChatUiModel(
            windowType = WindowType.NON_CHAT
        )
    }

    fun updateBridge() {
        val reader = currentReader ?: return
        val node = lastNode ?: return

        val title = reader.getChatTitle(node) ?: return
        val packageName = node.packageName?.toString() ?: return

        val newState = ExtractedMessengerChat(
            title = title,
            packageName = packageName,
            messages = reader.getMessages(node)
        )
        messageStateBridge.updateState(newState)
    }

    fun cleanup() {
        keyCheckJob?.cancel()
        scope.cancel()
        lastNode = null
        currentReader = null
        pendingMessage = null
        lastChatPackage = null
    }

    companion object {
        const val SYSTEM_UI_PACKAGE = "com.android.systemui"
        private val packageFilters = listOf(
            PackageFilter { it == BuildConfig.APPLICATION_ID },
            PackageFilter { it.startsWith("com.google.android.inputmethod") },
            PackageFilter { it == "com.ziipin.softkeyboard.iran" },
        )
    }

    fun interface PackageFilter {
        fun shouldSkip(packageName: String): Boolean
    }
}