package ir.sysfail.chatguard.features.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.sysfail.chatguard.domain.usecase.crypto.GetOrGenerateIdentityKeyUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewModel(
    private val getOrGenerateIdentityKeyUseCase: GetOrGenerateIdentityKeyUseCase
) : ViewModel() {
    init {
        initializePublicKey()
    }

    private fun initializePublicKey() = viewModelScope.launch(Dispatchers.IO) {
        getOrGenerateIdentityKeyUseCase.invoke()
    }
}