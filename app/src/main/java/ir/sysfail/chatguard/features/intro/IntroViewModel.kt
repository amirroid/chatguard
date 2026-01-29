package ir.sysfail.chatguard.features.intro

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.sysfail.chatguard.domain.models.crypto.IdentityCryptoKeyPair
import ir.sysfail.chatguard.domain.repository.SaveInternalIdentityKeyUseCase
import ir.sysfail.chatguard.domain.usecase.key.GenerateIdentityKeyUseCase
import ir.sysfail.chatguard.domain.usecase.key.GetIdentityKeyPairFromFileUseCase
import ir.sysfail.chatguard.ui_models.intro.IdentityKeyUsage
import ir.sysfail.chatguard.ui_models.intro.SelectKeyStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IntroViewModel(
    private val generateIdentityKeyUseCase: GenerateIdentityKeyUseCase,
    private val getIdentityKeyPairFromFileUseCase: GetIdentityKeyPairFromFileUseCase,
    private val saveInternalIdentityKeyUseCase: SaveInternalIdentityKeyUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(IntroScreenState())
    val state = _state.asStateFlow()

    private var currentVerifiedKeys: IdentityCryptoKeyPair? = null

    fun selectUsage(usage: IdentityKeyUsage) {
        currentVerifiedKeys = null
        _state.update {
            it.copy(
                currentUsage = usage,
                selectedKeyStatus = SelectKeyStatus.SELECTING
            )
        }
    }

    fun handlePickedIdentityKeyPairFile(selectedFile: Uri?) {
        if (selectedFile == null) {
            _state.update {
                it.copy(selectedKeyStatus = SelectKeyStatus.UNSELECTED)
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            getIdentityKeyPairFromFileUseCase.invoke(selectedFile)
                .onSuccess { keyPair ->
                    currentVerifiedKeys = keyPair
                    _state.update { it.copy(selectedKeyStatus = SelectKeyStatus.VERIFIED_KEY_PAIR) }
                }.onFailure {
                    _state.update { it.copy(selectedKeyStatus = SelectKeyStatus.UNVERIFIED_KEY_PAIR) }
                }
        }
    }

    fun saveKeys(onComplete: () -> Unit) = viewModelScope.launch(Dispatchers.Default) {
        _state.update { it.copy(processing = true) }
        when (state.value.currentUsage) {
            IdentityKeyUsage.IMPORT_EXISTING -> {
                if (currentVerifiedKeys == null) {
                    _state.update { it.copy(processing = false) }
                    return@launch
                }
                saveInternalIdentityKeyUseCase.invoke(currentVerifiedKeys!!)
                    .onSuccess {
                        withContext(Dispatchers.Main) {
                            onComplete.invoke()
                        }
                    }.onFailure {
                        _state.update { it.copy(processing = false) }
                    }
            }

            IdentityKeyUsage.GENERATE_NEW -> {
                generateIdentityKeyUseCase.invoke().onSuccess {
                    withContext(Dispatchers.Main) {
                        onComplete.invoke()
                    }
                }.onFailure {
                    _state.update { it.copy(processing = false) }
                }
            }
        }
    }
}