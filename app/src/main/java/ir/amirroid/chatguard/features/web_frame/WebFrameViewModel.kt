package ir.amirroid.chatguard.features.web_frame

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.amirroid.chatguard.R
import ir.amirroid.chatguard.core.messanger.models.MessengerPlatform
import ir.amirroid.chatguard.core.web_content_extractor.models.ButtonClickData
import ir.amirroid.chatguard.core.web_content_extractor.models.ButtonType
import ir.amirroid.chatguard.core.web_content_extractor.models.ExtractedElementMessage
import ir.amirroid.chatguard.core.web_content_extractor.models.ExtractedUserInfo
import ir.amirroid.chatguard.core.web_content_extractor.models.InfoMessageType
import ir.amirroid.chatguard.domain.models.crypto.CryptoKey
import ir.amirroid.chatguard.domain.usecase.key.AddUserPublicKeyUseCase
import ir.amirroid.chatguard.domain.usecase.key.GetPublicKeyUseCase
import ir.amirroid.chatguard.domain.usecase.steganography_crypto.ExtractPoeticPublicKeyUseCase
import ir.amirroid.chatguard.domain.usecase.steganography_crypto.GetPoeticSignedPublicKeyUseCase
import ir.amirroid.chatguard.domain.usecase.steganography_crypto.PoeticMessageDecryptionUseCase
import ir.amirroid.chatguard.domain.usecase.steganography_crypto.PoeticMessageEncryptionUseCase
import ir.amirroid.chatguard.domain.usecase.steganography_crypto.VerifyPoeticPublicKeyUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class WebFrameViewModel(
    private val getPublicKeyUseCase: GetPublicKeyUseCase,
    private val verifyPoeticPublicKeyUseCase: VerifyPoeticPublicKeyUseCase,
    private val getPoeticSignedPublicKeyUseCase: GetPoeticSignedPublicKeyUseCase,
    private val encryptionUseCase: PoeticMessageEncryptionUseCase,
    private val decryptionUseCase: PoeticMessageDecryptionUseCase,
    private val addUserPublicKeyUseCase: AddUserPublicKeyUseCase,
    private val getPoeticPublicKeyUseCase: ExtractPoeticPublicKeyUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _state = MutableStateFlow(WebFrameScreenState())
    val state = _state.asStateFlow()

    private val taskQueue = Channel<MessageTask>(Channel.UNLIMITED)
    private val processedMessageIds = ConcurrentHashMap.newKeySet<String>()
    private val taskResults = ConcurrentHashMap<String, TaskResult>()
    private val messageContentToId = ConcurrentHashMap<String, String>()
    private val messageToUsername = ConcurrentHashMap<String, String>()

    private val _events = Channel<WebFrameEvent>(capacity = Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var currentUsername: String? = null

    var lastLoadedUrl: String? = null

    private var loadsCount = 0

    private val platform = savedStateHandle.get<MessengerPlatform>("platform")!!
    private val _userPublicKey = MutableStateFlow<Result<CryptoKey>?>(null)

    init {
        startTaskProcessor()
    }

    fun updateBackgroundColor(color: String) {
        runCatching { color.toColorInt() }.getOrNull()?.let { color ->
            _state.update {
                it.copy(backgroundColor = Color(color))
            }
        }
    }

    fun handleUserInfoKey(userInfo: ExtractedUserInfo) = viewModelScope.launch(Dispatchers.IO) {
        currentUsername = userInfo.username

        getPublicKeyUseCase(userInfo.username, platform.packageName)
            .onSuccess {
                _events.send(WebFrameEvent.ClearInfoMessage)
            }
            .onFailure {
                _events.send(
                    WebFrameEvent.ShowInfoMessageResource(
                        R.string.encryption_key_was_not_found,
                        InfoMessageType.ERROR
                    )
                )
            }.also { result ->
                _userPublicKey.value = result
            }
    }

    fun onPageLoaded() {
        loadsCount++
        reapplyResults()
    }

    fun onNewMessagesDetected(messages: List<ExtractedElementMessage>) {
        viewModelScope.launch {
            messages.forEach { message ->
                handleMessageDetection(message)
            }
        }
    }

    private suspend fun handleMessageDetection(message: ExtractedElementMessage) {
        val currentId = message.id
        val messageContent = message.message

        currentUsername?.let { username ->
            messageToUsername[currentId] = username
        }

        val oldId = messageContentToId[messageContent]
        val oldResult = oldId?.let { taskResults[it] }

        if (oldResult != null && oldId != currentId) {
            processedMessageIds.remove(oldId)
            taskResults.remove(oldId)
            messageContentToId.remove(messageContent)

            currentUsername?.let { username ->
                messageToUsername.remove(oldId)
                messageToUsername[currentId] = username
            }

            processedMessageIds.add(currentId)
            messageContentToId[messageContent] = currentId
            taskResults[currentId] = oldResult.copy(loadIndex = loadsCount)

            emitResultEvents(currentId, oldResult)
            return
        }

        val existingResult = taskResults[currentId]

        if (existingResult != null) {
            val contentChanged = messageContent != existingResult.realMessage

            if (contentChanged) {
                processedMessageIds.remove(currentId)
                messageContentToId.remove(existingResult.realMessage)
                messageContentToId[messageContent] = currentId
                taskQueue.send(MessageTask(message))
            } else if (existingResult.loadIndex != loadsCount) {
                taskResults[currentId] = existingResult.copy(loadIndex = loadsCount)
                emitResultEvents(currentId, existingResult)
            }
        } else if (processedMessageIds.add(currentId)) {
            messageContentToId[messageContent] = currentId
            taskQueue.send(MessageTask(message))
        }
    }

    private fun reapplyResults() {
        viewModelScope.launch {
            taskResults.forEach { (messageId, result) ->
                taskResults[messageId] = result.copy(loadIndex = loadsCount)
                emitResultEvents(messageId, result)
            }
        }
    }

    private suspend fun emitResultEvents(messageId: String, result: TaskResult) {
        if (result.isPublicKey) {
            _events.send(
                WebFrameEvent.InjectButton(
                    text = R.string.use_key,
                    buttonId = "message_$messageId",
                    messageId = messageId,
                    buttonType = ButtonType.CHOOSE_KEY
                )
            )
        }

        result.updatedMessage?.let { newText ->
            _events.send(
                WebFrameEvent.UpdateMessageText(
                    messageId = messageId,
                    newText = newText
                )
            )
        }
    }

    private fun startTaskProcessor() {
        viewModelScope.launch(Dispatchers.IO) {
            taskQueue.consumeAsFlow()
                .collect { task ->
                    launch(Dispatchers.IO) {
                        processMessageTask(task)
                    }
                }
        }
    }

    fun sendPoeticPublicKey() = viewModelScope.launch(Dispatchers.IO) {
        getPoeticSignedPublicKeyUseCase.invoke().onSuccess {
            _events.send(WebFrameEvent.SendMessage(it))
        }
    }

    private suspend fun savePublicKey(publicKey: CryptoKey) {
        val username = currentUsername ?: return

        addUserPublicKeyUseCase(
            packageName = platform.packageName,
            username = username,
            key = publicKey
        ).onSuccess {
            clearResultsForUser(username)
            _events.send(WebFrameEvent.RefreshWebView)
        }
    }

    private fun clearResultsForUser(username: String) {
        val messageIdsToRemove = messageToUsername
            .filter { (_, user) -> user == username }
            .map { (messageId, _) -> messageId }
            .toSet()

        messageIdsToRemove.forEach { messageId ->
            val result = taskResults[messageId]

            processedMessageIds.remove(messageId)
            taskResults.remove(messageId)

            result?.realMessage?.let { content ->
                if (messageContentToId[content] == messageId) {
                    messageContentToId.remove(content)
                }
            }
            messageToUsername.remove(messageId)
        }
    }

    fun handleSendMessage(message: String) = viewModelScope.launch(Dispatchers.IO) {
        _userPublicKey.filterNotNull().firstOrNull()?.onSuccess { key ->
            encryptionUseCase(message, key)
                .onSuccess { encryptedMessage ->
                    _events.send(WebFrameEvent.SendMessage(encryptedMessage))
                }
        }?.onFailure {
            _events.send(WebFrameEvent.SendMessage(message))
        }
    }

    fun handleButtonClick(data: ButtonClickData) {
        val messageResult = taskResults[data.messageId] ?: return

        viewModelScope.launch(Dispatchers.IO) {
            when (data.buttonType) {
                ButtonType.CHOOSE_KEY -> {
                    getPoeticPublicKeyUseCase(messageResult.realMessage)
                        .onSuccess { publicKey ->
                            savePublicKey(publicKey)
                        }
                }
            }
        }
    }

    private suspend fun processMessageTask(task: MessageTask) {
        val isPublicKey = if (!task.data.isMyMessage) {
            verifyPoeticPublicKeyUseCase(task.data.message)
        } else false

        var updatedMessage: String? = null

        _userPublicKey.filterNotNull().firstOrNull()?.onSuccess { key ->
            decryptionUseCase(
                task.data.message,
                key,
                isAmSender = task.data.isMyMessage
            ).getOrNull()?.also {
                updatedMessage = "🔒 $it"
            }
        }

        val result = TaskResult(
            loadIndex = loadsCount,
            isPublicKey = isPublicKey,
            realMessage = task.data.message,
            updatedMessage = updatedMessage
        )

        taskResults[task.data.id] = result
        emitResultEvents(task.data.id, result)
    }

    override fun onCleared() {
        taskQueue.close()
        _events.close()
        processedMessageIds.clear()
        taskResults.clear()
        messageContentToId.clear()
        messageToUsername.clear()
        super.onCleared()
    }
}

private data class MessageTask(
    val data: ExtractedElementMessage
)

private data class TaskResult(
    val loadIndex: Int,
    val isPublicKey: Boolean,
    val realMessage: String,
    val updatedMessage: String?
)