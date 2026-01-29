package ir.sysfail.chatguard.features.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.sysfail.chatguard.domain.usecase.key.CheckIdentityKeyExistsUseCase
import ir.sysfail.chatguard.domain.usecase.key.GetIdentityKeyUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val getIdentityKeyUseCase: GetIdentityKeyUseCase,
    private val checkIdentityKeyExistsUseCase: CheckIdentityKeyExistsUseCase,
) : ViewModel() {
    private val _hasKeyPair = MutableStateFlow<Boolean?>(null)
    val hasKeyPair = _hasKeyPair.asStateFlow()

    init {
        checkExists()
    }

    fun checkExists() = viewModelScope.launch(Dispatchers.IO) {
        if (checkIdentityKeyExistsUseCase.invoke().getOrNull() == true) {
            _hasKeyPair.value = true
            getIdentityKeyUseCase.invoke()
        } else _hasKeyPair.value = false
    }
}