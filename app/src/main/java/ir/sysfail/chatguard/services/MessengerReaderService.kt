package ir.sysfail.chatguard.services

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import ir.sysfail.chatguard.core.floating_button.FloatingButtonController
import ir.sysfail.chatguard.features.main.MainActivity
import ir.sysfail.chatguard.features.messages.MessagesActivity
import ir.sysfail.chatguard.ui_models.chat.WindowType
import ir.sysfail.chatguard.utils.Constants
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
            messengerAccessibilityStateManager.state
                .collectLatest { state ->
                    floatingButtonController.setButtonColor(state.windowType.color)
                    if (state.windowType == WindowType.NON_MESSENGER) {
                        floatingButtonController.hide()
                    } else floatingButtonController.show()
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
        startForegroundService()
        startCommands()
    }


    private fun startCommands() {
        coroutineScope = CoroutineScope(Job() + Dispatchers.Main)
        messengerAccessibilityStateManager = get()
        floatingButtonController = FloatingButtonController(this)

        observeToState()
        addFabClickListener()

    }

    override fun onUnbind(intent: Intent?): Boolean {
        return true
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
        startCommands()
    }


    override fun onInterrupt() = Unit

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        cleanup()
    }

    private fun cleanup() {
        floatingButtonController.hide()
        messengerAccessibilityStateManager.cleanup()
        coroutineScope.cancel()
    }


    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.CHANNEL_ID,
                "ChatGuard Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "سرویس خواندن پیام‌های مسنجر"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, Constants.CHANNEL_ID)
            .setContentTitle("ChatGuard is active")
            .setContentText("Monitoring your messages…")
            .setSmallIcon(android.R.drawable.ic_secure)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(getPendingIntent())
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun getPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    companion object {
        const val NOTIFICATION_ID = 2
    }
}