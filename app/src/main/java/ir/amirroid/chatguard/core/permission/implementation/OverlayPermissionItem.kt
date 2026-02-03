package ir.amirroid.chatguard.core.permission.implementation

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import ir.amirroid.chatguard.core.permission.abstraction.PermissionItem

class OverlayPermissionItem : PermissionItem {
    override val name: String = "overlay"

    override fun isGranted(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    override fun requestAccess(context: Context) {
        runCatching {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:${context.packageName}".toUri()
            )
            context.startActivity(intent)
        }.onFailure {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            context.startActivity(intent)
        }
    }
}