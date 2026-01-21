package ir.sysfail.chatguard.core.bridge.abstraction

import kotlinx.coroutines.flow.StateFlow

interface StateBridge<T> {
    val state: StateFlow<T>

    fun updateState(newValue: T)
}