package ir.sysfail.chatguard.core.permission.implementation

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.net.toUri
import ir.sysfail.chatguard.core.permission.abstraction.PermissionItem
import ir.sysfail.chatguard.services.MessengerReaderService
import ir.sysfail.chatguard.utils.extensions.activityOrNull

class NotificationsPermissionItem : PermissionItem {
    override val name: String = "notification"

    override fun isGranted(context: Context): Boolean {
        return context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    override fun requestAccess(context: Context) {
        context.activityOrNull?.requestPermissions(
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            1
        )
    }
}