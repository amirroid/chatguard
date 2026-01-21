package ir.sysfail.chatguard.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import ir.sysfail.chatguard.core.floating_button.FloatingButtonController
import ir.sysfail.chatguard.features.messages.MessagesActivity
import ir.sysfail.chatguard.ui_models.chat.WindowType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get

class MessengerReaderService : AccessibilityService() {
    private lateinit var floatingButtonController: FloatingButtonController
    private lateinit var messengerAccessibilityStateManager: MessengerAccessibilityStateManager
    private lateinit var coroutineScope: CoroutineScope

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val rootNode = rootInActiveWindow ?: return

        if (
            event.eventType in listOf(
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                AccessibilityEvent.TYPE_VIEW_SCROLLED,
                AccessibilityEvent.TYPE_VIEW_FOCUSED,
                AccessibilityEvent.TYPE_VIEW_CLICKED,
                AccessibilityEvent.TYPE_WINDOWS_CHANGED,
            )
        ) {
            messengerAccessibilityStateManager.onNodeChanged(
                event = event,
                rootNode = rootNode
            )
        }
    }

    fun observeToState() {
        coroutineScope.launch {
            messengerAccessibilityStateManager.state.collectLatest { state ->
                floatingButtonController.setButtonColor(state.windowType.color)
//                if (state.windowType == WindowType.NON_MESSENGER) {
//                    floatingButtonController.hide()
//                } else floatingButtonController.show()
            }
        }
    }

    fun addFabClickListener() {
        floatingButtonController.setOnClickListener {
            val currentState = messengerAccessibilityStateManager.state.value

            if (currentState.windowType != WindowType.NON_CHAT) {
                messengerAccessibilityStateManager.updateBridge()

                val intent = Intent(this, MessagesActivity::class.java).apply {
                    flags += Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            }
        }
    }


    override fun onServiceConnected() {
        super.onServiceConnected()
        coroutineScope = CoroutineScope(Job() + Dispatchers.Main)
        messengerAccessibilityStateManager = get()
        floatingButtonController = FloatingButtonController(this)
        floatingButtonController.show()

        observeToState()
        addFabClickListener()
    }

    override fun onInterrupt() {
        floatingButtonController.hide()
        messengerAccessibilityStateManager.cleanup()
        coroutineScope.cancel()
    }
}