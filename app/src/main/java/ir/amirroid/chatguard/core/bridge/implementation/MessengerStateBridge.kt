package ir.amirroid.chatguard.core.bridge.implementation

import ir.amirroid.chatguard.core.bridge.abstraction.StateBridge
import ir.amirroid.chatguard.domain.models.message.ExtractedMessengerChat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class MessengerStateBridge : StateBridge<ExtractedMessengerChat?> {
    private val _state = MutableStateFlow<ExtractedMessengerChat?>(null)
    override val state: StateFlow<ExtractedMessengerChat?> = _state

    override fun updateState(newValue: ExtractedMessengerChat?) {
        _state.update { newValue }
    }
}