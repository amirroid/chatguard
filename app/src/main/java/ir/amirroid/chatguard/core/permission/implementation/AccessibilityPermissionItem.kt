package ir.amirroid.chatguard.core.permission.implementation

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import ir.amirroid.chatguard.core.permission.abstraction.PermissionItem
import ir.amirroid.chatguard.services.MessengerReaderService

class AccessibilityPermissionItem : PermissionItem {
    override val name: String = "accessibility"

    override fun isGranted(context: Context): Boolean {
        val expectedComponent = ComponentName(context, MessengerReaderService::class.java)

        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices
            .split(':')
            .map { ComponentName.unflattenFromString(it) }
            .any { it == expectedComponent }
    }

    override fun requestAccess(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    data = "package:${context.packageName}".toUri()
                }
            )
        }.onFailure {
            context.startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }
}