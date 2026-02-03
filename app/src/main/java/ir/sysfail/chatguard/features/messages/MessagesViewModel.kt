package ir.sysfail.chatguard.features.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.sysfail.chatguard.core.bridge.implementation.MessengerStateBridge
import ir.sysfail.chatguard.core.event.EventBus
import ir.sysfail.chatguard.domain.events.SendMessageEvent
import ir.sysfail.chatguard.domain.models.crypto.CryptoKey
import ir.sysfail.chatguard.domain.models.message.ExtractedMessengerChat
import ir.sysfail.chatguard.domain.usecase.key.GetPublicKeyUseCase
import ir.sysfail.chatguard.domain.usecase.key.AddUserPublicKeyUseCase
import ir.sysfail.chatguard.domain.usecase.steganography_crypto.ExtractPoeticPublicKeyUseCase
import ir.sysfail.chatguard.domain.usecase.steganography_crypto.GetPoeticSignedPublicKeyUseCase
import ir.sysfail.chatguard.domain.usecase.steganography_crypto.PoeticMessageDecryptionUseCase
import ir.sysfail.chatguard.domain.usecase.steganography_crypto.PoeticMessageEncryptionUseCase
import ir.sysfail.chatguard.domain.usecase.steganography_crypto.VerifyPoeticPublicKeyUseCase
import ir.sysfail.chatguard.ui_models.message.ChatMessageUiModel
import ir.sysfail.chatguard.ui_models.message.toUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MessagesViewModel(
    private val messageStateBridge: MessengerStateBridge,
    private val getPublicKeyUseCase: GetPublicKeyUseCase,
    private val verifyPoeticPublicKeyUseCase: VerifyPoeticPublicKeyUseCase,
    private val getPoeticSignedPublicKeyUseCase: GetPoeticSignedPublicKeyUseCase,
    private val encryptionUseCase: PoeticMessageEncryptionUseCase,
    private val decryptionUseCase: PoeticMessageDecryptionUseCase,
    private val addUserPublicKeyUseCase: AddUserPublicKeyUseCase,
    private val getPoeticPublicKeyUseCase: ExtractPoeticPublicKeyUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(MessagesScreenState())
    val state: StateFlow<MessagesScreenState> = _state.asStateFlow()

    private lateinit var currentChat: ExtractedMessengerChat
    private var recipientPublicKey: CryptoKey? = null

    init {
        initialize()
    }

    private fun initialize() = viewModelScope.launch(Dispatchers.IO) {
        currentChat = messageStateBridge.state.firstOrNull() ?: return@launch
        loadChatData()
    }

    private suspend fun loadChatData() {
        recipientPublicKey = getPublicKeyUseCase(
            username = currentChat.title,
            packageName = currentChat.packageName
        ).getOrNull()

        val processedMessages = processMessages()

        _state.update {
            it.copy(
                messages = processedMessages,
                hasPublicKey = recipientPublicKey != null
            )
        }
    }

    private suspend fun processMessages(): List<ChatMessageUiModel> {
        return currentChat.messages
            .mapIndexed { index, message -> message.toUiModel(index = index) }
            .map { message -> processMessage(message) }
    }

    private suspend fun processMessage(message: ChatMessageUiModel): ChatMessageUiModel {
        val isPublicKey = verifyPoeticPublicKeyUseCase(message.message)

        if (isPublicKey) {
            return message.copy(isPublicKey = true)
        }

        val theirKey = recipientPublicKey ?: return message

        val decryptedText = decryptionUseCase(
            message.message,
            theirKey,
            isAmSender = message.isMessageFromMe
        ).getOrNull()

        return if (decryptedText != null) {
            message.copy(message = decryptedText, isDecryptedMessage = true)
        } else {
            message
        }
    }

    fun acceptPublicKey(message: ChatMessageUiModel) = viewModelScope.launch(Dispatchers.IO) {
        if (!message.isPublicKey) return@launch

        getPoeticPublicKeyUseCase(message.message)
            .onSuccess { publicKey ->
                savePublicKey(publicKey)
            }
    }

    private suspend fun savePublicKey(publicKey: CryptoKey) {
        addUserPublicKeyUseCase(
            packageName = currentChat.packageName,
            username = currentChat.title,
            key = publicKey
        ).onSuccess {
            loadChatData()
        }
    }

    fun sendPublicKey(onSuccess: () -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        getPoeticSignedPublicKeyUseCase()
            .onSuccess { poeticKey ->
                publishSendEvent(poeticKey)
                withContext(Dispatchers.Main) { onSuccess.invoke() }
            }
    }

    fun sendMessage(onSuccess: () -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        val publicKey = recipientPublicKey ?: return@launch
        val messageText = state.value.currentNewMessageText

        if (messageText.isBlank()) return@launch

        encryptionUseCase(messageText, publicKey)
            .onSuccess { encryptedMessage ->
                publishSendEvent(encryptedMessage)
                clearMessageInput()
                withContext(Dispatchers.Main) { onSuccess.invoke() }
            }
    }

    private suspend fun publishSendEvent(message: String) {
        EventBus.publish(SendMessageEvent(message))
    }

    private fun clearMessageInput() {
        _state.update { it.copy(currentNewMessageText = "") }
    }

    fun updateMessageText(text: String) {
        _state.update { it.copy(currentNewMessageText = text) }
    }
}