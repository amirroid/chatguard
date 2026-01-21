package ir.sysfail.chatguard.core.permission.implementation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import ir.sysfail.chatguard.core.permission.abstraction.PermissionItem

@Stable
class PermissionState(
    private val permissions: List<PermissionItem>,
    private val context: Context
) {
    val permissionsGranted = mutableStateMapOf<String, Boolean>()

    init {
        checkPermissions()
    }

    fun checkPermissions() {
        permissions.forEach { permissionItem ->
            permissionsGranted[permissionItem.name] = permissionItem.isGranted(context)
        }
    }

    fun requestAccess(permissionName: String) {
        permissions.firstOrNull { it.name == permissionName }?.requestAccess(context)
    }
}

@Composable
fun rememberPermissionState(permissions: List<PermissionItem>): PermissionState {
    val context = LocalContext.current

    val state = remember { PermissionState(permissions, context) }

    LifecycleResumeEffect(Unit) {
        state.checkPermissions()
        onPauseOrDispose { }
    }

    return state
}