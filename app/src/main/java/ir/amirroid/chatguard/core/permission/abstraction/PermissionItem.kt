package ir.amirroid.chatguard.core.permission.abstraction

import android.content.Context

interface PermissionItem {
    val name: String
    fun isGranted(context: Context): Boolean
    fun requestAccess(context: Context)
}