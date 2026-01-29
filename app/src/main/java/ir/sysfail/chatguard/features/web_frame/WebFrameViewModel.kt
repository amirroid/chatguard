package ir.sysfail.chatguard.features.web_frame

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.sysfail.chatguard.R
import ir.sysfail.chatguard.core.messanger.models.MessengerPlatform
import ir.sysfail.chatguard.core.web_content_extractor.models.ButtonClickData
import ir.sysfail.chatguard.core.web_content_extractor.models.ButtonType
import ir.sysfail.chatguard.core.web_content_extractor.models.ExtractedElementMessage
import ir.sysfail.chatguard.core.web_content_extractor.models.ExtractedUserInfo
import ir.sysfail.chatguard.core.web_content_extractor.models.InfoMessageType
import ir.sysfail.chatguard.domain.models.crypto.CryptoKey
import ir.sysfail.chatguard.domain.usecase.crypto.GetPublicKeyUseCase
import ir.sysfail.chatguard.domain.usecase.key.AddUserPublicKeyUseCase
import ir.sysfail.chatguard.domain.usecase.steganography_crypto.ExtractPoeticPublicKeyUseCase
import ir.sysfail.chatguard.domain.usecase.steganography_crypto.GetPoeticSignedPublicKeyUseCase
import ir.sysfail.chatguard.domain.usecase.steganography_crypto.PoeticMessageDecryptionUseCase
import ir.sysfail.chatguard.domain.usecase.steganography_crypto.PoeticMessageEncryptionUseCase
import ir.sysfail.chatguard.domain.usecase.steganography_crypto.VerifyPoeticPublicKeyUseCase
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

    private val _events = Channel<WebFrameEvent>()
    val events = _events.receiveAsFlow()

    private var currentUsername: String? = null

    private var loadsCount = 0

    private val platform = savedStateHandle.get<MessengerPlatform>("platform")!!
    private val _userPublicKey = MutableStateFlow<Result<CryptoKey>?>(null)

    init {
        startTaskProcessor()
    }

    fun updateBackgroundColor(color: String) {
        _state.update {
            it.copy(backgroundColor = Color(android.graphics.Color.parseColor(color)))
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
                message.id.let { id ->
                    val existingResult = taskResults[id]

                    if (existingResult != null) {
                        if (existingResult.loadIndex != loadsCount) {
                            taskResults[id] = existingResult.copy(loadIndex = loadsCount)
                            emitResultEvents(id, existingResult)
                        }
                    } else if (processedMessageIds.add(id)) {
                        taskQueue.send(MessageTask(message))
                    }
                }
            }
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
        viewModelScope.launch {
            taskQueue.consumeAsFlow()
                .collect { task ->
                    launch {
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
        addUserPublicKeyUseCase(
            packageName = platform.packageName,
            username = currentUsername ?: return,
            key = publicKey
        ).onSuccess {
            _events.send(WebFrameEvent.RefreshWebView)
        }
    }

    fun handleSendMessage(message: String) = viewModelScope.launch {
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
            updatedMessage = decryptionUseCase(
                task.data.message,
                key,
                isAmSender = task.data.isMyMessage
            ).getOrNull()
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
        processedMessageIds.clear()
        taskResults.clear()
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