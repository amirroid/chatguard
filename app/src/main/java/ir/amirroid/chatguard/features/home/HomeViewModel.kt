package ir.amirroid.chatguard.features.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.amirroid.chatguard.domain.usecase.key.ClearCurrentKeysUseCase
import ir.amirroid.chatguard.domain.usecase.key.SaveIdentityKeysUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(
    private val clearCurrentKeysUseCase: ClearCurrentKeysUseCase,
    private val saveIdentityKeysUseCase: SaveIdentityKeysUseCase
) : ViewModel() {
    fun clearCurrentKeys(onComplete: () -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        clearCurrentKeysUseCase.invoke()
        withContext(Dispatchers.Main) { onComplete.invoke() }
    }

    fun saveKeys(uri: Uri, onSuccess: () -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        saveIdentityKeysUseCase.invoke(uri)
            .onSuccess {
                withContext(Dispatchers.Main) { onSuccess.invoke() }
            }
    }
}